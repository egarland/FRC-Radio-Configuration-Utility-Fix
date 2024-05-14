package frcradiokiosk;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class FrcNetworkConfig {
   private int myTeam;
   private String RobotIpAddress;
   private String DsLanIpAddress;
   private String DsWlanIpAddress;
   private String RadioIpAddress;
   private String subnet = "255.0.0.0";
   private String gateway;

   public FrcNetworkConfig(int team) {
      this.configure(team);
   }

   public static String buildBaseAddress(int team) {
      return "10." + team / 100 + "." + team % 100 + ".";
   }

   public final void configure(int team) {
      String base = buildBaseAddress(team);
      this.DsLanIpAddress = base + "5";
      this.DsWlanIpAddress = base + "9";
      this.RobotIpAddress = base + "2";
      this.RadioIpAddress = base + "1";
      this.gateway = base + "4";
      this.myTeam = team;
   }

   public InetAddress getRobotInetAddress() throws UnknownHostException {
      return InetAddress.getByName(this.RobotIpAddress);
   }

   public InetAddress getDsLanInetAddress() throws UnknownHostException {
      return InetAddress.getByName(this.DsLanIpAddress);
   }

   public InetAddress getDsWlanInetAddress() throws UnknownHostException {
      return InetAddress.getByName(this.DsWlanIpAddress);
   }

   public InetAddress getRadioInetAddress() throws UnknownHostException {
      return InetAddress.getByName(this.RadioIpAddress);
   }

   public String getRobotIpAddress() {
      return this.RobotIpAddress;
   }

   public String getDsLanIpAddress() {
      return this.DsLanIpAddress;
   }

   public String getDsWlanIpAddress() {
      return this.DsWlanIpAddress;
   }

   public String getRadioIpAddress() {
      return this.RadioIpAddress;
   }

   public String getSubnet() {
      return this.subnet;
   }

   public String getGateway() {
      return this.gateway;
   }

   public int getTeam() {
      return this.myTeam;
   }
}
