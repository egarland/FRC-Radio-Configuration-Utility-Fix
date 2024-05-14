package frcradiokiosk.openMesh;

import frcradiokiosk.FRCRadio;
import frcradiokiosk.RadioException;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OM5P_AN extends FRCRadio {
   public static final String MIN_ALLOWED_FIRMWARE = "19.0.0";
   public static final String DEFAULT_IP = "10.0.1.1";
   public static final String DEFAULT_IP_BASE = "10.0.0.";
   private static final String WRT_DEFAULT_IP = "192.168.1.1";
   private static final String WRT_IP_BASE = "192.168.1.";
   private static final String BW_LIMIT = "4000,";
   private static final Logger logger = LoggerFactory.getLogger(OM5P_AN.class);
   private Process firmwareLoadProc;
   private String myWpaKey;
   private FRCRadio.Mode mode;
   private boolean noSecurity;
   private String successMessage = "Success";

   public OM5P_AN(int team, String ssid, String key, FRCRadio.Mode mode, String password) {
      super(team, ssid);
      if (!key.isEmpty()) {
         this.myWpaKey = key;
         this.noSecurity = false;
      } else {
         this.myWpaKey = "";
         this.noSecurity = true;
      }

      this.mode = mode;
      Object[] logData = new Object[]{String.valueOf(team), this.getSSID(), mode.toString()};
      logger.info("New OM5P created, team: {}, ssid: {}, mode: {}", logData);
   }

   @Override
   public boolean broadcastPingable() {
      return false;
   }

   @Override
   public boolean requiresReset() {
      return false;
   }

   @Override
   public int getReconnectPings() {
      return 2;
   }

   @Override
   public int getReconnectWait() {
      return 20;
   }

   @Override
   public String getCustomSuccessMessage() {
      return this.successMessage;
   }

   @Override
   public void program() throws RadioException {
      this.program("192.168.1.1");
   }

   @Override
   public void program(String target) throws RadioException {
      try {
         logger.info("Programming OpenWRT settings");
         Socket sock = new Socket(target, 8888);
         DataOutputStream outToServer = new DataOutputStream(sock.getOutputStream());
         BufferedReader inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
         String received = "";

         while (received.length() - received.replace(":", "").length() < 3) {
            received = received + inFromServer.readLine();
         }

         System.out.println("From Server: " + received);
         String firmwareVersion = received.split(":|\n")[2].trim();
         String[] firmwareVersionSplit = firmwareVersion.split("\\.");
         String[] allowedVersionSplit = "19.0.0".split("\\.");
         boolean firmwareOK = false;
         if (Integer.parseInt(firmwareVersionSplit[0]) == Integer.parseInt(allowedVersionSplit[0])) {
            if (Integer.parseInt(firmwareVersionSplit[1]) > Integer.parseInt(allowedVersionSplit[1])) {
               firmwareOK = true;
            } else if (Integer.parseInt(firmwareVersionSplit[1]) == Integer.parseInt(allowedVersionSplit[1])
               && Integer.parseInt(firmwareVersionSplit[2]) >= Integer.parseInt(allowedVersionSplit[2])) {
               firmwareOK = true;
            }
         }

         if (!firmwareOK) {
            sock.close();
            throw new RadioException("Firmware too old", "Press OK to close open dialogs, then load new firmware");
         } else {
            String firewallString = this.firewall ? "Y," : "N,";
            String BWLimitString = this.BWLimit ? "4000," : ",";
            String DHCPString = "";
            if (this.mode != FRCRadio.Mode.BRIDGE && this.mode != FRCRadio.Mode.BRIDGE24) {
               DHCPString = "Y,";
            } else {
               DHCPString = "Y,";
            }

            String Chan_24 = "0,";
            String Chan_5 = "0,";
            String progString = this.mode.getChars()
               + ","
               + this.getTeam()
               + ","
               + this.getSSID()
               + ","
               + this.myWpaKey
               + ","
               + firewallString
               + BWLimitString
               + DHCPString
               + Chan_24
               + Chan_5
               + ",,"
               + this.date;
            outToServer.writeBytes(progString + "\n");

            try {
               Thread.sleep(3000L);
            } catch (InterruptedException var17) {
               java.util.logging.Logger.getLogger(OM5P_AN.class.getName()).log(Level.SEVERE, null, var17);
            }

            sock.close();
         }
      } catch (SocketException var18) {
         throw new RadioException("Failed to program radio settings", var18);
      } catch (IOException var191) {
         throw new RadioException("Failed to program radio settings", var191);
      }
   }

   @Override
   public String validate() throws RadioException {
      return "null";
   }

   @Override
   public String getDefaultIpAddress() {
      return "192.168.1.1";
   }

   @Override
   public String getDefaultIpBase() {
      return "192.168.1.";
   }

   public String readUntil(InputStream in) {
      logger.debug("reading until #");
      String prompt = "#";

      try {
         char lastChar = prompt.charAt(prompt.length() - 1);
         StringBuilder sb = new StringBuilder();
         boolean found = false;

         for (char ch = (char)in.read(); !found; ch = (char)in.read()) {
            sb.append(ch);
            if (ch == lastChar && sb.toString().endsWith(prompt)) {
               found = true;
            }
         }

         logger.trace(sb.toString());
         return sb.toString();
      } catch (IOException var71) {
         System.err.println(var71);
         return null;
      }
   }

   @Override
   public void checkMode(String targetIp) throws RadioException {
   }

   @Override
   public BufferedImage getImage() {
      try {
         return ImageIO.read(this.getClass().getResource("/frcradiokiosk/openMesh/om5p.png"));
      } catch (IOException var2) {
         java.util.logging.Logger.getLogger(OM5P_AN.class.getName()).log(Level.SEVERE, null, var2);
         return null;
      }
   }

   @Override
   public String[] getConfigInstructions() {
      return new String[]{
         "1) Connect power and Ethernet to the wireless bridge.",
         "2) Make sure to use the Ethernet port shown above.",
         "3) Wait for the Power light to turn and stay solid."
      };
   }

   @Override
   public String[] getResetInstructions() {
      return new String[]{
         "1) If Event WPA Kiosk: Follow on-screen prompts",
         "1) If Radio Config Utility: Select OpenMesh",
         "2) Unplug power from the radio",
         "3) Press the \"Load Firmware\" button.",
         "4) Follow the on-screen prompts."
      };
   }

   @Override
   public void prepareLoadFirmware(String adapter) throws RadioException {
      String devName = "";
      String singleDevName = "";

      try {
         ProcessBuilder pbNoArgs = new ProcessBuilder().command(System.getProperty("user.dir") + "\\ap51-flash.exe").redirectErrorStream(true);
         this.firmwareLoadProc = pbNoArgs.start();
         InputStream stdin = this.firmwareLoadProc.getInputStream();
         InputStreamReader isr = new InputStreamReader(stdin);
         BufferedReader br = new BufferedReader(isr);
         String line = "";

         try {
            this.firmwareLoadProc.waitFor();
         } catch (InterruptedException var12) {
            java.util.logging.Logger.getLogger(OM5P_AN.class.getName()).log(Level.SEVERE, null, var12);
         }

         int count = 0;
         int adapterCount = 0;

         while (br.ready()) {
            count++;
            String lastLine = line;
            line = br.readLine();
            if (line.contains(adapter)) {
               devName = lastLine.substring(3);
            } else if (line.contains("Description") && !line.contains("Adapter for loopback") && !line.contains("Ndis") && !line.contains("AsyncMac")) {
               singleDevName = lastLine.substring(3);
               adapterCount++;
            }
         }

         if (adapterCount == 1) {
            devName = singleDevName;
         }

         if (devName.isEmpty()) {
            throw new RadioException("Error finding NPF device name for adapter: " + adapter + "\nTry disabling all other adapters (using the Control Panel)");
         } else {
            ProcessBuilder pb = new ProcessBuilder()
               .command(
                  System.getProperty("user.dir") + "\\ap51-flash.exe",
                  devName,
                  System.getProperty("user.dir") + "\\firmwareOM5PAN.bin",
                  System.getProperty("user.dir") + "\\firmwareOM5PAC.bin"
               )
               .redirectErrorStream(true);
            this.firmwareLoadProc = pb.start();
         }
      } catch (IOException var13) {
         this.firmwareLoadProc.destroy();
         throw new RadioException("Error launching radio flashing utility.");
      }
   }

   @Override
   public void firmwareWaitForRadio() throws RadioException {
      InputStream stdin = this.firmwareLoadProc.getInputStream();
      InputStreamReader isr = new InputStreamReader(stdin);
      BufferedReader br = new BufferedReader(isr);
      String line = "";
      long startTime = System.currentTimeMillis();
      boolean foundRadio = false;

      while (!foundRadio && startTime + 60000L > System.currentTimeMillis()) {
         try {
            if (br.ready()) {
               line = line + br.readLine();
            }
         } catch (IOException var9) {
            java.util.logging.Logger.getLogger(OM5P_AN.class.getName()).log(Level.SEVERE, null, var9);
         }

         if (line != null && (line.contains("OM5P-AC") || line.contains("OM5P-AN"))) {
            foundRadio = true;
         }
      }

      if (!foundRadio) {
         java.util.logging.Logger.getLogger(OM5P_AN.class.getName()).log(Level.INFO, null, line);
         java.util.logging.Logger.getLogger(OM5P_AN.class.getName()).log(Level.SEVERE, null, "Timeout waiting for radio to load firmware");
         this.firmwareLoadProc.destroyForcibly();
         throw new RadioException("Timeout waiting for radio. Make sure you have launched the .exe not the .jar. Try using a switch.");
      }
   }

   @Override
   public void flashRadio() throws RadioException {
      try {
         if (!this.firmwareLoadProc.waitFor(120L, TimeUnit.SECONDS)) {
            this.firmwareLoadProc.destroyForcibly();
            throw new RadioException(
               "Timeout flashing radio. Make sure you have launched the .exe not the .jar and make sure not to unplug the radio until the operation completes."
            );
         }
      } catch (InterruptedException var2) {
         this.firmwareLoadProc.destroyForcibly();
         throw new RadioException("Interrupted while flashing radio.");
      }
   }

   @Override
   public String getFirmwareLoadInstructions() {
      return "1)Plug the radio Ethernet into the PC,\n2)Apply (or cycle) power to the radio. \n3)If the power LED blinks, power cycle again.";
   }

   @Override
   public void reset(String target) throws RadioException {
      throw new UnsupportedOperationException("Not supported yet.");
   }
}
