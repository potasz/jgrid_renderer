package jgrid.sunflow.client;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileFilter;

import jgrid.sunflow.data.RequiredFile;
import jgrid.sunflow.renderer.GridRenderer;
import jgrid.sunflow.renderer.RendererTypes;

import org.sunflow.core.Display;
import org.sunflow.image.Color;
import org.sunflow.system.ImagePanel;
import org.sunflow.system.UI;
import org.sunflow.system.UserInterface;
import org.sunflow.system.UI.Module;
import org.sunflow.system.UI.PrintLevel;

import com.nilo.plaf.nimrod.NimRODLookAndFeel;

/**
 * The main class implementing the client GUI.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1
 */
public class RendererGUI extends JFrame implements Display, UserInterface {

    ImagePanel imagePanel;

    private JMenuBar menuBar;

    private JMenu fileMenu;
    
    private JMenu renderMenu;
    
    private JMenu windowMenu;

    GridRenderer renderer;

    FileOpenAction fileOpenAction = new FileOpenAction("Open");

    RequiredFilesAction requiredFilesAction = new RequiredFilesAction("Add required files");

    AnimationFilesAction animFilesAction = new AnimationFilesAction("Add animation files");
    
    SaveAction saveAction = new SaveAction("Save image");
    private JLabel serviceNumberLabel;
    private JProgressBar renderProgressBar;
    private JLabel renderStatusLabel;
    private JPanel statusPanel;

    RenderAction simpleRenderAction = new RenderAction("Simple", RendererTypes.SIMPLE_RENDERER);

    RenderAction bucketRenderAction = new RenderAction("Bucket", RendererTypes.BUCKET_RENDERER);

    RenderAnimationAction renderAnimationAction = new RenderAnimationAction("Animation");
    
    JCheckBoxMenuItem animationMenuItem = new JCheckBoxMenuItem(renderAnimationAction);

    LogDialogAction logDialogAction = new LogDialogAction("Log panel");
    
    AboutDialogAction aboutDialogAction = new AboutDialogAction("About");
        
    JCheckBoxMenuItem logDialogMenuItem = new JCheckBoxMenuItem(logDialogAction);

    ShowProgressAction showProgressAction = new ShowProgressAction("Show progress");    

    JCheckBoxMenuItem showProgressMenuItem = new JCheckBoxMenuItem(showProgressAction);

    StopRenderingAction stopAction = new StopRenderingAction("Stop");
    
    ExitAction exitAction = new ExitAction("Exit");
    
    BenchmarkAction benchmarkAction = new BenchmarkAction("Benchmark services");
    
    BasicLafAction basicLafAction = new BasicLafAction("Default L&F");
    
    JCheckBoxMenuItem basicLafMenuItem = new JCheckBoxMenuItem(basicLafAction);    

    NimrodLafAction nimrodLafAction = new NimrodLafAction("Nimrod L&F");
    
    JCheckBoxMenuItem nimrodLafMenuItem = new JCheckBoxMenuItem(nimrodLafAction);    
    
    private RequiredFileDialog requiredDialog;

    private RequiredFileDialog animFilesDialog;
    
    RequiredFile mainFile;

    RequiredFile[] reqFiles;
    
    RequiredFile[] animFiles;

    private boolean inRendering = false;

    private String currentProgressString;
    private JSeparator jSeparator2;
    private JSeparator jSeparator1;

    protected int currentTaskLastP = 0;

    static JFileChooser fileChooser = new JFileChooser();

    static File baseDir = null;

    static boolean wasFileSelection = false;
    
    static boolean somethingToSave = false;
    
    private LogDialog logDialog;
    
    private AboutDialog aboutDialog;
    
    private FileFilter scFileFilter = new SCFileFilter();

    private String renderStatusText = "";
    
    static FileFilter defaultFileFilter = null;

    ArrayList<UpdateData> updates = new ArrayList<UpdateData>();
    
    DateFormat dateFormat = DateFormat.getDateTimeInstance();
    
