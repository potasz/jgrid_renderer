package jgrid.sunflow.renderer.task;

import static jgrid.sunflow.renderer.RendererTypes.BUCKET_RENDERER;

import java.io.File;
import java.rmi.RemoteException;
import java.util.EmptyStackException;

import jgrid.sunflow.data.Bucket;
import jgrid.sunflow.data.RequiredFile;
import jgrid.sunflow.renderer.GridRendererMaster;
import jgrid.sunflow.renderer.GridRendererWorker;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Display;
import org.sunflow.core.ImageSampler;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Options;
import org.sunflow.core.Scene;
import org.sunflow.image.Color;

/**
 * The worker task that is spawned to the Compute Service and that eventually renders the
 * buckets allocated by the {@link jgrid.sunflow.renderer.GridRendererMaster GridRendererMaster} process.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1
 */
public class RendererTask extends DefaultTask implements GridRendererWorker, ImageSampler {

    private static final long serialVersionUID = 6427858641203628311L;

    final GridRendererMaster parent;

    Scene scene;

    transient SunflowAPI sunflowAPI;

    int workerId;
    
    int currentFrame;

    private boolean stopped = false;

    private Thread renderThread = null;
    
    private RendererType simpleRendererTask;
    
    private BucketRendererTask bucketRendererTask;
    
    private RendererType currentRenderer = simpleRendererTask;
    
    private boolean fileAnimation = false;
    
    File mainFile;
    
    File[] animFiles;

    private Bucket currentBucket;
    
    /**
     * An implementation of the {@link org.sunflow.core.Display} interface
     * that sends the pixels to the master process.
     */
    public class RemoteDisplay implements Display {

        public void imageBegin(int w, int h, int bucketSize) {
        }

        public void imageEnd() {
        }

        public void imageFill(int x, int y, int w, int h, Color c, float alpha) {
        }

        public void imagePrepare(int x, int y, int w, int h, int id) {
        }

        public void imageUpdate(int x, int y, int w, int h, Color[] data, float[] alpha) {
            parent.imageUpdate(currentFrame, workerId, currentBucket.id, x, y, w, h, data, alpha);
            System.out.println("Worker#" + workerId + " sent back bucket " + currentBucket.id + " of frame " + currentFrame + ".");            
        }

    }

    /**
     * The default cosntructor.
     * @param workerId the id of this worker
     * @param parent the proxy object of the master process residing in the client application.
     */
    public RendererTask(int workerId, GridRendererMaster parent) {
        this.workerId = workerId;
        this.parent = parent;
        simpleRendererTask = new SimpleRendererTask(this);
        bucketRendererTask = new BucketRendererTask(this);
    }


    /** 
     * Sets the required files before a rendering session.
     * 
     * @see jgrid.sunflow.renderer.GridRendererWorker#setScene(jgrid.sunflow.data.RequiredFile, jgrid.sunflow.data.RequiredFile[], jgrid.sunflow.data.RequiredFile[], java.lang.String)
     */
    public void setScene(RequiredFile mainFile, RequiredFile[] reqFiles, RequiredFile[] animFiles, String rendererType) throws Exception {

//        monitor.grmStart("Grid renderer", "Worker#"+workerId, workerId);
        
//        monitor.grmReceive(100, 0, 1000);
        
//        monitor.grmBeginBlock(0, 0);
        if (renderThread != null) {
            try {
                renderThread.join();
            } catch (InterruptedException ignore) {}
        }
//        monitor.grmEndBlock(0, 0);
        
        try {
            if (BUCKET_RENDERER.equals(rendererType)) {
                currentRenderer = bucketRendererTask;
            } else {
                currentRenderer = simpleRendererTask;
            }

//            monitor.grmBeginBlock(1, 0);
                                
            this.mainFile = fileContext.getFile(mainFile.fileName, mainFile.forceDownload);
            System.out.println("Got main file:" + this.mainFile.getName());
            if (reqFiles != null) {
                for (int i = 0; i < reqFiles.length; i++) {
                    File file = fileContext.getFile(reqFiles[i].fileName, reqFiles[i].forceDownload);
                    System.out.println("Got required file:" + file.getName());
                }
            }
            if (animFiles != null) {
                this.animFiles = new File[animFiles.length];
                for (int i = 0; i < animFiles.length; i++) {
                    this.animFiles[i] = fileContext.getFile(animFiles[i].fileName, animFiles[i].forceDownload);
                    System.out.println("Got required file:" + this.animFiles[i].getName());
                }
                fileAnimation = true;
            }

//            monitor.grmEndBlock(1, 0);
//            monitor.grmBeginBlock(4, 0);

            sunflowAPI = SunflowAPI.create(this.mainFile.getAbsolutePath(), 1);
            stopped = false;
            
//            monitor.grmEndBlock(4, 0);

        } finally {
        }
    }
    
