package jgrid.sunflow.renderer;

import static jgrid.sunflow.renderer.RendererTypes.SIMPLE_RENDERER;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jgrid.comp.compute.ComputeService;
import jgrid.comp.compute.TaskControl;
import jgrid.comp.entry.ComputeNode;
import jgrid.comp.entry.Processor;
import jgrid.sunflow.data.Bucket;
import jgrid.sunflow.data.RequiredFile;
import jgrid.sunflow.renderer.task.DefaultTask;
import jgrid.sunflow.renderer.task.RendererTask;
import jnt.scimark2.SciMark2Result;
import jnt.scimark2.SciMark2Task;

import org.sunflow.core.BucketOrder;
import org.sunflow.core.Display;
import org.sunflow.core.ImageSampler;
import org.sunflow.core.Options;
import org.sunflow.core.Scene;
import org.sunflow.core.bucket.BucketOrderFactory;
import org.sunflow.core.display.FileDisplay;
import org.sunflow.image.Color;
import org.sunflow.system.Parser;
import org.sunflow.system.Timer;
import org.sunflow.system.UI;
import org.sunflow.system.Parser.ParserException;
import org.sunflow.system.UI.Module;

import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
import net.jini.core.lease.UnknownLeaseException;
import net.jini.core.lookup.ServiceID;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.DiscoveryGroupManagement;
import net.jini.discovery.LookupDiscoveryManager;
import net.jini.lease.LeaseRenewalManager;
import net.jini.lookup.LookupCache;
import net.jini.lookup.ServiceDiscoveryEvent;
import net.jini.lookup.ServiceDiscoveryListener;
import net.jini.lookup.ServiceDiscoveryManager;

import hu.vein.irt.jgrid.compute.ComputeServiceFactory;

/**
 * The main renderer class that behaves as the master process in distributing
 * the buckets to the workers. The Gridrenderer class manages the discovery
 * of the available services, spawns {@link jgrid.sunflow.renderer.task.RenderTask}
 * ojects to the remote services, and manages the whole rendering session.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1
 */
