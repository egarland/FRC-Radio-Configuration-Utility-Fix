package frcradiokiosk;

public class HostException extends Exception {
   private String userMessage = "";

   public HostException() {
   }

   public HostException(String message) {
      super(message);
   }

   public HostException(String message, String userMessage) {
      super(message);
      this.userMessage = userMessage;
   }

   public HostException(String message, Throwable cause) {
      super(message, cause);
   }

   public HostException(String message, String userMessage, Throwable cause) {
      super(message, cause);
      this.userMessage = userMessage;
   }

   public String getUserMessage() {
      return this.userMessage;
   }
}
