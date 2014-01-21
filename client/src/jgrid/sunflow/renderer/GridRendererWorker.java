package jgrid.sunflow.renderer;

import jgrid.comp.compute.Task;
import jgrid.sunflow.data.RequiredFile;

/**
 * This interface is used by the master process to communicate with the remote tasks.
 * The proxy objects of the spawned taks will implement this interface. This interface
 * can be comprehended as the communication protocol from the master to the workers direction.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1
 */
public interface GridRendererWorker extends Runnable, Task {

    /**
     * This method invoked before starting a new rendering session to prepare
     * the required files.
     * 
     * @param mainFile the main scene file to render
     * @param reqFiles the array of the required external files 
     * @param animFiles the array of the scene files in animation or <code>null</code>.
     * @param rendererType the type of the Sunflow renderer to use. See {@link RendererTypes}.
     * @throws Exception in case some unexpected event occured during uploading or parsing the files.
     */
    public void setScene(RequiredFile mainFile, RequiredFile[] reqFiles, RequiredFile[] animFiles, String rendererType)
        throws Exception;
    
    /**
     * Stops the current rendering session if there is any.
     */
    public void stop();
    
    /**
     * Gets the worker id.
     * @return the worker id.
     */
    public int getID();
}