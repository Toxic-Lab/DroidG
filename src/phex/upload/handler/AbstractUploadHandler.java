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
package phex.upload.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import phex.common.AltLocContainer;
import phex.common.AlternateLocation;
import phex.common.URN;
import phex.common.address.AddressUtils;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.http.GnutellaHeaderNames;
import phex.http.GnutellaRequest;
import phex.http.HTTPHeader;
import phex.http.HTTPHeaderNames;
import phex.http.HTTPRequest;
import phex.prefs.core.UploadPrefs;
import phex.security.PhexSecurityManager;
import phex.servent.Servent;
import phex.share.PartialShareFile;
import phex.share.ShareFile;
import phex.share.SharedFilesService;
import phex.upload.UploadManager;
import phex.upload.UploadState;
import phex.upload.UploadStatus;
import phex.upload.response.UploadResponse;

public abstract class AbstractUploadHandler implements UploadHandler
{
    /**
     * Indicates whether the connection is persistent or not. Like Keep-Alive
     * connections.
     */
    private boolean isPersistentConnection;
    
    /**
     * Indicates whether the upload is queued or not.
     */
    private boolean isUploadQueued;

    /**
     * The earliest timestamp the connection is allowed to come back with the  
     * next request attempt.
     */
    private long queueMinNextPollTime;
    
    /**
     * The maximum time in millis the connection can wait with the next 
     * request before it times out.
     */
    private int queueMaxNextPollTime;
    
    protected final SharedFilesService sharedFilesService;
    
    protected AbstractUploadHandler( SharedFilesService sharedFilesService )
    {
        this.sharedFilesService = sharedFilesService;
    }
    
    public UploadResponse determineUploadResponse( HTTPRequest httpRequest, 
        UploadState uploadState, UploadManager uploadMgr ) 
        throws IOException
    {
        String logMsg = "HTTP Request: " + httpRequest.buildHTTPRequestString();
        //NLogger.debug( AbstractUploadHandler.class, logMsg);
        uploadState.addToUploadLog( logMsg );
        
        // first thing to do before entering the handshake state is to verify if
        // we have capacity for this upload connection.
        if ( uploadState.getStatus() == UploadStatus.ACCEPTING_REQUEST )
        {// this is the first time we handle this UploadState...
            boolean succ = uploadMgr.validateAndCountAddress( 
                uploadState.getHostAddress() );
            if ( !succ )
            {
                isPersistentConnection = false;
                return UploadResponse.get503UploadLimitReachedForIP();
            }
        }

        GnutellaRequest gRequest = httpRequest.getGnutellaRequest();
        assert gRequest != null : "Not a Gnutella file request.";
        
        // ensure the requested file is available...
        ShareFile requestedFile = findShareFile( gRequest, uploadState );
        if (requestedFile == null)
        {
            isPersistentConnection = false;
            return UploadResponse.get404FileNotFound();
        }
        
        HTTPHeader header;
        
        // If this is a already queued request or a new request check if we are busy or are 
        // able to queue candidate. But don't let accepted connections get busy again...
        boolean queueRequest = false;
        if ( (isUploadQueued || uploadState.getStatus() == UploadStatus.ACCEPTING_REQUEST) 
              && uploadMgr.isHostBusy() )
        {
            header = httpRequest.getHeader( GnutellaHeaderNames.X_QUEUE );
            if (header == null || !UploadPrefs.AllowQueuing.get().booleanValue()
                || uploadMgr.isQueueLimitReached())
            {// Queuing is not supported
                isPersistentConnection = false;
                return UploadResponse.get503UploadLimitReached( requestedFile, uploadState );
            }
            queueRequest = true;
        }
        
        // now after we done all fail fast checks, accept this connection and 
        // enter the handshake status for it...
        uploadState.setStatus( UploadStatus.HANDSHAKE );
        
        // update port information..
        int port = -1;
        header = httpRequest.getHeader(GnutellaHeaderNames.X_NODE);
        if (header == null)
        {
            header = httpRequest.getHeader(GnutellaHeaderNames.X_LISTEN_IP);
        }
        if (header == null)
        {
            header = httpRequest.getHeader(GnutellaHeaderNames.LISTEN_IP);
        }
        if (header == null)
        {
            header = httpRequest.getHeader(GnutellaHeaderNames.X_MY_ADDRESS);
        }
        if (header != null)
        {
            // parse port
            port = AddressUtils.parsePort(header.getValue());            
        }
        if (port > 0)
        {
            DestAddress addi = uploadState.getHostAddress();
            uploadState.setHostAddress( new DefaultDestAddress( 
                addi.getIpAddress(), port ) );
        }

        // check for persistent connection status...
        handleConnectionHeader( httpRequest );
        
        // collect vendor infos...
        String vendor = null;
        header = httpRequest.getHeader( HTTPHeaderNames.USER_AGENT );
        if (header != null)
        {
            vendor = header.getValue();
        }
        else
        {
            vendor = "";
        }
        uploadState.setVendor( vendor );
        
        
        UploadResponse response = determineFailFastResponse( httpRequest, uploadState, requestedFile );
        if ( response != null )
        {
            return response;
        }
        
        isUploadQueued = queueRequest;
        if ( queueRequest )
        {
            // Queuing is supported
            int queuePosition = uploadMgr.getQueuedPosition( uploadState );
            if ( queuePosition < 0 )
            {// missing in queue list
                queuePosition = uploadMgr.addQueuedUpload( uploadState );
            }
            uploadState.setStatus( UploadStatus.QUEUED );

            int queueLength = uploadMgr.getUploadQueueSize();
            int uploadLimit = UploadPrefs.MaxParallelUploads.get().intValue();
            int pollMin = UploadPrefs.MinQueuePollTime.get().intValue();
            int pollMax = UploadPrefs.MaxQueuePollTime.get().intValue();

            queueMinNextPollTime = System.currentTimeMillis() + pollMin * 1000L;
            queueMaxNextPollTime = pollMax * 1000;
            return UploadResponse.get503Queued( queuePosition, queueLength, 
                uploadLimit, pollMin, pollMax, requestedFile, uploadState );
        }
        
        if ( !uploadMgr.containsUploadState( uploadState ) )
        {
            uploadMgr.addUploadState(uploadState);
        }
        
        // finalize upload response by sub-class...

        return finalizeUploadResponse( httpRequest, uploadState, requestedFile );
    }
    
