package frcradiokiosk;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

public class SequenceProgressDialog extends JDialog implements ISequenceProgressDialog {
   private JButton jAckButton;
   private JPanel jControlsPanel;
   private JLabel jCurrentStepLabel;
   private JPanel jInfoPanel;
   private JProgressBar jProgressBar;
   private JScrollPane jScrollPane1;
   private JTextArea jStepHistoryTextArea;

   public SequenceProgressDialog(Frame parent, boolean modal) {
      super(parent, "Configuration Progress", modal);
      this.initComponents();
      DefaultCaret caret = (DefaultCaret)this.jStepHistoryTextArea.getCaret();
      caret.setUpdatePolicy(2);
      this.jScrollPane1.setAutoscrolls(true);
      this.jProgressBar.setValue(0);
      this.jStepHistoryTextArea.setText("");
      this.jAckButton.setEnabled(false);
      this.setDefaultCloseOperation(0);
      this.setLocationRelativeTo(parent);
   }

   @Override
   public void setCurrentStep(String title, String step) {
      this.jCurrentStepLabel.setText(title);
      if (this.jStepHistoryTextArea.getText().isEmpty()) {
         this.jStepHistoryTextArea.append(step);
      } else {
         this.jStepHistoryTextArea.append("\n" + step);
      }
   }

   @Override
   public void clearMessages() {
      this.jStepHistoryTextArea.setText("");
   }

   @Override
   public void setProgress(int progress) {
      this.jProgressBar.setValue(progress);
   }

   @Override
   public void sequenceFinished() {
      this.jAckButton.setEnabled(true);
   }

   @Override
   public void showDialog() {
      this.getRootPane().setDefaultButton(this.jAckButton);
      this.setVisible(true);
   }

   private void initComponents() {
      this.jInfoPanel = new JPanel();
      this.jScrollPane1 = new JScrollPane();
      this.jStepHistoryTextArea = new JTextArea();
      this.jProgressBar = new JProgressBar();
      this.jCurrentStepLabel = new JLabel();
      this.jControlsPanel = new JPanel();
      this.jAckButton = new JButton();
      this.setDefaultCloseOperation(2);
      this.setMinimumSize(new Dimension(500, 300));
      this.jInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      this.jInfoPanel.setMinimumSize(new Dimension(400, 175));
      this.jInfoPanel.setPreferredSize(new Dimension(400, 175));
      this.jInfoPanel.setLayout(new GridBagLayout());
      this.jScrollPane1.setVerticalScrollBarPolicy(22);
      this.jScrollPane1.setAutoscrolls(true);
      this.jScrollPane1.setMinimumSize(new Dimension(23, 20));
      this.jScrollPane1.setPreferredSize(new Dimension(300, 20));
      this.jStepHistoryTextArea.setColumns(20);
      this.jStepHistoryTextArea.setEditable(false);
      this.jStepHistoryTextArea.setFont(new Font("Monospaced", 0, 15));
      this.jStepHistoryTextArea.setRows(6);
      this.jStepHistoryTextArea.setMinimumSize(new Dimension(80, 20));
      this.jScrollPane1.setViewportView(this.jStepHistoryTextArea);
      GridBagConstraints gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 2;
      gridBagConstraints.fill = 1;
      gridBagConstraints.weightx = 1.0;
      gridBagConstraints.weighty = 1.0;
      this.jInfoPanel.add(this.jScrollPane1, gridBagConstraints);
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 1;
      gridBagConstraints.fill = 2;
      gridBagConstraints.ipadx = 300;
      gridBagConstraints.ipady = 10;
      gridBagConstraints.insets = new Insets(5, 0, 5, 0);
      this.jInfoPanel.add(this.jProgressBar, gridBagConstraints);
      this.jCurrentStepLabel.setFont(new Font("Tahoma", 1, 18));
      this.jCurrentStepLabel.setText("Current Step");
      gridBagConstraints = new GridBagConstraints();
      gridBagConstraints.gridx = 0;
      gridBagConstraints.gridy = 0;
      gridBagConstraints.anchor = 21;
      gridBagConstraints.insets = new Insets(5, 10, 0, 0);
      this.jInfoPanel.add(this.jCurrentStepLabel, gridBagConstraints);
      this.getContentPane().add(this.jInfoPanel, "Center");
      this.jControlsPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
      this.jControlsPanel.setLayout(new FlowLayout(2));
      this.jAckButton.setText("OK");
      this.jAckButton.setMaximumSize(new Dimension(75, 23));
      this.jAckButton.setMinimumSize(new Dimension(75, 23));
      this.jAckButton.setPreferredSize(new Dimension(75, 23));
      this.jAckButton.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            SequenceProgressDialog.this.jAckButtonActionPerformed(evt);
         }
      });
      this.jControlsPanel.add(this.jAckButton);
      this.getContentPane().add(this.jControlsPanel, "South");
      this.pack();
   }

   private void jAckButtonActionPerformed(ActionEvent evt) {
      this.setVisible(false);
      this.dispose();
   }
}
