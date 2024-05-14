package frcradiokiosk;

public interface INicDialogShell {
   String[] getNicNames() throws HostException;

   void setSelectedNic(String var1);
}
