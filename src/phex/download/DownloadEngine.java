/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2008 Phex Development Group
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 *  --- SVN Information ---
 *  $Id$
 */
package phex.download;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import phex.download.ThexVerificationData.ThexData;
import phex.download.handler.DownloadHandler;
import phex.download.handler.DownloadHandlerException;
import phex.download.handler.HttpFileDownload;
import phex.download.handler.HttpThexDownload;
import phex.download.swarming.SWDownloadCandidate;
import phex.download.swarming.SWDownloadConstants;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SWDownloadSet;
import phex.download.swarming.SWDownloadCandidate.CandidateStatus;
import phex.host.UnusableHostException;
import phex.http.HTTPMessageException;
import phex.net.connection.Connection;

/**
 * This class is responsible to download a file using a HTTP connection.
 * The DownloadEngine is usually managed by a SWDownloadWorker.
 */
public class DownloadEngine
{
//    private static final Logger logger = LoggerFactory.getLogger( 
//        DownloadEngine.class );
    
    private enum Status
    {
        // Indicates the download engine is running
        RUNNING,
        // Indicates the download engine finished running.
        FINISHED,
        // Indicates the download engine failed to continue to run
        // because some kind of download problem
        FAILED,
        // Indicates the download engine was aborted from externally.
        // Usually per user request.
        ABORTED
    }
    
    private Status status;
    
    private Connection connection;
    
    /**
     * The download set containing the download file and download candidate.
     */
    private SWDownloadSet downloadSet;
    
    private DownloadHandler downloadHandler;
    
    /**
     * Create a download engine
     */
    public DownloadEngine( SWDownloadSet downloadSet )
    {
        this.downloadSet = downloadSet;
        status = Status.RUNNING;
    }
    
    public Connection getConnection()
    {
        return connection;
    }
    
    public SWDownloadSet getDownloadSet()
    {
        return downloadSet;
    }
        
    public void setConnection( DownloadConnection connection ) 
    {
        this.connection = connection;
        assert status == Status.RUNNING : "This DownloadEngine has an invalid status: " + status;
    }

    /**
     * Called from external, usually on user request, to
     * abort a download operation.
     */
    public void abortDownload()
    {
        status = Status.ABORTED;
        SWDownloadCandidate candidate = downloadSet.getCandidate();
        candidate.addToCandidateLog( "Download aborted." );
        stopInternalDownload();
    }
    
    /**
     * Called from internal to indicate the download has failed.
     */
    private void failDownload()
    {
        status = Status.FAILED;
        SWDownloadCandidate candidate = downloadSet.getCandidate();
        candidate.addToCandidateLog( "Download failed." );
        stopInternalDownload();
    }
        
    public void runEngine()
    {
        try
        {
            SWDownloadCandidate candidate = downloadSet.getCandidate();
            SWDownloadFile downloadFile = downloadSet.getDownloadFile();
            do
            {
                processRequest( );
            }
            while ( status == Status.RUNNING 
                && downloadHandler.isAcceptingNextRequest() 
                && downloadFile.isScopeAllocateable( candidate.getAvailableScopeList(), 
                    candidate.isAvailableScopeComplete() ) );
            if ( candidate.getStatus() == CandidateStatus.CONNECTING ||
                 candidate.getStatus() == CandidateStatus.ALLOCATING_SEGMENT ||
                 candidate.getStatus() == CandidateStatus.DOWNLOADING ||
                 candidate.getStatus() == CandidateStatus.PUSH_REQUEST ||
                 candidate.getStatus() == CandidateStatus.REQUESTING )
            {
                candidate.setStatus( CandidateStatus.WAITING );
            }
        }
        finally
        {
            status = Status.FINISHED;
            stopInternalDownload();
        }
    }

