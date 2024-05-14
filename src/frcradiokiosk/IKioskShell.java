package frcradiokiosk;

public interface IKioskShell extends INicDialogShell {
   void showErrorMessage(String var1);

   void showSuccessMessage(String var1);

   void logAttempt(int var1, boolean var2, String var3);
}
