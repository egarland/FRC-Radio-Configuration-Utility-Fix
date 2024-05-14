package frcradiokiosk;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NicDialog extends JDialog {
   private String selection = null;
   private JComboBox nicSelection;
   private static final Logger logger = LoggerFactory.getLogger(NicDialog.class);

   private NicDialog(Frame owner, final INicDialogShell kiosk, String[] nics) {
      super(owner, "Network Interfaces", true);
      this.setComponentOrientation(owner.getComponentOrientation());
      this.setResizable(false);
      this.setLayout(new BorderLayout());
      JPanel labelPanel = new JPanel();
      labelPanel.setLayout(new BoxLayout(labelPanel, 1));
      labelPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
      JLabel topLine = new JLabel("Please select a network interface using the drop down box below.");
      JLabel bottomLine = new JLabel("If no network interfaces are listed, connect the wireless bridge to the computer and click \"Refresh\"");
      labelPanel.add(topLine);
      labelPanel.add(bottomLine);
      JPanel nicPanel = new JPanel();
      nicPanel.setLayout(new FlowLayout(1));
      nicPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
      this.nicSelection = new JComboBox();
      this.refreshComboBox(nics);
      nicPanel.add(this.nicSelection);
      JButton refreshNics = new JButton("Refresh");
      refreshNics.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            try {
               NicDialog.this.refreshComboBox(kiosk.getNicNames());
            } catch (HostException var3) {
               NicDialog.logger.error("Failed to refresh network interface names", var3);
               KioskUtilities.showErrorMessage(NicDialog.this, "An error occurred while trying to retreive network interface names");
            }
         }
      });
      nicPanel.add(refreshNics);
      JButton okButton = new JButton("OK");
      final JButton cancelButton = new JButton("Cancel");
      ActionListener okCancelListener = new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            if (e.getSource().equals(cancelButton)) {
               NicDialog.this.selection = null;
            } else {
               NicDialog.this.selection = (String)NicDialog.this.nicSelection.getSelectedItem();
            }

            NicDialog.this.setVisible(false);
            NicDialog.this.dispose();
         }
      };
      okButton.addActionListener(okCancelListener);
      cancelButton.addActionListener(okCancelListener);
      JPanel okPanel = new JPanel();
      okPanel.setLayout(new FlowLayout(4));
      okPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
      okPanel.add(okButton);
      okPanel.add(cancelButton);
      this.add(labelPanel, "North");
      this.add(nicPanel, "Center");
      this.add(okPanel, "South");
      this.getRootPane().setDefaultButton(okButton);
      this.pack();
      this.setLocationRelativeTo(owner);
   }

   private void refreshComboBox(String[] choices) {
      this.nicSelection.removeAllItems();

      for (String choice : choices) {
         this.nicSelection.addItem(choice);
      }
   }

   private String getSelection() {
      return this.selection;
   }

   public static String showNicDialog(Frame owner, INicDialogShell kiosk, String[] nics) {
      NicDialog nd = new NicDialog(owner, kiosk, nics);
      nd.setVisible(true);
      return nd.getSelection();
   }
}
