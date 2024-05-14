package frcradiokiosk;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DAP1522 extends FRCRadio {
   public static final String DEFAULT_IP = "192.168.0.50";
   public static final String DEFAULT_IP_BASE = "192.168.0.";
   private static final String DEFAULT_USERNAME = "admin";
   private static final String DEFAULT_PASSWORD = "";
   private static String newPassword = "";
   private static final Logger logger = LoggerFactory.getLogger(DAP1522.class);
   private static final ResourceHandler handler = new ResourceHandler();
   private String myWpaKey;
   private FRCRadio.Mode mode;
   private boolean noSecurity;

   public DAP1522(int team, String ssid, String key, FRCRadio.Mode mode, String password) {
      super(team, ssid);
      if (!key.isEmpty()) {
         this.myWpaKey = key;
         this.noSecurity = false;
      } else {
         this.noSecurity = true;
      }

      this.mode = mode;
      if (!password.isEmpty()) {
         newPassword = password;
      }

      Object[] logData = new Object[]{String.valueOf(team), this.getSSID(), mode.toString()};
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
   public void program(String target) throws RadioException {
      throw new RadioException("Programming a non-default target is not supported for this bridge model");
   }

   @Override
   public boolean broadcastPingable() {
      return true;
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
      return "Bridge must be power-cycled for new settings to take effect";
   }

   @Override
   public BufferedImage getImage() {
      try {
         return ImageIO.read(this.getClass().getResource("/frcradiokiosk/RevA.png"));
      } catch (IOException var2) {
         java.util.logging.Logger.getLogger(DAP1522.class.getName()).log(Level.SEVERE, null, var2);
         return null;
      }
   }

   @Override
   public String[] getConfigInstructions() {
      return new String[]{
         "1) Ensure the mode switch is set to \"" + (this.mode.isAP() ? "AP" : "BRIDGE") + "\"",
         "2) Connect power and Ethernet to the wireless bridge",
         "3) Wait for the " + (this.mode.isAP() ? "blue AP" : "orange bridge") + " light to begin flashing"
      };
   }

   @Override
   public String[] getResetInstructions() {
      return new String[]{
         "1) Ensure the mode switch is set to \"" + (this.mode.isAP() ? "AP" : "BRIDGE") + "\"",
         "2) Connect power and Ethernet to the wireless bridge",
         "3) Wait for the " + (this.mode.isAP() ? "blue AP" : "orange bridge") + " light to begin flashing",
         "4) Press and hold the \"Reset\" button 10 seconds",
         "5) The " + (this.mode.isAP() ? "blue AP" : "orange bridge") + " light will stop flashing after a few seconds",
         "6) Once the " + (this.mode.isAP() ? "blue AP" : "orange bridge") + " resumes flashing, reset is complete"
      };
   }

   @Override
   public void program() throws RadioException {
      logger.info("Programming DAP1522 settings");

      try {
         logger.debug("Logging in");
         this.login("192.168.0.50");
         if (!this.checkCorrectMode("192.168.0.50")) {
            throw new RadioException(
               "Bridge is not in " + (this.mode.isAP() ? "Access Point" : "Bridge") + " mode", "Please switch the bridge to the correct mode and try again"
            );
         } else {
            this.wlan();
            if (this.mode.isAP()) {
               this.qos();
               this.dhcp();
            }

            if (!newPassword.equals("")) {
               this.admin();
            }

            this.network();
         }
      } catch (RadioException var2) {
         throw new RadioException(var2.getMessage(), var2.getUserMessage(), var2);
      }
   }

   private static String buildLoginUrl(String targetIp) {
      return "http://" + targetIp + "/login.php";
   }

   public static String buildWlanUrl(String targetIp) {
      return "http://" + targetIp + "/bsc_wlan.php";
   }

   public static String buildLanUrl(String targetIp) {
      return "http://" + targetIp + "/bsc_lan.php";
   }

   public static String buildStatusUrl(String targetIp) {
      return "http://" + targetIp + "/st_device.php";
   }

   public static String buildQosUrl(String targetIp) {
      return "http://" + targetIp + "/adv_trafficmanage.php";
   }

   private static String buildActivationUrl(String targetIp) {
      return "http://"
         + targetIp
         + "/bsc_lan.xgi?random_num="
         + randomNumber()
         + "&exeshell=submit%20COMMIT&exeshell=&exeshell=submit%20WAN&exeshell=%20submit%20CONSOLE";
   }

   private static String buildAdminUrl(String targetIp) {
      return "http://" + targetIp + "/tools_admin.php";
   }

   private static String buildDHCPUrl(String targetIp) {
      return "http://" + targetIp + "/adv_dhcp_server.php";
   }

   @Override
   public String validate() throws RadioException {
      logger.info("Validating DAP1522 settings");
      String message = "";
      String firmware = "null";

      try {
         if (!newPassword.equals("")) {
            this.login(this.getIpAddress(), "admin", newPassword);
         } else {
            this.login(this.getIpAddress());
         }

         String rxWlanPage = this.getPage(buildWlanUrl(this.getIpAddress()), 5, false);
         String rxStatusPage = this.getPage(buildStatusUrl(this.getIpAddress()), 5, false);
         firmware = this.readFirmwareVersion(rxStatusPage);
         message = message + this.checkSetting("Device Mode", this.mode.isAP() ? "Access Point" : "Bridge Mode", this.readDeviceMode(rxWlanPage), false);
         message = message + this.checkSetting("Wireless Radio Enabled", "Enabled", this.readWirelessEnabled(rxStatusPage), false);
         message = message + this.checkSetting("SSID", this.getSSID(), this.readRadioSSID(rxStatusPage), false);
         if (!this.noSecurity) {
            message = message + this.checkSetting("Security Type", "WPA2-Personal", this.readSecurityType(rxStatusPage), false);
            message = message + this.checkSetting("Cipher", "AES", this.readCipherType(rxStatusPage), false);
            message = message + this.checkSetting("WPA Key", this.myWpaKey, this.readRadioWpaKey(rxWlanPage), true);
         } else {
            message = message + this.checkSetting("Security Type", "Open", this.readSecurityType(rxStatusPage), false);
         }

         message = message + this.checkSetting("Wi-Fi Protected Setup", "Disabled", this.readWps(rxStatusPage), false);
         if (!this.mode.isAP()) {
            message = message + this.checkSetting("MAC Cloning", "", this.readMacCloning(rxWlanPage), false);
         }

         if (this.mode.isAP()) {
            String rxTrafficPage = this.getPage(buildQosUrl(this.getIpAddress()), 5, false);
            message = message + this.checkSetting("Ethernet to Wireless bandwith", "7000", this.readEthToWifiLimit(rxTrafficPage), false);
            message = message + this.checkSetting("Wireless to Ethernet bandwidth", "7000", this.readWifiToEthLimit(rxTrafficPage), false);
         }

         message = message + this.checkSetting("IP Addres", this.getIpAddress(), this.readIPaddress(rxStatusPage), false);
         message = message + this.checkSetting("Subnet Mask", this.getSubnet(), this.readSubnet(rxStatusPage), false);
         message = message + this.checkSetting("Gateway", this.getGateway(), this.readGateway(rxStatusPage), false);
      } catch (RadioException var61) {
         throw new RadioException("Error validating bridge settings: " + var61.getMessage(), var61);
      }

      if (!message.isEmpty()) {
         throw new RadioException(message);
      } else {
         return firmware;
      }
   }

   public String getRadioFirmware(String targetIp) throws RadioException {
      try {
         this.login(targetIp);
         String statusPage = this.getPage(buildStatusUrl(targetIp), 2, false);
         return this.readFirmwareVersion(statusPage);
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
         this.getPage("http://" + target + "/sys_cfg_valid.xgi?&exeshell=submit%20FRESET", 2, true);
         Thread.sleep(10000L);
         logger.debug("Reset process finished");
      } catch (InterruptedException var3) {
         throw new RadioException("Automated bridge reset interrupted", var3);
      } catch (RadioException var4) {
         throw new RadioException("Automated bridge reset failed", var4);
      }
   }

   @Override
   public void checkMode(String targetIp) throws RadioException {
      try {
         this.loginWithRetry(targetIp);
         if (!this.checkCorrectMode(targetIp)) {
            throw new RadioException(
               "Bridge is not in " + (this.mode.isAP() ? "Access Point" : "Bridge") + " mode", "Please switch the bridge to the correct mode and try again"
            );
         }
      } catch (RadioException var3) {
         throw new RadioException(var3.getMessage(), var3.getUserMessage(), var3);
      }
   }

   private boolean checkCorrectMode(String targetIp) throws RadioException {
      logger.debug("Check bridge mode process started");

      try {
         String rxWlanPage = this.getPage(buildWlanUrl(targetIp), 2, false);
         return this.readDeviceMode(rxWlanPage).equals(this.mode.isAP() ? "Access Point" : "Bridge Mode");
      } catch (RadioException var31) {
         throw new RadioException("Failed to check bridge operating mode", var31);
      }
   }

   public void loginWithRetry(String targetIp) throws RadioException {
      try {
         try {
            this.login(targetIp);
         } catch (InvalidLoginException var3) {
            if (newPassword.equals("")) {
               throw var3;
            }

            logger.debug("Default login credentials refused, trying competition credentials");
            this.login(targetIp, "admin", newPassword);
         }
      } catch (RadioException var4) {
         throw new RadioException(var4.getMessage(), var4);
      }
   }

   private void login(String targetIp) throws RadioException {
      this.login(targetIp, "admin", "");
   }

   private void login(String targetIp, String user, String pass) throws RadioException {
      logger.info("Login process started for target {}", targetIp);
      HashMap<String, String> data = new HashMap<>();
      data.put("ACTION_POST", "login");
      data.put("LOGIN_USER", user);
      data.put("LOGIN_PASSWD", pass);

      try {
         String response = this.pushSettings(buildLoginUrl(targetIp), data);
         if (!response.contains("<META HTTP-EQUIV=Refresh CONTENT='0; url=index.php'>")) {
            throw new InvalidLoginException();
         }
      } catch (InvalidLoginException var6) {
         throw new InvalidLoginException(var6);
      } catch (RadioException var71) {
         throw new RadioException("Bridge login failed", var71);
      }
   }

   private void wlan() throws RadioException {
      logger.info("Programming wireless settings");
      HashMap<String, String> data = new HashMap<>();
      data.put("ACTION_POST", "final");
      data.put("f_enable", "1");
      data.put("f_wps_enable", "0");
      data.put("f_wps_lock_enable", this.mode.isAP() ? "0" : "");
      data.put("f_ssid", this.getSSID());
      data.put("f_channel", this.mode == FRCRadio.Mode.AP24 ? "6" : "36");
      data.put("f_band", this.mode == FRCRadio.Mode.AP24 ? "0" : "1");
      data.put("f_mode", this.mode == FRCRadio.Mode.AP24 ? "6" : "8");
      data.put("f_auto_channel", this.mode.isAP() ? "1" : "0");
      data.put("f_transmission_rate", "31");
      data.put("f_channel_width", "0");
      data.put("f_wmm_enable", "");
      data.put("f_ap_hidden", "0");
      data.put("f_authentication", this.noSecurity ? "0" : "5");
      data.put("f_cipher", this.noSecurity ? "0" : "3");
      data.put("f_wep_len", "");
      data.put("f_wep_format", "");
      data.put("f_wep_def_key", "");
      data.put("f_wep1", "");
      data.put("f_wep2", "");
      data.put("f_wep3", "");
      data.put("f_wep4", "");
      data.put("f_wep", "");
      data.put("f_wpa_psk_type", this.noSecurity ? "0" : "1");
      data.put("f_wpa_psk", this.noSecurity ? "" : this.myWpaKey);
      data.put("f_radius_ip1", "");
      data.put("f_radius_port1", "");
      data.put("f_radius_secret1", "");
      data.put("f_grp_key_interval", this.mode.isAP() && !this.noSecurity ? "1800" : "");
      data.put("f_scan_value", "");
      data.put("f_macclone_enable", this.mode.isAP() ? "" : "0");
      data.put("f_macclone_macaddress", "");
      data.put("f_macclone_macsource", this.mode.isAP() ? "" : "0");
      data.put("matMacManual", "");

      try {
         this.pushSettings(buildWlanUrl("192.168.0.50"), data);
      } catch (RadioException var3) {
         throw new RadioException("Failed to program bridge wireless settings", var3);
      }
   }

   private void qos() throws RadioException {
      logger.info("Programming QOS settings");
      HashMap<String, String> data = new HashMap<>();
      data.put("ACTION_POST", "final");
      data.put("f_TrafficMgr_enable", "1");
      data.put("f_unlistclientstraffic", "1");
      data.put("f_Eth0ToWirelessPrimary", "7000");
      data.put("f_WirelessToEth0Primary", "7000");

      try {
         this.pushSettings(buildQosUrl("192.168.0.50"), data);
      } catch (RadioException var3) {
         throw new RadioException("Failed to program bridge QOS settings", var3);
      }
   }

   private void dhcp() throws RadioException {
      logger.info("Programming DHCP settings");
      HashMap<String, String> data = new HashMap<>();
      data.put("ACTION_POST", "final");
      data.put("dhcpsvr", "1");
      data.put("startip", this.getTeamBaseAddress() + "20");
      data.put("startipaddr", this.getTeamBaseAddress() + "20");
      data.put("endip", "199");
      data.put("endipaddr", this.getTeamBaseAddress() + "199");
      data.put("lease_seconds", "604800");
      data.put("leasetime", "10080");
      data.put("netmask", "255.255.255.0");

      try {
         this.pushSettings(buildDHCPUrl("192.168.0.50"), data);
      } catch (RadioException var3) {
         throw new RadioException("Failed to program bridge QOS settings", var3);
      }
   }

   private void admin() throws RadioException {
      logger.info("Programming admin password");
      HashMap<String, String> data = new HashMap<>();
      data.put("ACTION_POST", "1");
      data.put("admin_password1", newPassword);
      data.put("admin_password2", newPassword);

      try {
         this.pushSettings(buildAdminUrl("192.168.0.50"), data);
      } catch (RadioException var3) {
         throw new RadioException("Failed to program admin password", var3);
      }
   }

   private void network() throws RadioException {
      logger.info("Programming network settings");
      HashMap<String, String> data = new HashMap<>();
      data.put("ACTION_POST", "STATIC");
      data.put("f_device_name", "dlinkap");
      data.put("wan_type", "1");
      data.put("static_ipaddr", this.getIpAddress());
      data.put("static_netmask", "255.0.0.0");
      data.put("static_gateway", this.getGateway());

      try {
         this.pushSettings(buildLanUrl("192.168.0.50"), data);
         this.getPage(buildActivationUrl("192.168.0.50"), 5, true);
      } catch (RadioException var3) {
         throw new RadioException("Failed to program bridge network settings", var3);
      }
   }

   private static String randomNumber() {
      String ran = "";
      Calendar cal = Calendar.getInstance();
      String day = Integer.toString(cal.get(5));
      String month = Integer.toString(cal.get(2) + 1);
      String year = Integer.toString(cal.get(1));
      String seconds = Integer.toString(cal.get(13));
      String minutes = Integer.toString(cal.get(12));
      String hours = Integer.toString(cal.get(10));
      return year + "." + month + "." + day + "." + hours + "." + minutes + "." + seconds;
   }

   private String getPage(String url, int timeout, boolean ignoreTimeout) throws RadioException {
      logger.debug("getting page: {}", url);
      String data = "";
      URL siteUrl = null;
      HttpURLConnection conn = null;
      BufferedReader rd = null;

      Object var9;
      try {
         siteUrl = new URL(url);
         conn = (HttpURLConnection)siteUrl.openConnection();
         conn.setRequestMethod("GET");
         if (timeout > 0) {
            conn.setReadTimeout(timeout * 1000);
         }

         logger.debug("connection intialized, getting response");
         rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
         String line = "";

         while ((line = rd.readLine()) != null) {
            data = data + line;
            logger.trace(line);
         }

         logger.debug("Page received");
         return data;
      } catch (SocketTimeoutException var151) {
         if (!ignoreTimeout) {
            throw new RadioException("Timed out while getting " + url + " webpage", var151);
         }

         logger.debug("Timed out; OK");
         var9 = null;
      } catch (IOException var16) {
         throw new RadioException("Failed to get " + url + " webpage", var16);
      } finally {
         handler.closeReader(rd);
         handler.closeURLConnection(conn);
      }

      return (String)var9;
   }

   private String pushSettings(String url, HashMap<String, String> data) throws RadioException {
      logger.debug("Pushing data to {}", url);
      URL siteUrl = null;
      HttpURLConnection conn = null;
      DataOutputStream out = null;
      BufferedReader rd = null;
      String content = "";

      String var10;
      try {
         siteUrl = new URL(url);
         conn = (HttpURLConnection)siteUrl.openConnection();
         conn.setReadTimeout(15000);
         conn.setRequestMethod("POST");
         conn.setDoOutput(true);
         conn.setDoInput(true);
         out = new DataOutputStream(conn.getOutputStream());

         for (Entry<String, String> entry : data.entrySet()) {
            logger.trace(entry.getKey() + "=" + entry.getValue());
            content = content + entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), "UTF-8") + "&";
         }

         content = content.substring(0, content.length() - 1);
         out.writeBytes(content);
         out.flush();
         logger.debug("settings pushed to radio");
         logger.debug("getting response...");
         rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
         StringBuilder sb = new StringBuilder();

         String line;
         while ((line = rd.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
            logger.trace(line);
         }

         logger.debug("pushSettings process complete");
         var10 = sb.toString();
      } catch (IOException var14) {
         throw new RadioException("Failed to post data to " + url + " webpage", var14);
      } finally {
         handler.closeOutputStream(out);
         handler.closeReader(rd);
         handler.closeURLConnection(conn);
      }

      return var10;
   }

   private String checkSetting(String label, String expected, String found, boolean hide) {
      String message = "";
      if (!expected.toLowerCase().equals(found.toLowerCase())) {
         message = "Incorrect setting: " + label + ";" + (hide ? "" : " expected \"" + expected + "\", found \"" + found + "\"") + "\n";
      }

      return message;
   }

   private String readRadioSSID(String statusData) throws RadioException {
      String regex = "Network Name.*?l_tb.*?;(.*?)<";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate SSID");
      } else {
         return m.group(1);
      }
   }

   private String readRadioWpaKey(String wlanData) throws RadioException {
      String regex = "f_wpa.wpapsk1.value.*?\"(.*?)\"";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(wlanData);
      if (!m.find()) {
         throw new RadioException("Failed to validate WPA key");
      } else {
         return m.group(1);
      }
   }

   private String readDeviceMode(String wlanData) throws RadioException {
      String regex = "Wireless Mode.*?l_tb'><b>(.*?)<";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(wlanData);
      if (!m.find()) {
         throw new RadioException("Failed to validate operating mode");
      } else {
         return m.group(1);
      }
   }

   private String readIPaddress(String statusData) throws RadioException {
      String regex = "IP Address.*?l_tb.*?;(.*?)<";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate IP address");
      } else {
         return m.group(1);
      }
   }

   private String readSubnet(String statusData) throws RadioException {
      String regex = "Subnet Mask.*?l_tb.*?;(.*?)<";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate subnet mask");
      } else {
         return m.group(1);
      }
   }

   private String readGateway(String statusData) throws RadioException {
      String regex = "Default Gateway.*?l_tb.*?;(.*?)<";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate default gateway");
      } else {
         return m.group(1);
      }
   }

   private String readFirmwareVersion(String statusData) throws RadioException {
      String regex = "Firmware Version.*?l_tb.*?<b>(.*?),";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate firmware version");
      } else {
         return m.group(1).trim();
      }
   }

   private String readSecurityType(String statusData) throws RadioException {
      String type = null;
      String regex = this.noSecurity ? "Security Type.*?l_tb.*?;(.*?)/" : "Security Type.*?l_tb.*?;(.*?)&.*?;(.*?)/";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate security type");
      } else {
         return this.noSecurity ? m.group(1).trim() : m.group(1) + m.group(2).trim();
      }
   }

   private String readCipherType(String statusData) throws RadioException {
      String regex = "Security Type.*?l_tb.*?/.*?;(.*?)<";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate security cipher");
      } else {
         return m.group(1);
      }
   }

   private String readWps(String statusData) throws RadioException {
      String regex = "Wi-Fi Protected Setup.*?l_tb\">(.*?)/";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate Wi-Fi Protected Setup");
      } else {
         return m.group(1).trim();
      }
   }

   private String readMacCloning(String wlanData) throws RadioException {
      String regex = "<input name=\"maccloneenable\".*?value=\"1\"(.*?)>";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(wlanData);
      if (!m.find()) {
         throw new RadioException("Failed to validate MAC Cloning");
      } else {
         return m.group(1).trim();
      }
   }

   private String readWirelessEnabled(String statusData) throws RadioException {
      String regex = "Wireless Radio.*?l_tb.*?;(.*?)<";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(statusData);
      if (!m.find()) {
         throw new RadioException("Failed to validate radio state");
      } else {
         return m.group(1).trim();
      }
   }

   private String readEthToWifiLimit(String trafficData) throws RadioException {
      String regex = "name=Eth0ToWirelessPrimary.*?value=\"(.*?)\">kbits/sec";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(trafficData);
      if (!m.find()) {
         throw new RadioException("Failed to validate ethernet to wireless bandwidth limit");
      } else {
         return m.group(1).trim();
      }
   }

   private String readWifiToEthLimit(String trafficData) throws RadioException {
      String regex = "name=WirelessToEth0Primary.*?value=\"(.*?)\">kbits/sec";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(trafficData);
      if (!m.find()) {
         throw new RadioException("Failed to validate wireless to ethernet bandwidth limit");
      } else {
         return m.group(1).trim();
      }
   }
}
