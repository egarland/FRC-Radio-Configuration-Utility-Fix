package frcradiokiosk.DAP1522RevB;

import org.jdom2.Document;
import org.jdom2.Element;

public class AdminData {
   private Element deviceAccount;
   private Element entry;
   private Document document;
   private static final String defaultUsername = "Admin";
   private static final String defaultPassword = "";
   private static String newPassword = "";

   public AdminData(Document document) {
      this.document = document;
      Element root = document.getRootElement();
      this.deviceAccount = RevBUtil.getChildBySubChild(root, "module", "service", "DEVICE.ACCOUNT");
      this.entry = this.deviceAccount.getChild("device").getChild("account").getChild("entry");
   }

   public static void setStaticPassword(String password) {
      newPassword = password;
   }

   public static String getDefaultUsername() {
      return "Admin";
   }

   public static String getDefaultPassword() {
      return "";
   }

   public static String getStaticPassword() {
      return newPassword;
   }

   public void setStaticData() {
      this.setUsernameValue("Admin");
      this.setPasswordValue(newPassword);
   }

   private Element getUsername() {
      return this.entry.getChild("name");
   }

   private Element getPassword() {
      return this.entry.getChild("password");
   }

   public String getUsernameValue() {
      return this.getUsername().getText();
   }

   private void setUsernameValue(String value) {
      this.getUsername().setText(value);
   }

   public String getPasswordValue() {
      return this.getPassword().getText();
   }

   private void setPasswordValue(String value) {
      this.getPassword().setText(value);
   }

   public Document getDocument() {
      return this.document;
   }
}
