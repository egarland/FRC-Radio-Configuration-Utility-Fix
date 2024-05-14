package frcradiokiosk;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.commons.net.telnet.TelnetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Linksys54GL extends FRCRadio {
   public static final String DEFAULT_IP = "10.0.1.1";
   public static final String DEFAULT_IP_BASE = "10.0.0.";
   private static final String WRT_DEFAULT_IP = "192.168.1.1";
   private static final String WRT_5GHZ_PREFIX = "wireless.@wifi-iface[0].";
   private static final String WRT_5GHZ_REGEX = "(?im)wireless\\.@wifi-iface\\[0\\]\\.";
   private static final String WRT_NETWORK_PREFIX = "network.lan.";
   private static final String WRT_NETWORK_REGEX = "(?im)network\\.lan\\.";
   private static final Logger logger = LoggerFactory.getLogger(Linksys54GL.class);
   private TelnetClient telnet;
   private String myWpaKey;

   public Linksys54GL(int team, String key) {
      super(team, String.valueOf(team));
      this.myWpaKey = key;
      this.telnet = new TelnetClient();
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
      return 20;
   }

   @Override
   public String getCustomSuccessMessage() {
      return "";
   }

   @Override
   public void program() throws RadioException {
      this.program("10.0.1.1");
   }

   @Override
   public void program(String target) throws RadioException {
      try {
         logger.info("Programming OpenWRT settings");
         this.telnet.connect(target != null ? target : "10.0.1.1");
         InputStream in = this.telnet.getInputStream();
         PrintStream out = new PrintStream(this.telnet.getOutputStream());
         List<String> settings = new LinkedList<>();
         settings.add("wireless.@wifi-iface[0].ssid=" + this.getSSID());
         settings.add("wireless.@wifi-iface[0].key=" + this.myWpaKey);
         settings.add("network.lan.ipaddr=" + this.getIpAddress());
         settings.add("network.lan.netmask=" + this.getSubnet());
         settings.add("network.lan.gateway=" + this.getGateway());
         this.writeSettings(settings, out, in);
         this.telnet.disconnect();
      } catch (SocketException var5) {
         throw new RadioException("Failed to program radio settings", var5);
      } catch (IOException var61) {
         throw new RadioException("Failed to program radio settings", var61);
      }
   }

   @Override
   public String validate() throws RadioException {
      logger.info("Validating OpenWRT settings");
      String uciShowWireless = null;
      String uciShowLan = null;

      try {
         this.telnet.connect(this.getIpAddress());
         InputStream in = this.telnet.getInputStream();
         PrintStream out = new PrintStream(this.telnet.getOutputStream());
         this.readUntil(in);
         this.write("uci show wireless", out);
         uciShowWireless = this.readUntil(in);
         this.write("uci show network", out);
         uciShowLan = this.readUntil(in);
         this.telnet.disconnect();
      } catch (IOException var6) {
         throw new RadioException("Failed to reconnect to bridge for vaildation", var6);
      }

      logger.info("validating settings");
      String message = "";

      try {
         message = message + this.checkSetting("SSID", this.getSSID(), this.readRadioSSID(uciShowWireless));
         message = message + this.checkSetting("WPA Key", this.myWpaKey, this.readRadioWpaKey(uciShowWireless));
         message = message + this.checkSetting("IP Addres", this.getIpAddress(), this.readIPaddress(uciShowLan));
         message = message + this.checkSetting("Subnet Mask", this.getSubnet(), this.readSubnet(uciShowLan));
         message = message + this.checkSetting("Gateway", this.getGateway(), this.readGateway(uciShowLan));
      } catch (RadioException var51) {
         throw new RadioException("Error validating bridge settings: " + var51.getMessage(), var51);
      }

      if (!message.isEmpty()) {
         throw new RadioException(message);
      } else {
         return "null";
      }
   }

   @Override
   public void reset(String target) throws RadioException {
   }

   @Override
   public String getDefaultIpAddress() {
      return "10.0.1.1";
   }

   @Override
   public String getDefaultIpBase() {
      return "10.0.0.";
   }

   private void writeSettings(List<String> settings, PrintStream out, InputStream in) throws RadioException {
      logger.debug("writing settings");

      for (String setting : settings) {
         this.write("uci set " + setting, out);
         this.readUntil(in);
      }

      this.write("uci commit", out);
      this.readUntil(in);
      this.write("/etc/init.d/network restart", out);
      this.readUntil(in);

      try {
         Thread.sleep(5000L);
      } catch (InterruptedException var6) {
         logger.error("Interrupted while committing settings", var6);
      }
   }

   private void write(String value, PrintStream out) {
      logger.trace("writing {}", value);
      out.println(value);
      out.flush();
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

   private String checkSetting(String label, String expected, String found) {
      String message = "";
      if (!expected.equals(found)) {
         message = "Incorrect setting: " + label + "; expected \"" + expected + "\", found \"" + found + "\"";
      }

      return message;
   }

   private String readRadioSSID(String data) throws RadioException {
      String regex = "(?im)wireless\\.@wifi-iface\\[0\\]\\.ssid=(.*?)$";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(data);
      if (!m.find()) {
         throw new RadioException("Failed to validate SSID");
      } else {
         return m.group(1);
      }
   }

   private String readRadioWpaKey(String data) throws RadioException {
      String regex = "(?im)wireless\\.@wifi-iface\\[0\\]\\.key=(.*?)$";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(data);
      if (!m.find()) {
         throw new RadioException("Failed to validate WPA key");
      } else {
         return m.group(1);
      }
   }

   private String readIPaddress(String data) throws RadioException {
      String regex = "(?im)network\\.lan\\.ipaddr=(.*?)$";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(data);
      if (!m.find()) {
         throw new RadioException("Failed to validate IP address");
      } else {
         return m.group(1);
      }
   }

   private String readSubnet(String data) throws RadioException {
      String regex = "(?im)network\\.lan\\.netmask=(.*?)$";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(data);
      if (!m.find()) {
         throw new RadioException("Failed to validate subnet mask");
      } else {
         return m.group(1);
      }
   }

   private String readGateway(String data) throws RadioException {
      String regex = "(?im)network\\.lan\\.gateway=(.*?)$";
      Pattern pattern = Pattern.compile(regex);
      Matcher m = pattern.matcher(data);
      if (!m.find()) {
         throw new RadioException("Failed to validate default gateway");
      } else {
         return m.group(1);
      }
   }

   @Override
   public void checkMode(String targetIp) throws RadioException {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public BufferedImage getImage() {
      try {
         return ImageIO.read(this.getClass().getClassLoader().getResourceAsStream("images/Linksys54GL.jpg"));
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
}
