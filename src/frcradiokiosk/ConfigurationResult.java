package frcradiokiosk;

public class ConfigurationResult {
   private String mode;
   private String internalMessage;
   private String userMessage;
   private boolean pass;

   public ConfigurationResult(String mode, boolean pass, String internalMessage, String userMessage) {
      this.mode = mode;
      this.pass = pass;
      this.internalMessage = internalMessage;
      this.userMessage = userMessage;
   }

   public String getMode() {
      return this.mode;
   }

   public String getInternalMessage() {
      return this.internalMessage;
   }

   public String getUserMessage() {
      return this.userMessage;
   }

   public boolean passed() {
      return this.pass;
   }
}
