package frc.teamkiosk;

import frcradiokiosk.AppException;
import frcradiokiosk.ConfigurationResult;
import frcradiokiosk.FRCRadio;
import frcradiokiosk.HostComputer;
import frcradiokiosk.HostException;
import frcradiokiosk.IKioskShell;
import frcradiokiosk.InstructionsPanel;
import frcradiokiosk.KioskUtilities;
import frcradiokiosk.NicDialog;
import frcradiokiosk.RadioConfigSequence;
import frcradiokiosk.RadioFactory;
import frcradiokiosk.RadioFirmwareLoadSequence;
import frcradiokiosk.SequenceProgressDialog;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager.LookAndFeelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FRC_Team_Kiosk extends JFrame implements IKioskShell {
   private static final Logger logger = LoggerFactory.getLogger(FRC_Team_Kiosk.class);
   private static final String HELP_URL = "https://frc-docs.readthedocs.io/en/latest/docs/getting-started/getting-started-frc-control-system/radio-programming.html";
   private HostComputer host;
   private String customSsid = "";
   private String customKey = "";
   private InstructionsPanel instructions = null;
   private JMenuItem jAboutMenuItem;
   private JCheckBox jBWLimitCheckbox;
   private JLabel jBWLimitLabel;
   private JMenuItem jChangeNicMenuItem;
   private JButton jEventConfigureButton;
   private JTextField jEventSSIDSuffix;
   private JTextField jEventTeamNumberTextField;
   private JMenuItem jExitMenuItem;
   private JMenu jFileMenu;
   private JCheckBox jFirewallCheckbox;
   private JLabel jFirewallLabel;
   private JCheckBoxMenuItem jFmslCheckBoxMenuItem;
   private JPanel jGuidePanel;
   private JMenu jHelpMenu;
   private JMenuItem jHelpMenuItem;
   private JLabel jLabel2;
   private JButton jLoadFirmwareButton1;
   private JPanel jMainPanel;
   private JMenuBar jMenuBar;
   private ButtonGroup jModeButtonGroup;
   private JComboBox jModeComboBox;
   private JLabel jModeLabel;
   private JTextField jPasskeyTextField;
   private JComboBox jRadioComboBox;
   private JLabel jRadioLabel;
   private JLabel jSSIDSuffixLabel;
   private JPanel jTeamNumberPanel;
   private JMenu jToolsMenu;
   private JLabel jWpaLabel;

   public FRC_Team_Kiosk() {
      this.initComponents();
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      this.setLocation((dim.width - this.getSize().width) / 2, (dim.height - this.getSize().height) / 2);
      this.jRadioComboBox.removeAllItems();

      for (RadioFactory.RadioOptions choice : RadioFactory.RadioOptions.values()) {
         this.jRadioComboBox.addItem(choice.toString());
      }

      this.jRadioComboBox.addItemListener(new ItemListener() {
         @Override
         public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == 1) {
               for (RadioFactory.RadioOptions choice : RadioFactory.RadioOptions.values()) {
                  if (choice.toString().equals(FRC_Team_Kiosk.this.jRadioComboBox.getSelectedItem().toString())) {
                     RadioFactory.getInstance().selectRadio(choice);
                  }
               }
            }
         }
      });
      this.jModeComboBox.removeAllItems();

      for (FRCRadio.Mode choice : FRCRadio.Mode.values()) {
         if (choice != FRCRadio.Mode.BRIDGE24) {
            this.jModeComboBox.addItem(choice.getLongString());
         }
      }

      this.jModeComboBox.addItemListener(new ItemListener() {
         @Override
         public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == 1) {
               for (FRCRadio.Mode choice : FRCRadio.Mode.values()) {
                  if (choice.getLongString().equals(FRC_Team_Kiosk.this.jModeComboBox.getSelectedItem().toString())) {
                     RadioFactory.getInstance().setRadioMode(choice);
                  }
               }
            }
         }
      });
      this.jRadioComboBox.setSelectedItem(RadioFactory.getInstance().getDefaultRadioType().toString());
      this.jModeComboBox.setSelectedItem(FRCRadio.Mode.AP24.getLongString());
      this.jEventSSIDSuffix.addFocusListener(new FocusListener() {
         @Override
         public void focusGained(FocusEvent e) {
            FRC_Team_Kiosk.this.jEventSSIDSuffix.setText("");
            FRC_Team_Kiosk.this.jEventSSIDSuffix.setForeground(Color.black);
         }

         @Override
         public void focusLost(FocusEvent e) {
         }
      });
      this.jPasskeyTextField.addFocusListener(new FocusListener() {
         @Override
         public void focusGained(FocusEvent e) {
            FRC_Team_Kiosk.this.jPasskeyTextField.setText("");
            FRC_Team_Kiosk.this.jPasskeyTextField.setForeground(Color.black);
         }

         @Override
         public void focusLost(FocusEvent e) {
         }
      });
      this.instructions = new InstructionsPanel(true);
      this.jGuidePanel.add(this.instructions);
      this.getRootPane().setDefaultButton(this.jEventConfigureButton);
      this.pack();
      this.setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getClassLoader().getResource("images/frc-64x64.png")));
      this.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            FRC_Team_Kiosk.this.kill();
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
      JPanel errorPanel = new JPanel();
      errorPanel.setLayout(new BoxLayout(errorPanel, 1));
      JLabel errorMessage = new JLabel();
      JLabel helpLink = new JLabel();
      errorMessage.setText(message);
      helpLink.setText(
         "<html><a href=\"https://frc-docs.readthedocs.io/en/latest/docs/getting-started/getting-started-frc-control-system/radio-programming.html\">More Help</a></html>"
      );
      KioskUtilities.addWebLinkListener(
         helpLink, "https://frc-docs.readthedocs.io/en/latest/docs/getting-started/getting-started-frc-control-system/radio-programming.html"
      );
      errorPanel.add(errorMessage);
      errorPanel.add(helpLink);
      KioskUtilities.showErrorMessage(this, errorPanel);
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
      this.jEventSSIDSuffix.setText("");
      this.jEventSSIDSuffix.setVisible(show);
      this.jSSIDSuffixLabel.setVisible(show);
   }

   private void initComponents() {
      this.jModeButtonGroup = new ButtonGroup();
      this.jMainPanel = new JPanel();
      this.jTeamNumberPanel = new JPanel();
      this.jLabel2 = new JLabel();
      this.jEventTeamNumberTextField = new JTextField();
      this.jWpaLabel = new JLabel();
      this.jPasskeyTextField = new JTextField();
      this.jEventConfigureButton = new JButton();
      this.jLoadFirmwareButton1 = new JButton();
      this.jRadioLabel = new JLabel();
      this.jRadioComboBox = new JComboBox();
      this.jModeLabel = new JLabel();
      this.jModeComboBox = new JComboBox();
      this.jEventSSIDSuffix = new JTextField();
      this.jSSIDSuffixLabel = new JLabel();
      this.jFirewallCheckbox = new JCheckBox();
      this.jFirewallLabel = new JLabel();
      this.jBWLimitCheckbox = new JCheckBox();
      this.jBWLimitLabel = new JLabel();
      this.jGuidePanel = new JPanel();
      this.jMenuBar = new JMenuBar();
      this.jFileMenu = new JMenu();
      this.jExitMenuItem = new JMenuItem();
      this.jToolsMenu = new JMenu();
      this.jChangeNicMenuItem = new JMenuItem();
      this.jFmslCheckBoxMenuItem = new JCheckBoxMenuItem();
      this.jHelpMenu = new JMenu();
      this.jHelpMenuItem = new JMenuItem();
      this.jAboutMenuItem = new JMenuItem();
      this.setDefaultCloseOperation(3);
      this.setTitle("FRC Radio Configuration Utility");
      this.setMinimumSize(new Dimension(740, 580));
      this.setPreferredSize(new Dimension(740, 580));
      this.jMainPanel.setMinimumSize(new Dimension(600, 575));
      this.jMainPanel.setPreferredSize(new Dimension(600, 575));
      this.jMainPanel.setLayout(new GridBagLayout());
      this.jTeamNumberPanel.setLayout(new GridBagLayout());
      this.jLabel2.setFont(new Font("Tahoma", 1, 24));
      this.jLabel2.setText("Team Number:");
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.insets = new Insets(5, 5, 5, 5);
      this.jTeamNumberPanel.add(this.jLabel2, gridBagConstraints);
      this.jEventTeamNumberTextField.setFont(new Font("Tahoma", 1, 30));
      this.jEventTeamNumberTextField.setHorizontalAlignment(0);
      this.jEventTeamNumberTextField.setText("0");
      this.jEventTeamNumberTextField.setMinimumSize(new Dimension(200, 40));
      this.jEventTeamNumberTextField.setPreferredSize(new Dimension(200, 40));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.fill = 2;
      gridBagConstraints.insets = new Insets(5, 0, 5, 10);
      this.jTeamNumberPanel.add(this.jEventTeamNumberTextField, gridBagConstraints);
      this.jWpaLabel.setFont(new Font("Tahoma", 1, 24));
      this.jWpaLabel.setHorizontalAlignment(11);
      this.jWpaLabel.setText("WPA Key:");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.anchor = 22;
      gridBagConstraints.insets = new Insets(0, 5, 6, 5);
      this.jTeamNumberPanel.add(this.jWpaLabel, gridBagConstraints);
      this.jPasskeyTextField.setFont(new Font("Tahoma", 1, 30));
      this.jPasskeyTextField.setForeground(new Color(204, 204, 204));
      this.jPasskeyTextField.setHorizontalAlignment(0);
      this.jPasskeyTextField.setText("Optional");
      this.jPasskeyTextField.setMinimumSize(new Dimension(200, 40));
      this.jPasskeyTextField.setPreferredSize(new Dimension(200, 40));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.insets = new Insets(0, 0, 5, 10);
      this.jTeamNumberPanel.add(this.jPasskeyTextField, gridBagConstraints);
      this.jEventConfigureButton.setFont(new Font("Tahoma", 1, 30));
      this.jEventConfigureButton.setIcon(new ImageIcon(this.getClass().getResource("/frcradiokiosk/document-save-2.png")));
      this.jEventConfigureButton.setText("Configure");
      this.jEventConfigureButton.setMaximumSize(new Dimension(300, 40));
      this.jEventConfigureButton.setMinimumSize(new Dimension(300, 40));
      this.jEventConfigureButton.setPreferredSize(new Dimension(300, 40));
      this.jEventConfigureButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            FRC_Team_Kiosk.this.jEventConfigureButtonActionPerformed(evt);
         }
      });
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 5;
      gridBagConstraints.gridwidth = 4;
      gridBagConstraints.weightx = 1.0;
      gridBagConstraints.insets = new Insets(0, 10, 0, 0);
      this.jTeamNumberPanel.add(this.jEventConfigureButton, gridBagConstraints);
      this.jLoadFirmwareButton1.setFont(new Font("Tahoma", 1, 30));
      this.jLoadFirmwareButton1.setIcon(new ImageIcon(this.getClass().getResource("/frcradiokiosk/icon-firmware.png")));
      this.jLoadFirmwareButton1.setText("Load Firmware");
      this.jLoadFirmwareButton1.setActionCommand("LoadFirmware");
      this.jLoadFirmwareButton1.setMaximumSize(new Dimension(300, 40));
      this.jLoadFirmwareButton1.setMinimumSize(new Dimension(300, 40));
      this.jLoadFirmwareButton1.setPreferredSize(new Dimension(300, 40));
      this.jLoadFirmwareButton1.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            FRC_Team_Kiosk.this.jLoadFirmwareButton1ActionPerformed(evt);
         }
      });
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 4;
      gridBagConstraints.gridwidth = 2;
      gridBagConstraints.weightx = 1.0;
      gridBagConstraints.insets = new Insets(0, 10, 0, 0);
      this.jTeamNumberPanel.add(this.jLoadFirmwareButton1, gridBagConstraints);
      this.jRadioLabel.setFont(new Font("Tahoma", 1, 24));
      this.jRadioLabel.setText("Radio:");
      this.jRadioLabel.setMaximumSize(new Dimension(82, 25));
      this.jRadioLabel.setMinimumSize(new Dimension(82, 25));
      this.jRadioLabel.setPreferredSize(new Dimension(82, 25));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 3;
      gridBagConstraints.anchor = 13;
      this.jTeamNumberPanel.add(this.jRadioLabel, gridBagConstraints);
      this.jRadioComboBox.setFont(new Font("Tahoma", 0, 18));
      this.jRadioComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Radio Selection"}));
      this.jRadioComboBox.setMinimumSize(new Dimension(200, 40));
      this.jRadioComboBox.setPreferredSize(new Dimension(200, 40));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 3;
      gridBagConstraints.anchor = 17;
      gridBagConstraints.insets = new Insets(2, 0, 2, 0);
      this.jTeamNumberPanel.add(this.jRadioComboBox, gridBagConstraints);
      this.jModeLabel.setFont(new Font("Tahoma", 1, 24));
      this.jModeLabel.setText("Mode:");
      this.jModeLabel.setMaximumSize(new Dimension(80, 25));
      this.jModeLabel.setMinimumSize(new Dimension(80, 25));
      this.jModeLabel.setPreferredSize(new Dimension(80, 25));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 4;
      gridBagConstraints.anchor = 13;
      this.jTeamNumberPanel.add(this.jModeLabel, gridBagConstraints);
      this.jModeComboBox.setFont(new Font("Tahoma", 0, 18));
      this.jModeComboBox.setModel(new DefaultComboBoxModel<>(new String[]{"Mode Selection"}));
      this.jModeComboBox.setMinimumSize(new Dimension(200, 30));
      this.jModeComboBox.setPreferredSize(new Dimension(200, 40));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 4;
      gridBagConstraints.anchor = 17;
      gridBagConstraints.insets = new Insets(2, 0, 2, 0);
      this.jTeamNumberPanel.add(this.jModeComboBox, gridBagConstraints);
      this.jEventSSIDSuffix.setFont(new Font("Tahoma", 1, 30));
      this.jEventSSIDSuffix.setForeground(new Color(204, 204, 204));
      this.jEventSSIDSuffix.setHorizontalAlignment(0);
      this.jEventSSIDSuffix.setText("Optional");
      this.jEventSSIDSuffix.setToolTipText("Appended to Team Number when forming SSID");
      this.jEventSSIDSuffix.setMinimumSize(new Dimension(170, 40));
      this.jEventSSIDSuffix.setPreferredSize(new Dimension(170, 40));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 3;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.fill = 2;
      gridBagConstraints.insets = new Insets(5, 0, 5, 10);
      this.jTeamNumberPanel.add(this.jEventSSIDSuffix, gridBagConstraints);
      this.jSSIDSuffixLabel.setFont(new Font("Tahoma", 1, 24));
      this.jSSIDSuffixLabel.setHorizontalAlignment(11);
      this.jSSIDSuffixLabel.setText("Robot Name:");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.insets = new Insets(5, 5, 5, 5);
      this.jTeamNumberPanel.add(this.jSSIDSuffixLabel, gridBagConstraints);
      this.jFirewallCheckbox.setFont(new Font("Tahoma", 1, 24));
      this.jFirewallCheckbox.setToolTipText("Enable firewall to emulate field network");
      this.jFirewallCheckbox.setHorizontalAlignment(2);
      this.jFirewallCheckbox.setHorizontalTextPosition(2);
      this.jFirewallCheckbox.setMaximumSize(new Dimension(120, 20));
      this.jFirewallCheckbox.setMinimumSize(new Dimension(120, 20));
      this.jFirewallCheckbox.setPreferredSize(new Dimension(150, 20));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 3;
      gridBagConstraints.gridy = 1;
      this.jTeamNumberPanel.add(this.jFirewallCheckbox, gridBagConstraints);
      this.jFirewallLabel.setFont(new Font("Tahoma", 1, 24));
      this.jFirewallLabel.setHorizontalAlignment(11);
      this.jFirewallLabel.setText("Firewall:");
      this.jFirewallLabel.setToolTipText("Enable Firewall to emulate field network");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.anchor = 22;
      gridBagConstraints.insets = new Insets(5, 5, 5, 5);
      this.jTeamNumberPanel.add(this.jFirewallLabel, gridBagConstraints);
      this.jBWLimitCheckbox.setFont(new Font("Tahoma", 1, 24));
      this.jBWLimitCheckbox.setSelected(true);
      this.jBWLimitCheckbox.setToolTipText("Enable bandwidth limiting to emulate field configuration");
      this.jBWLimitCheckbox.setHorizontalAlignment(2);
      this.jBWLimitCheckbox.setHorizontalTextPosition(2);
      this.jBWLimitCheckbox.setMaximumSize(new Dimension(120, 20));
      this.jBWLimitCheckbox.setMinimumSize(new Dimension(120, 20));
      this.jBWLimitCheckbox.setPreferredSize(new Dimension(150, 20));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 3;
      gridBagConstraints.gridy = 3;
      this.jTeamNumberPanel.add(this.jBWLimitCheckbox, gridBagConstraints);
      this.jBWLimitLabel.setFont(new Font("Tahoma", 1, 24));
      this.jBWLimitLabel.setHorizontalAlignment(11);
      this.jBWLimitLabel.setText("BW Limit:");
      this.jBWLimitLabel.setToolTipText("Enable bandwidth limiting to emulate field configuration");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 2;
      gridBagConstraints.gridy = 3;
      gridBagConstraints.anchor = 22;
      gridBagConstraints.insets = new Insets(5, 5, 5, 5);
      this.jTeamNumberPanel.add(this.jBWLimitLabel, gridBagConstraints);
      this.jMainPanel.add(this.jTeamNumberPanel, new GridBagConstraints());
      this.jGuidePanel.setLayout(new BoxLayout(this.jGuidePanel, 1));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 6;
      gridBagConstraints.gridwidth = 3;
      gridBagConstraints.anchor = 11;
      gridBagConstraints.weighty = 1.0;
      this.jMainPanel.add(this.jGuidePanel, gridBagConstraints);
      this.getContentPane().add(this.jMainPanel, "Center");
      this.jFileMenu.setText("File");
      this.jExitMenuItem.setText("Exit");
      this.jExitMenuItem.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            FRC_Team_Kiosk.this.jExitMenuItemActionPerformed(evt);
         }
      });
      this.jFileMenu.add(this.jExitMenuItem);
      this.jMenuBar.add(this.jFileMenu);
      this.jToolsMenu.setText("Tools");
      this.jChangeNicMenuItem.setText("Change Network Interface");
      this.jChangeNicMenuItem.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            FRC_Team_Kiosk.this.jChangeNicMenuItemActionPerformed(evt);
         }
      });
      this.jToolsMenu.add(this.jChangeNicMenuItem);
      this.jFmslCheckBoxMenuItem.setText("FMS Offseason Mode");
      this.jFmslCheckBoxMenuItem.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            FRC_Team_Kiosk.this.jFmslCheckBoxMenuItemActionPerformed(evt);
         }
      });
      this.jToolsMenu.add((JMenuItem)this.jFmslCheckBoxMenuItem);
      this.jMenuBar.add(this.jToolsMenu);
      this.jHelpMenu.setText("Help");
      this.jHelpMenuItem.setText("Help");
      this.jHelpMenuItem.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            FRC_Team_Kiosk.this.jHelpMenuItemActionPerformed(evt);
         }
      });
      this.jHelpMenu.add(this.jHelpMenuItem);
      this.jAboutMenuItem.setText("About");
      this.jAboutMenuItem.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            FRC_Team_Kiosk.this.jAboutMenuItemActionPerformed(evt);
         }
      });
      this.jHelpMenu.add(this.jAboutMenuItem);
      this.jMenuBar.add(this.jHelpMenu);
      this.setJMenuBar(this.jMenuBar);
      this.getAccessibleContext().setAccessibleName("2015 FRC Bridge Configuration Utility");
      this.pack();
   }

   private void jAboutMenuItemActionPerformed(ActionEvent evt) {
      Package p = FRC_Team_Kiosk.class.getPackage();
      String title = p.getImplementationTitle();
      String version = p.getImplementationVersion();
      String vendor = p.getImplementationVendor();
      String message = title + "\n" + vendor + "\nVersion: " + version;
      JOptionPane.showMessageDialog(this, message, "About", -1);
   }

   private void jChangeNicMenuItemActionPerformed(ActionEvent evt) {
      try {
         String[] nics = this.getNicNames();
         if (nics != null) {
            String name = NicDialog.showNicDialog(this, this, nics);
            if (name != null) {
               this.setSelectedNic(name);
            }
         }
      } catch (HostException var41) {
         logger.error("Failed to refresh and retreive network interface list", var41);
         this.showErrorMessage("An error occurred while retreiving the list of network interfaces");
      }
   }

   private void jExitMenuItemActionPerformed(ActionEvent evt) {
      this.kill();
   }

   private void jEventConfigureButtonActionPerformed(ActionEvent evt) {
      try {
         int team = KioskUtilities.checkTeamNumStringSyntax(this.jEventTeamNumberTextField.getText());
         FRCRadio radio;
         if (this.jFmslCheckBoxMenuItem.isSelected() && !this.customSsid.isEmpty()) {
            radio = RadioFactory.getInstance().getRadio(team, this.customSsid, this.customKey, "");
         } else {
            String key = "";
            String ssid = "";
            if (!this.jPasskeyTextField.getText().equalsIgnoreCase("Optional")) {
               key = KioskUtilities.checkKeySyntax(this.jPasskeyTextField.getText().trim());
            }

            if (!this.jEventSSIDSuffix.getText().equalsIgnoreCase("Optional") && !this.jEventSSIDSuffix.getText().isEmpty()) {
               ssid = KioskUtilities.checkSsidSyntax(team + "_" + this.jEventSSIDSuffix.getText().trim());
               radio = RadioFactory.getInstance().getRadio(team, ssid, key, "");
            } else {
               radio = RadioFactory.getInstance().getRadio(team, key);
            }
         }

         radio.setFirewall(this.jFirewallCheckbox.isSelected());
         radio.setBWLimit(this.jBWLimitCheckbox.isSelected());
         radio.setComment("Home");
         ConfigurationResult var7 = RadioConfigSequence.RunConfigSequence(radio, this.host, this, new SequenceProgressDialog(this, true));
      } catch (AppException var61) {
         this.showErrorMessage(var61.getUserMessage());
         logger.error("Error starting program sequence: " + var61.getMessage(), var61);
      }
   }

   private void jFmslCheckBoxMenuItemActionPerformed(ActionEvent evt) {
      if (this.jFmslCheckBoxMenuItem.isSelected()) {
         boolean ssidOk = false;
         boolean wpaOk = false;
         boolean cancelled = false;
         String ssid = null;
         String wpa = null;

         while (!ssidOk && !cancelled) {
            try {
               if (ssid == null) {
                  ssid = JOptionPane.showInputDialog(this, "Please enter a SSID.\nSSIDs must be 1-31 characters long, and only contain alphanumeric characters");
                  ssid = ssid.trim();
               } else {
                  ssid = JOptionPane.showInputDialog(
                     this, "Please enter a SSID.\nSSIDs must be 1-31 characters long, and only contain alphanumeric characters", ssid
                  );
                  ssid = ssid.trim();
               }

               if (ssid != null) {
                  KioskUtilities.checkSsidSyntax(ssid);
                  ssidOk = true;
               } else {
                  cancelled = true;
               }
            } catch (AppException var9) {
               this.showErrorMessage(var9.getUserMessage());
            }
         }

         while (!wpaOk && !cancelled) {
            try {
               if (wpa == null) {
                  wpa = JOptionPane.showInputDialog(
                     this, "Please enter a WPA key.\nWPA keys must be 8-15 characters long, and only contain alphanumeric characters"
                  );
                  wpa = wpa.trim();
               } else {
                  wpa = JOptionPane.showInputDialog(
                     this, "Please enter a WPA key.\nWPA keys must be 8-15 characters long, and only contain alphanumeric characters", wpa
                  );
                  wpa = wpa.trim();
               }

               if (wpa != null) {
                  KioskUtilities.checkKeySyntax(wpa);
                  wpaOk = true;
               } else {
                  cancelled = true;
               }
            } catch (AppException var8) {
               this.showErrorMessage(var8.getUserMessage());
            }
         }

         if (cancelled) {
            this.jFmslCheckBoxMenuItem.setSelected(false);
         } else {
            this.customSsid = ssid;
            this.customKey = wpa;
            this.showWpaKeyControls(false);
            this.jModeComboBox.setSelectedItem(FRCRadio.Mode.BRIDGE.getLongString());
            this.jModeComboBox.setEnabled(false);
            this.jFirewallCheckbox.setSelected(true);
            this.jBWLimitCheckbox.setSelected(true);
         }
      } else {
         this.showWpaKeyControls(true);
         this.jModeComboBox.setEnabled(true);
      }
   }

   private void jLoadFirmwareButton1ActionPerformed(ActionEvent evt) {
      FRCRadio radio = RadioFactory.getInstance().getRadio(0, "");
      RadioFirmwareLoadSequence.RunFirmwareLoadSequence(radio, this.host, this, new SequenceProgressDialog(this, true));
   }

   private void jHelpMenuItemActionPerformed(ActionEvent evt) {
      JLabel helpLink = new JLabel();
      helpLink.setText(
         "<html><a href=\"https://frc-docs.readthedocs.io/en/latest/docs/getting-started/getting-started-frc-control-system/radio-programming.html\">ScreenSteps Radio Configuration Documentation</a></html>"
      );
      KioskUtilities.addWebLinkListener(
         helpLink, "https://frc-docs.readthedocs.io/en/latest/docs/getting-started/getting-started-frc-control-system/radio-programming.html"
      );
      JOptionPane.showMessageDialog(this, helpLink, "About", -1);
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
            FRC_Team_Kiosk.logger.info("Starting kiosk...");
            FRC_Team_Kiosk theGui = new FRC_Team_Kiosk();
            theGui.setVisible(true);
            theGui.startup();
         }
      });
   }
}
