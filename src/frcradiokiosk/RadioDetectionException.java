package frcradiokiosk;

public class RadioDetectionException extends Exception {
   private String userMessage = "Please ensure that: \n   -WiFi connections are disabled on this computer\n   -the wireless bridge is the only device connected via ethernet\n\nIf this error still occurs after the above conditions are met,\ntry power cycling or manually resetting the bridge";

   public RadioDetectionException() {
   }

   public RadioDetectionException(String message) {
      super(message);
   }

   public RadioDetectionException(String message, String userMessage) {
      super(message);
      this.userMessage = userMessage;
   }

   public RadioDetectionException(String message, Throwable cause) {
      super(message, cause);
   }

   public RadioDetectionException(String message, String userMessage, Throwable cause) {
      super(message, cause);
      this.userMessage = userMessage;
   }

   public String getUserMessage() {
      return this.userMessage;
   }
}
