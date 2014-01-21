package jgrid.sunflow.renderer.task;

import java.io.Serializable;
import java.rmi.RemoteException;

import jgrid.comp.compute.FileContext;
import jgrid.comp.compute.HostContext;
import jgrid.comp.compute.Task;
import jgrid.comp.compute.monitor.GRMMonitor;

/**
 * A simple class implementing the {@link jgrid.comp.compute.Task} interface.
 * 
 * @author Szabolcs Pota
 * @version 0.1.2
 * @since 0.1
 */
public class DefaultTask implements Task {

    private static final String FILE_CONTEXT = "file.context"; //$NON-NLS-1$

    protected transient HostContext hostContext;
    protected transient FileContext fileContext;
//    protected GRMMonitor monitor = null;
    protected boolean isCancelled = false;
    
    public Serializable execute() {
        return null;
    }

    public void setHostContext(HostContext context) {
        this.hostContext = context;
        fileContext = (FileContext) hostContext.getContextObject(FILE_CONTEXT);
//        monitor = (GRMMonitor) hostContext.getContextObject("jgrid.comp.compute.monitor.GRMMonitor");
    }

    public void cancel() throws RemoteException {
        isCancelled = true;
    }

    public void resume() throws RemoteException {
    }

    public void suspend() throws RemoteException {
    }
}
