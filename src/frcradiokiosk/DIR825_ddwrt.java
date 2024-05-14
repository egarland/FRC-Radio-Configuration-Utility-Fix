package frcradiokiosk;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DIR825_ddwrt extends FRCRadio {
   public static final String DEFAULT_IP = "192.168.1.1";
   public static final String DEFAULT_IP_BASE = "192.168.1.";
   private FRCRadio.Mode mode;
   private String modeString;
   private static final String DEFAULT_USER = "root";
   private static final String DEFAULT_PASSWORD = "admin";
   private String newPasswd = "admin";
   private static final String LOGIN_TOKEN = "DD-WRT login:";
   private static final String PROMPT_TOKEN = "root@DD-WRT:~#";
   private static final String COMMAND = "nvram";
   private String myWpaKey;
   private String channel;
   private String radioOn = "ath1";
   private String radioOff = "ath0";
   private String netMode = "n5-only";
   private RadioTelnetConnection radioComm = new RadioTelnetConnection();
   private static final Logger logger = LoggerFactory.getLogger(DIR825_ddwrt.class);

   protected static String convertChannelIdToValue(String id) {
      if (DIR825_ddwrt.Ap5Channels.getIds().contains(id)) {
         return DIR825_ddwrt.Ap5Channels.getChannelByText(id).getValue();
      } else {
         return DIR825_ddwrt.Ap2Channels.getIds().contains(id) ? DIR825_ddwrt.Ap2Channels.getChannelByText(id).getValue() : null;
      }
   }

   public static FRCRadio.Mode getModeByChannelId(String id) {
      if (DIR825_ddwrt.Ap5Channels.getIds().contains(id)) {
         return FRCRadio.Mode.AP5;
      } else {
         return DIR825_ddwrt.Ap2Channels.getIds().contains(id) ? FRCRadio.Mode.AP24 : FRCRadio.Mode.AP5;
      }
   }

   public static String getDefaultChannelByMode(FRCRadio.Mode mode) {
      return mode == FRCRadio.Mode.AP5 ? DIR825_ddwrt.Ap5Channels.CH36.toString() : DIR825_ddwrt.Ap2Channels.CH1.toString();
   }

   public DIR825_ddwrt(int team, String ssid, String key, FRCRadio.Mode mode, String channelNumber, String password) {
      super(team, ssid);
      this.mode = mode;
      this.modeString = mode == FRCRadio.Mode.BRIDGE ? "wet" : "ap";
      if (this.mode == FRCRadio.Mode.AP24) {
         this.radioOn = "ath0";
         this.radioOff = "ath1";
         this.netMode = "n2-only";
      }

      this.myWpaKey = key;
      this.channel = convertChannelIdToValue(channelNumber);
      if (this.channel == null) {
         this.channel = convertChannelIdToValue(getDefaultChannelByMode(mode));
      } else if (this.channel.isEmpty()) {
         this.channel = convertChannelIdToValue(getDefaultChannelByMode(mode));
      }

      if (!password.isEmpty()) {
         this.newPasswd = password;
      }
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
      return 10;
   }

   @Override
   public int getReconnectWait() {
      return 75;
   }

   @Override
   public String getCustomSuccessMessage() {
      return "";
   }

   @Override
   public void program() throws RadioException {
      this.program("192.168.1.1");
   }

   @Override
   public void program(String target) throws RadioException {
      try {
         logger.info("Programming DD-WRT settings");
         this.radioComm.connect(target != null ? target : "192.168.1.1");
         this.loginWithRetry();
         this.wireless();
         this.lan();
         this.misc();
         this.radioComm.write("setuserpasswd root " + this.newPasswd);
         this.reboot();
      } catch (RadioException var6) {
         throw new RadioException(var6.getMessage(), var6.getUserMessage(), var6);
      } finally {
         this.radioComm.close();
      }
   }

   @Override
   public String validate() throws RadioException {
      logger.info("Validating DD-WRT settings");
      String message = "";

      try {
         this.radioComm.connect(this.getIpAddress());
         this.login("root", this.newPasswd, false);
         message = message + this.checkSetting("SSID", this.getSSID(), this.getSetting(this.radioOn + "_ssid"));
         message = message + this.checkSetting("Radio mode", this.modeString, this.getSetting(this.radioOn + "_mode"));
         message = message + this.checkSetting("Radio Network Mode", this.netMode, this.getSetting(this.radioOn + "_net_mode"));
         message = message + this.checkSetting("Firewall", "off", this.getSetting("filter"));
         message = message + this.checkSetting("Unused Radio", "disabled", this.getSetting(this.radioOff + "_net_mode"));
         message = message + this.checkSetting("DHCP Server", "static", this.getSetting("lan_proto"));
         if (!this.myWpaKey.isEmpty()) {
            message = message + this.checkSetting("Security Type", "psk2", this.getSetting(this.radioOn + "_security_mode"));
            message = message + this.checkSetting("AKM", "psk2", this.getSetting(this.radioOn + "_akm"));
            message = message + this.checkSetting("Cipher", "aes", this.getSetting(this.radioOn + "_crypto"));
            message = message + this.checkSetting("WPA Key", this.myWpaKey, this.getSetting(this.radioOn + "_wpa_psk"));
         } else {
            message = message + this.checkSetting("Security Type", "", this.getSetting(this.radioOn + "_security_mode"));
         }

         message = message + this.checkSetting("IP Addres", this.getIpAddress(), this.getSetting("lan_ipaddr"));
         message = message + this.checkSetting("Subnet Mask", this.getSubnet(), this.getSetting("lan_netmask"));
         message = message + this.checkSetting("Gateway", this.getGateway(), this.getSetting("lan_gateway"));
      } catch (RadioException var6) {
         throw new RadioException("Error validating bridge settings: " + var6.getMessage(), var6);
      } finally {
         this.radioComm.close();
      }

      if (!message.isEmpty()) {
         throw new RadioException(message);
      } else {
         return "null";
      }
   }

   @Override
   public void reset(String target) throws RadioException {
      try {
         this.radioComm.connect(target);
         this.loginWithRetry();
         this.radioComm.write("mtd erase nvram;reboot");
         Thread.sleep(5000L);
      } catch (InterruptedException var7) {
         throw new RadioException("Failed to reset radio", var7);
      } catch (RadioException var8) {
         throw new RadioException("Failed to reset radio", var8);
      } finally {
         this.radioComm.close();
      }
   }

   @Override
   public String getDefaultIpAddress() {
      return "192.168.1.1";
   }

   @Override
   public String getDefaultIpBase() {
      return "192.168.1.";
   }

   private void wireless() throws RadioException {
      try {
         List<String> settings = new LinkedList<>();
         settings.add(this.radioOff + "_net_mode=disabled");
         settings.add(this.radioOn + "_mode=" + this.modeString);
         settings.add(this.radioOn + "_net_mode=" + this.netMode);
         settings.add(this.radioOn + "_ssid=" + this.getSSID());
         settings.add(this.radioOn + "_channel=" + this.channel);
         settings.add(this.radioOn + "_scanlist=" + this.channel);
         if (!this.myWpaKey.isEmpty()) {
            settings.add(this.radioOn + "_security_mode=psk2");
            settings.add(this.radioOn + "_akm=psk2");
            settings.add(this.radioOn + "_crypto=aes");
            settings.add(this.radioOn + "_wpa_psk=" + this.myWpaKey);
         } else {
            settings.add(this.radioOn + "_security_mode=");
         }

         this.radioComm.writeSettings(settings, "nvram", "root@DD-WRT:~#");
      } catch (RadioException var21) {
         throw new RadioException("Failed to program wireless settings", var21);
      }
   }

   private void lan() throws RadioException {
      try {
         List<String> settings = new LinkedList<>();
         settings.add("lan_ipaddr=" + this.getIpAddress());
         settings.add("lan_netmask=" + this.getSubnet());
         settings.add("lan_gateway=" + this.getGateway());
         this.radioComm.writeSettings(settings, "nvram", "root@DD-WRT:~#");
      } catch (RadioException var21) {
         throw new RadioException("Failed to program network settings", var21);
      }
   }

   private void misc() throws RadioException {
      try {
         List<String> settings = new LinkedList<>();
         settings.add("lan_proto=static");
         settings.add("filter=off");
         settings.add("rc_startup=\"echo 0 > /proc/sys/net/ipv4/icmp_echo_ignore_broadcasts\"");
         this.radioComm.writeSettings(settings, "nvram", "root@DD-WRT:~#");
         this.radioComm.write("echo 0 > /proc/sys/net/ipv4/icmp_echo_ignore_broadcasts");
      } catch (RadioException var21) {
         throw new RadioException("Failed to program administrative radio settings", var21);
      }
   }

   private void reboot() {
      this.radioComm.write("reboot");

      try {
         Thread.sleep(5000L);
      } catch (InterruptedException var2) {
         logger.error("Interrupted while committing settings", var2);
      }
   }

   private void loginWithRetry() throws RadioException {
      try {
         try {
            this.login("root", this.newPasswd, false);
         } catch (InvalidLoginException var2) {
            if (this.newPasswd.equals("admin")) {
               throw var2;
            }

            this.login("root", "admin", true);
         }
      } catch (InvalidLoginException var3) {
         throw new RadioException(var3.getMessage(), var3.getUserMessage(), var3);
      } catch (RadioException var4) {
         throw new RadioException(var4.getMessage(), var4.getUserMessage(), var4);
      }
   }

   private void login(String user, String passwd, boolean isRetry) throws InvalidLoginException, RadioException {
      try {
         if (!isRetry) {
            this.radioComm.readUntil("DD-WRT login:");
         }

         this.radioComm.write(user);
         this.radioComm.readUntil("Password: ");
         this.radioComm.write(passwd);
         if (this.radioComm.readUntil(new String[]{"root@DD-WRT:~#", "DD-WRT login:"}).contains("Login incorrect")) {
            throw new InvalidLoginException();
         }
      } catch (InvalidLoginException var5) {
         throw var5;
      } catch (RadioException var6) {
         throw new RadioException("Login failed", var6.getUserMessage(), var6);
      }
   }

   private String getSetting(String header) throws RadioException {
      this.radioComm.write("nvram get " + header);
      String retval = this.radioComm.readUntil("root@DD-WRT:~#");
      Pattern pattern = Pattern.compile("(?im)" + header + "\r\n^(.*?)$");
      Matcher m = pattern.matcher(retval);
      if (!m.find()) {
         throw new RadioException("Failed to retreive " + header);
      } else {
         return m.group(1);
      }
   }

   private String checkSetting(String label, String expected, String found) {
      String message = "";
      if (!expected.equals(found)) {
         message = "Incorrect setting: " + label + "; expected \"" + expected + "\", found \"" + found + "\"";
      }

      return message;
   }

   @Override
   public void checkMode(String targetIp) throws RadioException {
   }

   @Override
   public BufferedImage getImage() {
      try {
         return ImageIO.read(this.getClass().getClassLoader().getResourceAsStream("images/DIR_825.jpg"));
      } catch (IOException var2) {
         java.util.logging.Logger.getLogger(DAP1522.class.getName()).log(Level.SEVERE, null, var2);
         return null;
      }
   }

   @Override
   public String[] getConfigInstructions() {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public String[] getResetInstructions() {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public static enum Ap2Channels {
      AUTO("2.4GHz Auto", "default"),
      CH1("1", "2412"),
      CH2("2", "2417"),
      CH3("3", "2422"),
      CH4("4", "2427"),
      CH5("5", "2432"),
      CH6("6", "2437"),
      CH7("7", "2442"),
      CH8("8", "2447"),
      CH9("9", "2452"),
      CH10("10", "2457"),
      CH11("11", "2462");

      private static final Map<String, DIR825_ddwrt.Ap2Channels> lookup = new HashMap<>();
      private String text;
      private String value;

      private Ap2Channels(String text, String value) {
         this.text = text;
         this.value = value;
      }

      @Override
      public String toString() {
         return this.text;
      }

      protected String getValue() {
         return this.value;
      }

      private static DIR825_ddwrt.Ap2Channels getChannelByText(String string) {
         return lookup.get(string);
      }

      private static Set<String> getIds() {
         return lookup.keySet();
      }

      static {
         for (DIR825_ddwrt.Ap2Channels channel : values()) {
            lookup.put(channel.toString(), channel);
         }
      }
   }

   public static enum Ap5Channels {
      AUTO("5GHz Auto", "default"),
      CH36("36", "5180"),
      CH40("40", "5200"),
      CH44("44", "5220"),
      CH48("48", "5240"),
      CH149("149", "5745"),
      CH153("153", "5765"),
      CH157("157", "5785"),
      CH161("161", "5805"),
      CH165("165", "5825");

      private static final Map<String, DIR825_ddwrt.Ap5Channels> lookup = new HashMap<>();
      private String text;
      private String value;

      private Ap5Channels(String text, String value) {
         this.text = text;
         this.value = value;
      }

      @Override
      public String toString() {
         return this.text;
      }

      protected String getValue() {
         return this.value;
      }

      private static DIR825_ddwrt.Ap5Channels getChannelByText(String string) {
         return lookup.get(string);
      }

      private static Set<String> getIds() {
         return lookup.keySet();
      }

      static {
         for (DIR825_ddwrt.Ap5Channels channel : values()) {
            lookup.put(channel.toString(), channel);
         }
      }
   }
}
