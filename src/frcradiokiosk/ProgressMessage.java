package frcradiokiosk;

public class ProgressMessage {
   private String title;
   private String message;

   public ProgressMessage(String message) {
      this(message, message);
   }

   public ProgressMessage(String title, String message) {
      this.title = title;
      this.message = message;
   }

   public String getTitle() {
      return this.title;
   }

   public String getMessage() {
      return this.message;
   }
}