    /**
     * Allows subclasses to evaluate the HTTPRequest and decide to fail
     * fast before the request goes into possible queue state or evaluates
     * the handshake further.
     * @return the UploadResponse in case a sub-class decides to fail this
     * request or null in case the handshake can be evaluated further.
     */
    protected abstract UploadResponse determineFailFastResponse( HTTPRequest httpRequest,
        UploadState uploadState, ShareFile requestedFile );
    
    /**
     * Allows the sub-class to finally finish the specific request and form the upload 
     * response, after all common upload processing is finished. 
     * @param httpRequest
     * @param uploadState
     * @param requestedFile
     * @return the upload response
     * @throws IOException 
     */
    protected abstract UploadResponse finalizeUploadResponse( HTTPRequest httpRequest,
        UploadState uploadState, ShareFile requestedFile ) throws IOException;
    
    
    /**
     * @param requestedShareFile
     * @param sharedFileURN
     */
    protected void handleAltLocRequestHeader( HTTPRequest httpRequest, UploadState uploadState,
        ShareFile requestedShareFile, URN sharedFileURN, PhexSecurityManager securityService )
    {
        // collect alternate locations from request...
        List<AlternateLocation> allAltLocs = new ArrayList<AlternateLocation>();
        
        HTTPHeader[] headers = httpRequest.getHeaders( GnutellaHeaderNames.ALT_LOC );
        List<AlternateLocation> altLocList = AltLocContainer.parseUriResAltLocFromHeaders( 
            headers, securityService );
        allAltLocs.addAll(altLocList);
        
        headers = httpRequest.getHeaders( GnutellaHeaderNames.X_ALT_LOC );
        altLocList = AltLocContainer.parseUriResAltLocFromHeaders( headers, securityService );
        allAltLocs.addAll(altLocList);
        
        headers = httpRequest.getHeaders( GnutellaHeaderNames.X_ALT );
        altLocList = AltLocContainer.parseCompactIpAltLocFromHeaders( headers,
            sharedFileURN, securityService );
        allAltLocs.addAll(altLocList);
        
        if ( allAltLocs.size() == 0 )
        {
            return;
        }
        
        AltLocContainer altLocContainer = requestedShareFile.getAltLocContainer();
        for ( AlternateLocation altLoc : allAltLocs )
        {
            String logMsg = "Adding AltLoc " + altLoc.getHTTPString();
            //NLogger.debug( UploadEngine.class, logMsg);
            uploadState.addToUploadLog( logMsg );
            altLocContainer.addAlternateLocation( altLoc );
        }
    }

