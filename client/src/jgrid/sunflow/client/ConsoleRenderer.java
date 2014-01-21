package jgrid.sunflow.client;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import jgrid.sunflow.data.RequiredFile;
import jgrid.sunflow.renderer.GridRenderer;
import jgrid.sunflow.renderer.RendererTypes;

import org.sunflow.core.display.FileDisplay;
import org.sunflow.core.display.FrameDisplay;
import org.sunflow.system.Timer;

/**
 * The console version of the distributed renderer.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1.1
 */
public class ConsoleRenderer {
    
    private static File baseDir;
    private static String outputFile;

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("USAGE: ConsoleRenderer OUTPUT_IMAGE WORKER_NUM MAIN_FILE <t | f> [REQ_FILE <t | f> ...]\n\n" +
                               "Where t means that the file is cacheable, f is not cacheable.\n" +
			        "File paths should be given relative to the FILE_DOWNLOAD_ROOT_DIR specified in the setenv file.");
            System.exit(1);
        }
        GridRenderer renderer;
        try {
            renderer = new GridRenderer(args[0]);
            outputFile = args[1];
            
            System.out.println("Waiting for services...");
            Thread.sleep(10000);
            
            int workerNum = Integer.parseInt(args[2]);
            
            ArrayList<RequiredFile> reqFiles = new ArrayList<RequiredFile>();
            String filename = args[3];
            boolean cacheable = args[4].equalsIgnoreCase("t");
            RequiredFile mainFile = new RequiredFile(filename, !cacheable);
            System.out.printf("Main file: %s, cacheable: %s.\n", filename, cacheable);
            for (int i = 5; i < args.length; i++) {
                filename = args[i];
                cacheable = true;
                if (++i < args.length) {
                   cacheable = args[i].equalsIgnoreCase("t"); 
                }
                reqFiles.add(new RequiredFile(filename, !cacheable));
                System.out.printf("Required file: %s, cacheable: %s.\n", filename, cacheable);                
            }

            
            RequiredFile[] reqFilesArr = reqFiles.toArray(new RequiredFile[reqFiles.size()]);

            String bd = System.getProperty("file.basedir");
            if (bd == null) {
                System.out.println("The root directory for file download is not set!\n"+
                                   "Please set it with the \"file.basedir\" System property.");
                System.exit(1);
            } else {        
                baseDir = new File(bd);
                if (!baseDir.exists()) {
                    System.out.println("The root directory for file download points to a non existing directory!\n"+
                                       "Please set it correctly with the \"file.basedir\" System property.");
                    System.exit(1);                    
                }
            }

            //            renderer.setMainFile(baseDir.getAbsolutePath(), mainFile);
            renderer.setMainFile(baseDir.getAbsolutePath(), mainFile);
            renderer.setRendererType(RendererTypes.BUCKET_RENDERER);
            renderer.setRequiredFiles(reqFilesArr);
            renderer.setAnimationFiles(null);
            renderer.setMaxWorkerNumber(workerNum);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }        
        renderer.render(new FileDisplay(outputFile));
        try {
            renderer.cancel();
        } catch (RemoteException ignore) {}
        
        System.exit(0);
    }
}
