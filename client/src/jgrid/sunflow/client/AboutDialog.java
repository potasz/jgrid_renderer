package jgrid.sunflow.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JButton;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * The About dialog diplaying basic informations like client version.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1.2
 */
public class AboutDialog extends JDialog {

    Timer animation;

    private JPanel wavesPanel;

    private JLabel jLabel1;

    private JLabel jLabel2;

    private JLabel jLabel3;
    private JButton jButton1;

    public AboutDialog(Frame owner, boolean modal) {
        super(owner, modal);

        initComponents();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation((int)((screen.width - getWidth()) / 2), (int)((screen.getHeight() - getHeight()) / 2));

        initAnimation();
    }

    private void initComponents() {
        this.setTitle("About");
        this.setResizable(false);
        this.setSize(413, 267);

        wavesPanel = new CurvesPanel();
        GridBagLayout jPanel1Layout = new GridBagLayout();
        getContentPane().add(wavesPanel, BorderLayout.CENTER);
        wavesPanel.setLayout(jPanel1Layout);
        wavesPanel.setPreferredSize(new java.awt.Dimension(405, 211));

        jLabel1 = new JLabel();
        jLabel1.setFont(new java.awt.Font("Tahoma",1,18));
        jLabel1.setForeground(new java.awt.Color(254,254,254));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Distributed rendering client");
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        wavesPanel.add(jLabel1, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.2, GridBagConstraints.SOUTH, GridBagConstraints.NONE, new Insets(12, 0, 0, 0), 0, 0));

        jLabel2 = new JLabel();
        jLabel2.setForeground(new java.awt.Color(254,254,254));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("<html><p align=\"center\">This is an open source application licensed under Apache license version 2.0.<br><br>Author: Szabolcs P&oacute;ta (szabolcs.pota@gmail.com)<br>Web: <strong>http://sfgrid.geneome.net</strong></p></html>"); // NOI18N
        jLabel2.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        wavesPanel.add(jLabel2, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.7, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        jLabel2.setFont(new java.awt.Font("Tahoma",0,11));

        jLabel3 = new JLabel();
        jLabel3.setForeground(new java.awt.Color(254,254,254));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("version 0.1.2, August 2007");
        jLabel3.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        wavesPanel.add(jLabel3, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(6, 0, 30, 0), 0, 0));
        jLabel3.setFont(new java.awt.Font("Tahoma",0,11));
        {
            jButton1 = new JButton();
            wavesPanel.add(jButton1, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 6, 0), 0, 0));
            jButton1.setText("Close");
            jButton1.setFont(new java.awt.Font("Tahoma",0,12));
            jButton1.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    jButton1ActionPerformed(evt);
                }
            });
        }

    }

    /**
     * Start the animation on the frame background.
     */
    private void initAnimation() {
        animation = new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                wavesPanel.repaint();
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                AboutDialog dialog = new AboutDialog(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            animation.start();
        } else {
            animation.stop();
        }
        super.setVisible(b);
    }
    
    private void jButton1ActionPerformed(ActionEvent evt) {
        setVisible(false);
    }

}