    PrintWriter out = null;
    
    Thread imageLoader;
    
    private static int BASIC_LAF = 0;
    
    private static int NIMROD_LAF = 1;
    
    private int selectedLaf = BASIC_LAF;
    
    /**
     * Stores images update data to enable asynchronous update.
     */
    class UpdateData {
        final int x;
        final int y;
        final int w;
        final int h;
        final Color[] data;
        final float[] alpha;


        public UpdateData(final int x, final int y, final int w, final int h, final Color[] data, final float[] alpha) {
            super();
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.data = data;
            this.alpha = alpha;
        }
        
        
    }
    
    /**
     * Default contructor.
     * 
     * @param configFile the Jini configuration file
     */
    public RendererGUI(String configFile) {
        super("Sunflow on JGrid");
        
        try {
            out = new PrintWriter(new FileWriter("client.log", true), true);
            String currDate = dateFormat.format(new Date());
            out.println("-----------------------------------------------------------");
            out.println("GUI Client started at " + currDate);
            out.println();
        } catch (IOException e1) {
            System.err.println("Cannot open log file.");
            e1.printStackTrace();
        }
        
        logDialog = new LogDialog(this, false);
        aboutDialog = new AboutDialog(this, true);
        
        UI.set(this);
        try {
            renderer = new GridRenderer(configFile);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        init();
        Dimension dim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point((dim.width - getWidth())/2, (dim.height - getHeight())/2));
        
        String bd = System.getProperty("file.basedir");
        if (bd == null) {
            JOptionPane.showMessageDialog(this,
                                          "The root directory for file download is not set!\n"+
                                          "Please set it with the \"file.basedir\" System property.",
                                          "Initialization error",
                                          JOptionPane.WARNING_MESSAGE);
        } else {        
            baseDir = new File(bd);
            if (!baseDir.exists()) {
                JOptionPane.showMessageDialog(this,
                                              "The root directory for file download points to a non existing directory!\n"+
                                              "Please set it correctly with the \"file.basedir\" System property.",
                                              "Initialization error",
                                              JOptionPane.WARNING_MESSAGE);            
            } else {
                baseDirectorySet();
            } 
        }
        
        try {
            imageLoader.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setVisible(true);
    }

    /**
     * A File filter to show only the Sunflow .sc files.
     *
     */
    class SCFileFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory() || f.getName().toLowerCase().endsWith(".sc")) {
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public String getDescription() {
            return "Sunflow .SC files";
        }            
    }
    
    /**
     * The action to open main files.
     * 
     */
    class FileOpenAction extends AbstractAction {

        public FileOpenAction(String label) {
            super(label);
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            if (!wasFileSelection) {
                fileChooser.setCurrentDirectory(baseDir);
                wasFileSelection = true;
            }
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setDialogTitle("Open main file");
            fileChooser.setFileFilter(scFileFilter);
            int res = fileChooser.showOpenDialog(RendererGUI.this);
            if (res == JFileChooser.APPROVE_OPTION) {
                int res2 = JOptionPane.showConfirmDialog(RendererGUI.this, "Use cached file if available?", "Caching", JOptionPane.YES_NO_OPTION);
                boolean forceDowload = (res2 == JOptionPane.YES_OPTION) ? false : true;                
                try {
                    mainFile = new RequiredFile(getRelativePath(fileChooser.getSelectedFile()), forceDowload);
                    fileOpened();
                    setTitle("Sunflow on JGrid - " + fileChooser.getSelectedFile().getName());
                    JOptionPane.showMessageDialog(RendererGUI.this, "File " + fileChooser.getSelectedFile().getName() + " opened successfully.", "File open", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(RendererGUI.this, ex.getMessage(), "File open error", JOptionPane.ERROR_MESSAGE);
                } 
            }
        }
    }

    /**
     * The action to open the required files dialog.
     *
     */
    class RequiredFilesAction extends AbstractAction {

