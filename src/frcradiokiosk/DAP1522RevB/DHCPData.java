package frcradiokiosk.DAP1522RevB;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class DHCPData {
   private Element dhcpBridge1Module;
   private Element dhcp1;
   private Element inf;
   private Element runtimeModule;
   private Document document;

   public DHCPData(Document document) {
      this.document = document;
      XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

      try {
         xmlOutputter.output(document, new FileOutputStream("original.xml"));
      } catch (IOException var4) {
         Logger.getLogger(DHCPData.class.getName()).log(Level.SEVERE, null, var4);
      }

      this.dhcpBridge1Module = RevBUtil.getModule(document, "DHCPS4.BRIDGE-1");
      Element dhcps4 = this.dhcpBridge1Module.getChild("dhcps4");
      this.dhcp1 = RevBUtil.getChildBySubChild(dhcps4, "entry", "uid", "DHCPS4-1");
      this.inf = this.dhcpBridge1Module.getChild("inf");
      this.runtimeModule = RevBUtil.getModule(document, "RUNTIME.INF.BRIDGE-1");
   }

   private Element getIPStart() {
      return this.dhcp1.getChild("start");
   }

   private Element getIPEnd() {
      return this.dhcp1.getChild("end");
   }

   private Element getNetwork() {
      return this.dhcp1.getChild("network");
   }

   private Element getSubnetMask() {
      return this.dhcp1.getChild("mask");
   }

   private Element getDHCPs4() {
      return this.inf.getChild("dhcps4");
   }

   public Element getRuntimeBridgeFatLadyElement(boolean add) {
      Element fatLady = this.runtimeModule.getChild("FATLADY");
      if (fatLady == null & add) {
         fatLady = new Element("FATLADY");
         this.runtimeModule.addContent(fatLady);
      }

      return fatLady;
   }

   public void setStaticData() {
      this.getIPEnd().setText("199");
      this.getSubnetMask().setText("24");
      this.getDHCPs4().setText("DHCPS4-1");
      this.getRuntimeBridgeFatLadyElement(true).setText("ignore");
   }

   public String getIPStartValue() {
      return this.getIPStart().getText();
   }

   public String getIPEndValue() {
      return this.getIPEnd().getText();
   }

   public String getNetworkValue() {
      return this.getNetwork().getText();
   }

   public String getSubnetMaskValue() {
      return this.getSubnetMask().getText();
   }

   public String getDHCPs4Value() {
      return this.getDHCPs4().getText();
   }

   public void setNetwork(String value) {
      this.getNetwork().setText(value);
   }

   public void setIPStart(String value) {
      this.getIPStart().setText(value);
   }

   public Document getDocument() {
      XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());

      try {
         xmlOutputter.output(this.document, new FileOutputStream("modified.xml"));
      } catch (IOException var3) {
         Logger.getLogger(DHCPData.class.getName()).log(Level.SEVERE, null, var3);
      }

      return this.document;
   }
}
