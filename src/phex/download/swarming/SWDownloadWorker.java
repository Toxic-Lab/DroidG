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
package phex.download.swarming;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import phex.common.Environment;
import phex.common.address.IpAddress;
import phex.connection.ConnectionFailedException;
import phex.download.DownloadConnection;
import phex.download.DownloadEngine;
import phex.download.PushHandler;
import phex.download.swarming.SWDownloadCandidate.CandidateStatus;
import phex.net.repres.SocketFacade;
import phex.prefs.core.NetworkPrefs;

public class SWDownloadWorker implements Runnable
{
    /**
     * A temporary worker indicates a worker that is used to wait for a valid
     * download set. Only one temporary worker should be in the system. Once
     * a valid download set is found the worker will lose its temporary status.
     * This flag will help to limit the worker count and only hold as many
     * workers as required and necessary.
     */
    private volatile boolean isTemporaryWorker;

    private volatile boolean isRunning;

    private volatile DownloadEngine downloadEngine;
    
    /**
     * Indicates if the download worker is inside the critical download section.
     * The critical section is the section in which modifications to the download
     * segment occur. Before a Phex shutdown a worker thread needs to finish 
     * cleanly to ensure that not corruption to segment data occurs. (Invalid
     * segment sizes).
     */
    private volatile boolean insideCriticalSection;
    
    /**
     * Indicates if the download was stopped from externally.
     * Usually per user request.
     */
    private volatile boolean isDownloadStopped;
    
    /**
     * The thread in which the worker is running.
     */
    private volatile Thread workerThread;
    private Object workerThreadLock = new Object();
    
    private final SwarmingManager downloadService;

    public SWDownloadWorker( SwarmingManager downloadService )
    {
        this.downloadService = downloadService;
    }

    /**
     * Sets the temporary worker status.
     * @param state
     * @see isTemporaryWorker
     */
    public void setTemporaryWorker(boolean state)
    {
        isTemporaryWorker = state;
    }

    /**
     * Returns the temporary worker status.
     * @return the temporary worker status.
     */
    public boolean isTemporaryWorker()
    {
        return isTemporaryWorker;
    }

    public boolean isInsideCriticalSection()
    {
        return insideCriticalSection;
    }
    
    public void run()
    {
        synchronized( workerThreadLock )
        {
            workerThread = Thread.currentThread();
        }
        try
        {
            innerRun();
        }
        finally
        {
            synchronized( workerThreadLock )
            {
                workerThread = null;
                workerThreadLock.notify();
            }
        }
    }
    
    private void innerRun()
    {
        try
        {
            SWDownloadSet downloadSet;
            
            while ( isRunning )
            {
                boolean isStopped = downloadService.checkToStopWorker(this);
                if ( isStopped )
                {
                    break;
                }
                
                isDownloadStopped = false;
                //NLogger.debug( SWDownloadWorker.class, 
                //    " - Allocating DownloadSet - " + this );
                downloadSet = downloadService.allocateDownloadSet(this);
                if ( downloadSet == null )
                {
                    if ( isTemporaryWorker )
                    {
                        try
                        {
                            downloadService.waitForNotify();
                        }
                        catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }
                    else
                    {
                        // no download set acquired after handling last download...
                        // break away from further trying...
                        break;
                    }
                }

                //NLogger.debug( SWDownloadWorker.class,
//                    "Allocated DownloadSet: "
//                    + downloadSet.toString() + " - " + this);
                try
                {
                    handleDownload(downloadSet);
                }
                finally
                {
                    //NLogger.debug( SWDownloadWorker.class,
//                        "Releasing DownloadSet: " 
//                        + downloadSet.toString() + " - " + this);
                    downloadSet.releaseDownloadSet();
                }
            }
        }
        finally
        {
            // if the worker should run... give notice about crash
            downloadService.notifyWorkerShoutdown(this, !isRunning);
            
            //NLogger.debug( SWDownloadWorker.class,
             //   "Download worker finished: " + this);
        }
    }

    public void startWorker()
    {
        isRunning = true;
        Environment.getInstance().executeOnThreadPool( this,
            "SWDownloadWorker-" + Integer.toHexString(hashCode()) );
        //NLogger.debug( SWDownloadWorker.class,
        //    "Started SWDownloadWorker " + this);
    }

    public void stopWorker()
    {
        //NLogger.debug( SWDownloadWorker.class,
        //    "Download worker has been instructed to stop running: " + this);
        isRunning = false;
        isDownloadStopped = true;
        if ( downloadEngine != null )
        {
            downloadEngine.abortDownload();
            downloadEngine = null;
        }
        synchronized( workerThreadLock )
        {
            if ( workerThread != null )
            {
                workerThread.interrupt();
            }
        }
    }
    
