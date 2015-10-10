package phex.download.handler;

import java.io.IOException;

import phex.host.UnusableHostException;
import phex.http.HTTPMessageException;

public interface DownloadHandler
{
    /**
     * Performs download pre process operations.
     * @throws IOException 
     */
    public void preProcess() throws DownloadHandlerException;
    
    /**
     * Handles the handshake for the download. 
     */
    public void processHandshake() 
        throws IOException, UnusableHostException, HTTPMessageException;

    /**
     * Process the actual download data transfer.
     * @throws IOException
     */
    public void processDownload() throws IOException;
    
    /**
     * Performs download post process operations.
     */
    public void postProcess();
    
    /**
     * Stops the download.
     */
    public void stopDownload();
    
    /**
     * Indicates whether the connection is keept alive and the next request can 
     * be send.
     * @return true if the next request can be send on this connection
     */
    public boolean isAcceptingNextRequest();
}
