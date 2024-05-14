package frcradiokiosk;

public class IllegalRadioException extends RadioException {
   public IllegalRadioException(String message) {
      super(message);
   }

   public IllegalRadioException(String message, String userMessage) {
      super(message, userMessage);
   }

   public IllegalRadioException(String message, Throwable cause) {
      super(message, cause);
   }

   public IllegalRadioException(String message, String userMessage, Throwable cause) {
      super(message, userMessage, cause);
   }
}
