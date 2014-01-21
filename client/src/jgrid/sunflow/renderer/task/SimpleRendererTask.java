package jgrid.sunflow.renderer.task;

import java.io.Serializable;

import jgrid.sunflow.data.Bucket;

import org.sunflow.core.Display;
import org.sunflow.core.IntersectionState;
import org.sunflow.core.Options;
import org.sunflow.core.Scene;
import org.sunflow.core.ShadingState;
import org.sunflow.image.Color;

/**
 * A wrapper class for the Sunflow SimpleRenderer.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1
 */
public class SimpleRendererTask implements RendererType, Serializable {
    
    private Scene scene;
    
    boolean stop = false;
    
    public SimpleRendererTask(RendererTask mainTask) {
    }

    /* (non-Javadoc)
     * @see jgrid.sunflow.renderer.RendererType#prepare(org.sunflow.core.Options, org.sunflow.core.Scene, int, int)
     */
    public boolean prepare(Options opts, Scene scene, int w, int h) {
        this.scene = scene;
        stop = false;
        return true;
    }

    /* (non-Javadoc)
     * @see jgrid.sunflow.renderer.RendererType#renderBucket(jgrid.sunflow.data.Bucket, org.sunflow.core.IntersectionState)
     */
    public void renderBucket(Display display, Bucket bucket, int threadID, IntersectionState istate) {        
        // pixel sized extents
        int x0 = bucket.x0 * bucket.width;
        int y0 = bucket.y0 * bucket.height;
        int bw = Math.min(bucket.width, bucket.imageWidth - x0);
        int bh = Math.min(bucket.height, bucket.imageHeight - y0);

        Color[] bucketRGB = new Color[bw * bh];
        float[] bucketAlpha = new float[bw * bh];

        for (int y = 0, i = 0; y < bh; y++) {
            for (int x = 0; x < bw; x++, i++) {

                if (stop) return;
                
                ShadingState state = scene.getRadiance(istate, x0 + x, bucket.imageHeight - 1 - (y0 + y), 0.0, 0.0, 0.0, 0, 0, null);
                bucketRGB[i] = (state != null) ? state.getResult() : Color.BLACK;
                bucketAlpha[i] = (state != null) ? 1 : 0;
            }
        }

        if (stop) return;

//        mainTask.monitor.grmSend(100, 0, bucketRGB.length);
        
        // update pixels
        display.imageUpdate(x0, y0, bw, bh, bucketRGB, bucketAlpha);
    }
    
    public void stop() {
        stop = true;
    }
}
