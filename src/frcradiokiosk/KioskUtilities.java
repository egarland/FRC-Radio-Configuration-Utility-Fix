package frcradiokiosk;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class KioskUtilities {
   public static int checkTeamNumStringSyntax(String teamString) throws AppException {
      if (!teamString.isEmpty() && teamString.matches("[\\d]{1,4}") && Integer.valueOf(teamString) != 0) {
         return Integer.parseInt(teamString);
      } else {
         throw new AppException("Invalid team number: " + teamString, "Team number must be an integer value 1-9999");
      }
   }

   public static String checkKeySyntax(String keyString) throws AppException {
      if (!keyString.matches("[^,]{8,30}") & !keyString.isEmpty()) {
         throw new AppException("Invalid WPA Key: " + keyString, "WPA key must be 8-30 characters long and may not contain commas");
      } else {
         return keyString;
      }
   }

   public static String checkSsidSyntax(String ssidString) throws AppException {
      if (ssidString.isEmpty()) {
         throw new AppException("Invalid SSID: " + ssidString, "SSID must be at least 1 character long");
      } else if (ssidString.startsWith("_")) {
         throw new AppException("Invalid SSID: " + ssidString, "SSID may not start with an underscore");
      } else if (!ssidString.matches("[\\w]{1,31}")) {
         throw new AppException(
            "Invalid SSID: " + ssidString,
            "SSID must be 1-31 characters long, must not start with an underscore, and may only contain letters, digits, and underscores"
         );
      } else {
         return ssidString;
      }
   }

   public static void addWebLinkListener(JLabel label, final String link) {
      label.setCursor(new Cursor(12));
      label.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            try {
               Desktop.getDesktop().browse(new URI(link));
            } catch (URISyntaxException | IOException var3) {
            }
         }
      });
   }

   public static void showSuccessMessage(Component owner, String message) {
      JOptionPane.showMessageDialog(owner, message, "Complete", -1);
   }

   public static void showErrorMessage(Component owner, Object message) {
      JOptionPane.showMessageDialog(owner, message, "Error", 0);
   }

   public static void showWarningMessage(Component owner, String message) {
      JOptionPane.showMessageDialog(owner, message, "Warning", 2);
   }

   public static boolean showConfirmationMessage(Component owner, String message) {
      return 0 == JOptionPane.showConfirmDialog(owner, message, "Warning", 0, 2);
   }
}
