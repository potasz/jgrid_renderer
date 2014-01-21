package jgrid.sunflow.data;

/**
 * A class defining a renderable bucket that is allocated to the workes
 * by the master process.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 */
public class Bucket implements java.io.Serializable {

    public int id;

    public int imageWidth;

    public int imageHeight;

    public int x0;

    public int y0;
    
    public int width;
    
    public int height;

    public int total;

    /**
     * The main constructor.
     * 
     * @param id the bucket id
     * @param imageWidth the width of the whole image
     * @param imageHeight the height of the whole image
     * @param x0 the x offset in bucket sized units
     * @param y0 the y offset in bucket sized units
     * @param width the width of this bucket
     * @param height the height of this bucket
     * @param total the total number of the buckets
     */
    public Bucket(int id, int imageWidth, int imageHeight, int x0, int y0, int width, int height, int total) {
        this.id = id;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.x0 = x0;
        this.y0 = y0;
        this.width = width;
        this.height = height;
        this.total = total;
    }

    /**
     * A simple constructor for lookup a bucket by id in a HashMap.
     * 
     * @param id the bucket id
     */
    public Bucket(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Bucket)) {
            return false;
        }
        Bucket other = (Bucket)obj;
        return (this.id == other.id);
    }
}
