package frcradiokiosk.DAP1522RevB;

import frcradiokiosk.AppException;
import frcradiokiosk.FRCRadio;
import frcradiokiosk.InvalidLoginException;
import frcradiokiosk.RadioException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;

public class RevBUtil {
   private RevBUtil(Document document) {
   }

   public static void checkLoginSuccessful(Document document) throws InvalidLoginException {
      Element report = document.getRootElement();
      if (!report.getChildText("RESULT").equals("SUCCESS")) {
         throw new InvalidLoginException();
      }
   }

   public static void checkPostSuccessful(Document document) throws RadioException {
      Element hedwig = document.getRootElement();
      if (!hedwig.getChildText("result").equals("OK") || !hedwig.getChildText("message").equals("")) {
         throw new RadioException("Configuration post command unsuccessful");
      }
   }

   public static void checkActivateSuccessful(Document document) throws RadioException {
      Element pigwidgeon = document.getRootElement();
      if (!pigwidgeon.getChildText("result").equals("OK") || !pigwidgeon.getChildText("message").equals("")) {
         throw new RadioException("Configuration activation command unsuccessful");
      }
   }

   public static boolean inCorrectMode(Document document, FRCRadio.Mode desiredMode) {
      Element switchMode = getModule(document, "RUNTIME.SWITCHMODE");
      String switchPos = switchMode.getChild("runtime").getChild("device").getChildText("switchmode");
      String xml = "";
      switch (desiredMode) {
         case AP24:
            xml = "AP2G";
            break;
         case AP5:
            xml = "AP5G";
            break;
         default:
            xml = "APCLI";
      }

      return switchPos.equals(xml);
   }

   public static Element getModule(Document document, String moduleName) {
      Element rootNode = document.getRootElement();
      return getChildBySubChild(rootNode, "module", "service", moduleName);
   }

   public static Element getChildBySubChild(Element element, String childName, String subChild, String value) {
      for (Element childNode : element.getChildren(childName)) {
         if (childNode.getChildText(subChild).equals(value)) {
            return childNode;
         }
      }

      return null;
   }

   private static File getTempXMLFile(String originalFileName) throws AppException {
      InputStream in = null;
      OutputStream out = null;
      File original = new File(originalFileName);
      File temp = new File("temp.xml");

      File e;
      try {
         in = new FileInputStream(original);
         out = new FileOutputStream(temp);
         IOUtils.copy(in, out);
         e = temp;
      } catch (IOException var10) {
         throw new AppException("failed to create temp file", var10);
      } finally {
         if (in != null) {
            IOUtils.closeQuietly(in);
         }

         if (out != null) {
            IOUtils.closeQuietly(out);
         }
      }

      return e;
   }
}