    /**
     * Waits/blocks until this download worker finishes its duty, without
     * interrupting it. 
     */
    public void waitTillFinished()
    {
        synchronized( workerThreadLock )
        {
            try
            {
                while ( workerThread != null )
                {
                    workerThreadLock.wait( 5000 );
                }
            }
            catch (InterruptedException e)
            {
                //NLogger.error( SWDownloadWorker.class, e, e );
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning()
    {
        return isRunning;
    }

    /**
     * Handles a specific SWDownloadSet to start the download for.
     * @param downloadSet the download set containing the download configuration.
     */
    private void handleDownload(SWDownloadSet downloadSet)
    {
        //NLogger.debug( SWDownloadWorker.class,
        //    "handleDownload() with: " + downloadSet + " - " + this);
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();
        SWDownloadCandidate downloadCandidate = downloadSet
            .getCandidate();
        
        if ( !isRunning || isDownloadStopped )
        {
            return;
        }
        
        if ( downloadCandidate.isPushNeeded() )
        {
            connectDownloadEngineViaPush( downloadSet, false );
        }
        else
        {
            connectDownloadEngine(downloadSet);
        }

        if ( downloadEngine == null ) { return; }
        if ( !isRunning || isDownloadStopped ) { return; }
        try
        {
            insideCriticalSection = true;
            startDownload(downloadSet);
        }
        finally
        {
            // unset possible queued candidate...
            downloadFile.removeQueuedCandidate( downloadCandidate );
            
            downloadEngine = null;
            
            //NLogger.debug( SWDownloadWorker.class,
            //    "Releasing DownloadSegment: " + downloadSet.toString() + " - " + this);
            downloadSet.releaseDownloadSegment();
            // segment download completed
            downloadFile.verifyStatus();
            insideCriticalSection = false;
        }
    }

    /**
     * Connects the download engine to the host with a direct connection.
     */
    private void connectDownloadEngine(SWDownloadSet downloadSet)
    {
        if ( !isRunning || isDownloadStopped ) { return; }
        
        //NLogger.debug( SWDownloadWorker.class,
        //    "connectDownloadEngine with: " + downloadSet + " - " + this);
        SWDownloadCandidate downloadCandidate = downloadSet.getCandidate();

        // invalidate the download engine
        downloadEngine = null;
        try
        {
            DownloadConnection connection = new DownloadConnection( 
                downloadCandidate );
            // this call sets the CONNECTING status when it is
            // performing the connect operation.
            connection.connect( NetworkPrefs.TcpConnectTimeout.get().intValue() );
            
            if ( !isRunning || isDownloadStopped ) { return; }
            
            downloadEngine = new DownloadEngine( downloadSet );
            downloadEngine.setConnection( connection );
        }
        catch (ConnectionFailedException exp)
        {
            // indicates a general communication error while connecting
            downloadCandidate.addToCandidateLog( exp.toString() );
            //NLogger.debug( SWDownloadWorker.class, exp.toString() );
            
            // trying push - setting failed status is directed to connectDownloadEngineViaPush()
            connectDownloadEngineViaPush( downloadSet, true );
            return;
        }
        catch ( SocketTimeoutException exp)
        {
            // indicates a general communication error while connecting
            downloadCandidate.addToCandidateLog( exp.toString() );
            //NLogger.debug( SWDownloadWorker.class, exp.toString() );

            // trying push - setting failed status is directed to connectDownloadEngineViaPush()
            connectDownloadEngineViaPush( downloadSet, true );
            return;
        }
        catch ( UnknownHostException exp)
        {
            // indicates that we failed to determine the IP address of a host.
            downloadCandidate.addToCandidateLog( exp.toString() );
            //NLogger.debug( SWDownloadWorker.class, exp.toString() );

            // trying push - setting failed status is directed to connectDownloadEngineViaPush()
            connectDownloadEngineViaPush( downloadSet, true );
            return;
        }
        catch ( IOException exp )
        {
            downloadCandidate.addToCandidateLog( exp.toString() );
            // TODO3 log this as error to handle different cases on some try 
            // again on others remove
            //NLogger.error( SWDownloadWorker.class, "HardError at Host: "
//                + downloadCandidate.getHostAddress()
//                + " Vendor: " + downloadCandidate.getVendor(),
//                exp );
            
            assert downloadEngine == null : 
                "Download Engine is initialized. If this can possible happen we need to stop it.";

            // unknown error trying a push - setting failed status is directed
            // to connectDownloadEngineViaPush()
            connectDownloadEngineViaPush( downloadSet, true );
            return;
        }
    }

    /**
     * Connects the download engine via a push request.
     * @param downloadSet the download set to use.
     * @param failedBefore if true a previous standard connection try failed just
     *        before this try, false otherwise. This is used to determine the right
     *        status combinations to set and prevent double count of a single failed
     *        connection try.
     */
    private void connectDownloadEngineViaPush( SWDownloadSet downloadSet, boolean failedBefore )
    {
        if ( !isRunning || isDownloadStopped ) { return; }
        
        //NLogger.debug( SWDownloadWorker.class,
        //    "connectDownloadEngineViaPush with: " + downloadSet + " - " + this);
        SWDownloadCandidate downloadCandidate = downloadSet.getCandidate();
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();

        // invalidate the download engine
        downloadEngine = null;
        
        IpAddress ipAddress = downloadCandidate.getHostAddress().getIpAddress();
        // indicate if the candidate might be reachable through LAN
        boolean isLANReachable = NetworkPrefs.ConnectedToLAN.get().booleanValue() 
            && ipAddress != null && ipAddress.isSiteLocalIP();
        
        // force a status switch to ensure a possible failed status setting is 
        // detected correct.. prevents possible endless loops in race situations.
        if ( downloadCandidate.getStatus() == CandidateStatus.CONNECTION_FAILED )
        {
            downloadCandidate.setStatus( CandidateStatus.CONNECTING, -1, "Forced status switch." );
        }

        // if we are behind a firewall there is no chance to successfully push
        // if the candidate is not reachable through LAN.
        if ( downloadSet.getServent().isFirewalled() && !isLANReachable )
        {
            //NLogger.debug( SWDownloadWorker.class,
//                this.toString() + downloadCandidate.toString()
//                + " Cant PUSH -> I'm firewalled and candidate not reachable by LAN" );
            downloadCandidate.addToCandidateLog( 
                "Cant PUSH -> I'm firewalled and candidate not reachable by LAN" );
            
            downloadCandidate.setStatus(
                    CandidateStatus.CONNECTION_FAILED);
            // candidate must have push. can't directly connect
            if ( downloadCandidate.isPushNeeded() )
            {
                downloadFile.markCandidateBad( downloadCandidate );
                //downloadFile.markCandidateIgnored( downloadCandidate, 
                //    "CandidateStatusReason_PushRequired");
                // no bad alt loc in this case... others might connect correct...
            }
            return;
        }
        if ( downloadCandidate.getGUID() == null )
        {
            //NLogger.debug( SWDownloadWorker.class,
//                this.toString() + downloadCandidate.toString()
//                + " Cant PUSH -> No candidate GUID." );
            downloadCandidate.addToCandidateLog( 
                "Cant PUSH -> No candidate GUID." );
            downloadCandidate.setStatus(
                    CandidateStatus.CONNECTION_FAILED);
            return;
        }
        if ( !isRunning || isDownloadStopped ) 
        {
            if ( failedBefore )
            {
                downloadCandidate.setStatus(CandidateStatus.CONNECTION_FAILED);
            }
            return;
        }
        downloadCandidate.setStatus(
                CandidateStatus.PUSH_REQUEST);
        SocketFacade socket = PushHandler.requestSocketViaPush( 
            downloadSet.getServent(), downloadCandidate );
        if ( socket == null )
        {
            downloadCandidate.setStatus(
                    CandidateStatus.CONNECTION_FAILED);
            // candidate must have push. can't directly connect
            if ( downloadCandidate.isPushNeeded() )
            {
                downloadFile.markCandidateIgnored( downloadCandidate, 
                    "CandidateStatusReason_PushRouteFailed" );
                // no bad alt loc in this case... others might connect correct...
            }
            //NLogger.debug( SWDownloadWorker.class,
            //    "Push request fails for candidate: " + downloadCandidate );
            downloadCandidate.addToCandidateLog( 
                "Push request fails for candidate: " + downloadCandidate );
            return;
        }
        if ( !isRunning || isDownloadStopped ) { return; }
        
        DownloadConnection connection = new DownloadConnection( 
            downloadCandidate, socket );
        
        downloadEngine = new DownloadEngine( downloadSet );
        downloadEngine.setConnection( connection );
    }

    /**
     * Execute the actual download routine.
     */
    private void startDownload(SWDownloadSet downloadSet)
    {
        //NLogger.debug( SWDownloadWorker.class,
        //    "startDownload with: " + downloadSet + " - " + this);
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();
        SWDownloadCandidate downloadCandidate = downloadSet
            .getCandidate();
        
        downloadCandidate.addToCandidateLog( "Start download." );
        
        // we came that far proves that we can successful connect to this candidate
        // we can use it as good alt loc
        // in cases where the http handshake revises this determination the
        // alt loc will be adjusted accordingly.
        downloadFile.addGoodAltLoc(downloadCandidate);
        downloadFile.markCandidateGood(downloadCandidate);
        
        downloadEngine.runEngine();
    }
    
    @Override
    public String toString()
    {
        return "[SWDownloadWorker@" + Integer.toHexString(hashCode()) + ":running:" + isRunning + ",tempWorker:"
            + isTemporaryWorker + ",engine:" + downloadEngine + "]";
    }
}