    private void processRequest( )
    {
        SWDownloadCandidate candidate = downloadSet.getCandidate();
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();
        try
        {
            do
            {
                // init and preprocess handler.
                initDownloadHandler();
                if ( status != Status.RUNNING )
                {
                    return;
                }
                
                processDownloadHandlerHandshake();
                if ( status != Status.RUNNING )
                {
                    return;
                }
                
                holdPossibleQueueState();
                if ( status != Status.RUNNING )
                {
                    return;
                }
            }
            while ( candidate.isRemotlyQueued() || candidate.isRangeUnavailable() );
            
            // unset possible queued candidate...
            downloadFile.removeQueuedCandidate( candidate );
            
            downloadFile.setStatus( SWDownloadConstants.STATUS_FILE_DOWNLOADING );
            candidate.setStatus( CandidateStatus.DOWNLOADING );
    
            try
            {
                downloadHandler.processDownload();
            }
            catch ( MalformedURLException exp )
            {
                failDownload();
                candidate.addToCandidateLog( exp );
                candidate.setStatus( CandidateStatus.CONNECTION_FAILED );
                // might need to handle different cases on some try again on others 
                // remove
//                logger.error( "Error at Host: "
//                    + candidate.getHostAddress()
//                    + " Vendor: " + candidate.getVendor(), exp );
            }
            catch ( IOException exp )
            {
                failDownload();
                candidate.addToCandidateLog( exp.toString() );
                candidate.setStatus( CandidateStatus.CONNECTION_FAILED );
                // might need to handle different cases on some try again on others 
                // remove
//                logger.warn( "Error at Host: {} Vendor: {}", candidate.getHostAddress(), candidate.getVendor() );
//                logger.warn( exp.toString(), exp );
            }
        }
        finally
        {
            if ( downloadHandler != null )
            {
                downloadHandler.postProcess();
            }
        }
    }
    
    /**
     * Initializes the download handler.
     */
    private void initDownloadHandler()
    {
        downloadHandler = possiblyInitThexHandler();
        if ( downloadHandler == null )
        {
            downloadHandler = new HttpFileDownload( this );
        }
        try
        {
            downloadHandler.preProcess();
        }
        catch ( DownloadHandlerException exp )
        {
            SWDownloadCandidate candidate = downloadSet.getCandidate();
            candidate.addToCandidateLog( "No segment to allocate." );
            failDownload();
        }
    }
    
