package frcradiokiosk.DAP1522RevB;

import frcradiokiosk.DAP1522;
import frcradiokiosk.FRCRadio;
import frcradiokiosk.IllegalRadioException;
import frcradiokiosk.InvalidLoginException;
import frcradiokiosk.RadioConnection;
import frcradiokiosk.RadioException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DAP1522revB extends FRCRadio {
   public static final String DEFAULT_IP = "192.168.0.50";
   public static final String DEFAULT_IP_BASE = "192.168.0.";
   private static final Logger logger = LoggerFactory.getLogger(DAP1522revB.class);
   private String myWpaKey;
   private FRCRadio.Mode mode;
   private boolean noSecurity;
   private static String[] validFirmwareVersions = new String[]{"2.00", "2.01", "2.02", "2.03", "2.07"};
   private static String[] validHardwareVersions = new String[]{"B1"};
   private static final String resetCommand = "EVENT=FRESET";
   private final String adminConfigCommand = "SERVICES=DEVICE.ACCOUNT%2CRUNTIME.SWITCHMODE";
   private final String switchConfigCommand = "SERVICES=%2CRUNTIME.SWITCHMODE";
   private final String wlanConfigCommand = "SERVICES=WIFI.PHYINF%2CPHYINF.WIFI%2CRUNTIME.PHYINF%2CRUNTIME.DFS%2CMACCLONE.WLAN-2%2CRUNTIME.SWITCHMODE";
   private final String lanConfigCommand = "SERVICES=DEVICE.HOSTNAME%2CINET.BRIDGE-1%2CINET.BRIDGE-2%2CINET.BRIDGE-3%2CRUNTIME.PHYINF%2CRUNTIME.SWITCHMODE";
   private final String qosConfigCommand = "SERVICES=TRAFFICCTRL.BRIDGE-1%2CRUNTIME.SWITCHMODE";
   private final String dhcpConfigCommand = "SERVICES=DHCPS4.BRIDGE-1%2CRUNTIME.INF.BRIDGE-1%2CRUNTIME.SWITCHMODE";
   private static final String activateCommand = "ACTIONS=SETCFG%2CSAVE%2CACTIVATE";
   private static XMLOutputter xout = null;

   public DAP1522revB(int team, String ssid, String key, FRCRadio.Mode mode, String password) {
      super(team, ssid);
      if (!key.equals("")) {
         this.myWpaKey = key;
         this.noSecurity = false;
      } else {
         this.noSecurity = true;
      }

      if (xout == null) {
         xout = new XMLOutputter(Format.getPrettyFormat());
      }

      this.mode = mode;
      if (!password.isEmpty()) {
         AdminData.setStaticPassword(password);
      }

      Object[] logData = new Object[]{String.valueOf(team), this.getSSID(), mode.getLongString()};
      logger.info("New DAP1522 created, team: {}, ssid: {}, mode: {}", logData);
   }

   @Override
   public String getDefaultIpAddress() {
      return "192.168.0.50";
   }

   @Override
   public String getDefaultIpBase() {
      return "192.168.0.";
   }

   @Override
   public boolean broadcastPingable() {
      return false;
   }

   @Override
   public boolean requiresReset() {
      return true;
   }

   @Override
   public int getReconnectPings() {
      return 10;
   }

   @Override
   public int getReconnectWait() {
      return 20;
   }

   @Override
   public String getCustomSuccessMessage() {
      return "";
   }

   @Override
   public BufferedImage getImage() {
      try {
         return ImageIO.read(this.getClass().getResource("/frcradiokiosk/RevB.png"));
      } catch (IOException var2) {
         java.util.logging.Logger.getLogger(DAP1522.class.getName()).log(Level.SEVERE, null, var2);
         return null;
      }
   }

   @Override
   public String[] getConfigInstructions() {
      return new String[]{
         "1) Ensure the mode switch is set to \"" + this.mode.getShortString() + "\"",
         "2) Connect power and Ethernet to the wireless bridge",
         "3) Wait for the blue power and " + (this.mode.isAP() ? "AP" : "bridge") + " lights to turn on"
      };
   }

   @Override
   public String[] getResetInstructions() {
      return new String[]{
         "1) Ensure the mode switch is set to \"" + this.mode.getShortString() + "\"",
         "2) Connect power and Ethernet to the wireless bridge",
         "3) Wait for the blue power and " + (this.mode.isAP() ? "AP" : "bridge") + " lights to turn on",
         "4) Press and hold the \"Reset\" button 10 seconds",
         "5) The blue " + (this.mode.isAP() ? "AP" : "bridge") + " light will turn off after a few seconds",
         "6) Once the blue " + (this.mode.isAP() ? "AP" : "bridge") + " light turns on again, reset is complete"
      };
   }

   @Override
   public void program(String targetIp) throws RadioException {
      logger.info("Programming DAP1522 settings");

      try {
         this.login(targetIp);
         if (!this.checkCorrectMode(targetIp)) {
            throw new RadioException("Bridge is not in " + this.mode.toString() + " mode", "Please switch the bridge to the correct mode and try again");
         } else {
            this.checkRadioAttribute("firmware version", this.getRadioFirmware("192.168.0.50"), validFirmwareVersions);
            this.network(targetIp);
            this.wlan(targetIp);
            if (!AdminData.getStaticPassword().equals(AdminData.getDefaultPassword())) {
               this.admin(targetIp);
            }

            if (this.mode.isAP()) {
               this.qos(targetIp);
               this.dhcp(targetIp);
            }
         }
      } catch (RadioException var3) {
         throw new RadioException(var3.getMessage(), var3.getUserMessage(), var3);
      }
   }

   @Override
   public void program() throws RadioException {
      this.program("192.168.0.50");
   }

   @Override
   public String validate() throws RadioException {
      logger.info("Validating DAP1522 settings");
      String message = "";
      String firmware = "null";

      try {
         if (!AdminData.getStaticPassword().equals(AdminData.getDefaultPassword())) {
            this.login(this.getIpAddress(), AdminData.getDefaultUsername(), AdminData.getStaticPassword());
         } else {
            this.login(this.getIpAddress());
         }

         WlanData wlanData = new WlanData(
            this.getConfig(this.getIpAddress(), "SERVICES=WIFI.PHYINF%2CPHYINF.WIFI%2CRUNTIME.PHYINF%2CRUNTIME.DFS%2CMACCLONE.WLAN-2%2CRUNTIME.SWITCHMODE"),
            this.mode
         );
         LanData lanData = new LanData(
            this.getConfig(
               this.getIpAddress(), "SERVICES=DEVICE.HOSTNAME%2CINET.BRIDGE-1%2CINET.BRIDGE-2%2CINET.BRIDGE-3%2CRUNTIME.PHYINF%2CRUNTIME.SWITCHMODE"
            )
         );
         QosData qosData = new QosData(this.getConfig(this.getIpAddress(), "SERVICES=TRAFFICCTRL.BRIDGE-1%2CRUNTIME.SWITCHMODE"));
         DHCPData dhcpData = new DHCPData(this.getConfig(this.getIpAddress(), "SERVICES=DHCPS4.BRIDGE-1%2CRUNTIME.INF.BRIDGE-1%2CRUNTIME.SWITCHMODE"));
         firmware = this.getRadioFirmware(this.getIpAddress());
         message = message + this.validateWireless(wlanData);
         if (this.mode.isAP()) {
            message = message + this.validateQos(qosData);
            message = message + this.validateDHCP(dhcpData);
         }

         message = message + this.validateNetwork(lanData);
      } catch (RadioException var71) {
         throw new RadioException("Error validating bridge settings: " + var71.getMessage(), var71);
      }

      if (!message.isEmpty()) {
         throw new RadioException(message);
      } else {
         return firmware;
      }
   }

   private String getRadioFirmware(String targetIp) throws RadioException {
      try {
         String data = RadioConnection.createInputConnection("http://" + targetIp, 5).getPage(false);
         return this.readFirmwareVersion(data);
      } catch (RadioException var31) {
         throw new RadioException("Failed to get bridge firmware version", var31);
      }
   }

   @Override
   public void reset(String target) throws RadioException {
      logger.info("Resetting DAP1522 at {}", target);

      try {
         this.loginWithRetry(target);
         logger.debug("Reset process started");
         Document response = RadioConnection.createOutputConnection("http://" + target + "/service.cgi", false).pushData("EVENT=FRESET");
         RevBUtil.checkPostSuccessful(response);
         Thread.sleep(10000L);
         logger.debug("Reset process finished");
      } catch (InterruptedException var3) {
         throw new RadioException("Automated bridge reset interrupted", var3);
      } catch (RadioException var41) {
         throw new RadioException("Automated bridge reset failed", var41);
      }
   }

   @Override
   public void checkMode(String targetIp) throws RadioException {
      try {
         this.loginWithRetry(targetIp);
         if (!this.checkCorrectMode(targetIp)) {
            throw new RadioException("Bridge is not in " + this.mode.toString() + " mode", "Please switch the bridge to the correct mode and try again");
         }
      } catch (RadioException var3) {
         throw new RadioException(var3.getMessage(), var3.getUserMessage(), var3);
      }
   }

   private Boolean checkCorrectMode(String targetIp) throws RadioException {
      logger.debug("Check mode process started");

      try {
         return RevBUtil.inCorrectMode(this.getConfig(targetIp, "SERVICES=%2CRUNTIME.SWITCHMODE"), this.mode);
      } catch (RadioException var3) {
         throw new RadioException("Failed to check bridge operating mode", var3);
      }
   }

   private void loginWithRetry(String targetIp) throws RadioException {
      try {
         try {
            this.login(targetIp);
         } catch (InvalidLoginException var3) {
            if (AdminData.getStaticPassword().equals(AdminData.getDefaultPassword())) {
               throw var3;
            }

            logger.debug("Default login credentials refused, trying alternate credentials");
            this.login(targetIp, AdminData.getDefaultUsername(), AdminData.getStaticPassword());
         }
      } catch (RadioException var4) {
         throw new RadioException(var4.getMessage(), var4);
      }
   }

   private void login(String targetIp) throws RadioException {
      this.login(targetIp, AdminData.getDefaultUsername(), AdminData.getDefaultPassword());
   }

   private void login(String targetIp, String user, String password) throws RadioException {
      logger.info("Login process started for target {}", targetIp);

      try {
         RadioConnection.createInputConnection("http://" + targetIp, 5).getPage(false);
         Document response = RadioConnection.createOutputConnection("http://" + targetIp + "/session.cgi", false)
            .pushData("REPORT_METHOD=xml&ACTION=login_plaintext&USER=" + user + "&PASSWD=" + password + "&CAPTCHA=");
         RevBUtil.checkLoginSuccessful(response);
      } catch (InvalidLoginException var5) {
         throw new InvalidLoginException(var5);
      } catch (RadioException var61) {
         throw new RadioException("Bridge login failed", var61);
      }
   }

   private Document getConfig(String targetIp, String command) throws RadioException {
      logger.debug("Getting config for target {}", targetIp);

      try {
         return RadioConnection.createOutputConnection("http://" + targetIp + "/getcfg.php", false).pushData(command);
      } catch (RadioException var4) {
         throw new RadioException("Failed to get current config", var4);
      }
   }

   private void wlan(String targetIp) throws RadioException {
      logger.info("Programming wireless settings");

      try {
         WlanData wlanData = new WlanData(
            this.getConfig(targetIp, "SERVICES=WIFI.PHYINF%2CPHYINF.WIFI%2CRUNTIME.PHYINF%2CRUNTIME.DFS%2CMACCLONE.WLAN-2%2CRUNTIME.SWITCHMODE"), this.mode
         );
         wlanData.setSsidValue(this.getSSID());
         if (!this.noSecurity) {
            wlanData.setSecurity(this.myWpaKey);
         }

         wlanData.setStaticData();
         Document response = RadioConnection.createOutputConnection(this.getHedwig(targetIp), true).pushData(wlanData.getDocument());
         RevBUtil.checkPostSuccessful(response);
         this.activateSettings(targetIp);
      } catch (RadioException var41) {
         throw new RadioException("Failed to program bridge wireless settings", var41);
      }
   }

   private void network(String targetIp) throws RadioException {
      logger.info("Programming network settings");

      try {
         LanData lanData = new LanData(
            this.getConfig(targetIp, "SERVICES=DEVICE.HOSTNAME%2CINET.BRIDGE-1%2CINET.BRIDGE-2%2CINET.BRIDGE-3%2CRUNTIME.PHYINF%2CRUNTIME.SWITCHMODE")
         );
         lanData.setIpAddrValue(this.getIpAddress());
         lanData.setGatewayValue(this.getGateway());
         lanData.setStaticData();
         Document response = RadioConnection.createOutputConnection(this.getHedwig(targetIp), true).pushData(lanData.getDocument());
         RevBUtil.checkPostSuccessful(response);
         this.activateSettings(targetIp);
      } catch (RadioException var41) {
         throw new RadioException("Failed to program bridge network settings", var41);
      }
   }

   private void admin(String targetIp) throws RadioException {
      logger.info("Programming admin settings");

      try {
         AdminData adminData = new AdminData(this.getConfig(targetIp, "SERVICES=DEVICE.ACCOUNT%2CRUNTIME.SWITCHMODE"));
         adminData.setStaticData();
         Document response = RadioConnection.createOutputConnection(this.getHedwig(targetIp), true).pushData(adminData.getDocument());
         RevBUtil.checkPostSuccessful(response);
         this.activateSettings(targetIp);
      } catch (RadioException var41) {
         throw new RadioException("Failed to program bridge admin settings", var41);
      }
   }

   private void qos(String targetIp) throws RadioException {
      logger.info("Programming QOS settings");

      try {
         QosData qos = new QosData(this.getConfig(targetIp, "SERVICES=TRAFFICCTRL.BRIDGE-1%2CRUNTIME.SWITCHMODE"));
         qos.setStaticData();
         Document response = RadioConnection.createOutputConnection(this.getHedwig(targetIp), true).pushData(qos.getDocument());
         RevBUtil.checkPostSuccessful(response);
         this.activateSettings(targetIp);
      } catch (RadioException var41) {
         throw new RadioException("Failed to program bridge QOS settings", var41);
      }
   }

   private void dhcp(String targetIp) throws RadioException {
      logger.info("Programming DHCP settings");

      try {
         DHCPData dhcp = new DHCPData(this.getConfig(targetIp, "SERVICES=DHCPS4.BRIDGE-1%2CRUNTIME.INF.BRIDGE-1%2CRUNTIME.SWITCHMODE"));
         dhcp.setStaticData();
         dhcp.setIPStart(this.getTeamBaseAddress() + "20");
         dhcp.setNetwork(this.getTeamBaseAddress() + "0");
         Document response = RadioConnection.createOutputConnection(this.getHedwig(targetIp), true).pushData(dhcp.getDocument());
         RevBUtil.checkPostSuccessful(response);
         this.activateSettings(targetIp);
      } catch (RadioException var41) {
         throw new RadioException("Failed to program bridge DHCP settings", var41);
      }
   }

   private void activateSettings(String targetIp) throws RadioException {
      logger.info("Activating settings");

      try {
         Document response = RadioConnection.createOutputConnection(this.getPigwidgeon(targetIp), false).pushData("ACTIONS=SETCFG%2CSAVE%2CACTIVATE");
         RevBUtil.checkActivateSuccessful(response);
      } catch (RadioException var31) {
         throw new RadioException("Failed to activate settings", var31);
      }
   }

   private String getHedwig(String targetIp) {
      return "http://" + targetIp + "/hedwig.cgi";
   }

   private String getPigwidgeon(String targetIp) {
      return "http://" + targetIp + "/pigwidgeon.cgi";
   }

   private String validateWireless(WlanData wlanData) {
      String message = "";
      message = message + this.checkSetting("Wireless Radio Enabled", true, wlanData.isWifiEnabled());
      message = message + this.checkSetting("SSID", this.getSSID(), wlanData.getSsidValue(), false);
      message = message + this.checkSetting("Security Type", this.noSecurity ? "OPEN" : "WPA2PSK", wlanData.getAuthTypeValue(), false);
      message = message + this.checkSetting("Cipher", this.noSecurity ? "NONE" : "AES", wlanData.getEncrTypeValue(), false);
      if (!this.noSecurity) {
         message = message + this.checkSetting("WPA Key", this.myWpaKey, wlanData.getWpaKeyValue(), true);
      }

      message = message + this.checkSetting("Wi-Fi Protected Setup Disabled", true, wlanData.isWpsDisabled());
      if (!this.mode.isAP()) {
         message = message + this.checkSetting("MAC Cloning", "Disabled", wlanData.getMacCloneValue(), false);
      } else {
         message = message + this.checkSetting("Bandwidth", "20", wlanData.getBandwidth(), false);
         if (this.mode == FRCRadio.Mode.AP24) {
            message = message + this.checkSetting("Wireless Mode", "gn", wlanData.getWlMode(), false);
         }
      }

      return message;
   }

   private String validateNetwork(LanData lanData) {
      String message = "";
      message = message + this.checkSetting("IP Address", this.getIpAddress(), lanData.getIpAddrValue(), false);
      message = message + this.checkSetting("Subnet Mask", "8", lanData.getMaskValue(), false);
      return message + this.checkSetting("Gateway", this.getGateway(), lanData.getGatewayValue(), false);
   }

   private String validateQos(QosData qosData) {
      return this.checkSetting("Valid QOS settings", true, qosData.validateStaticData());
   }

   private String validateDHCP(DHCPData dhcpData) {
      String message = "";
      message = message + this.checkSetting("DCHP Start", this.getTeamBaseAddress() + "20", dhcpData.getIPStartValue(), false);
      message = message + this.checkSetting("DHCP Network", this.getTeamBaseAddress() + "0", dhcpData.getNetworkValue(), false);
      return message + this.checkSetting("DHCP Enabled", "DHCPS4-1", dhcpData.getDHCPs4Value(), false);
   }

   private void checkRadioAttribute(String attributeName, String actualValue, String[] validValues) throws IllegalRadioException {
      boolean valueOk = false;

      for (String value : validValues) {
         valueOk |= value.equals(actualValue);
      }

      if (!valueOk) {
         throw new IllegalRadioException("Unsupported " + attributeName + " found (" + actualValue + ")");
      }
   }

   private String checkSetting(String label, String expected, String found, boolean hide) {
      String message = "";
      if (!expected.toLowerCase().equals(found.toLowerCase())) {
         message = "Incorrect setting: " + label + ";" + (hide ? "" : " expected \"" + expected + "\", found \"" + found + "\"") + "\n";
      }

      return message;
   }

   private String checkSetting(String label, boolean expected, boolean found) {
      String message = "";
      if (expected ^ found) {
         message = "Incorrect setting: " + label + "; expected \"" + expected + "\", found \"" + found + "\"\n";
      }

      return message;
   }

   private String readFirmwareVersion(String statusData) throws RadioException {
      String regex = "Firmware Version : (.*?)<";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate firmware version");
      } else {
         return m.group(1).trim();
      }
   }

   private String readHardwareVersion(String statusData) throws RadioException {
      String regex = "Hardware Version : (.*?)<";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate hardware version");
      } else {
         return m.group(1).trim();
      }
   }
}
