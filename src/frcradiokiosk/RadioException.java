package frcradiokiosk;

public class RadioException extends Exception {
   private String userMessage = "Please reset the bridge and try again";

   public RadioException() {
   }

   public RadioException(String message) {
      super(message);
   }

   public RadioException(String message, String userMessage) {
      super(message);
      this.userMessage = userMessage;
   }

   public RadioException(String message, Throwable cause) {
      super(message, cause);
   }

   public RadioException(String message, String userMessage, Throwable cause) {
      super(message, cause);
      this.userMessage = userMessage;
   }

   public String getUserMessage() {
      return this.userMessage;
   }
}
