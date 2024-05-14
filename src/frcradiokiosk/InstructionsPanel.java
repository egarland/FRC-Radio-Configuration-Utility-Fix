package frcradiokiosk;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstructionsPanel extends JPanel implements RadioFactory.RadioFactoryChangeListener {
   private static final Logger logger = LoggerFactory.getLogger(InstructionsPanel.class);
   private boolean teamMode;
   private boolean manualWpa = true;
   private JLabel jConfigHeaderLabel;
   private JPanel jConfigPanel;
   private JPanel jDefaultConfigPanel;
   private JPanel jDefaultResetPanel;
   private JLabel jLabel3;
   private JLabel jPicLabel;
   private JPanel jResetPanel;

   public InstructionsPanel(boolean teamMode, boolean useManualWpa) {
      this.initComponents();
      this.teamMode = teamMode;
      RadioFactory.getInstance().registerListener(this);
      this.useManualWpa(this.manualWpa);
   }

   public InstructionsPanel(boolean teamMode) {
      this(teamMode, teamMode);
   }

   @Override
   public void factoryParametersChanged(RadioFactory.RadioOptions radio, FRCRadio.Mode mode) {
      logger.info("Radio Parameter change");
      this.updateInstructions(RadioFactory.getInstance().getRadio(1, ""));
   }

   public final void useManualWpa(boolean manual) {
      this.manualWpa = manual;
      this.updateInstructions(RadioFactory.getInstance().getRadio(1, ""));
   }

   public void updateInstructions() {
      this.updateInstructions(RadioFactory.getInstance().getRadio(1, ""));
   }

   public void updateInstructions(FRCRadio dummyRadio) {
      logger.info("Updating instructions");
      this.jConfigPanel.removeAll();
      this.jResetPanel.removeAll();
      BufferedImage radioPic = dummyRadio.getImage();
      this.jPicLabel.setIcon(new ImageIcon(radioPic));
      String[] radioSpecific = dummyRadio.getConfigInstructions();

      for (String string : radioSpecific) {
         JLabel label = new JLabel(string);
         label.setFont(new Font(label.getFont().getName(), 0, this.teamMode ? 12 : 18));
         this.jConfigPanel.add(label);
      }

      JLabel stepN1 = new JLabel(radioSpecific.length + 1 + ") Enter your team number" + (this.manualWpa ? ", and a WPA key (optional)," : "") + " above");
      JLabel stepN2 = new JLabel(radioSpecific.length + 2 + ") Press \"Configure\", the process should take 15-60 seconds");
      stepN1.setFont(new Font(stepN1.getFont().getName(), 0, this.teamMode ? 12 : 18));
      stepN2.setFont(new Font(stepN2.getFont().getName(), 0, this.teamMode ? 12 : 18));
      this.jConfigPanel.add(stepN1);
      this.jConfigPanel.add(stepN2);
      this.jConfigPanel.revalidate();

      for (String string : dummyRadio.getResetInstructions()) {
         JLabel label = new JLabel(string);
         label.setFont(new Font(label.getFont().getName(), 0, this.teamMode ? 12 : 14));
         this.jResetPanel.add(label);
      }

      this.jConfigHeaderLabel.setFont(new Font(this.jConfigHeaderLabel.getFont().getName(), 1, this.teamMode ? 16 : 24));
      this.jResetPanel.revalidate();
      this.repaint();
   }

   private void initComponents() {
      this.jDefaultConfigPanel = new JPanel();
      this.jDefaultResetPanel = new JPanel();
      this.jConfigPanel = new JPanel();
      this.jResetPanel = new JPanel();
      this.jLabel3 = new JLabel();
      this.jConfigHeaderLabel = new JLabel();
      this.jPicLabel = new JLabel();
      this.setLayout(new GridBagLayout());
      this.jDefaultConfigPanel.setLayout(new BoxLayout(this.jDefaultConfigPanel, 3));
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.anchor = 21;
      this.add(this.jDefaultConfigPanel, gridBagConstraints);
      this.jDefaultResetPanel.setLayout(new BoxLayout(this.jDefaultResetPanel, 3));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.anchor = 21;
      this.add(this.jDefaultResetPanel, gridBagConstraints);
      this.jConfigPanel.setLayout(new BoxLayout(this.jConfigPanel, 1));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.insets = new Insets(0, 10, 0, 10);
      this.add(this.jConfigPanel, gridBagConstraints);
      this.jResetPanel.setLayout(new BoxLayout(this.jResetPanel, 1));
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.insets = new Insets(0, 10, 0, 10);
      this.add(this.jResetPanel, gridBagConstraints);
      this.jLabel3.setFont(new Font("Tahoma", 1, 16));
      this.jLabel3.setHorizontalAlignment(0);
      this.jLabel3.setText("If asked to reset your wireless bridge:");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 1;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = 1;
      gridBagConstraints.anchor = 21;
      gridBagConstraints.insets = new Insets(15, 10, 0, 10);
      this.add(this.jLabel3, gridBagConstraints);
      this.jConfigHeaderLabel.setFont(new Font("Tahoma", 1, 16));
      this.jConfigHeaderLabel.setHorizontalAlignment(0);
      this.jConfigHeaderLabel.setText("To program your wireless bridge:");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.anchor = 21;
      gridBagConstraints.insets = new Insets(15, 10, 0, 10);
      this.add(this.jConfigHeaderLabel, gridBagConstraints);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridwidth = 2;
      this.add(this.jPicLabel, gridBagConstraints);
   }
}
