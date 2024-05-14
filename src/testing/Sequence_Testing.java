package testing;

import frcradiokiosk.AppException;
import frcradiokiosk.DAP1522;
import frcradiokiosk.DIR825_ddwrt;
import frcradiokiosk.FRCRadio;
import frcradiokiosk.HostComputer;
import frcradiokiosk.HostException;
import frcradiokiosk.IKioskShell;
import frcradiokiosk.InstructionsPanel;
import frcradiokiosk.KioskUtilities;
import frcradiokiosk.NicDialog;
import frcradiokiosk.RadioConfigSequence;
import frcradiokiosk.RadioFirmwareLoadSequence;
import frcradiokiosk.SequenceProgressDialog;
import frcradiokiosk.DAP1522RevB.DAP1522revB;
import frcradiokiosk.openMesh.OM5P_AN;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager.LookAndFeelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sequence_Testing extends JFrame implements IKioskShell {
   private static final Logger logger = LoggerFactory.getLogger(Sequence_Testing.class);
   private HostComputer host;
   private InstructionsPanel instructions = null;
   private JCheckBox jBWLimitCheckbox;
   private JLabel jCommentLabel;
   private JTextField jCommentTextField;
   private JButton jEventConfigureButton;
   private JButton jEventLoadFirmwareButton;
   private JTextField jEventTeamNumberTextField;
   private JMenuItem jExitMenuItem;
   private JMenu jFileMenu;
   private JCheckBox jFirewallCheckbox;
   private JComboBox jFuncComboBox;
   private JPanel jGuidePanel;
   private JLabel jLabel2;
   private JLabel jLabel3;
   private JPanel jMainPanel;
   private JMenuBar jMenuBar;
   private ButtonGroup jModeButtonGroup;
   private JComboBox jModeComboBox;
   private JLabel jModeLabel;
   private JLabel jModeLabel1;
   private JPanel jModePanel;
   private JPanel jModePanel1;
   private JTextField jPasskeyTextField;
   private JTextField jPasswordTextField;
   private JLabel jPwdLabel;
   private JComboBox jRadioComboBox;
   private JLabel jRadioLabel;
   private JPanel jRadioPanel;
   private JPanel jSelectionPanel;
   private JTextField jSsidTextField;
   private JPanel jTeamNumberPanel;
   private JLabel jWpaLabel;

   public Sequence_Testing() {
      this.initComponents();
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      this.setLocation((dim.width - this.getSize().width) / 2, (dim.height - this.getSize().height) / 2);
      this.jRadioComboBox.removeAllItems();

      for (Sequence_Testing.AllRadioOptions choice : Sequence_Testing.AllRadioOptions.values()) {
         this.jRadioComboBox.addItem(choice.toString());
      }

      this.jModeComboBox.removeAllItems();

      for (FRCRadio.Mode choice : FRCRadio.Mode.values()) {
         this.jModeComboBox.addItem(choice.getLongString());
      }

      this.jFuncComboBox.removeAllItems();

      for (FRCRadio.RadioType type : FRCRadio.RadioType.values()) {
         this.jFuncComboBox.addItem(type.toString());
      }

      this.jRadioComboBox.setSelectedItem(Sequence_Testing.AllRadioOptions.OM5P_AN.toString());
      this.jModeComboBox.setSelectedItem(FRCRadio.Mode.BRIDGE.getLongString());
      this.jFuncComboBox.setSelectedItem(FRCRadio.RadioType.DEFAULT.toString());
      this.instructions = new InstructionsPanel(true);
      this.jGuidePanel.add(this.instructions);
      this.instructions.updateInstructions(this.getRadio(1, "1", "", this.getMode(), "", ""));
      ActionListener instructionsUpdate = new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent e) {
            FRCRadio.Mode mode = Sequence_Testing.this.getMode();
            int team = 1;
            String ssid = "1";
            String key = "";
            String password = "";
            FRCRadio radio = Sequence_Testing.this.getRadio(team, ssid, key, mode, "", password);
            Sequence_Testing.this.instructions.updateInstructions(radio);
         }
      };
      this.jRadioComboBox.addActionListener(instructionsUpdate);
      this.jModeComboBox.addActionListener(instructionsUpdate);
      this.getRootPane().setDefaultButton(this.jEventConfigureButton);
      this.pack();
      this.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            Sequence_Testing.this.kill();
         }
      });
   }

   public void startup() {
      try {
         this.host = new HostComputer();
         logger.debug("host computer initialized");
         String[] nics = this.getNicNames();
         if (nics == null) {
            throw new HostException("Failed to identify network interfaces");
         }

         String name = NicDialog.showNicDialog(this, this, nics);
         if (name != null) {
            this.setSelectedNic(name);
         } else {
            logger.info("nic selection cancelled");
            this.kill();
         }
      } catch (HostException var31) {
         logger.error("failed to initialize host computer network settings", var31);
         this.showErrorMessageKill("An error occured while initializing the computer network settings");
      }
   }

   @Override
   public String[] getNicNames() throws HostException {
      try {
         return this.host.getInterfaceNames(true);
      } catch (HostException var2) {
         throw new HostException("Failed to refresh and retreive network interface list", var2);
      }
   }

   @Override
   public void setSelectedNic(String name) {
      this.host.setSelectedInterface(name);
   }

   @Override
   public void showSuccessMessage(String message) {
      KioskUtilities.showSuccessMessage(this, message);
   }

   @Override
   public void showErrorMessage(String message) {
      KioskUtilities.showErrorMessage(this, message);
   }

   private void showErrorMessageKill(String message) {
      this.showErrorMessage(message + ";\nThe application must shut down");
      this.kill();
   }

   @Override
   public void logAttempt(int team, boolean pass, String message) {
      Object[] params = new Object[]{team, message};
      logger.info("Radio configuration " + (pass ? "successful" : "failed") + " for team {}; {}", params);
   }

   public void kill() {
      logger.info("Shutting down kiosk");
      RadioConfigSequence.killSequence();
      System.exit(0);
   }

   private void showWpaKeyControls(boolean show) {
      this.jPasskeyTextField.setVisible(show);
      this.jWpaLabel.setVisible(show);
      this.instructions.useManualWpa(show);
   }

   private void initComponents() {
      this.jModeButtonGroup = new ButtonGroup();
      this.jMainPanel = new JPanel();
      this.jTeamNumberPanel = new JPanel();
      this.jLabel2 = new JLabel();
      this.jEventTeamNumberTextField = new JTextField();
      this.jWpaLabel = new JLabel();
      this.jPasskeyTextField = new JTextField();
      this.jLabel3 = new JLabel();
      this.jSsidTextField = new JTextField();
      this.jPwdLabel = new JLabel();
      this.jPasswordTextField = new JTextField();
      this.jCommentTextField = new JTextField();
      this.jCommentLabel = new JLabel();
      this.jEventLoadFirmwareButton = new JButton();
      this.jGuidePanel = new JPanel();
      this.jEventConfigureButton = new JButton();
      this.jFirewallCheckbox = new JCheckBox();
      this.jBWLimitCheckbox = new JCheckBox();
      this.jSelectionPanel = new JPanel();
      this.jRadioPanel = new JPanel();
      this.jRadioLabel = new JLabel();
      this.jRadioComboBox = new JComboBox();
      this.jModePanel = new JPanel();
      this.jModeLabel = new JLabel();
      this.jModeComboBox = new JComboBox();
      this.jModePanel1 = new JPanel();
      this.jModeLabel1 = new JLabel();
      this.jFuncComboBox = new JComboBox();
      this.jMenuBar = new JMenuBar();
      this.jFileMenu = new JMenu();
      this.jExitMenuItem = new JMenuItem();
      this.setDefaultCloseOperation(3);
      this.setTitle("FRC Bridge Configuration Utility");
      this.setMinimumSize(new Dimension(640, 480));
      this.setPreferredSize(new Dimension(862, 768));
      this.jMainPanel.setMinimumSize(new Dimension(600, 575));
      this.jMainPanel.setPreferredSize(new Dimension(600, 575));
      this.jMainPanel.setLayout(new GridBagLayout());
      this.jTeamNumberPanel.setLayout(new GridBagLayout());
      this.jLabel2.setFont(new Font("Tahoma", 1, 36));
      this.jLabel2.setText("Team Number:");
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.anchor = 22;
      gridBagConstraints.weighty = 1.0;
      gridBagConstraints.insets = new Insets(5, 5, 5, 5);
      this.jTeamNumberPanel.add(this.jLabel2, gridBagConstraints);
      this.jEventTeamNumberTextField.setFont(new Font("Tahoma", 1, 36));
      this.jEventTeamNumberTextField.setHorizontalAlignment(0);
      this.jEventTeamNumberTextField.setText("0");
      this.jEventTeamNumberTextField.setMinimumSize(new Dimension(200, 55));
      this.jEventTeamNumberTextField.setPreferredSize(new Dimension(200, 55));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.fill = 2;
      gridBagConstraints.insets = new Insets(5, 0, 5, 0);
      this.jTeamNumberPanel.add(this.jEventTeamNumberTextField, gridBagConstraints);
      this.jWpaLabel.setFont(new Font("Tahoma", 1, 36));
      this.jWpaLabel.setHorizontalAlignment(11);
      this.jWpaLabel.setText("WPA Key:");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 3;
      gridBagConstraints.anchor = 22;
      gridBagConstraints.weighty = 0.5;
      gridBagConstraints.insets = new Insets(0, 5, 6, 5);
      this.jTeamNumberPanel.add(this.jWpaLabel, gridBagConstraints);
      this.jPasskeyTextField.setFont(new Font("Tahoma", 1, 36));
      this.jPasskeyTextField.setHorizontalAlignment(0);
      this.jPasskeyTextField.setMinimumSize(new Dimension(200, 55));
      this.jPasskeyTextField.setPreferredSize(new Dimension(200, 55));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 3;
      gridBagConstraints.insets = new Insets(0, 0, 5, 0);
      this.jTeamNumberPanel.add(this.jPasskeyTextField, gridBagConstraints);
      this.jLabel3.setFont(new Font("Tahoma", 1, 36));
      this.jLabel3.setText("SSID:");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.anchor = 22;
      gridBagConstraints.weighty = 0.5;
      gridBagConstraints.insets = new Insets(5, 5, 5, 5);
      this.jTeamNumberPanel.add(this.jLabel3, gridBagConstraints);
      this.jSsidTextField.setFont(new Font("Tahoma", 1, 36));
      this.jSsidTextField.setHorizontalAlignment(0);
      this.jSsidTextField.setMinimumSize(new Dimension(200, 55));
      this.jSsidTextField.setPreferredSize(new Dimension(200, 55));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = 2;
      gridBagConstraints.insets = new Insets(1, 0, 5, 0);
      this.jTeamNumberPanel.add(this.jSsidTextField, gridBagConstraints);
      this.jPwdLabel.setFont(new Font("Tahoma", 1, 36));
      this.jPwdLabel.setHorizontalAlignment(11);
      this.jPwdLabel.setText("Password:");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.anchor = 22;
      gridBagConstraints.insets = new Insets(0, 5, 6, 5);
      this.jTeamNumberPanel.add(this.jPwdLabel, gridBagConstraints);
      this.jPasswordTextField.setFont(new Font("Tahoma", 1, 36));
      this.jPasswordTextField.setHorizontalAlignment(0);
      this.jPasswordTextField.setMinimumSize(new Dimension(200, 55));
      this.jPasswordTextField.setPreferredSize(new Dimension(200, 55));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.insets = new Insets(0, 0, 5, 0);
      this.jTeamNumberPanel.add(this.jPasswordTextField, gridBagConstraints);
      this.jCommentTextField.setFont(new Font("Tahoma", 1, 36));
      this.jCommentTextField.setHorizontalAlignment(0);
      this.jCommentTextField.setMinimumSize(new Dimension(200, 55));
      this.jCommentTextField.setPreferredSize(new Dimension(200, 55));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 4;
      gridBagConstraints.insets = new Insets(0, 0, 5, 0);
      this.jTeamNumberPanel.add(this.jCommentTextField, gridBagConstraints);
      this.jCommentLabel.setFont(new Font("Tahoma", 1, 36));
      this.jCommentLabel.setHorizontalAlignment(11);
      this.jCommentLabel.setText("Comment:");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 4;
      gridBagConstraints.anchor = 22;
      gridBagConstraints.weighty = 0.5;
      gridBagConstraints.insets = new Insets(0, 5, 6, 5);
      this.jTeamNumberPanel.add(this.jCommentLabel, gridBagConstraints);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.gridheight = 2;
      gridBagConstraints.weightx = 1.0;
      gridBagConstraints.insets = new Insets(5, 5, 5, 5);
      this.jMainPanel.add(this.jTeamNumberPanel, gridBagConstraints);
      this.jEventLoadFirmwareButton.setFont(new Font("Tahoma", 1, 36));
      this.jEventLoadFirmwareButton.setIcon(new ImageIcon(this.getClass().getResource("/frcradiokiosk/document-save-2.png")));
      this.jEventLoadFirmwareButton.setLabel("Load Firmware");
      this.jEventLoadFirmwareButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            Sequence_Testing.this.jEventLoadFirmwareButtonActionPerformed(evt);
         }
      });
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 3;
      this.jMainPanel.add(this.jEventLoadFirmwareButton, gridBagConstraints);
      this.jGuidePanel.setLayout(new BoxLayout(this.jGuidePanel, 1));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 5;
      this.jMainPanel.add(this.jGuidePanel, gridBagConstraints);
      this.jEventConfigureButton.setFont(new Font("Tahoma", 1, 36));
      this.jEventConfigureButton.setIcon(new ImageIcon(this.getClass().getResource("/frcradiokiosk/document-save-2.png")));
      this.jEventConfigureButton.setText("Configure");
      this.jEventConfigureButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            Sequence_Testing.this.jEventConfigureButtonActionPerformed(evt);
         }
      });
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.weightx = 1.0;
      gridBagConstraints.weighty = 1.0;
      this.jMainPanel.add(this.jEventConfigureButton, gridBagConstraints);
      this.jFirewallCheckbox.setFont(new Font("Tahoma", 1, 24));
      this.jFirewallCheckbox.setText("Firewall?");
      this.jFirewallCheckbox.setToolTipText("Emulate the field firewall");
      this.jFirewallCheckbox.setAlignmentX(0.5F);
      this.jFirewallCheckbox.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            Sequence_Testing.this.jFirewallCheckboxActionPerformed(evt);
         }
      });
      this.jMainPanel.add(this.jFirewallCheckbox, new GridBagConstraints());
      this.jBWLimitCheckbox.setFont(new Font("Tahoma", 1, 24));
      this.jBWLimitCheckbox.setSelected(true);
      this.jBWLimitCheckbox.setText("Bandwidth Limit?");
      this.jBWLimitCheckbox.setToolTipText("Limit the bandwidth to emulate the field");
      this.jBWLimitCheckbox.setAlignmentX(0.5F);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.ipady = 50;
      this.jMainPanel.add(this.jBWLimitCheckbox, gridBagConstraints);
      this.getContentPane().add(this.jMainPanel, "Center");
      this.jSelectionPanel.setLayout(new GridBagLayout());
      this.jRadioLabel.setFont(new Font("Tahoma", 0, 18));
      this.jRadioLabel.setText("Radio:");
      this.jRadioPanel.add(this.jRadioLabel);
      this.jRadioComboBox.setFont(new Font("Tahoma", 0, 18));
      this.jRadioComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Radio Selection"}));
      this.jRadioComboBox.setMinimumSize(new Dimension(200, 30));
      this.jRadioComboBox.setPreferredSize(new Dimension(200, 30));
      this.jRadioPanel.add(this.jRadioComboBox);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.insets = new Insets(5, 5, 0, 10);
      this.jSelectionPanel.add(this.jRadioPanel, gridBagConstraints);
      this.jModeLabel.setFont(new Font("Tahoma", 0, 18));
      this.jModeLabel.setText("Mode:");
      this.jModePanel.add(this.jModeLabel);
      this.jModeComboBox.setFont(new Font("Tahoma", 0, 18));
      this.jModeComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Mode Selection"}));
      this.jModeComboBox.setMinimumSize(new Dimension(200, 30));
      this.jModeComboBox.setPreferredSize(new Dimension(200, 30));
      this.jModePanel.add(this.jModeComboBox);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.insets = new Insets(5, 10, 0, 5);
      this.jSelectionPanel.add(this.jModePanel, gridBagConstraints);
      this.jModeLabel1.setFont(new Font("Tahoma", 0, 18));
      this.jModeLabel1.setText("Function:");
      this.jModePanel1.add(this.jModeLabel1);
      this.jFuncComboBox.setFont(new Font("Tahoma", 0, 18));
      this.jFuncComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Function Selection"}));
      this.jFuncComboBox.setMinimumSize(new Dimension(200, 30));
      this.jFuncComboBox.setPreferredSize(new Dimension(200, 30));
      this.jModePanel1.add(this.jFuncComboBox);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.insets = new Insets(5, 10, 0, 5);
      this.jSelectionPanel.add(this.jModePanel1, gridBagConstraints);
      this.getContentPane().add(this.jSelectionPanel, "South");
      this.jFileMenu.setText("File");
      this.jExitMenuItem.setText("Exit");
      this.jExitMenuItem.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            Sequence_Testing.this.jExitMenuItemActionPerformed(evt);
         }
      });
      this.jFileMenu.add(this.jExitMenuItem);
      this.jMenuBar.add(this.jFileMenu);
      this.setJMenuBar(this.jMenuBar);
      this.pack();
   }

   private void jExitMenuItemActionPerformed(ActionEvent evt) {
      this.kill();
   }

   private void jEventLoadFirmwareButtonActionPerformed(ActionEvent evt) {
      try {
         int team = KioskUtilities.checkTeamNumStringSyntax(this.jEventTeamNumberTextField.getText());
         String ssid = KioskUtilities.checkSsidSyntax(this.jSsidTextField.getText());
         String key = KioskUtilities.checkKeySyntax(this.jPasskeyTextField.getText());
         String password = this.jPasswordTextField.getText();
         FRCRadio.Mode mode = this.getMode();
         FRCRadio radio = this.getRadio(team, ssid, key, mode, key, password);
         if (radio != null) {
            RadioFirmwareLoadSequence.RunFirmwareLoadSequence(radio, this.host, this, new SequenceProgressDialog(this, true));
         }
      } catch (AppException var81) {
         this.showErrorMessage(var81.getUserMessage());
         logger.error("Error starting firmware sequence: " + var81.getMessage(), var81);
      }
   }

   private void jEventConfigureButtonActionPerformed(ActionEvent evt) {
      try {
         int team = KioskUtilities.checkTeamNumStringSyntax(this.jEventTeamNumberTextField.getText());
         String ssid = KioskUtilities.checkSsidSyntax(this.jSsidTextField.getText());
         String key = KioskUtilities.checkKeySyntax(this.jPasskeyTextField.getText());
         String password = this.jPasswordTextField.getText();
         FRCRadio.Mode mode = this.getMode();
         FRCRadio radio = this.getRadio(team, ssid, key, mode, key, password);
         radio.setComment(this.jCommentTextField.getText());
         if (radio != null) {
            FRCRadio.RadioType type = null;

            for (FRCRadio.RadioType t : FRCRadio.RadioType.values()) {
               if (t.toString().equals(this.jFuncComboBox.getSelectedItem().toString())) {
                  type = t;
               }
            }

            switch (type) {
               case PRACTICE:
                  radio.makePracticeRadio(1, 1);
                  break;
               case FIELD_TEST:
                  radio.makeFieldTestRadio();
            }

            radio.setFirewall(this.jFirewallCheckbox.isSelected());
            radio.setBWLimit(this.jBWLimitCheckbox.isSelected());
            RadioConfigSequence.RunConfigSequence(radio, this.host, this, new SequenceProgressDialog(this, true));
         }
      } catch (AppException var131) {
         this.showErrorMessage(var131.getUserMessage());
         logger.error("Error starting program sequence: " + var131.getMessage(), var131);
      }
   }

   private void jFirewallCheckboxActionPerformed(ActionEvent evt) {
   }

   private FRCRadio.Mode getMode() {
      for (FRCRadio.Mode m : FRCRadio.Mode.values()) {
         if (m.getLongString().equals(this.jModeComboBox.getSelectedItem().toString())) {
            return m;
         }
      }

      return FRCRadio.Mode.AP24;
   }

   private FRCRadio getRadio(int team, String ssid, String key, FRCRadio.Mode mode, String channel, String password) {
      Sequence_Testing.AllRadioOptions radioModel = Sequence_Testing.AllRadioOptions.DAP1522_Rev_B;

      for (Sequence_Testing.AllRadioOptions choice : Sequence_Testing.AllRadioOptions.values()) {
         if (choice.toString().equals(this.jRadioComboBox.getSelectedItem().toString())) {
            radioModel = choice;
         }
      }

      switch (radioModel) {
         case DAP1522_Rev_A:
            return new DAP1522(team, ssid, key, mode, password);
         case DIR825_DDWRT:
            return new DIR825_ddwrt(team, ssid, key, mode, "", password);
         case OM5P_AN:
            return new OM5P_AN(team, ssid, key, mode, password);
         default:
            return new DAP1522revB(team, ssid, key, mode, password);
      }
   }

   public static void main(String[] args) {
      try {
         for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
               UIManager.setLookAndFeel(info.getClassName());
               break;
            }
         }
      } catch (ClassNotFoundException var5) {
      } catch (InstantiationException var6) {
      } catch (IllegalAccessException var7) {
      } catch (UnsupportedLookAndFeelException var8) {
      }

      EventQueue.invokeLater(new Runnable() {
         @Override
         public void run() {
            Sequence_Testing.logger.info("Starting kiosk...");
            Sequence_Testing theGui = new Sequence_Testing();
            theGui.setVisible(true);
            theGui.startup();
         }
      });
   }

   private static enum AllRadioOptions {
      DIR825_DDWRT("DIR825 DD-WRT"),
      DAP1522_Rev_A("DAP1522 RevA"),
      DAP1522_Rev_B("DAP1522 RevB"),
      OM5P_AN("OM5P_AC"),
      HAP_AC_Lite("HAP_AC_Lite");

      private String string;

      private AllRadioOptions(String string) {
         this.string = string;
      }

      @Override
      public String toString() {
         return this.string;
      }
   }
}
