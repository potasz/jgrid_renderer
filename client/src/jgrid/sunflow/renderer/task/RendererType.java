package jgrid.sunflow.renderer.task;

import jgrid.sunflow.data.Bucket;

import org.sunflow.core.Display;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Options;
import org.sunflow.core.Scene;

/**
 * This interface is used in the {@link RendererTask} to invoke the 
 * Simple or Bucket Sunflow renderer.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1
 */
public interface RendererType {

    /**
     * Prepares the rendering.
     * @param opts Options
     * @param scene the scene object
     * @param w image width
     * @param h image height
     * @return <code>true</code> if there was no error
     */
    public boolean prepare(Options opts, Scene scene, int w, int h);

    /**
     * Renders the given bucket.
     * @param display the Display to use
     * @param bucket the bucket to render.
     * @param threadID the worker id
     * @param istate {@link IntersectionState}
     */
    public void renderBucket(Display display, Bucket bucket, int threadID, IntersectionState istate);
    
    /**
     * Stops the rendering of the current bucket. 
     */
    public void stop();
}