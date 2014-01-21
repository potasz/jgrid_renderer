package jgrid.sunflow.client;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A dilaog to siplay log messages.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1
 */
public class LogDialog extends JDialog {
    
    private RendererGUI renderGUI;
    
    public LogDialog(RendererGUI owner, boolean modal) {
        super(owner, modal);
        this.renderGUI = owner;
        initGUI();
        pack();
    }
    
    private void initGUI() {
        try {
            {
                this.setPreferredSize(new Dimension(600, 400));
                this.setTitle("Log panel");
            }
            {
                jScrollPane1 = new JScrollPane();
                getContentPane().add(jScrollPane1, BorderLayout.CENTER);
                jScrollPane1.setPreferredSize(new java.awt.Dimension(300, 300));
                {
                    logTextArea = new JTextArea();
                    jScrollPane1.setViewportView(logTextArea);
                    logTextArea.setText("");
                    logTextArea.setEditable(false);
                }
            }
            {
                buttonPanel = new JPanel();
                GridBagLayout jPanel1Layout = new GridBagLayout();
                getContentPane().add(buttonPanel, BorderLayout.SOUTH);
                buttonPanel.setLayout(jPanel1Layout);
                {
                    clearButton = new JButton();
                    buttonPanel.add(clearButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 6, 6), 0, 0));
                    clearButton.setText("Clear");
                    clearButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            clearButtonActionPerformed(evt);
                        }
                    });
                }
                {
                    hideButton = new JButton();
                    buttonPanel.add(hideButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 6, 6), 0, 0));
                    hideButton.setText("Hide");
                    hideButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            hideButtonActionPerformed(evt);
                        }
                    });
                }
                {
                    jPanel1 = new JPanel();
                    buttonPanel.add(jPanel1, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void clearButtonActionPerformed(ActionEvent evt) {
        logTextArea.setText("");
    }
    
    private void hideButtonActionPerformed(ActionEvent evt) {
        setVisible(false);
        renderGUI.setLogDialogSelected(false);
    }
    
    public void append(String line) {
        logTextArea.append(line + "\n");
        logTextArea.setCaretPosition(logTextArea.getText().length());
    }
    
    private JScrollPane jScrollPane1;
    private JPanel buttonPanel;
    private JButton clearButton;
    private JButton hideButton;
    private JPanel jPanel1;
    private JTextArea logTextArea;

}
