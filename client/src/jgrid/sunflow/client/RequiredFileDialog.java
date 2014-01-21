package jgrid.sunflow.client;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import jgrid.sunflow.data.RequiredFile;

/**
 * The dialog to manage the set of required files. It is used both to
 * select the external files required for rendering and to select the
 * scene files required for the animation.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1
 */
public class RequiredFileDialog extends JDialog {

    private JButton okButton;

    private JButton removeButton;

    private JTable fileTable;

    private JButton addButton;

    private FileTableModel fileTableModel;

    private JScrollPane tableScrollPane;
    private JButton cachableButton;

    private DefaultCellEditor fileTableCellEditor;

    private FileTableCellRenderer fileTableCellRenderer;
    
    private boolean cacheAll = true;

    private FileFilter fileFilter;
        
    private class FileTableModel extends AbstractTableModel {

        ArrayList<RequiredFile> fileList = new ArrayList<RequiredFile>();

        public void addFiles(RequiredFile[] files) {
            for (int i = 0; i < files.length; i++) {
                if (files[i] != null && !fileList.contains(files[i])) {
                    fileList.add(files[i]);
                }
            }
            fireTableDataChanged();
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "File";
                case 1:
                    return "Cachable";
                default:
                    return "????";
            }
        }

        public int getRowCount() {
            return fileList.size();
        }

        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return fileList.get(row).fileName;
                case 1:
                    return new Boolean(!fileList.get(row).forceDownload);
                default:
                    return "????";
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                boolean forceDownload = !((Boolean)aValue).booleanValue();
                RequiredFile oldItem = fileList.get(rowIndex);
                fileList.set(rowIndex, new RequiredFile(oldItem.fileName, forceDownload));
                System.out.println("Item " + oldItem.fileName + " changed to " + forceDownload);
            }
        }

        public void removeFile(int[] indexes) {
            RequiredFile[] removeFiles = new RequiredFile[indexes.length];
            for (int i = 0; i < indexes.length; i++) {
                removeFiles[i] = fileList.get(indexes[i]);
            }
            for (int i = 0; i < removeFiles.length; i++) {
                fileList.remove(removeFiles[i]);
            }
            fireTableDataChanged();
        }
        
        public RequiredFile[] getFiles() {
            return fileList.toArray(new RequiredFile[fileList.size()]);
        }

        public void clearFiles() {
            fileList.clear();
            fireTableDataChanged();
        }
        
        public void setAllForceDownload(boolean forceDownload) {
            for (int i = 0; i < fileList.size(); i++) {
                RequiredFile oldItem = fileList.get(i);
                fileList.set(i, new RequiredFile(oldItem.fileName, forceDownload));
            }
            fireTableDataChanged();
        }
    }

    private class FileTableCellRenderer extends DefaultTableCellRenderer {

        private JCheckBox checkBox;

        public FileTableCellRenderer(JCheckBox checkBox) {
            this.checkBox = checkBox;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (column == 1) {
                if (isSelected) {
                       checkBox.setForeground(table.getSelectionForeground());
                       checkBox.setBackground(table.getSelectionBackground());
                 } else {
                        checkBox.setForeground(table.getForeground());
                        checkBox.setBackground(table.getBackground());
                 }
                boolean bValue = ((Boolean)value).booleanValue();
                checkBox.setSelected(bValue);
                checkBox.setText(value.toString());
                return checkBox;
            } else {
                return comp;
            }
        }
    }

    
    /**
     * Default constructor.
     * @param owner the owner component of this dilaog
     * @param title the title of the dialog
     * @param modal sets modality
     * @param fileFilter the file fileter to apply in the FileChooser
     * @throws HeadlessException
     */
    public RequiredFileDialog(Frame owner, String title, boolean modal, FileFilter fileFilter) throws HeadlessException {
        super(owner, title, modal);
        this.fileFilter = fileFilter;
        initGUI();
        pack();
        Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point((dim.width - getWidth())/2, (dim.height - getHeight())/2));
    }

    private void initGUI() {
        try {
            {
                GridBagLayout thisLayout = new GridBagLayout();
                thisLayout.rowWeights = new double[] {0.1, 0.1, 0.1, 0.1};
                thisLayout.rowHeights = new int[] {7, 7, 7, 7};
                thisLayout.columnWeights = new double[] {0.1, 0.1};
                thisLayout.columnWidths = new int[] {7, 7};
                getContentPane().setLayout(thisLayout);
                this.setResizable(true);
                                
                {
                    okButton = new JButton();
                    getContentPane().add(okButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 1.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 6, 6), 0, 0));

                    okButton.setText("OK");
                    okButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            okButtonActionPerformed(evt);
                        }
                    });
                }
                {
                    removeButton = new JButton();
                    getContentPane().add(removeButton, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 6), 0, 0));
                    removeButton.setText("Remove");
                    removeButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            removeButtonActionPerformed(evt);
                        }
                    });
                }
                {
                    addButton = new JButton();
                    getContentPane().add(addButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 6), 0, 0));
                    addButton.setText("Add");
                    addButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            addButtonActionPerformed(evt);
                        }
                    });
                }
                {
                    fileTableModel = new FileTableModel();
                    fileTable = new JTable(fileTableModel);
                    fileTableCellEditor = new DefaultCellEditor(new JCheckBox());
                    fileTable.getColumnModel().getColumn(1).setCellEditor(fileTableCellEditor);
                    fileTableCellRenderer = new FileTableCellRenderer(new JCheckBox());
                    fileTable.getColumnModel().getColumn(1).setCellRenderer(fileTableCellRenderer);
                    fileTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

                    tableScrollPane = new JScrollPane();
                    getContentPane().add(tableScrollPane, new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(6, 6, 5, 6), 0, 0));
                    tableScrollPane.getViewport().add(fileTable, null);
                    tableScrollPane.setPreferredSize(new Dimension(400, 300));
                }
                {
                    cachableButton = new JButton();
                    getContentPane().add(cachableButton, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 6), 0, 0));
                    cachableButton.setText("Toggle caching");
                    cachableButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent evt) {
                            cachableButtonActionPerformed(evt);
                        }
                    });                    
                }
                this.setDefaultCloseOperation(HIDE_ON_CLOSE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void cachableButtonActionPerformed(ActionEvent evt) {
        cacheAll = !cacheAll;
        fileTableModel.setAllForceDownload(!cacheAll);        
    }

    private void removeButtonActionPerformed(ActionEvent evt) {
        int[] indexes = fileTable.getSelectedRows();
        fileTableModel.removeFile(indexes);
    }

    private void okButtonActionPerformed(ActionEvent evt) {
        setVisible(false);
    }
    
    private void addButtonActionPerformed(ActionEvent evt) {
        if (!RendererGUI.wasFileSelection) {
            RendererGUI.fileChooser.setCurrentDirectory(RendererGUI.baseDir);
            RendererGUI.wasFileSelection = true;
        }
        RendererGUI.fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        RendererGUI.fileChooser.setMultiSelectionEnabled(true);
        RendererGUI.fileChooser.setDialogTitle("Add required files");
        RendererGUI.fileChooser.setFileFilter(fileFilter);
        int res = RendererGUI.fileChooser.showDialog(RequiredFileDialog.this, "Add");
        
        if (res == JFileChooser.APPROVE_OPTION) {
            File[] files = RendererGUI.fileChooser.getSelectedFiles();
            RequiredFile[] reqFiles = new RequiredFile[files.length];
            for (int i = 0; i < files.length; i++) {
                String relPath = null;
                try {
                    relPath = RendererGUI.getRelativePath(files[i]);
                    reqFiles[i] = new RequiredFile(relPath, false);
                    System.out.println("added req" + reqFiles[i].fileName);
                } catch (Exception e) {
                    reqFiles[i] = null;
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Warning", JOptionPane.ERROR_MESSAGE);
                }
            }
            fileTableModel.addFiles(reqFiles);
        }
    }

    /**
     * @return the selceted files
     */
    public RequiredFile[] getRequiredFiles() {
        return fileTableModel.getFiles();
    }

    /**
     * Clear the set of selected files.
     */
    public void clearFiles() {
        fileTableModel.clearFiles();
    }

}
