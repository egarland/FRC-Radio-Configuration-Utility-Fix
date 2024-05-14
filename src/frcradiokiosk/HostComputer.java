package frcradiokiosk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostComputer {
   private HostComputer.OS myOS;
   private HashMap<String, String> myNicMap;
   private String mySelectedInterface;
   private String userName;
   private ResourceHandler handler;
   private static final Logger logger = LoggerFactory.getLogger(HostComputer.class);

   public HostComputer() throws HostException {
      try {
         this.handler = new ResourceHandler();
         this.myOS = this.getOS();
         this.userName = System.getProperty("user.name");
         logger.debug("Operating system: {}, user: {}", this.myOS.toString(), this.userName);
         this.myNicMap = this.buildNicMap(this.buildNicList());
      } catch (HostException var2) {
         throw new HostException("Failed to initialize host computer settings", var2);
      }
   }

   public void refreshNicMap() throws HostException {
      logger.info("refreshing NICs");

      try {
         this.myNicMap = this.buildNicMap(this.buildNicList());
      } catch (HostException var2) {
         throw new HostException("Failed to refresh network interfaces", var2);
      }
   }

   private HostComputer.OS getOS() throws HostException {
      logger.info("Retreiving OS");
      String os = System.getProperty("os.name");
      logger.debug("Raw OS: {}", os);
      if (os.matches("(?i)win.*?xp")) {
         return HostComputer.OS.WINDOWS_XP;
      } else if (os.matches("(?i)win.*?vista")) {
         return HostComputer.OS.WINDOWS_VISTA;
      } else if (os.matches("(?i)win.*?7")) {
         return HostComputer.OS.WINDOWS_7;
      } else if (os.matches("(?i)win.*?2003")) {
         return HostComputer.OS.WINDOWS_SERVER;
      } else if (os.matches("(?i)win.*?8.*")) {
         return HostComputer.OS.WINDOWS_8;
      } else if (os.matches("(?i)win.*?10*")) {
         return HostComputer.OS.WINDOWS_10;
      } else if (os.matches("(?i)mac.*?")) {
         return HostComputer.OS.MAC;
      } else if (os.matches("(?i).*?nux")) {
         return HostComputer.OS.LINUX;
      } else {
         throw new HostException("Could not detect operating system, found: " + os);
      }
   }

   private List<NetworkInterface> buildNicList() throws HostException {
      logger.info("Building NIC list");
      Enumeration<NetworkInterface> nics = null;
      List<NetworkInterface> nicList = new ArrayList<>();

      try {
         nics = NetworkInterface.getNetworkInterfaces();
      } catch (SocketException var6) {
         throw new HostException("Failed to retrieve network interfaces", var6);
      }

      if (nics == null) {
         throw new HostException("Error detecting network interfaces");
      } else {
         while (nics.hasMoreElements()) {
            NetworkInterface n = nics.nextElement();

            try {
               if (!(n.isLoopback() | n.isVirtual())) {
                  nicList.add(n);
               }
            } catch (SocketException var5) {
               throw new HostException("Failed to check nic" + n.getName() + "virtual or loopback status", var5);
            }
         }

         logger.debug("{} network interfaces found", nicList.size());
         return nicList;
      }
   }

   private HashMap<String, String> buildNicMap(List<NetworkInterface> nicList) throws HostException {
      logger.info("Building NIC map");
      HashMap<String, String> nicMap = new HashMap<>();
      if (this.myOS == HostComputer.OS.WINDOWS_7
         | this.myOS == HostComputer.OS.WINDOWS_VISTA
         | this.myOS == HostComputer.OS.WINDOWS_XP
         | this.myOS == HostComputer.OS.WINDOWS_SERVER
         | this.myOS == HostComputer.OS.WINDOWS_8
         | this.myOS == HostComputer.OS.WINDOWS_10) {
         String ipConfigAll = this.windowsIpConfigAll();
         String[] adapters = ipConfigAll.split("adapter");

         for (NetworkInterface temp : nicList) {
            logger.debug("Currently processing network interface {}", temp.getName());
            if (!nicMap.containsValue(temp.getName())) {
               String mac = this.getNicMacAddress(temp);
               if (mac != null) {
                  String match = this.findAdapter(mac, adapters);
                  if (match != null & nicMap.containsKey(match)) {
                     logger.error("{} adapter found but already mapped Old: {}  New: {}", new Object[]{match, nicMap.get(match), temp.getName()});
                  } else if (match != null) {
                     nicMap.put(match, temp.getName());
                     Object[] logData = new Object[]{match, temp.getName(), mac, temp.getDisplayName()};
                     logger.debug("NIC added, windows name: {}, internal name: {}, MAC address {}, display name: {}", logData);
                  }
               } else {
                  logger.debug("Null MAC returned for network interface {}", temp.getName());
               }
            } else {
               logger.debug("Network interface {} already in NIC map", temp.getName());
            }
         }
      } else {
         for (NetworkInterface tempx : nicList) {
            nicMap.put(tempx.getName(), tempx.getName());
         }
      }

      return nicMap;
   }

   private String getNicMacAddress(NetworkInterface nic) throws HostException {
      byte[] macByte = new byte[6];

      try {
         macByte = nic.getHardwareAddress();
         if (macByte != null) {
            String mac = "";

            for (int i = 0; i < macByte.length; i++) {
               mac = mac + String.format("%02x%s", macByte[i], i < macByte.length - 1 ? "-" : "");
            }

            return mac.trim();
         } else {
            return null;
         }
      } catch (SocketException var51) {
         throw new HostException("Failed to get MAC address for" + nic.getName(), var51);
      }
   }

   private String findAdapter(String mac, String[] adapters) throws HostException {
      logger.debug("finding adapter with mac {}", mac);
      ArrayList<String> matches = new ArrayList<>();

      for (int i = 1; i < adapters.length; i++) {
         String regex = "(?im)\\A(.*?):.*?Physical Address.*?:.*?(([0-9a-fA-F][0-9a-fA-F]-){5}([0-9a-fA-F][0-9a-fA-F])).*?";
         Pattern pattern = Pattern.compile(regex);
         Matcher m = pattern.matcher(adapters[i]);
         if (m.find()) {
            String macRx = m.group(2).trim();
            String infName = m.group(1).trim();
            if (mac.equalsIgnoreCase(macRx)) {
               matches.add(infName);
               logger.debug("adapter found, name = {}, mac = {}", infName, macRx);
            }
         }
      }

      return matches.size() == 1 ? matches.get(0) : null;
   }

   private String windowsIpConfigAll() throws HostException {
      logger.debug("Getting ipconfig/all response");
      String ipConfigAll = "";
      Process ipConfig = null;

      String var8;
      try {
         ipConfig = Runtime.getRuntime().exec("ipconfig /all");
         InputStream in = ipConfig.getInputStream();
         InputStreamReader isr = new InputStreamReader(in);
         BufferedReader br = new BufferedReader(isr);
         String line = null;

         while ((line = br.readLine()) != null) {
            ipConfigAll = ipConfigAll + line;
            logger.trace("ipconfig/all response: {}", line);
         }

         int exitVal = ipConfig.waitFor();
         if (exitVal != 0) {
            logger.error("ipconfig/all exit value = {}", exitVal);
         }

         var8 = ipConfigAll;
      } catch (IOException var13) {
         throw new HostException("Failed to read ipconfig/all command output", var13);
      } catch (InterruptedException var141) {
         throw new HostException("Interrupted while waiting for ipconfig/all command to finish", var141);
      } finally {
         this.handler.closeProcess(ipConfig);
      }

      return var8;
   }

   private HashMap<String, String> getNICmap() {
      return this.myNicMap;
   }

   public String[] getInterfaceNames(boolean ethernetOnly) throws HostException {
      ArrayList<String> namesOut = new ArrayList<>();

      try {
         this.refreshNicMap();
         List<String> nicNames = new ArrayList<>(this.myNicMap.keySet());
         Collections.sort(nicNames);

         for (String nicName : nicNames) {
            if (!nicName.toLowerCase().contains("bluetooth")
               && (!ethernetOnly || ethernetOnly & !nicName.toLowerCase().contains("wireless") & !nicName.toLowerCase().contains("wi-fi"))) {
               namesOut.add(nicName);
            }
         }

         return namesOut.toArray(new String[namesOut.size()]);
      } catch (HostException var61) {
         throw new HostException("Failed to get network interface names", var61);
      }
   }

   public void setSelectedInterface(String inf) {
      this.mySelectedInterface = inf;
      logger.info("Selected NIC: {}", inf);
   }

   public String getSelectedInterface() {
      return this.mySelectedInterface;
   }

   public void changeIpAddress(String address, String subnet, boolean add) throws HostException {
      logger.info((add ? "Adding " : "Changing ") + "IP address {} with subnet {}", address, subnet);
      String[] command = this.buildIpChangeCommand(address, subnet, add);
      Process ipChange = null;

      try {
         if (!this.checkIpAddress(address)) {
            ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
            ipChange = pb.start();
            InputStream stdin = ipChange.getInputStream();
            InputStreamReader isr = new InputStreamReader(stdin);
            BufferedReader br = new BufferedReader(isr);
            String output = "";
            int exitValue = ipChange.waitFor();

            while (br.ready()) {
               output = output + br.readLine();
            }

            if (exitValue != 0) {
               if (output.contains("administrator")) {
                  logger.error("IP change exit status: {}. Non-administrator", exitValue);
                  throw new HostException("Failed to change IP Address, make sure to run the .exe file not the .jar");
               }

               if (output.contains("object already exists")) {
                  logger.error("IP change exit status: {}. IP conflict", exitValue);
                  throw new HostException("Failed to change IP Address, please check for an adapter with the address: " + address);
               }

               logger.error("IP change exit status: {}", exitValue);
               throw new HostException("Failed to change IP Address, please ensure the bridge is connected to this computer and try again");
            }

            for (int count = 1; !this.checkIpAddress(address) & count <= 10; count++) {
               logger.debug("Check IP attempt: {}", count);
               Thread.sleep(1000L);
            }

            if (!this.checkIpAddress(address)) {
               throw new HostException("IP address not " + (add ? "added" : "changed") + "");
            }
         }
      } catch (InterruptedException var18) {
         throw new HostException("Interrupted while waiting for ip " + (add ? "add" : "change") + " command to finish", var18);
      } catch (IOException var19) {
         throw new HostException("Failed to execute command to " + (add ? "add" : "change") + " ip address", var19);
      } catch (HostException var201) {
         throw new HostException(var201.getMessage(), var201);
      } finally {
         this.handler.closeProcess(ipChange);
      }
   }

   private String[] buildIpChangeCommand(String address, String subnet, boolean add) {
      String[] command = null;
      switch (this.myOS) {
         case WINDOWS_XP:
         case WINDOWS_SERVER:
         case WINDOWS_VISTA:
            command = new String[]{"netsh", "interface", "ip", add ? "add" : "set", "address", this.mySelectedInterface, add ? "" : "static", address, subnet};
            break;
         case WINDOWS_7:
         case WINDOWS_8:
         case WINDOWS_10:
            if (add) {
               command = new String[]{"netsh", "interface", "ipv4", "add", "address", this.mySelectedInterface, address, subnet};
            } else {
               command = new String[]{"netsh", "interface", "ipv4", "set", "address", this.mySelectedInterface, "static", address, subnet};
            }
            break;
         case MAC:
         case LINUX:
            command = new String[]{
               this.userName.equals("root") ? "" : "sudo", "ifconfig", this.mySelectedInterface, add ? " add " : " ", address, "netmask", subnet, "up"
            };
      }

      logger.info("IP command: {}", String.join(" ", command));
      return command;
   }

   public void restoreDHCP() throws HostException {
      logger.info("Restoring adapter to DHCP");
      String command = this.buildDHCPCommand();
      Process ipChange = null;

      try {
         ipChange = Runtime.getRuntime().exec(command);
         int exitValue = ipChange.waitFor();
         if (exitValue != 0) {
            logger.error("IP change exit status: {}", exitValue);
         }

         Thread.sleep(5000L);
      } catch (InterruptedException var8) {
         throw new HostException("Interrupted while waiting for dhcp restore command to finish", var8);
      } catch (IOException var91) {
         throw new HostException("Failed to execute command to restore dhcp address", var91);
      } finally {
         this.handler.closeProcess(ipChange);
      }
   }

   private String buildDHCPCommand() {
      String command = null;
      switch (this.myOS) {
         case WINDOWS_XP:
         case WINDOWS_SERVER:
         case WINDOWS_VISTA:
            command = "netsh interface ip set address \"" + this.mySelectedInterface + "\" dhcp";
            break;
         case WINDOWS_7:
         case WINDOWS_8:
         case WINDOWS_10:
            command = "netsh interface ipv4 set address \"" + this.mySelectedInterface + "\" dhcp";
      }

      logger.info("DHCP command: {}", command);
      return command;
   }

   public boolean checkIpAddress(String address) throws HostException {
      boolean match = false;

      try {
         String[] ips = this.getLocalhostIp();

         for (int i = 0; i < ips.length; i++) {
            match |= address.equals(ips[i]);
         }

         return match;
      } catch (HostException var51) {
         throw new HostException("Failed to check ip address", var51);
      }
   }

   private NetworkInterface getSelectedNetworkInf() throws HostException {
      try {
         if (!this.myNicMap.containsKey(this.mySelectedInterface)) {
            throw new HostException("Network interface name " + this.mySelectedInterface + " not found in map of available interfaces");
         } else {
            return NetworkInterface.getByName(this.myNicMap.get(this.mySelectedInterface));
         }
      } catch (SocketException var2) {
         throw new HostException("Failed to get reference to selected network interface (" + this.mySelectedInterface + ")");
      }
   }

   public String getSelectedNetworkInfDisplayName() throws HostException {
      return this.getSelectedNetworkInf().getDisplayName();
   }

   public byte[] getMacAddress() throws HostException {
      byte[] mac = new byte[6];

      try {
         NetworkInterface network = this.getSelectedNetworkInf();
         return network.getHardwareAddress();
      } catch (SocketException var3) {
         throw new HostException("Failed to get MAC address", var3);
      } catch (HostException var41) {
         throw new HostException("Failed to get MAC address", var41);
      }
   }

   public String[] getLocalhostIp() throws HostException {
      try {
         List<String> addressStrings = new LinkedList<>();
         String ipRegex = "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$";
         List<InetAddress> addresses = this.getLocalhostAddresses();

         for (int count = 0; addresses.isEmpty() && count < 20; count++) {
            addresses = this.getLocalhostAddresses();
            logger.debug("Selected interface currently contains " + addresses.size() + " addresses");
            Thread.sleep(1000L);
         }

         if (addresses.isEmpty()) {
            throw new HostException("No IPv4 addresses found");
         } else {
            for (InetAddress address : addresses) {
               String temp = address.toString().split("/")[1];
               if (temp.matches(ipRegex)) {
                  addressStrings.add(temp);
               }
            }

            return addressStrings.toArray(new String[addressStrings.size()]);
         }
      } catch (InterruptedException var7) {
         throw new HostException("Failed to get localhost IP address", var7);
      } catch (HostException var8) {
         throw new HostException("Failed to get localhost IP address", var8);
      }
   }

   private List<InetAddress> getLocalhostAddresses() throws HostException {
      try {
         List<InetAddress> ips = new ArrayList<>();

         for (NetworkInterface nic : this.buildNicList()) {
            List<InterfaceAddress> addrs = nic.getInterfaceAddresses();
            if (addrs.size() > 0) {
               logger.debug("Getting addresses for nic: " + nic.getDisplayName() + " index " + nic.getIndex() + " : " + addrs.size());
               ips.addAll(Collections.list(nic.getInetAddresses()));
            }
         }

         return ips;
      } catch (HostException var5) {
         throw new HostException("Failed to get network interface or its inet addresses", var5);
      }
   }

   public void flushArp() throws HostException {
      logger.info("flushing ARP cache");
      String command = null;
      switch (this.myOS) {
         case WINDOWS_XP:
         case WINDOWS_SERVER:
         case WINDOWS_VISTA:
         case WINDOWS_7:
         case WINDOWS_8:
         case WINDOWS_10:
            command = "netsh interface ip delete arpcache";
            break;
         case MAC:
            command = "sudo arp -a -d";
            break;
         case LINUX:
            command = "ip neigh flush all";
      }

      logger.debug("flush command: {}", command);
      Process flush = null;

      try {
         flush = Runtime.getRuntime().exec(command);
         int exitValue = flush.waitFor();
         if (exitValue != 0) {
            logger.error("ARP cache flush command exit status: {}", exitValue);
         }
      } catch (InterruptedException var8) {
         throw new HostException("Interrupted while flushing arp cache", var8);
      } catch (IOException var91) {
         throw new HostException("Failed to execute command to arp cache flush", var91);
      } finally {
         this.handler.closeProcess(flush);
      }
   }

   public String[] pingBroadcast(String target) throws HostException {
      logger.info("Ping broadcast to {}", target);
      String command = "";
      BufferedReader br = null;
      Process ping = null;
      switch (this.myOS) {
         case WINDOWS_XP:
         case WINDOWS_SERVER:
         case WINDOWS_VISTA:
         case WINDOWS_7:
         case WINDOWS_8:
         case WINDOWS_10:
            command = "ping -n 10 " + target;
            break;
         case MAC:
            command = "ping -c 10 " + target;
            break;
         case LINUX:
            command = "ping -c 10 -b " + target;
      }

      String[] var16;
      try {
         logger.debug("Ping broadcast command: {}", command);
         ping = Runtime.getRuntime().exec(command);
         br = new BufferedReader(new InputStreamReader(ping.getInputStream()));
         ArrayList<String> retVal = new ArrayList<>();

         String line;
         while ((line = br.readLine()) != null) {
            if (!line.equals("")) {
               logger.trace("Broadcast response: {}", line);
               String regex = "from.*?\\b((?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))\\b:";
               Pattern p = Pattern.compile(regex);
               Matcher m = p.matcher(line);
               if (m.find()) {
                  String current = m.group(1);
                  if (!retVal.contains(current)) {
                     retVal.add(current);
                  }
               }
            }
         }

         var16 = retVal.toArray(new String[retVal.size()]);
      } catch (IOException var15) {
         throw new HostException("Failed to send ping broadcast", var15);
      } finally {
         this.handler.closeReader(br);
         logger.debug("Ping input stream closed");
         this.handler.closeProcess(ping);
         logger.debug("Ping broadcast process destroyed");
      }

      return var16;
   }

   public boolean pingUntil(String target, int replies, int timeout) throws HostException {
      logger.info("Pinging {} until {} replies or {} attempts", new Object[]{target, replies, timeout});
      String command = "";
      int successfulReplies = 0;
      BufferedReader br = null;
      Process ping = null;
      switch (this.myOS) {
         case WINDOWS_XP:
         case WINDOWS_SERVER:
         case WINDOWS_VISTA:
         case WINDOWS_7:
         case WINDOWS_8:
         case WINDOWS_10:
            command = "ping -n " + timeout + " -w 1000 " + target;
            break;
         case MAC:
            command = "ping -c " + timeout + " " + target;
            break;
         case LINUX:
            command = "ping -c " + timeout + " " + target;
      }

      boolean var18;
      try {
         logger.debug("Ping command: {}", command);
         ping = Runtime.getRuntime().exec(command);
         br = new BufferedReader(new InputStreamReader(ping.getInputStream()));

         String line;
         while ((line = br.readLine()) != null & successfulReplies < replies) {
            if (!line.equals("")) {
               logger.trace("Ping response: {}", line);
               String regex = "from.*?\\b((?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))\\b:";
               Pattern p = Pattern.compile(regex);
               Matcher m = p.matcher(line);
               if (m.find()) {
                  String source = m.group(1).trim();
                  logger.trace("Source ip = {}", source);
                  if (source.equals(target)) {
                     logger.trace("Successful replies: {}", ++successfulReplies);
                  }
               }
            }
         }

         logger.debug("Ping completed, successful replies: {}", successfulReplies);
         var18 = successfulReplies >= replies;
      } catch (IOException var17) {
         throw new HostException("Failed to ping " + target, var17);
      } finally {
         this.handler.closeReader(br);
         logger.debug("Ping input stream closed");
         this.handler.closeProcess(ping);
         logger.debug("Ping broadcast process destroyed");
      }

      return var18;
   }

   private static enum OS {
      WINDOWS_XP,
      WINDOWS_VISTA,
      WINDOWS_7,
      WINDOWS_SERVER,
      WINDOWS_8,
      WINDOWS_10,
      MAC,
      LINUX;
   }
}