    /**
     * Evaluate the CONNECTION header and check for a persistent connection.
     * A connection is assumed to be persistent if its a HTTP 1.1 connection
     * with no 'Connection: close' header. Or a HTTP connection with
     * 'Connection: Keep-Alive' header.
     */
    public void handleConnectionHeader( HTTPRequest httpRequest )
    {
        HTTPHeader header = httpRequest.getHeader( HTTPHeaderNames.CONNECTION );
        if ( HTTPRequest.HTTP_11.equals( httpRequest.getHTTPVersion() ) )
        {
            if ( header != null && header.getValue().equalsIgnoreCase("CLOSE") )
            {
                isPersistentConnection = false;
            }
            else
            {
                isPersistentConnection = true;
            }
        }
        else
        {
            if ( header != null && header.getValue().equalsIgnoreCase("KEEP-ALIVE") )
            {
                isPersistentConnection = true;
            }
            else
            {
                isPersistentConnection = false;
            }
        }
    }

    public boolean isPersistentConnection()
    {
        return isPersistentConnection;
    }
    
    public boolean isQueued()
    {
        return isUploadQueued;
    }
    
    /**
     * Returns the earliest timestamp the connection is allowed to come back with the  
     * next request attempt.
     */
    public long getQueueMinNextPollTime()
    {
        return queueMinNextPollTime;
    }
    
    /**
     * Returns he maximum time in millis the connection can wait with the next 
     * request before it times out.
     */
    public int getQueueMaxNextPollTime()
    {
        return queueMaxNextPollTime;
    }
    
    
    
    
    
    
    
    
    
    
    
    protected ShareFile findShareFile( GnutellaRequest gRequest, UploadState uploadState )
    {
        ShareFile shareFile = null;

        // first check for a URN
        URN requestURN = gRequest.getURN();
        if (requestURN != null)
        {// get request contains urn
            if (!(requestURN.isSha1Nid()))
            {
                requestURN = new URN("urn:sha1:" + requestURN.getSHA1Nss());
            }
            shareFile = sharedFilesService.getFileByURN(requestURN);
            // look for partials..
            if (shareFile == null && UploadPrefs.SharePartialFiles.get().booleanValue() )
            {
                SwarmingManager swMgr = Servent.getInstance().getDownloadService();
                SWDownloadFile dwFile = swMgr.getDownloadFileByURN(requestURN);
                if (dwFile != null)
                {
                    shareFile = new PartialShareFile(dwFile);
                }
            }
        }
        // file index is -1 when parsing was wrong
        else if (gRequest.getFileIndex() != -1)
        {
            int index = gRequest.getFileIndex();
            shareFile = sharedFilesService.getFileByIndex(index);
            if (shareFile != null)
            {
                String shareFileName = shareFile.getFileName();
                // if filename dosn't match
                if (!gRequest.getFileName().equalsIgnoreCase(shareFileName))
                {
                    String logMsg = "Requested index '" + index 
                        + "' with filename '" + shareFileName
                        + "' dosn't match request filename '" 
                        + gRequest.getFileName() + "'.";
                    //NLogger.debug( UploadEngine.class, logMsg);
                    uploadState.addToUploadLog( logMsg );
                    shareFile = null;
                }
            }
            else
            {
                // TODO currently this will not work right because the file hash
                // contains
                // the full path name informations of the file. But we only look
                // for the filename.
                // TODO this should be also used if the index returns a file
                // with a different filename then the requested filename
                if (gRequest.getFileName() != null)
                {
                    shareFile = sharedFilesService.getFileByName(gRequest.getFileName());
                }
            }
        }
        return shareFile;
    }
}
