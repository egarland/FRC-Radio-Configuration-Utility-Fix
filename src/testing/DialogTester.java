package testing;

import frcradiokiosk.HostException;
import frcradiokiosk.IKioskShell;
import frcradiokiosk.NicDialog;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.GroupLayout.Alignment;
import javax.swing.UIManager.LookAndFeelInfo;

public class DialogTester extends JFrame implements IKioskShell {
   private JButton jButton1;

   public DialogTester() {
      this.initComponents();
   }

   private void initComponents() {
      this.jButton1 = new JButton();
      this.setDefaultCloseOperation(3);
      this.jButton1.setText("Show nic dialog");
      this.jButton1.addActionListener(new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent evt) {
            DialogTester.this.jButton1ActionPerformed(evt);
         }
      });
      GroupLayout layout = new GroupLayout(this.getContentPane());
      this.getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup().addGap(147, 147, 147).addComponent(this.jButton1).addContainerGap(148, 32767))
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(Alignment.LEADING)
            .addGroup(layout.createSequentialGroup().addGap(115, 115, 115).addComponent(this.jButton1).addContainerGap(162, 32767))
      );
      this.pack();
   }

   private void jButton1ActionPerformed(ActionEvent evt) {
      String[] nics = new String[]{"nic1", "nic2", "nic3"};
      if (nics != null) {
         String name = NicDialog.showNicDialog(this, this, nics);
         if (name != null) {
            this.setSelectedNic(name);
         }
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
         Logger.getLogger(DialogTester.class.getName()).log(Level.SEVERE, null, var5);
      } catch (InstantiationException var6) {
         Logger.getLogger(DialogTester.class.getName()).log(Level.SEVERE, null, var6);
      } catch (IllegalAccessException var7) {
         Logger.getLogger(DialogTester.class.getName()).log(Level.SEVERE, null, var7);
      } catch (UnsupportedLookAndFeelException var8) {
         Logger.getLogger(DialogTester.class.getName()).log(Level.SEVERE, null, var8);
      }

      EventQueue.invokeLater(new Runnable() {
         @Override
         public void run() {
            new DialogTester().setVisible(true);
         }
      });
   }

   @Override
   public void showErrorMessage(String message) {
      JOptionPane.showMessageDialog(this, message, "Error", 0);
   }

   @Override
   public void showSuccessMessage(String message) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public void logAttempt(int team, boolean pass, String message) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public String[] getNicNames() throws HostException {
      throw new HostException("test exception");
   }

   @Override
   public void setSelectedNic(String name) {
      throw new UnsupportedOperationException("Not supported yet.");
   }
}
