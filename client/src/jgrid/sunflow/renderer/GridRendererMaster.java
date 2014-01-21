package jgrid.sunflow.renderer;

import java.util.EmptyStackException;

import jgrid.comp.compute.Task;
import jgrid.sunflow.data.Bucket;

import org.sunflow.image.Color;

/**
 * This interface is used by the worker tasks spawned to the Compute Services to
 * invoke methods on the master process residing in the client virtual machine.
 * This interface can be comprehended as the communication protocol from the workers
 * to the master direction.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1
 */
public interface GridRendererMaster extends Task {

    /**
     * This method is invoked when a worker finished the rendering of a bucket and wishes to send
     * back the resulting pixels.
     * 
     * @param frameNum the frame number which this pixels belong to
     * @param workerId the id of the seding worker
     * @param bucketId the id of the bucket which this pixels belong to
     * @param x0 the x coordinate of the offset of the pixels
     * @param y0 the y coordinate of the offset of the pixels
     * @param width the width of the bucket
     * @param height the height of the bucket
     * @param pixels the actual data in pixels
     * @param alpha alpha channel information
     */
    public void imageUpdate(int frameNum, int workerId, int bucketId, int x0, int y0, int width, int height, Color[] pixels, float[] alpha);

    /**
     * This method is invoked when a worker requests a new bucket from the master.
     * 
     * @param frameNum the number of the current frame.
     * @param workerId the id of the worker.
     * @return a renderable bucket if there is one available
     * @throws EmptyStackException if there is no more buckets left.
     */
    public Bucket getNextBucket(int frameNum, int workerId) throws EmptyStackException;
    
    /**
     * This method is invoked when some exception occured that hinders the rendering process.
     * 
     * @param workerId the id of the worker
     * @param message the message
     * @param ex the exception
     */
    public void announceException(int workerId, String message, Exception ex);
}