public class GridRenderer extends DefaultTask 
    implements ImageSampler, GridRendererMaster, ServiceDiscoveryListener {

    //    private Scene scene;

    private Display fileDisplay;

    private Display guiDisplay;

    private int imageWidth = 1024;

    private int imageHeight = 768;

    ArrayList<Bucket> bucketStack = new ArrayList<Bucket>();

    private LookupDiscoveryManager ldm;

    private ServiceDiscoveryManager sdm;

    private LookupCache cache;

    private ComputeService localNode;

    private GridRendererMaster proxy;

    private LeaseRenewalManager lrmgr = new LeaseRenewalManager();

    private RequiredFile mainFile;

    private RequiredFile[] reqFiles;

    HashMap<Integer, Object> workerProxies = new HashMap<Integer, Object>();

    HashMap<Integer, WorkerThread> workerThreads = new HashMap<Integer, WorkerThread>();
    
    HashMap<ServiceID, ArrayList<Integer>> serviceToWorkersMap = new HashMap<ServiceID, ArrayList<Integer>>();

    HashMap<Integer, String> workersToServiceNameMap = new HashMap<Integer, String>();
    
    HashMap<ServiceID, String> idToServiceNameMap = new HashMap<ServiceID, String>();

    final Object workerLock = new Object();
    
    int bucketCnt;
    
    int generatedBuckets;

    int totalBuckets = 0;

    private Timer globalTimer;

    private ArrayList<Bucket> unfinishedBuckets = new ArrayList<Bucket>();

    int currentFrame = 0;

    private int totalFrames;

    private Timer frameTimer;

    private String rendererType = SIMPLE_RENDERER;
    
    private static int BUCKET_SIZE = 32;

    //row, column, diagonal, spiral, hilbert, random
    private static final String BUCKET_ORDER = "hilbert";
    
    private HashMap<ServiceID, ComputeService> services = new HashMap<ServiceID, ComputeService>();
    
    private int taskID = 0;

    private boolean renderFinished = false;

    private final Object renderLock = new Object();

    private RequiredFile[] animFiles;
    
    private HashMap<Integer, Integer> totalRenderHistogram = new HashMap<Integer, Integer>();

    private HashMap<Integer, Integer> currentRenderHistogram = new HashMap<Integer, Integer>();
    
    private ArrayList<Integer> activeWorkers = new ArrayList<Integer>();

    private int failedWorkerNumber = 0;

    private boolean doAnimation;

    private int maxWorkerNumber = 0;

//    private TraceVisualizerDialog traceDialog;
    
//    private class NameFilter implements ServiceItemFilter {
//
//        private final String name;
//
//        public NameFilter(String name) {
//            this.name = name;
//        }
//
//        public boolean check(ServiceItem item) {
//            if (getName(item).equals(name))
//                return true;
//            else
//                return false;
//        }
//
//        private String getName(ServiceItem it) {
//            Entry[] attrs = it.attributeSets;
//            for (int i = 0; i < attrs.length; i++) {
//                if (attrs[i] instanceof Name) {
//                    return ((Name) attrs[i]).name;
//                }
//            }
//            return "";
//        }
//    }
    
    /**
     * A thread that wrapes the proxy of a spawned tasks and
     * invokes the remote methods on it when the rendering session starts.
     */
    public class WorkerThread extends Thread {
        
        private final int id;
        private final GridRendererWorker worker;
        private final Object notifier = new Object();
        private boolean renderingStarted = false; 
        private boolean stopped = false;
        
        public WorkerThread(int id, GridRendererWorker worker) {
            this.id = id;
            this.worker = worker;
        }
        
        public void startRendering() {
            renderingStarted = true;
            stopped = false;
            synchronized (notifier) {
                notifier.notifyAll();
            }            
        }
        
        public void stopRendering() {
            try {
                worker.stop();
            } catch (Exception ignore) {                
            } finally {
                stopped = true;
            }
        }
        
        public void cancel() {
            try {
                ((TaskControl)worker).cancel();
            } catch (Exception ignore) {
            } finally {
                stopped = true;
            }            
        }
        
        public Lease getLease() {
            return ((TaskControl)worker).getLease();
        }
        
        public void run() {
            while (!isCancelled) {
                while (!renderingStarted && !isCancelled) {
                    try {
                        synchronized (notifier) {
                            notifier.wait(1000);
                        }
                    } catch (InterruptedException ignore) {
                    }
                }
                renderingStarted = false;                

                if (isCancelled) break;                
                
                try {
                    UI.printInfo(Module.API, "Setting scene on task#%d", id);
                    
//                    monitor.grmSend(id, 0, 1000);
                    
                    worker.setScene(mainFile, reqFiles, animFiles, rendererType);
                    worker.run();
                    UI.printInfo(Module.API, "Task#%d started rendering...", id);
                } catch (Exception e) {
                    GridRenderer.this.announceException(id, "Exception during rendering", e);
                } finally {
//                    try {
//                        traceDialog.addStream(((TaskControl)worker).getTraceFile());
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }
            }
        }
        
        boolean doBreak() {
            return isCancelled || stopped;
        }                
    }
    
    /** 
     * This method is invoked when a new service is discovered.
     * 
     * @see net.jini.lookup.ServiceDiscoveryListener#serviceAdded(net.jini.lookup.ServiceDiscoveryEvent)
     */
    public void serviceAdded(ServiceDiscoveryEvent event) {
        ComputeService service = (ComputeService)(event.getPostEventServiceItem().service);
        ServiceID id = event.getPostEventServiceItem().serviceID;
        Entry[] entries = event.getPostEventServiceItem().attributeSets;
        
        int procnum = 1;
        String serviceName = "n/a";
       
        // look for the Processor attribute
        for (int i = 0; i < entries.length; i++) {
            if (entries[i] instanceof Processor) {
                Processor p = (Processor) entries[i];
                procnum = p.number;
                System.out.println("Processor number " + procnum);
            } else if (entries[i] instanceof ComputeNode) {
                serviceName = ((ComputeNode)entries[i]).name;
            }
        }
        
        serviceName = "n/a".equals(serviceName) ? id.toString() : serviceName;
        
        // if a new service is discovered
        if (!services.containsKey(id)) {
            services.put(id, service);
            idToServiceNameMap.put(id, serviceName);
            UI.printInfo(Module.API, "Available services: %s", services.size());
            
            for (int i = 0; i < procnum; i++) {                
                // create a new task
                GridRendererWorker worker = null;
                int tid = getNextTaskID();
                worker = new RendererTask(tid, proxy);
                try {
                    // spawn task to the remote service
                    Object workerProxy = service.spawn(worker, 60000);
                    // let the lease renewal manager renew the lease
                    lrmgr.renewFor(((TaskControl)workerProxy).getLease(), Lease.FOREVER, null);
                    // some accounting to manage tasks and services
                    synchronized (workerLock) {
                        workerProxies.put(tid, workerProxy);
                        WorkerThread t = new WorkerThread(tid, (GridRendererWorker)workerProxy);
                        t.start();
                        workerThreads.put(tid, t);

                        ArrayList<Integer> list = serviceToWorkersMap.get(id);
                        if (list == null) {
                            list = new ArrayList<Integer>();
                        }
                        if (!list.contains(tid)) {
                            list.add(tid);
                        }
                        serviceToWorkersMap.put(id, list);
                        
                        workersToServiceNameMap.put(tid, serviceName);
                        totalRenderHistogram.put(tid, 0);
                        currentRenderHistogram.put(tid, 0);                        
                    }
                    UI.printInfo(Module.API, "Task %d spawned to service %s.", tid, serviceName);
                    UI.printInfo(Module.API, "Available workers: %d", workerThreads.size());                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void serviceChanged(ServiceDiscoveryEvent ignore) {}

    /**
     * This method is invoked when a service disappeares.
     * 
     * @see net.jini.lookup.ServiceDiscoveryListener#serviceRemoved(net.jini.lookup.ServiceDiscoveryEvent)
     */
    public void serviceRemoved(ServiceDiscoveryEvent event) {
        ServiceID id = event.getPreEventServiceItem().serviceID;
        services.remove(id);
        idToServiceNameMap.remove(id);
        UI.printInfo(Module.API, "Service %s removed.", id.toString());
        try {
        synchronized (workerLock) {
            ArrayList<Integer> workerList = serviceToWorkersMap.remove(id);
            for (Integer tid : workerList) {
                workerProxies.remove(tid);
                workerThreads.remove(tid);                
                workersToServiceNameMap.remove(tid);
            }
        }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        UI.printInfo(Module.API, "Available workers: %s", workerThreads.size());
    }
    
    /**
     * The main constructor.
     * @param configFile the Jini configuration file.
     * @throws Exception can be ConfigurationException or any Exception durin 
     * initiating the service discovery.
     */
    public GridRenderer(String configFile) throws Exception {
        Configuration config = null;
        if (configFile != null) {
            config = ConfigurationProvider.getInstance(new String[] { configFile });
        }
        if (config == null) {
            ldm = new LookupDiscoveryManager(DiscoveryGroupManagement.ALL_GROUPS,
                                             new LookupLocator[] { new LookupLocator("jini://blade00.venus.vein.hu") },
                                             null);
            sdm = new ServiceDiscoveryManager(ldm, lrmgr);
        } else {
            sdm = new ServiceDiscoveryManager(null, null, config);
        }

        // we are discoverying only compute services
        ServiceTemplate tmpl = new ServiceTemplate(null, new Class[] { ComputeService.class }, null);
        cache = sdm.createLookupCache(tmpl, null /*new NameFilter("Compute Service")*/, this);

        localNode = ComputeServiceFactory.createLocaleService(config);

        Object o = localNode.spawn(this, Long.MAX_VALUE);
        proxy = (GridRendererMaster) o;
        lrmgr.renewFor(((TaskControl) o).getLease(), Lease.FOREVER, null);
        
//        traceDialog = new TraceVisualizerDialog();
//        traceDialog.setVisible(true);
        UI.printInfo(Module.API, "Simple Grid Renderer initialized.", (Object[]) null);
    }

    /**
     * Methos defined by the ImageSampler interface.
     * 
     * @see org.sunflow.core.ImageSampler#prepare(org.sunflow.core.Options, org.sunflow.core.Scene, int, int)
     */
    public boolean prepare(Options opts, Scene scene, int w, int h) {
        //        this.scene = scene;
        imageWidth = w;
        imageHeight = h;

        return true;
    }

    /**
     * Sets the main scene file to render.
     * @param dirname the name of the HTTP servers root directory
     * @param mainFile the main file wrapped in a {@link jgrid.sunflow.data.RequiredFile}
     * @throws IOException if the file cannot be read.
     * @throws ParserException if the syntax of the scene file is incorrect.
     */
    public void setMainFile(String dirname, RequiredFile mainFile) throws IOException, ParserException {

        this.mainFile = mainFile;
        String filename = mainFile.fileName;

        // look for the imgar{} block to specify image dimension
        if (filename.endsWith(".sc")) {
            Parser p = new Parser(dirname + "/" + filename);
            while (true) {
                String token = p.getNextToken();
                if (token == null) {
                    break;
                }
                if ("image".equals(token)) {
                    p.checkNextToken("{");
                    p.checkNextToken("resolution");
                    imageWidth = p.getNextInt();
                    imageHeight = p.getNextInt();
                    break;
                }                 
            }
        } else if (filename.endsWith(".java")) {
            BufferedReader in = new BufferedReader(new FileReader(dirname + "/" + filename));
            Pattern resolution = Pattern.compile(".*resolution\\((.*),(.*)\\);.*");
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // look for the given name
                Matcher resMatcher = resolution.matcher(inputLine);
                if (resMatcher.matches()) {
                    imageWidth = Integer.parseInt(resMatcher.group(1).trim());
                    imageHeight = Integer.parseInt(resMatcher.group(2).trim());
                }
            }
        }
        UI.printInfo(Module.API, "Image resolution: %dx%d", imageWidth, imageHeight);
    }

    /**
     * Sets the array of the required files
     * @param reqFiles array of {@link jgrid.sunflow.data.RequiredFile} objects.
     */
    public void setRequiredFiles(RequiredFile[] reqFiles) {
        this.reqFiles = reqFiles;
    }

    /**
     * Sets the type of the Sunflow renderer. Specify RenderTypes.SIMPLE_RENDERER for
     * using the SimpleRenderer or sepcify RenderTypes.BUCKET_RENDERER for using the
     * BucketRenderer.
     * 
     * @param rendererType the type of the renderer.
     */
    public void setRendererType(String rendererType) {
        this.rendererType = rendererType;
    }

    /**
     * @return returns the type if the renderer. See {@link RendererTypes}.
     */
    public String getRendererType() {
        return rendererType;
    }
    
    /**
     * Sets the array of the scene files required for the animation. If you set an
     * array that is not <code>null</code> the rendering will be automatically
     * switch to animation mode. If you set <code>null</code> only the main file
     * will be rendered.
     * 
     * @param animFiles array of {@link jgrid.sunflow.data.RequiredFile} objects.
     */
    public void setAnimationFiles(RequiredFile[] animFiles) {
        this.animFiles = animFiles;
    }  
    public void setMaxWorkerNumber(int number) {
        maxWorkerNumber  = number;
    }
    /**
     * Returns the current number of workers.
     * @return the number of workers
     */
    public int getWorkerNumber() {
        return workerThreads.keySet().size();
    }
    
    /**
     * Starts the rendering session using the specified {@link org.sunflow.core.Display}.
     * 
     * @see org.sunflow.core.ImageSampler#render(org.sunflow.core.Display)
     */
    public void render(Display display) {

//        try {
//            monitor.grmStart("Grid renderer", "Master", 100);
//            traceDialog.addStream(((TaskControl)proxy).getTraceFile());            
//        } catch (SecurityException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        // discovering ComputeServices
        UI.printInfo(Module.API, "Discovered %d services.", services.size());

        if (getWorkerNumber() == 0) {
            UI.printInfo(Module.API, "No workers available. Rendering stopped.", (Object)null);
            //             delegateRenderer.render(fileDisplay);
            return;
        }

        renderFinished = false;        
        guiDisplay = display;

        activeWorkers.clear();
        failedWorkerNumber  = 0;
        
        bucketStack.clear();
        unfinishedBuckets.clear();
        
        for (Integer tid : currentRenderHistogram.keySet()) {
            currentRenderHistogram.put(tid, 0);
        }

        guiDisplay.imageBegin(imageWidth, imageHeight, BUCKET_SIZE);
        
        if (animFiles != null) {
            totalFrames = animFiles.length + 1;
            doAnimation = true;
        } else {
            totalFrames = 1;
            doAnimation = false;
        }
        
        currentFrame = 0;

        if (doAnimation) {
            this.fileDisplay = new FileDisplay(getFrameFileName(currentFrame));
            this.fileDisplay.imageBegin(imageWidth, imageHeight, BUCKET_SIZE);
        }

        generatedBuckets = generateBuckets(BUCKET_SIZE, BUCKET_ORDER);
        bucketCnt = generatedBuckets;
        
        UI.taskStart("Rendering", 0, generatedBuckets);

        UI.printInfo(Module.API, "%d buckets genarated.", bucketCnt);

        // start task
        String currDate = DateFormat.getDateTimeInstance().format(new Date());
        if (doAnimation) {
            UI.printInfo(Module.API, "Rendering animation '%s' started at %s", mainFile.fileName, currDate);
            UI.printInfo(Module.API, "Frame number: %d", totalFrames);            
        } else {
            UI.printInfo(Module.API, "Rendering of scene '%s' started at %s", mainFile.fileName, currDate);
        }
        UI.printInfo(Module.API, "Render type: %s. Bucket size %d. Bucket order: %s. Worker number: %d",
                     rendererType, BUCKET_SIZE, BUCKET_ORDER, getWorkerNumber());

        globalTimer = new Timer();
        globalTimer.start();
        frameTimer = new Timer();
        frameTimer.start();

//        monitor.grmBeginBlock(0, 0);
        
        WorkerThread[] workers = getWorkers();
        
        int workerNum = getWorkerNumber();
        if (maxWorkerNumber > 0) {
            workerNum = Math.min(workerNum, maxWorkerNumber);
        }
        UI.printInfo(Module.API, "Number of workers used: %d/%d", workerNum, getWorkerNumber());
        for (int i = 0; i < workerNum; i++) {
            workers[i].startRendering();
        }

//        monitor.grmEndBlock(0, 0);
        
//        monitor.grmBeginBlock(1, 0);
        
        while (!renderFinished) {
            synchronized (renderLock) {
                try {
                    renderLock.wait();
                } catch (InterruptedException ignore) {}
            }
        }
       
//        monitor.grmEndBlock(1, 0);
        
//        monitor.grmExit();
    }

    /**
     * Generate stripes of the scene that will be distributed to the workers
     * and fills the bucket stack according to the given <code>bucketOrdering</code>.
     * @param height the height of a stripe
     * @return the number of generated stripes
     */
    private int generateStripes(int height) {
        bucketStack.clear();
        // create renderable intervals
        int intervalNum = imageHeight / height;
        int lag = imageHeight % height;
        if (lag > 0) {
            intervalNum++;
        }
        int offset = 0;
        for (int i = 0; i < intervalNum; i++) {
            if (i == intervalNum - 1 && lag > 0) {
                height = lag;
            }
            bucketStack.add(new Bucket(i, imageWidth, imageHeight, 0, offset, imageWidth, height, intervalNum));
            offset += height;
        }

        return intervalNum;
    }

    /**
     * Generate buckets that have <code>size</code> width and height and fills 
     * the bucket stack according to the given <code>bucketOrdering</code>.
     * @param size the size of a bucket in pixels.
     * @param bucketOrdering the bucket ordering. See {@link org.sunflow.core.bucket.BucketOrderFactory BucketOrderFactory}
     * @return
     */
    private int generateBuckets(int size, String bucketOrdering) {
        int numBucketsX = (imageWidth + size - 1) / size;
        int numBucketsY = (imageHeight + size - 1) / size;
        int numBuckets = numBucketsX * numBucketsY;

        BucketOrder bucketOrder = BucketOrderFactory.create(bucketOrdering);
        int[] bucketCoords = bucketOrder.getBucketSequence(numBucketsX, numBucketsY);        
        
        for (int i = 0; i < bucketCoords.length; i+=2) {
            int bx, by;
            bx = bucketCoords[i];
            by = bucketCoords[i+1];
            bucketStack.add(new Bucket(i, imageWidth, imageHeight, bx, by, size, size, numBuckets));
        }

        return numBuckets;
    }

    /**
     * This method is invoked by remote workers upon finishing to render a bucket.
     * 
     * @see jgrid.sunflow.renderer.GridRendererMaster#imageUpdate(int, int, int, int, int, int, int, org.sunflow.image.Color[], float[])
     */
    public synchronized void imageUpdate(int frameNum, int workerId, int bucketId, int x0, int y0, int width, int height,
                            Color[] cols, float[] alpha) {

//        monitor.grmReceive(workerId, 0, pixels.length);
        
        Bucket intv = new Bucket(bucketId);
        boolean wasone = unfinishedBuckets.remove(intv);
        if (!wasone) {
            //System.out.println("Tried to remove non existent interval.");
            return;
        }

        if (doAnimation) fileDisplay.imageUpdate(x0, y0, width, height, cols, alpha);
        guiDisplay.imageUpdate(x0, y0, width, height, cols, alpha);

        bucketCnt--;
        UI.taskUpdate(generatedBuckets - bucketCnt);
        
        totalRenderHistogram.put(workerId, totalRenderHistogram.get(workerId) + 1);
        currentRenderHistogram.put(workerId, currentRenderHistogram.get(workerId) + 1);
        
        // if this is the last bucket of the current frame
        if (bucketCnt <= 0) {
            frameTimer.end();
            UI.printInfo(Module.API, "Render time of frame %d: %s", frameNum + 1, frameTimer.toString());
            if (doAnimation) fileDisplay.imageEnd();
            guiDisplay.imageEnd();
            // if this is the last frame
            if (frameNum >= (totalFrames - 1)) {
                stopRendering();
            } 
            // if we have more frames left
            else {
                bucketCnt = generateBuckets(BUCKET_SIZE, BUCKET_ORDER);
                currentFrame++;
                guiDisplay.imageBegin(imageWidth, imageHeight, BUCKET_SIZE);
                if (doAnimation) {
                    fileDisplay = new FileDisplay(getFrameFileName(currentFrame));
                    fileDisplay.imageBegin(imageWidth, imageHeight, BUCKET_SIZE);
                }
                frameTimer.start();
                globalTimer.end();
                activeWorkers.clear();
                UI.taskStart("Rendering", 0, generatedBuckets);
                UI.printInfo(Module.API, "Rendering of frame#%d started at %s", currentFrame, globalTimer.toString());
            }
        }
    }

    /** 
     * This method is invoked by remote workes when they request a new bucket to render.
     * This method will pop the next bucket from the render stack, if it is empty than it
     * checks if there are unfinished buckets left, if there ara no more buckets then 
     * an EmptyStackException is thrown.
     * 
     * @see jgrid.sunflow.renderer.GridRendererMaster#getNextInterval()
     */
    public synchronized Bucket getNextBucket(int frameNum, int workerId) throws EmptyStackException {
        
        if (frameNum < currentFrame) {
            throw new EmptyStackException();
        }
        Bucket bucket = null;
        if (!bucketStack.isEmpty()) {
            //int idx = (int) (Math.random() * bucketStack.size());
            bucket = bucketStack.remove(0);
            unfinishedBuckets.add(bucket);
            //UI.printInfo(Module.API, "[SGR] Interval #%d requested by Worker#%d.", bucket.id, workerId);
        } else if (!unfinishedBuckets.isEmpty()) {
            bucket = unfinishedBuckets.remove(0);
            unfinishedBuckets.add(bucket);
            //UI.printInfo(Module.API, "[SGR] Unfinished interval #%d requested by Worker#%d.", bucket.id, workerId);
        } else {
            throw new EmptyStackException();
        }
        int x0 = bucket.x0 * bucket.width;
        int y0 = bucket.y0 * bucket.height;
        int bw = Math.min(bucket.width, bucket.imageWidth - x0);
        int bh = Math.min(bucket.height, bucket.imageHeight - y0);

//        monitor.grmSend(workerId, 0, bw*bh);
        
        guiDisplay.imagePrepare(x0, y0, bw, bh, workerId);
        if (!activeWorkers.contains(workerId)) {
            activeWorkers.add(workerId);
            UI.printInfo(Module.API, "Active workers: %d", activeWorkers.size());
        }

        return bucket;
    }

    /**
     * This method is invoked by remote workers or the WorkerThreads to announce some
     * exception. If all available worker announces some exception after starting a
     * rendering session the rendering will be stopped.
     * 
     * @see jgrid.sunflow.renderer.GridRendererMaster#announceException(int, java.lang.String, java.lang.Exception)
     */
    public synchronized void announceException(int workerId, String message, Exception ex) {
        if (ex != null) {
            ex.printStackTrace();
            UI.printError(Module.API, "Exception from task %d on service %s: %s", workerId, workersToServiceNameMap.get(workerId), ex.getMessage());
        } else {
            UI.printError(Module.API, "Exception from task %d on service %s: %s", workerId, workersToServiceNameMap.get(workerId), message);
        }
        if (++failedWorkerNumber >= workerThreads.keySet().size()) {
            if (currentFrame >= (totalFrames - 1)) {
                stopRendering();
            } else {
                UI.printError(Module.API, "Frame#%d skipped due to exceptions.", currentFrame);
                failedWorkerNumber = 0;
            }
        }
    }

    /**
     * Utility method to get the name entry out of the ComputeNode
     * attribute of the given ServiceItem or "n/a" if there is no
     * such attribute.
     * @param it the ServiceItem
     * @return the name entry of the ComputeNode attribute
     */
    private String getComputeNode(ServiceItem it) {
        Entry[] attrs = it.attributeSets;
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i] instanceof ComputeNode) {
                return ((ComputeNode) attrs[i]).name;
            }
        }
        return "n/a";
    }

    /**
     * Finishes current and future renderings by cancelling
     * all taks. After invoking this method no more rendering
     * can be made. This method is typicallt invoked when exiting
     * the application.
     * 
     * @see jgrid.sunflow.renderer.task.DefaultTask#cancel()
     */
    @Override
    public void cancel() throws RemoteException {
        super.cancel();        
        WorkerThread[] workers = getWorkers();
        for (int i =0; i < workers.length;  i++) {
            workers[i].cancel();
            try {
                lrmgr.cancel(workers[i].getLease());
            } catch (UnknownLeaseException ignore) {}
        }
        
//        monitor.grmExit();
    }
    
    /**
     * @return the current array of worker threads
     */
    WorkerThread[] getWorkers() {
        WorkerThread[] workers;
        synchronized (workerLock) {
            workers = new WorkerThread[workerThreads.values().size()];
            workers = workerThreads.values().toArray(workers);
        }
        return workers;
    }
    
    /**
     * Force the worker threads to invoke the stop method on the remote tasks.
     */
    void stopWorkers() {
        WorkerThread[] workers = getWorkers();
        for (int i = 0; i < workers.length; i++) {
            workers[i].stopRendering();
        }
    }
    
    /**
     * Returns the rendering time of the last rendering session.
     * 
     * @return rendering time in String format.
     */
    public String getRenderTime() {        
        if (globalTimer == null) {
            return "no time";
        } else 
            return globalTimer.toString();
    }

    /**
     * Gets the name of the file where the given frame will be saved to.
     * @param frameNum the number of the frame.
     * @return the file name to save of the given frame.  
     */
    String getFrameFileName(int frameNum) {
        String prefix = String.valueOf(1000 + frameNum).substring(1).concat(".png");
        return "frames/frame" + prefix;
    }
    
    /**
     * @return a uniqe task id for spawning a new task to a remote service.
     */
    private int getNextTaskID() {
        return taskID ++;
    }

    /**
     * Stops the current rendering session.
     */
    public void stopRendering() {
        globalTimer.end();
        UI.taskStop();
        UI.printInfo(Module.API, "Render time: %s", globalTimer.toString());
        for (Integer workerId : currentRenderHistogram.keySet()) {
            UI.printInfo(Module.API, "Worker#%d on %s rendered %d (total %d) buckets.", workerId, workersToServiceNameMap.get(workerId), currentRenderHistogram.get(workerId), totalRenderHistogram.get(workerId));
        }
        stopWorkers();
        renderFinished  = true;
        synchronized (renderLock) {
            renderLock.notifyAll();
        }
        
//        monitor.grmExit();
    }
    
    class BenchmarkThread extends Thread {
        public final ComputeService service;
        public final String serviceName;
        public double score = 0.0;
        
        public BenchmarkThread(ComputeService service, String serviceName) {
            this.service = service;
            this.serviceName = serviceName;
        }
        
        public void run() {
            try {
                SciMark2Result res = (SciMark2Result) service.execute(new SciMark2Task(true));
                score = res.getComposite();
            } catch (Exception e) {
                UI.printError(Module.API, "Error in executing benchmark on %s: %s", serviceName, e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void doBenchmark() {
        BenchmarkThread[] threads = new BenchmarkThread[getWorkerNumber()];
        int i = 0;
        for (ServiceID id: services.keySet()) {
            ComputeService service = services.get(id);
            String serviceName = idToServiceNameMap.get(id);
            
            for (int j = 0; j < serviceToWorkersMap.get(id).size(); j++) {
                UI.printInfo(Module.API, "Starting benchmark on %s...", serviceName);
                
                threads[i] = new BenchmarkThread(service, serviceName);
                threads[i++].start();                
            }
        }

        double totalScore = 0.0;
        for (int j = 0; j < threads.length; j++) {
            try {
                threads[j].join();
                UI.printInfo(Module.API, "Scimark2 composite score of %s: %f", threads[j].serviceName, threads[j].score);
                totalScore += threads[j].score;
            } catch (InterruptedException ignore) {}            
        }
        UI.printInfo(Module.API, "Total score for all %d serives: %f", threads.length, totalScore);

    }
}