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

public class RadioFirmwareLoadSequence extends SwingWorker<ConfigurationResult, ProgressMessage> {
   private FRCRadio teamRadio;
   private HostComputer host;
   private String sequenceError;
   private String dialogMessage;
   private boolean pass;
   private static final Logger logger = LoggerFactory.getLogger(RadioFirmwareLoadSequence.class);
   private static RadioFirmwareLoadSequence sequence = null;

   public static synchronized ConfigurationResult RunFirmwareLoadSequence(
      FRCRadio radio, HostComputer host, final IKioskShell shell, final ISequenceProgressDialog dialog
   ) {
      sequence = new RadioFirmwareLoadSequence(radio, host) {
         @Override
         protected void process(List<ProgressMessage> messages) {
            for (ProgressMessage message : messages) {
               if (message.getTitle() != "clear") {
                  dialog.setCurrentStep(message.getTitle(), message.getMessage());
               } else {
                  dialog.clearMessages();
               }
            }
         }

         @Override
         protected void done() {
            ConfigurationResult result = null;

            try {
               result = this.get();
               if (result.passed()) {
                  String message = "Radio firmware flashed successfully";
                  if (!result.getUserMessage().isEmpty()) {
                     message = message + ";\n" + result.getUserMessage();
                  }

                  shell.showSuccessMessage(message);
               } else {
                  shell.showErrorMessage(result.getUserMessage());
               }
            } catch (ExecutionException var8) {
               shell.showErrorMessage("Firmware flash sequence failed to execute");
               RadioFirmwareLoadSequence.logger.error(var8.getMessage(), var8);
            } catch (InterruptedException var9) {
               shell.showErrorMessage("Firmware flash sequence failed to execute");
               RadioFirmwareLoadSequence.logger.error(var9.getMessage(), var9);
            } catch (CancellationException var101) {
               shell.showErrorMessage("Firmware flash sequence cancelled");
               RadioFirmwareLoadSequence.logger.error(var101.getMessage(), var101);
            } finally {
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
         java.util.logging.Logger.getLogger(RadioFirmwareLoadSequence.class.getName()).log(Level.SEVERE, null, var5);
         return null;
      } catch (ExecutionException var6) {
         java.util.logging.Logger.getLogger(RadioFirmwareLoadSequence.class.getName()).log(Level.SEVERE, null, var6);
         return null;
      }
   }

   public static void killSequence() {
      if (sequence != null && !sequence.isDone()) {
         logger.debug("cancelling radio firmware sequence");
         boolean sequenceCancelled = sequence.cancel(true);
         if (sequenceCancelled) {
            logger.debug("Radio firmware sequence cancelled successfully");
         } else {
            logger.error("Failed to cancel radio firmware sequence");
         }
      }
   }

   private RadioFirmwareLoadSequence(FRCRadio radio, HostComputer hostComp) {
      this.teamRadio = radio;
      this.host = hostComp;
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
         this.publish(new ProgressMessage[]{new ProgressMessage("Preparing to load fimware")});
         this.teamRadio.prepareLoadFirmware(this.host.getSelectedNetworkInfDisplayName());
         this.setProgress(10);
         this.publish(new ProgressMessage[]{new ProgressMessage(this.teamRadio.getFirmwareLoadInstructions())});
         this.teamRadio.firmwareWaitForRadio();
         this.setProgress(40);
         this.publish(new ProgressMessage[]{new ProgressMessage("clear")});
         this.publish(new ProgressMessage[]{new ProgressMessage("Radio Found. Flashing firmware")});
         this.teamRadio.flashRadio();
         this.setProgress(100);
         this.pass = true;
         logger.debug("firmware sequence successful");
      } catch (RadioException var6) {
         this.logError(var6, var6.getUserMessage());
      } finally {
         if (!this.pass & this.sequenceError == null) {
            this.logError(new AppException("Internal error"), "Bridge firmware sequence failed: Internal Error");
         }

         if (this.pass) {
            this.publish(new ProgressMessage[]{new ProgressMessage("Radio successfully flashed")});
         } else {
            this.publish(new ProgressMessage[]{new ProgressMessage("Bridge Firmware Flash Sequence Failed", this.sequenceError)});
         }

         return new ConfigurationResult(
            "Firmware", this.pass, this.pass ? validationRetVal : this.sequenceError, this.pass ? this.teamRadio.getCustomSuccessMessage() : this.dialogMessage
         );
      }
   }

   private void logError(Exception e, String userMessage) {
      logger.error("Bridge firmware sequence failed: " + e.getMessage(), e);
      this.sequenceError = e.getMessage();
      this.dialogMessage = "Bridge firmware load failed: " + e.getMessage() + "\n" + userMessage;
      this.pass = false;
   }
}