    private void processDownloadHandlerHandshake()
    {
        ////logger.debug( "process handshake with: {} - {}", downloadSet, this);
        SWDownloadCandidate downloadCandidate = downloadSet.getCandidate();
        try
        {
            downloadCandidate.setStatus( CandidateStatus.REQUESTING );
            downloadHandler.processHandshake();
        }
        catch (RemotelyQueuedException exp)
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            // must first set queue parameters to update waiting time when settings
            // status.
            downloadCandidate.updateXQueueParameters(exp.getXQueueParameters());
            downloadCandidate.setStatus( CandidateStatus.REMOTLY_QUEUED );
        }
        catch ( ReconnectException exp )
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            //logger.debug( "{} {}", downloadCandidate, exp.getMessage());
            // a simple reconnect should be enough here...
            failDownload();
            downloadCandidate.setStatus( CandidateStatus.WAITING);
        }
        catch ( RangeUnavailableException exp )
        {
            SWDownloadFile downloadFile = downloadSet.getDownloadFile();
            downloadCandidate.addToCandidateLog( exp.toString() );
            //logger.debug( "{} :: {}", exp.toString(), downloadCandidate );
            
            boolean isScopeListAvailable = 
                downloadCandidate.getAvailableScopeList() != null &&
                downloadCandidate.getAvailableScopeList().size() > 0;
            // we can retry immediately if we have a filled available scope list
            // and a scope in this list is allocatable.
            if ( isScopeListAvailable && 
                 downloadFile.isScopeAllocateable( downloadCandidate.getAvailableScopeList(),
                     downloadCandidate.isAvailableScopeComplete() ) )
            {
                downloadCandidate.setStatus( CandidateStatus.RANGE_UNAVAILABLE );
            }
            else
            {
                failDownload();
                int waitTime = exp.getWaitTimeInSeconds() > 0 ? exp.getWaitTimeInSeconds() : -1;
                downloadCandidate.setStatus( CandidateStatus.RANGE_UNAVAILABLE,
                    waitTime );
            }
        }
        catch ( HostBusyException exp )
        {
            failDownload();
            downloadCandidate.setStatus( CandidateStatus.BUSY, 
                exp.getWaitTimeInSeconds() );
            //logger.debug( "{} {}", downloadCandidate, exp.getMessage());
        }
        catch (UnusableHostException exp)
        {
            failDownload();
            
            downloadCandidate.addToCandidateLog( exp.toString() );
            // file not available or wrong http header.
            //logger.debug( exp.toString(), exp );
            //logger.debug( "Removing download candidate: {}", downloadCandidate);

            SWDownloadFile downloadFile = downloadSet.getDownloadFile();
            if ( exp instanceof FileNotAvailableException )
            {
                downloadFile.markCandidateIgnored(downloadCandidate, 
                    "CandidateStatusReason_FileNotFound");
            }
            else
            {
                downloadFile.markCandidateIgnored(downloadCandidate, 
                    "CandidateStatusReason_Unusable");
            }
            downloadFile.addBadAltLoc( downloadCandidate );
        }
        catch ( HTTPMessageException exp )
        {
            failDownload();
            downloadCandidate.addToCandidateLog( exp.toString() );

            // wrong http header.
            //logger.warn( exp.toString(), exp );
            
            SWDownloadFile downloadFile = downloadSet.getDownloadFile();
            downloadFile.markCandidateIgnored(downloadCandidate, 
                "CandidateStatusReason_HTTPError");
            downloadFile.addBadAltLoc( downloadCandidate );
        }
        catch ( SocketTimeoutException exp )
        {
            failDownload();
            downloadCandidate.addToCandidateLog( exp.toString() );
            downloadCandidate.setStatus( CandidateStatus.CONNECTION_FAILED);
            //logger.debug( exp.toString(), exp );
        }
        catch ( SocketException exp )
        {
            failDownload();
            downloadCandidate.addToCandidateLog( exp.toString() );
            downloadCandidate.setStatus( CandidateStatus.CONNECTION_FAILED);
            //logger.debug( exp.toString(), exp );
        }
        catch (IOException exp)
        {
            failDownload();
            downloadCandidate.addToCandidateLog( exp.toString() );
            downloadCandidate.setStatus( CandidateStatus.CONNECTION_FAILED );
            // might need to handle different cases on some try again on others 
            // remove
            //logger.warn( "Error at Host: {} Vendor: {}", 
//                downloadCandidate.getHostAddress(),
//                downloadCandidate.getVendor() );
            //logger.warn( exp.toString(), exp );
        }
    }
    
    private DownloadHandler possiblyInitThexHandler( )
    {
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();
        if ( downloadFile.getFileURN() == null )
        {
            return null;
        }
        
        if ( !downloadSet.getCandidate().isThexSupported() )
        {
            return null;
        }
        
        ThexVerificationData thexVerif = downloadFile.getThexVerificationData();
        synchronized( thexVerif )
        {
            if ( thexVerif.isThexRequested() )
            {
                return null;
            }
            ThexData thexData = thexVerif.getThexData();
            if ( thexData != null && thexData.isGoodQuality() )
            {
                return null;
            }
            thexVerif.setThexRequested( true );
            HttpThexDownload handler = new HttpThexDownload( this, thexVerif );
            return handler;
        }
    }
    
    /**
     * Holds the download to process the queue state if the candidate is
     * remotely queued.
     * Stops the download engine if the queue position breaks.
     */
    private void holdPossibleQueueState( )
    {
        SWDownloadCandidate candidate = downloadSet.getCandidate();
        
        if ( !candidate.isRemotlyQueued() )
        {
            return;
        }
        
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();
        
        boolean succ = downloadFile.addAndValidateQueuedCandidate( candidate );
        if ( !succ )
        {
            failDownload();
            return;
        }
        
        try
        {
            int sleepTime = candidate.getXQueueParameters().getRequestSleepTime();
            Thread.sleep( sleepTime );
        }
        catch (InterruptedException exp)
        {// interrupted while sleeping
            //logger.debug( "Interrupted Worker sleeping for queue.");
            failDownload();
            candidate.setStatus( CandidateStatus.CONNECTION_FAILED );
        }
    }
    
    private void stopInternalDownload()
    {
        if ( downloadHandler != null )
        {
            downloadHandler.stopDownload();
        }

        if ( connection != null )
        {
            connection.disconnect();
        }
    }

}