        public RequiredFilesAction(String label) {
            super(label);
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            requiredDialog.setVisible(true);
            reqFiles = requiredDialog.getRequiredFiles();
            requiredFilesAdded();
        }
    }
    
    /**
     * The action to open the animation files dialog.
     *
     */   
    class AnimationFilesAction extends AbstractAction {

        public AnimationFilesAction(String label) {
            super(label);
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
            setEnabled(true);
        }

        public void actionPerformed(ActionEvent e) {
            animFilesDialog.setVisible(true);
            animFiles = animFilesDialog.getRequiredFiles();
            animationFilesAdded();
        }
    }    

    /**
     * The action to save the current image
     *
     */
    class SaveAction extends AbstractAction {

        public SaveAction(String label) {
            super(label);
            setEnabled(false);
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle("Save rendered image");
            int res = fileChooser.showSaveDialog(RendererGUI.this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file.exists()) {
                    int res2 = JOptionPane.showConfirmDialog(RendererGUI.this, "File already exists. Do you wish to override?");
                    if (res2 == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
                try {
                    imagePanel.save(file.getAbsolutePath());
                    JOptionPane.showMessageDialog(RendererGUI.this, "Image " + file.getName() + " was saved successfully.", "File save", JOptionPane.INFORMATION_MESSAGE);
                    imageSaved();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(RendererGUI.this, ex.getMessage(), "File save error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * The action to start rendering.
     *
     */
    class RenderAction extends AbstractAction {

        String rendererType;
        public RenderAction(String label, String type) {
            super(label);
            this.rendererType = type;
            if (type.equals( RendererTypes.BUCKET_RENDERER )) {
                putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_MASK));
            } else {
                putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_MASK));
            }
            setEnabled(false);           
        }

        public void actionPerformed(ActionEvent e) {
            
            if (renderer.getWorkerNumber() == 0) {
                JOptionPane.showMessageDialog(RendererGUI.this, "No Compute Service available.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }  
            
            new Thread( new Runnable() {

                public void run() {
                    try {                      
                        renderingStarted();
                        renderer.setMainFile(baseDir.getAbsolutePath(), mainFile);
                        renderer.setRequiredFiles(reqFiles);                        
                        renderer.setRendererType(rendererType);
                        if (animationMenuItem.isSelected()) {
                            renderer.setAnimationFiles(animFiles);
                        } else {
                            renderer.setAnimationFiles(null);
                        }
                        renderer.render(RendererGUI.this);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        renderingFinished();
                        JOptionPane.showMessageDialog(RendererGUI.this, "Rendering finished. Render time: " + renderer.getRenderTime() + ".", "Rendering finished", JOptionPane.INFORMATION_MESSAGE);                        
                    }
                }                      
            }).start();
        }
    }    

    /**
     * The action to toggle animation mode
     *
     */
    class RenderAnimationAction extends AbstractAction {

        public RenderAnimationAction(String label) {
            super(label);
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent e) {
            
            if (animFiles == null || animFiles.length == 0) {
                JOptionPane.showMessageDialog(RendererGUI.this, "There are no animation files. Please add the files before switching to animation mode.", "Warning", JOptionPane.WARNING_MESSAGE);
                animationMenuItem.setSelected(false);
            }           
        }
    }
    
    /**
     * The action to stop the current animation process.
     *
     */
    class StopRenderingAction extends AbstractAction {

        public StopRenderingAction(String label) {
            super(label);
            setEnabled(false);
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));            
        }

        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    renderer.stopRendering();                    
                }                               
            });
        }
    }     
   
    /**
     * The action to exit the application
     *
     */
    class ExitAction extends AbstractAction {

        public ExitAction(String label) {
            super(label);
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));            
        }

        public void actionPerformed(ActionEvent e) {
            if (inRendering) {
                int res = JOptionPane.showConfirmDialog(RendererGUI.this, "Rendering in progress. Do you wish to stop it?", "Warning", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    try {
                        renderer.cancel();
                    } catch (RemoteException ignore) {}                    
                } else {
                    return;
                }
            }            
            if (somethingToSave) {
                int res = JOptionPane.showConfirmDialog(RendererGUI.this, "Current image is unsaved. Do you wish to save it now?", "Warning", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                        saveAction.actionPerformed(e);
                }
            }
            RendererGUI.this.dispose();
            if (out != null) {
                String currDate = dateFormat.format(new Date());
                out.println("GUI Client closed at " + currDate);
                out.println();
                out.close();
            }
            System.exit(0);
        }
    }  

    /**
     * The action to open the log dialog.
     *
     */
    class LogDialogAction extends AbstractAction {

        public LogDialogAction(String label) {
            super(label);
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_MASK));
        }

        public void actionPerformed(ActionEvent e) {
            logDialog.setVisible(!logDialog.isVisible());
        }
    }

    /**
     * The action to select the default L&F.
     *
     */
    class BasicLafAction extends AbstractAction {

        public BasicLafAction(String label) {
            super(label);
        }

        public void actionPerformed(ActionEvent e) {
            if (selectedLaf == NIMROD_LAF) {
                try {
                    UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
                    SwingUtilities.updateComponentTreeUI(RendererGUI.this);
                    SwingUtilities.updateComponentTreeUI(animFilesDialog);
                    SwingUtilities.updateComponentTreeUI(requiredDialog);
                    SwingUtilities.updateComponentTreeUI(fileChooser);
                    SwingUtilities.updateComponentTreeUI(aboutDialog);
                    SwingUtilities.updateComponentTreeUI(logDialog);
                    selectedLaf = BASIC_LAF;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }       
            }
            basicLafMenuItem.setSelected(true);
            nimrodLafMenuItem.setSelected(false);
        }
    }
    
    /**
     * The action to select the default L&F.
     *
     */
    class NimrodLafAction extends AbstractAction {

        public NimrodLafAction(String label) {
            super(label);
        }

        public void actionPerformed(ActionEvent e) {
            if (selectedLaf == BASIC_LAF) {
                try {
                    LookAndFeel laf = new NimRODLookAndFeel();
                    UIManager.setLookAndFeel(laf);
                    SwingUtilities.updateComponentTreeUI(RendererGUI.this);
                    SwingUtilities.updateComponentTreeUI(animFilesDialog);
                    SwingUtilities.updateComponentTreeUI(requiredDialog);
                    SwingUtilities.updateComponentTreeUI(fileChooser);
                    SwingUtilities.updateComponentTreeUI(aboutDialog);
                    SwingUtilities.updateComponentTreeUI(logDialog);
                    selectedLaf = NIMROD_LAF;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    basicLafMenuItem.setSelected(true);
                    nimrodLafMenuItem.setSelected(false);
                }       
            }
            basicLafMenuItem.setSelected(false);
            nimrodLafMenuItem.setSelected(true);
        }
    }
    
    /**
     * The action to open the log dialog.
     *
     */
    class AboutDialogAction extends AbstractAction {

        public AboutDialogAction(String label) {
            super(label);            
        }

        public void actionPerformed(ActionEvent e) {
            aboutDialog.setVisible(true);
        }
    }
    
    /**
     * The action to toggle betweem modes to show or hide rendering progress.
     *
     */
    class ShowProgressAction extends AbstractAction {

        public ShowProgressAction(String label) {
            super(label);
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK));
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    /**
     * The action to stop the current animation process.
     *
     */
    class BenchmarkAction extends AbstractAction {

        public BenchmarkAction(String label) {
            super(label);
//            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));            
        }

        public void actionPerformed(ActionEvent e) {
            Thread t = new Thread() {
                public void run() {
                    BenchmarkAction.this.setEnabled(false);
                    try {
                        renderStatusLabel.setText("Benchmarking services...");
                        renderer.doBenchmark();
                    } finally {
                        BenchmarkAction.this.setEnabled(true);
                        renderStatusLabel.setText("Benchmark finished.");
                        JOptionPane.showMessageDialog(RendererGUI.this, "Benchmark finished. See the output in the Log dialog.", "Benchmark finished", JOptionPane.INFORMATION_MESSAGE);
                    }
                }                               
            };
            t.start();
        }
    } 

    /**
     * Initalizes the GUI
     */
    public void init() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                    exitAction.actionPerformed(null);
            }
        });
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        
        defaultFileFilter = fileChooser.getFileFilter();
        
        requiredDialog = new RequiredFileDialog(RendererGUI.this, "Manage required files", true, defaultFileFilter);
        animFilesDialog = new RequiredFileDialog(RendererGUI.this, "Manage animation files", true, scFileFilter);

        GridBagLayout thisLayout = new GridBagLayout();
        getContentPane().setLayout(thisLayout);
        {
        
            imagePanel = new ImagePanel();
            loadMainImage();
            getContentPane().add(imagePanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        }
        {
            statusPanel = new JPanel();
            GridBagLayout statusPanelLayout = new GridBagLayout();
            statusPanelLayout.rowWeights = new double[] {0.1};
            statusPanelLayout.rowHeights = new int[] {7};
            statusPanelLayout.columnWeights = new double[] {0.1, 0.1, 0.1};
            statusPanelLayout.columnWidths = new int[] {7, 7, 7};
            statusPanel.setLayout(statusPanelLayout);
            getContentPane().add(statusPanel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(6, 0, 0, 0), 0, 0));
            statusPanel.setPreferredSize(new java.awt.Dimension(10, 30));
            statusPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            {
                renderStatusLabel = new JLabel();
                statusPanel.add(renderStatusLabel, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 6, 0, 0), 0, 0));
                renderStatusLabel.setText("No rendering yet");
                renderStatusLabel.setPreferredSize(new java.awt.Dimension(148, 18));
                renderStatusLabel.setMinimumSize(new java.awt.Dimension(148, 18));
            }
            {
                renderProgressBar = new JProgressBar();
                statusPanel.add(renderProgressBar, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 6, 0, 0), 0, 0));
                renderProgressBar.setPreferredSize(new java.awt.Dimension(148, 18));
                renderProgressBar.setSize(148, 18);
                renderProgressBar.setFocusCycleRoot(true);
                renderProgressBar.setMinimumSize(new java.awt.Dimension(148, 18));
            }
            {
                serviceNumberLabel = new JLabel();
                statusPanel.add(serviceNumberLabel, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 6, 0, 6), 0, 0));
                serviceNumberLabel.setText("Available services: 0");
            }
            {
                jSeparator1 = new JSeparator();
                statusPanel.add(jSeparator1, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.VERTICAL, new Insets(2, 6, 2, 0), 0, 0));
                jSeparator1.setOrientation(SwingConstants.VERTICAL);
            }
            {
                jSeparator2 = new JSeparator();
                statusPanel.add(jSeparator2, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(2, 6, 2, 0), 0, 0));
                jSeparator2.setOrientation(SwingConstants.VERTICAL);
            }
        }
        {
            menuBar = new JMenuBar();
            setJMenuBar(menuBar);
            {
                fileMenu = new JMenu();
                menuBar.add(fileMenu);
                fileMenu.setText("File");
                {
                    fileMenu.add(fileOpenAction);
                    fileMenu.add(requiredFilesAction);
                    fileMenu.add(animFilesAction);
                    fileMenu.addSeparator();
                    fileMenu.add(saveAction);
                    fileMenu.addSeparator();
                    fileMenu.add(exitAction);
                }
                
                renderMenu = new JMenu();
                renderMenu.setText("Rendering");
                menuBar.add(renderMenu);
                {
                    renderMenu.add(simpleRenderAction);
                    renderMenu.add(bucketRenderAction);
//                    renderMenu.add(renderAnimationAction);
                    renderMenu.add(animationMenuItem);
                    renderMenu.addSeparator();
                    renderMenu.add(stopAction);
                }
                windowMenu = new JMenu();
                menuBar.add(windowMenu);
                windowMenu.setText("Window");
                {                
                    windowMenu.add(logDialogMenuItem);
                    showProgressMenuItem.setSelected(true);
                    windowMenu.add(showProgressMenuItem);
                    windowMenu.addSeparator();
                    windowMenu.add(benchmarkAction);
                    windowMenu.addSeparator();
                    windowMenu.add(basicLafMenuItem);
                    windowMenu.add(nimrodLafMenuItem);
                    basicLafMenuItem.setSelected(false);
                    nimrodLafMenuItem.setSelected(true);
                    windowMenu.addSeparator();
                    windowMenu.add(aboutDialogAction);
                }
            }
        }
        pack();
    }

    // Methods to implement the Display interface
    
    public void imageBegin(int w, int h, int bucketSize) {
        //imageFill(0, 0, w, h, Color.BLACK);
        imagePanel.imageBegin(w, h, bucketSize);
    }

    public void imagePrepare(final int x, final int y, final int w, final int h, final int id) {
        if (showProgressMenuItem.isSelected()) {
            SwingUtilities.invokeLater(new Runnable() {
    
                public void run() {
                    imagePanel.imagePrepare(x, y, w, h, id);
                }            
            });
        }
    }

    public void imageUpdate(final int x, final int y, final int w, final int h, final Color[] data, final float[] alpha) {
        if (showProgressMenuItem.isSelected()) {
            SwingUtilities.invokeLater(new Runnable() {
    
                public void run() {
                    for (UpdateData u : updates) {
                        imagePanel.imageUpdate(u.x, u.y, u.w, u.h, u.data, u.alpha);
                    }
                    updates.clear();
                    imagePanel.imageUpdate(x, y, w, h, data, alpha);
                }            
            });
        } else {
            updates.add(new UpdateData(x, y, w, h, data, alpha));
        }
    }

    public void imageFill(int x, int y, int w, int h, Color c, float alpha) {
        imagePanel.imageFill(x, y, w, h, c, alpha);
    }

    public void imageEnd() {
        SwingUtilities.invokeLater(new Runnable() {    
            public void run() {
                for (UpdateData u : updates) {
                    imagePanel.imageUpdate(u.x, u.y, u.w, u.h, u.data, u.alpha);
                }
                imagePanel.imageEnd();
                updates.clear();
            }            
        });        
    }

    // Methods called by the actions on different events.
    
    public void fileOpened() {
        simpleRenderAction.setEnabled(true);
        bucketRenderAction.setEnabled(true);
        renderAnimationAction.setEnabled(true);
    }
    
    public void requiredFilesAdded() {
        
    }

    public void animationFilesAdded() {
        
    }
    
    public void baseDirectorySet() {
        fileOpenAction.setEnabled(true);
        requiredFilesAction.setEnabled(true);
        wasFileSelection = false;
    }
    
    private void imageSaved() {
        somethingToSave = false;
    }

    public void renderingStarted() {        
        inRendering  = true;
        saveAction.setEnabled(true);
        simpleRenderAction.setEnabled(false);
        bucketRenderAction.setEnabled(false);
        renderAnimationAction.setEnabled(false);
        stopAction.setEnabled(true);
        renderStatusText = "Rendering started...";
        renderStatusLabel.setText(renderStatusText);
    }
    
    public void renderingFinished() {
        inRendering = false;
        somethingToSave = true;
        simpleRenderAction.setEnabled(true);
        bucketRenderAction.setEnabled(true);
        renderAnimationAction.setEnabled(true);
        stopAction.setEnabled(false);
        renderStatusLabel.setText("Rendering finished in " + renderer.getRenderTime());
    }
    
    public void setLogDialogSelected(boolean isSelected) {
        logDialogMenuItem.setSelected(isSelected);
    }
    
    /**
     * Utility method to get the path of File <code>f</code> relative to the base directory.
     * @param f a File 
     * @return the relative path
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public static String getRelativePath(File f) throws IllegalArgumentException, IOException {
        if (baseDir == null) {
            return f.getAbsolutePath().replace("\\", "/");
        } else { 
            String fPath = f.getAbsolutePath();
            String bdPath = baseDir.getCanonicalPath();
    
            fPath = fPath.toLowerCase();
            bdPath = bdPath.toLowerCase();
    
            if (fPath.startsWith(bdPath)) {
                String path = f.getAbsolutePath().substring(baseDir.getCanonicalPath().length() + 1);
                path = path.replace("\\", "/");
                return path;
            }  else {
                throw new IllegalArgumentException("File " + f.getName() + " is not within the base directory. Basedir path: " + bdPath + ". File path: " + fPath);
            }
        }
    }
    

    /* (non-Javadoc)
     * @see org.sunflow.system.UserInterface#print(org.sunflow.system.UI.Module, org.sunflow.system.UI.PrintLevel, java.lang.String)
     */
    public void print(Module m, PrintLevel level, String s) {        
        if (s != null) {
            if (s.startsWith("Available workers")) {
                serviceNumberLabel.setText(s);
            } else if (s.startsWith("Rendering of frame")) {
                renderStatusLabel.setText(s);
                renderStatusText  = s;
            } else if (s.startsWith("Active workers")) {
                renderStatusLabel.setText(renderStatusText + " (" + s + ")");
            } else {
                logDialog.append("[" + m.toString() + "]\t" + s);
            }
        }
        if (out != null) {
            String currentDate = dateFormat.format(new Date());
            out.println("[" + m.toString() + "]\t" + currentDate + "\t" + s);
        }
    }

    /* (non-Javadoc)
     * @see org.sunflow.system.UserInterface#taskStart(java.lang.String, int, int)
     */
    public void taskStart(String s, int min, int max) {        
        renderProgressBar.setMinimum(min);
        renderProgressBar.setMaximum(max);
        renderProgressBar.setStringPainted(true);
        currentProgressString = s; 
        renderProgressBar.setString(s);
        renderProgressBar.setValue(min);
        currentTaskLastP = -1;
    }

    /* (non-Javadoc)
     * @see org.sunflow.system.UserInterface#taskStop()
     */
    public void taskStop() {        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                renderProgressBar.setValue(renderProgressBar.getMinimum());
                renderProgressBar.setString("");
                renderProgressBar.setEnabled(false);
            }
        });
    }

    /* (non-Javadoc)
     * @see org.sunflow.system.UserInterface#taskUpdate(int)
     */
    public void taskUpdate(int current) {
        final int taskCurrent = current;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                renderProgressBar.setValue(taskCurrent);
                int p = (int) (100.0 * renderProgressBar.getPercentComplete());
                if (p > currentTaskLastP ) {
                    renderProgressBar.setString(currentProgressString + " [" + p + "%]");
                    currentTaskLastP = p;
                }
            }
        });
    }
        
    private void loadMainImage() {
        imageLoader = new Thread(new Runnable() {
            public void run() {
                try {
                    BufferedImage img = ImageIO.read(getClass().getResource("/resources/maintitle.png"));            
                    int width = img.getWidth();
                    int height = img.getHeight();
                    Color[] pixels = new Color[width * height];
                    float[] alpha = new float[width * height];
                    for (int i = 0; i < width; i++) {
                        for (int j = 0; j < height; j++) {
                            pixels[j * width + i] = new Color(img.getRGB(i, j));
                            alpha[j * width + i] = 1.0f;
                        }
                    }
                    imagePanel.imageBegin(width, height, 32);
                    imagePanel.imageUpdate(0, 0, width, height, pixels, alpha);
                    imagePanel.imageEnd();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        imageLoader.start();        
    }
    
    /**
     * The main method.
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Please specify a config file.");
        }
        try {
            UIManager.setLookAndFeel( new NimRODLookAndFeel() );
        } catch (Exception ex) {
            ex.printStackTrace();
        }       
             
        RendererGUI gui = new RendererGUI(args[0]);
    }
    
}