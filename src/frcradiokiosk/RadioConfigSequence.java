package frcradiokiosk;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadioConfigSequence extends SwingWorker<ConfigurationResult, ProgressMessage> {
   private FRCRadio teamRadio;
   private HostComputer host;
   private String sequenceError;
   private String dialogMessage;
   private boolean pass;
   private static final Logger logger = LoggerFactory.getLogger(RadioConfigSequence.class);
   private static RadioConfigSequence sequence = null;

   public static synchronized ConfigurationResult RunConfigSequence(
      final FRCRadio radio, HostComputer host, final IKioskShell shell, final ISequenceProgressDialog dialog
   ) {
      sequence = new RadioConfigSequence(host, radio) {
         @Override
         protected void process(List<ProgressMessage> messages) {
            for (ProgressMessage message : messages) {
               dialog.setCurrentStep(message.getTitle(), message.getMessage());
            }
         }

         @Override
         protected void done() {
            ConfigurationResult result = null;

            try {
               result = this.get();
               if (result.passed()) {
                  String message = radio.getModeString() + " bridge programmed successfully";
                  if (!result.getUserMessage().isEmpty()) {
                     message = message + ";\n" + result.getUserMessage();
                  }

                  shell.showSuccessMessage(message);
               } else {
                  shell.showErrorMessage(result.getUserMessage());
               }
            } catch (ExecutionException var8) {
               shell.showErrorMessage("Configuration sequence failed to execute");
               RadioConfigSequence.logger.error(var8.getMessage(), var8);
            } catch (InterruptedException var9) {
               shell.showErrorMessage("Configuration sequence failed to execute");
               RadioConfigSequence.logger.error(var9.getMessage(), var9);
            } catch (CancellationException var101) {
               shell.showErrorMessage("Configuration sequence cancelled");
               RadioConfigSequence.logger.error(var101.getMessage(), var101);
            } finally {
               if (radio.isDefaultMode() && result != null) {
                  shell.logAttempt(radio.getTeam(), result.passed(), result.getInternalMessage());
               }

               dialog.sequenceFinished();
            }
         }
      };
      sequence.addPropertyChangeListener(new PropertyChangeListener() {
         @Override
         public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("progress")) {
               dialog.setProgress((Integer)evt.getNewValue());
            }
         }
      });
      sequence.execute();
      dialog.showDialog();

      try {
         return sequence.get();
      } catch (InterruptedException var5) {
         java.util.logging.Logger.getLogger(RadioConfigSequence.class.getName()).log(Level.SEVERE, null, var5);
      } catch (ExecutionException var6) {
         java.util.logging.Logger.getLogger(RadioConfigSequence.class.getName()).log(Level.SEVERE, null, var6);
      }

      return null;
   }

   public static void killSequence() {
      if (sequence != null && !sequence.isDone()) {
         logger.debug("cancelling radio configuration sequence");
         boolean sequenceCancelled = sequence.cancel(true);
         if (sequenceCancelled) {
            logger.debug("Radio configuration sequence cancelled successfully");
         } else {
            logger.error("Failed to cancel radio configuration sequence");
         }
      }
   }

   private RadioConfigSequence(HostComputer host, FRCRadio radio) {
      this.host = host;
      this.teamRadio = radio;
   }

   protected ConfigurationResult doInBackground() {
      return this.program();
   }

   @Override
   protected void process(List<ProgressMessage> messages) {
      for (ProgressMessage pm : messages) {
         logger.debug(pm.getTitle() + "; " + pm.getMessage());
      }
   }

   @Override
   protected void done() {
      logger.debug("sequence complete");
   }

   private ConfigurationResult program() {
      this.sequenceError = null;
      this.pass = false;
      String validationRetVal = "null";
      this.setProgress(0);

      try {
         this.publish(new ProgressMessage[]{new ProgressMessage("Configuring computer IP address")});
         logger.debug("flushing arp cache");
         this.host.flushArp();
         logger.debug("Setting up computer IP address");
         this.host.changeIpAddress(this.teamRadio.getDefaultIpBase() + "51", "255.0.0.0", false);
         this.host.changeIpAddress("10.0.0.51", "255.0.0.0", true);
         this.setProgress(10);
         logger.debug("finding radio");
         String target = this.findRadio(this.teamRadio);
         this.teamRadio.checkMode(target);
         if (this.teamRadio.requiresReset()) {
            this.resetRadio(this.teamRadio, target);
            target = this.teamRadio.getDefaultIpAddress();
         }

         this.setProgress(50);
         this.publish(new ProgressMessage[]{new ProgressMessage("Configuring bridge settings")});
         logger.debug("Programming radio settings");
         if (target.equals(this.teamRadio.getDefaultIpAddress())) {
            this.teamRadio.program();
         } else {
            this.teamRadio.program(target);
         }

         this.setProgress(70);
         logger.debug("Reconnecting for validation");
         this.publish(new ProgressMessage[]{new ProgressMessage("Reconnecting to bridge at team IP address")});
         if (!this.host.pingUntil(this.teamRadio.getIpAddress(), this.teamRadio.getReconnectPings(), this.teamRadio.getReconnectWait())) {
            throw new RadioDetectionException("Could not reconnect to bridge for validation");
         } else {
            this.setProgress(80);
            logger.debug("Validating radio settings");
            this.publish(new ProgressMessage[]{new ProgressMessage("Verifying bridge settings")});
            validationRetVal = this.teamRadio.validate();
            this.setProgress(100);
            this.pass = true;
            this.host.restoreDHCP();
            logger.debug("sequence successful");
         }
      } catch (HostException var8) {
         this.logError(var8, "Please ensure the bridge is connected to this computer and try again");
      } catch (RadioException var9) {
         this.logError(var9, var9.getUserMessage());
         this.host.restoreDHCP();
      } catch (RadioDetectionException var101) {
         this.logError(var101, var101.getUserMessage());
         this.host.restoreDHCP();
      } finally {
         if (!this.pass & this.sequenceError == null) {
            this.logError(new AppException("Internal error"), "Bridge sequence failed: Internal Error");
         }

         if (this.pass) {
            this.publish(new ProgressMessage[]{new ProgressMessage(this.teamRadio.getModeString() + " successfully programmed")});
         } else {
            this.publish(new ProgressMessage[]{new ProgressMessage("Bridge Programming Sequence Failed", this.sequenceError)});
         }

         return new ConfigurationResult(
            this.teamRadio.isDefaultMode() ? String.valueOf(this.teamRadio.getTeam()) : this.teamRadio.getModeString(),
            this.pass,
            this.pass ? validationRetVal : this.sequenceError,
            this.pass ? this.teamRadio.getCustomSuccessMessage() : this.dialogMessage
         );
      }
   }

   private void logError(Exception e, String userMessage) {
      logger.error("Bridge sequence failed: " + e.getMessage(), e);
      this.sequenceError = e.getMessage();
      this.dialogMessage = "Bridge configuration failed: " + e.getMessage() + "\n" + userMessage;
      this.pass = false;
   }

   private String findRadio(FRCRadio radio) throws RadioException, HostException, RadioDetectionException {
      try {
         logger.debug("Checking for bridge at default address");
         this.publish(new ProgressMessage[]{new ProgressMessage("Checking for bridge at expected IP addresses")});
         if (radio.isConnected(radio.getDefaultIpAddress(), 40)) {
            logger.debug("bridge found at default address");
            this.publish(new ProgressMessage[]{new ProgressMessage("Bridge found at default IP address")});
            return radio.getDefaultIpAddress();
         } else if (radio.isConnected(radio.getIpAddress(), 5)) {
            logger.debug("bridge found at team IP address ({})", radio.getIpAddress());
            this.publish(new ProgressMessage[]{new ProgressMessage("Bridge found at team IP address (" + radio.getIpAddress() + ")")});
            return radio.getIpAddress();
         } else if (!radio.broadcastPingable()) {
            throw new RadioDetectionException("Could not locate bridge");
         } else {
            logger.debug("checking for bridge at FRC addresses");
            return this.searchByBroadcast();
         }
      } catch (HostException var3) {
         throw new HostException(var3.getMessage(), var3);
      } catch (RadioException var4) {
         throw new RadioException(var4.getMessage(), var4.getUserMessage(), var4);
      } catch (RadioDetectionException var5) {
         throw new RadioDetectionException(var5.getMessage(), var5);
      }
   }

   private String searchByBroadcast() throws HostException, RadioDetectionException {
      try {
         this.publish(new ProgressMessage[]{new ProgressMessage("Checking for bridge at other addresses")});
         String[] sources = this.host.pingBroadcast("10.255.255.255");
         String foundAtIp;
         if (sources.length == 1) {
            if (this.host.checkIpAddress(sources[0])) {
               throw new RadioDetectionException("Could not locate the bridge");
            }

            foundAtIp = sources[0];
         } else {
            if (sources.length != 2) {
               if (sources.length == 0) {
                  throw new RadioDetectionException("Could not locate the bridge");
               }

               throw new RadioDetectionException("Multiple devices detected");
            }

            if (this.host.checkIpAddress(sources[0])) {
               foundAtIp = sources[1];
            } else {
               if (!this.host.checkIpAddress(sources[1])) {
                  throw new RadioDetectionException("Multiple devices detected");
               }

               foundAtIp = sources[0];
            }
         }

         this.publish(new ProgressMessage[]{new ProgressMessage("Bridge found at " + foundAtIp)});
         logger.debug("bridge found at {}", foundAtIp);
         return foundAtIp;
      } catch (HostException var31) {
         throw new HostException(var31.getMessage(), var31);
      }
   }

   private void resetRadio(FRCRadio radio, String targetIp) throws RadioDetectionException, RadioException, HostException {
      try {
         logger.debug("Resetting bridge");
         this.publish(new ProgressMessage[]{new ProgressMessage("Resetting bridge")});
         radio.reset(targetIp);
         this.setProgress(40);
         logger.debug("Reconnecting to now-reset radio");
         this.publish(new ProgressMessage[]{new ProgressMessage("Checking for bridge at default IP address")});
         if (this.host.pingUntil(radio.getDefaultIpAddress(), radio.getReconnectPings(), 45)) {
            logger.debug("bridge found at default address");
            this.publish(new ProgressMessage[]{new ProgressMessage("Bridge found at default IP address")});
         } else {
            throw new RadioDetectionException("Could not reconnect to bridge after reset");
         }
      } catch (HostException var4) {
         throw new HostException(var4.getMessage(), var4);
      } catch (RadioException var5) {
         throw new RadioException(var5.getMessage(), var5.getUserMessage(), var5);
      }
   }
}
