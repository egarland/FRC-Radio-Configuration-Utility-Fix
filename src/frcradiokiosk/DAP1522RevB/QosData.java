package frcradiokiosk.DAP1522RevB;

import org.jdom2.Document;
import org.jdom2.Element;

public class QosData {
   private Element trafficModule;
   private Element trafficEntry;
   private Element qos;
   private Element qosProtocol;
   private Document document;
   private final String bandwidth = "7";
   private final String qosEnabled = "1";
   private final String qosType = "1";
   private final String lowPriority = "3";
   private final String highPriority = "0";
   private final String controlStart = "1100";
   private final String controlEnd = "1200";
   private final String otherData = "1735";

   public QosData(Document document) {
      this.document = document;
      this.trafficModule = RevBUtil.getModule(document, "TRAFFICCTRL.BRIDGE-1");
      Element trafficCtrl = this.trafficModule.getChild("trafficctrl");
      this.trafficEntry = RevBUtil.getChildBySubChild(trafficCtrl, "entry", "uid", "TRAFFICCTRL-1");
      this.qos = this.trafficEntry.getChild("qos");
      this.qosProtocol = this.qos.getChild("protocol");
   }

   public void setStaticData() {
      this.getDownlinkElement().setText("7");
      this.getUplinkElement().setText("7");
      this.getQosEnableElement().setText("1");
      this.getQosTypeElement().setText("1");
      this.getWebPriorityElement().setText("3");
      this.getMailPriorityElement().setText("3");
      this.setUserQosData(this.getUser1Element(), "0", "1100", "1200");
      this.setUserQosData(this.getUser2Element(), "3", "1735", "1735");
      Element whichSubmit = new Element("whichsubmit").setText("qos");
      this.trafficEntry.getChild("trafficmgr").addContent(whichSubmit);
   }

   private void setUserQosData(Element user, String priority, String start, String end) {
      user.getChild("priority").setText(priority);
      user.getChild("startport").setText(start);
      user.getChild("endport").setText(end);
   }

   public boolean validateStaticData() {
      boolean valid = true;
      valid &= this.getDownlinkElement().getText().equals("7");
      valid &= this.getUplinkElement().getText().equals("7");
      valid &= this.getQosEnableElement().getText().equals("1");
      valid &= this.getQosTypeElement().getText().equals("1");
      valid &= this.getWebPriorityElement().getText().equals("3");
      valid &= this.getMailPriorityElement().getText().equals("3");
      valid &= this.validateUserQosData(this.getUser1Element(), "0", "1100", "1200");
      return valid & this.validateUserQosData(this.getUser2Element(), "3", "1735", "1735");
   }

   private boolean validateUserQosData(Element user, String priority, String start, String end) {
      boolean valid = true;
      valid &= user.getChildText("priority").equals(priority);
      valid &= user.getChildText("startport").equals(start);
      return valid & user.getChildText("endport").equals(end);
   }

   private Element getBandwitdthElement() {
      return this.trafficEntry.getChild("updownlinkset").getChild("bandwidth");
   }

   private Element getDownlinkElement() {
      return this.getBandwitdthElement().getChild("downlink");
   }

   private Element getUplinkElement() {
      return this.getBandwitdthElement().getChild("uplink");
   }

   private Element getQosEnableElement() {
      return this.qos.getChild("enable");
   }

   private Element getQosTypeElement() {
      return this.qos.getChild("qostype");
   }

   private Element getWebPriorityElement() {
      return this.qosProtocol.getChild("web").getChild("priority");
   }

   private Element getMailPriorityElement() {
      return this.qosProtocol.getChild("mail").getChild("priority");
   }

   private Element getUser1Element() {
      return this.qosProtocol.getChild("user1");
   }

   private Element getUser2Element() {
      return this.qosProtocol.getChild("user2");
   }

   public Document getDocument() {
      return this.document;
   }
}
