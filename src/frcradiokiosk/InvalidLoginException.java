package frcradiokiosk;

public class InvalidLoginException extends RadioException {
   public InvalidLoginException() {
      super("Bridge login failed; invalid login credentials", "Please reset the bridge and try again");
   }

   public InvalidLoginException(Throwable cause) {
      super("Bridge login failed; invalid login credentials", "Please reset the bridge and try again", cause);
   }
}
