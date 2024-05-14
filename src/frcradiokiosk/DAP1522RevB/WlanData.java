package frcradiokiosk.DAP1522RevB;

import frcradiokiosk.FRCRadio;
import org.jdom2.Document;
import org.jdom2.Element;

public class WlanData {
   private Element wifiModule;
   private Element wifiEntry;
   private Element wifiPhyInf;
   private Element apWifiPhyInf;
   private Element runtimeDFSModule;
   private Element runtimePhyInfModule;
   private Element macCloneModule;
   private Document document;
   public final String authType = "WPA2PSK";
   public final String encrType = "AES";
   public final String noAuth = "OPEN";
   public final String noEncr = "NONE";
   public final String wpsDisabled = "0";
   public final String wpsConfigured = "1";
   public final String wlMode = "gn";
   public final String bandwidth = "20";
   private FRCRadio.Mode mode;

   public WlanData(Document document, FRCRadio.Mode mode) {
      this.document = document;
      this.mode = mode;
      this.wifiModule = RevBUtil.getModule(document, "WIFI.PHYINF");
      Element wifi = this.wifiModule.getChild("wifi");
      this.wifiEntry = RevBUtil.getChildBySubChild(wifi, "entry", "uid", mode.isAP() ? "WIFI-1" : "WIFI-3");
      this.wifiPhyInf = RevBUtil.getChildBySubChild(this.wifiModule, "phyinf", "wifi", mode.isAP() ? "WIFI-1" : "WIFI-3");
      this.runtimeDFSModule = RevBUtil.getModule(document, "RUNTIME.DFS");
      this.runtimePhyInfModule = RevBUtil.getModule(document, "RUNTIME.PHYINF");
      this.macCloneModule = RevBUtil.getModule(document, "MACCLONE.WLAN-2");
   }

   public void setStaticData() {
      this.getWpsEnabledElement().setText("0");
      this.getWpsConfiguredElement().setText("1");
      this.getWifiActivateElement(true).setText("ignore");
      this.getRuntimeDfsFatLadyElement(true).setText("ignore");
      if (this.mode.isAP()) {
         if (this.mode == FRCRadio.Mode.AP24) {
            this.getWlModeElement().setText("gn");
         }

         this.getBandwidthElement().setText("20");
         this.getMacCloneFatLadyElement(true).setText("ignore");
         this.getMacCloneSetCfg(true).setText("ignore");
      }

      if (this.mode == FRCRadio.Mode.BRIDGE) {
         RevBUtil.getChildBySubChild(this.wifiModule, "phyinf", "wifi", "WIFI-1").getChild("active").setText("0");
      }
   }

   private Element getSsidElemnt() {
      return this.wifiEntry.getChild("ssid");
   }

   private Element getAuthTypeElement() {
      return this.wifiEntry.getChild("authtype");
   }

   private Element getEncrTypeElement() {
      return this.wifiEntry.getChild("encrtype");
   }

   private Element getNetworkKeyElement(boolean add) {
      Element nwKey = this.wifiEntry.getChild("nwkey");
      if (nwKey == null & add) {
         nwKey = new Element("nwkey");
         this.wifiEntry.addContent(nwKey);
      }

      return nwKey;
   }

   private Element getPasskeyElement(boolean add) {
      Element passkey = this.getNetworkKeyElement(add).getChild("psk");
      if (passkey == null & add) {
         passkey = new Element("psk");
         passkey.addContent(new Element("passphrase"));
         passkey.addContent(new Element("key"));
         this.getNetworkKeyElement(add).addContent(passkey);
      }

      return passkey;
   }

   private Element getWpaKeyElement(boolean add) {
      Element passkey = this.getPasskeyElement(add);
      return passkey == null ? null : passkey.getChild("key");
   }

   private Element getWlModeElement() {
      return this.wifiPhyInf.getChild("media").getChild("wlmode");
   }

   private Element getWpsElement() {
      return this.wifiEntry.getChild("wps");
   }

   private Element getWpsEnabledElement() {
      return this.getWpsElement().getChild("enable");
   }

   private Element getWpsConfiguredElement() {
      return this.getWpsElement().getChild("configured");
   }

   private Element getMacCloneElement() {
      return this.wifiPhyInf.getChild("macclone");
   }

   private Element getMacCloneEnabledElement() {
      return this.getMacCloneElement().getChild("type");
   }

   private Element getWifiActivateElement(boolean add) {
      Element activate = this.wifiModule.getChild("ACTIVATE");
      if (activate == null & add) {
         activate = new Element("ACTIVATE");
         this.wifiModule.addContent(activate);
      }

      return activate;
   }

   private Element getRuntimeDfsFatLadyElement(boolean add) {
      Element fatLady = this.runtimeDFSModule.getChild("FATLADY");
      if (fatLady == null & add) {
         fatLady = new Element("FATLADY");
         this.runtimeDFSModule.addContent(fatLady);
      }

      return fatLady;
   }

   private Element getMacCloneFatLadyElement(boolean add) {
      Element fatLady = this.macCloneModule.getChild("FATLADY");
      if (fatLady == null & add) {
         fatLady = new Element("FATLADY");
         this.macCloneModule.addContent(fatLady);
      }

      return fatLady;
   }

   private Element getMacCloneSetCfg(boolean add) {
      Element setCfg = this.macCloneModule.getChild("SETCFG");
      if (setCfg == null & add) {
         setCfg = new Element("SETCFG");
         this.macCloneModule.addContent(setCfg);
      }

      return setCfg;
   }

   private Element getBandwidthElement() {
      return this.wifiPhyInf.getChild("media").getChild("dot11n").getChild("bandwidth" + (this.mode == FRCRadio.Mode.AP5 ? "_Aband" : ""));
   }

   public String getSsidValue() {
      return this.getSsidElemnt().getText();
   }

   public void setSsidValue(String value) {
      this.getSsidElemnt().setText(value);
   }

   public String getWpaKeyValue() {
      return this.getWpaKeyElement(false) == null ? null : this.getWpaKeyElement(false).getText();
   }

   public void setSecurity(String wpaKey) {
      this.getWpaKeyElement(true).setText(wpaKey);
      this.getAuthTypeElement().setText("WPA2PSK");
      this.getEncrTypeElement().setText("AES");
   }

   public String getAuthTypeValue() {
      return this.getAuthTypeElement().getText();
   }

   public String getEncrTypeValue() {
      return this.getEncrTypeElement().getText();
   }

   public boolean isWifiEnabled() {
      return this.wifiPhyInf.getChild("active").getText().equals("1");
   }

   public boolean isWpsDisabled() {
      return this.getWpsEnabledElement().getText().equals("0");
   }

   public boolean isWpsConfigured() {
      return this.getWpsConfiguredElement().getText().equals("1");
   }

   public String getMacCloneValue() {
      return this.getMacCloneEnabledElement().getText();
   }

   public String getBandwidth() {
      return this.getBandwidthElement().getText();
   }

   public String getWlMode() {
      return this.getWlModeElement().getText();
   }

   public Document getDocument() {
      return this.document;
   }
}