    /**
     * Starts the rendering.
     */
    public void run() {
        renderThread  = new Thread(new Runnable() {

            public void run() {
                System.out.println("Task starts rendering");

//                monitor.grmBeginBlock(2, 0);
                try {
                    if (sunflowAPI != null) {            
                       if (fileAnimation) {
                            
//                            monitor.grmBeginBlock(3,0);
                            
                            currentFrame = 0;
                            sunflowAPI.render(SunflowAPI.DEFAULT_OPTIONS, RendererTask.this, new RemoteDisplay());
                            
//                            monitor.grmEndBlock(3, 0);
                            Display display = new RemoteDisplay();
                            
                            for (int i = 0; i < animFiles.length; i++) {
                                if (doBreak()) {
                                    break;
                                }
//                                monitor.grmBeginBlock(3, i + 1);
                                
                                currentFrame++;
                                sunflowAPI.reset();
                                if (sunflowAPI.include(animFiles[i].getAbsolutePath())) {
                                    sunflowAPI.render(SunflowAPI.DEFAULT_OPTIONS, RendererTask.this, display);
                                } else {
                                    parent.announceException(workerId, "Parser exception in file: " + animFiles[i].getName(), null);
                                }
                                
//                                monitor.grmEndBlock(3, i + 1);
                            }
                        } else {
//                            monitor.grmBeginBlock(3, 0);
                            
                            currentFrame = 0;
                            sunflowAPI.render(SunflowAPI.DEFAULT_OPTIONS, RendererTask.this, new RemoteDisplay());
                            
//                            monitor.grmEndBlock(3, 0);
                        }
                    } else {
                        parent.announceException(workerId, "Sunflow API is null. Possible parse exception", null);
                    }
                } finally {
//                    monitor.grmEndBlock(2, 0);
//                    monitor.grmExit();
                }
            }
        });
        renderThread.start();
    }

    /** 
     * Called by the SunflowAPI when the rendering is started.
     * 
     * @see org.sunflow.core.ImageSampler#render(org.sunflow.core.Display)
     */
    public void render(Display display) {
        IntersectionState istate = new IntersectionState();
        while (!doBreak()) {
            try {
                currentBucket = parent.getNextBucket(currentFrame, workerId);
                
//                monitor.grmReceive(100, 0, bucket.width * bucket.height);
                
                System.out.println("Worker#" + workerId + " got bucket " + currentBucket.id + " of frame" + currentFrame + ".");
                currentRenderer.renderBucket(display, currentBucket, workerId, istate);
            } catch (EmptyStackException e) {
                break;
            }
        }
    }
    
    /**
     * Called by the SunflowAPI before rendering the scene.
     * 
     * @see org.sunflow.core.ImageSampler#prepare(org.sunflow.core.Options, org.sunflow.core.Scene, int, int)
     */
    public boolean prepare(Options opts, Scene scene, int w, int h) {
        this.scene = scene;
        return currentRenderer.prepare(opts, scene, w, h);
    }
        
    /** 
     * Stops the current rendering and returns the task into a waiting state.
     * 
     * @see jgrid.sunflow.renderer.GridRendererWorker#stop()
     */
    public void stop() {
        stopped  = true;
        currentRenderer.stop();
    }
    
    /**
     * Returns the worker id.
     * 
     * @see jgrid.sunflow.renderer.GridRendererWorker#getID()
     */
    public int getID() {
        return workerId;
    }

    /**
     * Check wether some breaking condition was set to true.
     * @return return if the task was cancelled or stopped.
     */
    boolean doBreak() {
        return isCancelled || stopped;
    }
    
    /**
     * Cancels the current task by removing it from the Compute Service.
     * 
     * @see jgrid.sunflow.renderer.task.DefaultTask#cancel()
     */
    @Override
    public void cancel() throws RemoteException {
        super.cancel();
        currentRenderer.stop();
    }
}