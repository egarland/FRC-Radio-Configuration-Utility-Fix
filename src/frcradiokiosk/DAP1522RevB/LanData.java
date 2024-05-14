package frcradiokiosk.DAP1522RevB;

import org.jdom2.Document;
import org.jdom2.Element;

public class LanData {
   private Element deviceHostname;
   private Element inetBridge1;
   private Element inet1;
   private Element ipv4;
   private Document document;
   public final String staticAddress = "1";
   public final String mask = "8";

   public LanData(Document document) {
      this.document = document;
      this.deviceHostname = RevBUtil.getModule(document, "DEVICE.HOSTNAME");
      this.inetBridge1 = RevBUtil.getModule(document, "INET.BRIDGE-1");
      Element inet = this.inetBridge1.getChild("inet");
      this.inet1 = RevBUtil.getChildBySubChild(inet, "entry", "uid", "INET-1");
      this.ipv4 = this.inet1.getChild("ipv4");
   }

   public void setStaticData() {
      this.getStatic().setText("1");
      this.getMask().setText("8");
      this.getActivate(this.deviceHostname, true).setText("ignore");
      this.getActivate(this.inetBridge1, true).setText("delay");
      this.getActivateDelay(true).setText("1");
   }

   private Element getStatic() {
      return this.ipv4.getChild("static");
   }

   private Element getIpAddr() {
      return this.ipv4.getChild("ipaddr");
   }

   private Element getMask() {
      return this.ipv4.getChild("mask");
   }

   private Element getGateway() {
      return this.ipv4.getChild("gateway");
   }

   private Element getActivate(Element module, boolean add) {
      Element activate = module.getChild("ACTIVATE");
      if (activate == null & add) {
         activate = new Element("ACTIVATE");
         module.addContent(activate);
      }

      return activate;
   }

   private Element getActivateDelay(boolean add) {
      Element activateDelay = this.inetBridge1.getChild("ACTIVATE_DELAY");
      if (activateDelay == null & add) {
         activateDelay = new Element("ACTIVATE_DELAY");
         this.inetBridge1.addContent(activateDelay);
      }

      return activateDelay;
   }

   public String getIpAddrValue() {
      return this.getIpAddr().getText();
   }

   public void setIpAddrValue(String value) {
      this.getIpAddr().setText(value);
   }

   public String getGatewayValue() {
      return this.getGateway().getText();
   }

   public void setGatewayValue(String value) {
      this.getGateway().setText(value);
   }

   public boolean isUsingStaticAddress() {
      return this.getStatic().getText().equals("1");
   }

   public String getMaskValue() {
      return this.getMask().getText();
   }

   public Document getDocument() {
      return this.document;
   }
}
