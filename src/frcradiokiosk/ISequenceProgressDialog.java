package frcradiokiosk;

public interface ISequenceProgressDialog {
   void setProgress(int var1);

   void setCurrentStep(String var1, String var2);

   void clearMessages();

   void sequenceFinished();

   void showDialog();
}
