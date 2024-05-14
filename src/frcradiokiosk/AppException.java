package frcradiokiosk;

public class AppException extends Exception {
   private String userMessage = "";

   public AppException() {
   }

   public AppException(String message) {
      super(message);
   }

   public AppException(String message, String userMessage) {
      super(message);
      this.userMessage = userMessage;
   }

   public AppException(String message, Throwable cause) {
      super(message, cause);
   }

   public AppException(String message, String userMessage, Throwable cause) {
      super(message, cause);
      this.userMessage = userMessage;
   }

   public String getUserMessage() {
      return this.userMessage;
   }
}
