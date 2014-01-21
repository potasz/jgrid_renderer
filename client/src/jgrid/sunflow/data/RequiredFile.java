package jgrid.sunflow.data;

import java.io.Serializable;

/**
 * A wrapper class to send the name of a file that is required for the rendering
 * of a scene. It can be either the main scene, some external file or a scene 
 * file in the animation sequence.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 */
public class RequiredFile implements Serializable {
    
    private static final long serialVersionUID = -1965841695858374401L;

    public final String fileName;
    
    public final boolean forceDownload;

    /**
     * The default constructor
     * @param fileName the file name relative to the HTTP servere root directory
     * @param forceDownload if <code>true</code> the given file is downloaded on every
     * usage, otherwise the given file will be cached after the first download.
     */
    public RequiredFile(final String fileName, final boolean forceDownload) {
        this.fileName = fileName;
        this.forceDownload = forceDownload;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RequiredFile)) {
            return false;
        }
        RequiredFile orf = (RequiredFile) obj;
        return orf.fileName.equals(this.fileName);
    }
}
