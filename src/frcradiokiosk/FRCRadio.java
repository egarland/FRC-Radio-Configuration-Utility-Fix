package frcradiokiosk;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FRCRadio {
   private FrcNetworkConfig myConfig;
   private FRCRadio.RadioType myType = FRCRadio.RadioType.DEFAULT;
   private String ssid;
   private int practiceField = 1;
   private int practiceRadio = 1;
   protected String country = "US";
   protected boolean firewall = false;
   protected boolean BWLimit = true;
   protected boolean DHCP = true;
   protected String commentString = "";
   protected long date;
   private static final Logger logger = LoggerFactory.getLogger(FRCRadio.class);

   public FRCRadio(int team, String customSsid) {
      logger.info("Creating radio");
      this.myConfig = new FrcNetworkConfig(team);
      logger.info("NetworkConfig created");
      this.ssid = customSsid;
      logger.info("SSID set");
   }

   public abstract boolean broadcastPingable();

   public abstract boolean requiresReset();

   public abstract int getReconnectPings();

   public abstract int getReconnectWait();

   public abstract String getCustomSuccessMessage();

   public abstract void program(String var1) throws RadioException;

   public abstract void program() throws RadioException;

   public abstract String validate() throws RadioException;

   public abstract void reset(String var1) throws RadioException;

   public abstract void checkMode(String var1) throws RadioException;

   public abstract String getDefaultIpAddress();

   public abstract String getDefaultIpBase();

   public abstract BufferedImage getImage();

   public abstract String[] getConfigInstructions();

   public abstract String[] getResetInstructions();

   public void makePracticeRadio(int radioNumber, int field) {
      this.myType = FRCRadio.RadioType.PRACTICE;
      this.practiceRadio = radioNumber;
      this.practiceField = field;
      this.buidPracticeSsid();
   }

   private void buidPracticeSsid() {
      this.ssid = "PracticeField" + String.valueOf(this.practiceField);
   }

   public void makeFieldTestRadio() {
      this.myType = FRCRadio.RadioType.FIELD_TEST;
   }

   public boolean isDefaultMode() {
      return this.myType == FRCRadio.RadioType.DEFAULT;
   }

   public FRCRadio.RadioType getMode() {
      return this.myType;
   }

   public String getModeString() {
      if (this.myType == FRCRadio.RadioType.DEFAULT) {
         return "Bridge for Team " + this.getTeam();
      } else if (this.myType == FRCRadio.RadioType.FIELD_TEST) {
         return this.myType.toString() + " bridge for Team " + this.getTeam();
      } else {
         return this.myType == FRCRadio.RadioType.PRACTICE ? this.myType.toString() + "_" + this.practiceRadio + " bridge" : this.myType.toString();
      }
   }

   public String getIpAddress() {
      return this.myType == FRCRadio.RadioType.PRACTICE ? "10.0.0." + String.valueOf(this.practiceRadio) : this.myConfig.getRadioIpAddress();
   }

   public String getSubnet() {
      return this.myConfig.getSubnet();
   }

   public String getGateway() {
      return this.myType == FRCRadio.RadioType.PRACTICE ? "10.0.0.10" : this.myConfig.getGateway();
   }

   public String getTeamBaseAddress() {
      return FrcNetworkConfig.buildBaseAddress(this.myConfig.getTeam());
   }

   public final int getTeam() {
      return this.myConfig.getTeam();
   }

   public String getSSID() {
      return this.ssid;
   }

   public Boolean isConnected(String targetIp, int wait) throws RadioException {
      try {
         boolean isConnected = false;
         int timeOut = wait * 1000;
         long elapsed = 0L;

         for (long startTime = System.currentTimeMillis(); !isConnected && elapsed <= (long)timeOut; elapsed = System.currentTimeMillis() - startTime) {
            isConnected = Inet4Address.getByName(targetIp).isReachable((int)((long)timeOut - elapsed + 100L));
         }

         return isConnected;
      } catch (UnknownHostException var9) {
         throw new RadioException("An error occurred while connecting to the bridge", var9);
      } catch (IOException var101) {
         throw new RadioException("An error occurred while connecting to the bridge", var101);
      }
   }

   public void setCountry(String countryCode) {
      this.country = countryCode;
   }

   public void setFirewall(boolean firewallChoice) {
      this.firewall = firewallChoice;
   }

   public void setBWLimit(boolean BWLimitChoice) {
      this.BWLimit = BWLimitChoice;
   }

   public void setDHCP(boolean dhcpChoice) {
      this.DHCP = dhcpChoice;
   }

   public void setComment(String comment) {
      this.commentString = comment;
   }

   public void setDate(long val) {
      this.date = val;
   }

   public void prepareLoadFirmware(String adapter) throws RadioException {
      throw new RadioException("Firmware loading not implemented for this device");
   }

   public void firmwareWaitForRadio() throws RadioException {
      throw new RadioException("Firmware loading not implemented for this device");
   }

   public void flashRadio() throws RadioException {
      throw new RadioException("Firmware loading not implemented for this device");
   }

   public String getFirmwareLoadInstructions() {
      return "Firmware loading not implemented for this device";
   }

   public static enum Country {
      Australia("Australia", "AU"),
      Brazil("Brazil", "BR"),
      Canada("Canada", "CA"),
      Chile("Chile", "CL"),
      China("China", "CN"),
      Columbia("Columbia", "CO"),
      CzechRepublic("Czech Republic", "CZ"),
      Denmark("Denmark", "DK"),
      DominicanRepublic("Dominican Republic", "DO"),
      Ecuador("Ecuador", "EC"),
      France("France", "FR"),
      Germany("Germany", "DE"),
      India("India", "IN"),
      Israel("Israel", "IL"),
      Japan("Japan", "JP"),
      Mexico("Mexico", "MX"),
      Netherlands("Netherlands", "NL"),
      Poland("Poland", "PL"),
      Singapore("Singapore", "SG"),
      Taiwan("Taiwan", "TW"),
      Turkey("Turkey", "TR"),
      USA("USA", "US"),
      UnitedArabEmirates("United Arab Emirates", "AE"),
      UnitedKingdom("United Kingdom", "GB");

      String longString;
      String shortString;

      private Country(String longString, String shortString) {
         this.longString = longString;
         this.shortString = shortString;
      }

      public String getLongString() {
         return this.longString;
      }

      public String getShortString() {
         return this.shortString;
      }
   }

   public static enum Mode {
      BRIDGE("Bridge", "BRIDGE", "B5"),
      AP24("2.4GHz Access Point", "AP 2.4GHz", "AP24"),
      AP5("5GHz Access Point", "AP 5GHz", "AP5"),
      BRIDGE24("2.4GHz Bridge", "BRIDGE24", "B24");

      String longString;
      String shortString;
      String chars;

      private Mode(String longString, String shortString, String chars) {
         this.longString = longString;
         this.shortString = shortString;
         this.chars = chars;
      }

      public String getLongString() {
         return this.longString;
      }

      public String getShortString() {
         return this.shortString;
      }

      public String getChars() {
         return this.chars;
      }

      public boolean isAP() {
         return this != BRIDGE;
      }
   }

   public static enum RadioType {
      DEFAULT,
      PRACTICE,
      FIELD_TEST,
      PRACTICE_AP;
   }
}
