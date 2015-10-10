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
package phex.upload;

import java.io.IOException;

import phex.common.address.DestAddress;
import phex.common.bandwidth.BandwidthController;
import phex.http.HTTPMessageException;
import phex.http.HTTPProcessor;
import phex.http.HTTPRequest;
import phex.io.buffer.BufferSize;
import phex.io.buffer.ByteBuffer;
import phex.net.connection.Connection;
import phex.net.repres.SocketFacade;
import phex.prefs.core.NetworkPrefs;
import phex.servent.Servent;
import phex.share.SharedFilesService;
import phex.statistic.SimpleStatisticProvider;
import phex.statistic.StatisticProviderConstants;
import phex.statistic.StatisticsManager;
import phex.upload.handler.FileUploadHandler;
import phex.upload.handler.ThexUploadHandler;
import phex.upload.handler.UploadHandler;
import phex.upload.response.UploadResponse;
import phex.utils.StringUtils;
import phex.utils.VendorCodes;

/**
 * The UploadEngine is handling the process of uploading a file. This includes
 * evaluating and responding to the HTTPRequest.
 * 
 * TODO when the ShareFile switched during a sequence of request Phex will
 * not count the new upload correctly. But this is a rather rare case...
 */
public class UploadEngine
{    
    private final UploadManager uploadMgr;
    
    private final SharedFilesService sharedFilesService;
    
    private final Connection connection;

    /**
     * The current HTTPRequest.
     */
    private HTTPRequest httpRequest;

    /**
     * The upload info object of the current upload.
     */
    private UploadState uploadState;

    /**
     * Flag indicating if the upload is counted inside this upload engine.
     */
    public boolean isUploadCounted;

    /**
     * The upload handler for this engine.
     */
    private UploadHandler uploadHandler;

    public UploadEngine( Connection connection, HTTPRequest httpRequest, 
        UploadManager uploadManager, SharedFilesService sharedFilesService )
    {
        this.sharedFilesService = sharedFilesService;
        this.connection = connection;
        this.uploadMgr = uploadManager;
        connection.setBandwidthController( 
            uploadMgr.getUploadBandwidthController() );
        this.httpRequest = httpRequest;
        isUploadCounted = false;
        
        SocketFacade socket = connection.getSocket();
        DestAddress hostAddress = socket.getRemoteAddress();
        uploadState = new UploadState( hostAddress, VendorCodes.UNKNOWN, uploadManager );
    }

    public void startUpload()
    {
        //NLogger.debug( UploadEngine.class, "Start upload.");
        uploadState.addToUploadLog( "Start upload." );
        boolean followUpRequestAvailable = false;
        try
        {
            do
            {
                try
                {
                    if ( httpRequest.getGnutellaRequest().isTigerTreeRequest() )
                    {
                        uploadHandler = new ThexUploadHandler( sharedFilesService );
                    }
                    else
                    {
                        uploadHandler = new FileUploadHandler( sharedFilesService, Servent.getInstance() );
                    }
                    
                    UploadResponse response = uploadHandler.determineUploadResponse( 
                        httpRequest, uploadState, uploadMgr );
                    sendHTTPResponse( response.buildHTTPResponseString() );
                    if ( uploadHandler.isQueued() )
                    {
                        connection.getSocket().setSoTimeout( uploadHandler.getQueueMaxNextPollTime() );
                    }
                    else if ( response.remainingBody() > 0  
                        && !httpRequest.isHeadRequest() )
                    {// when not queued and not head request and body available
                        uploadState.setUploadEngine( this );
                        connection.getSocket().setSoTimeout( NetworkPrefs.TcpRWTimeout.get().intValue() );
                        sendResponseData( response );
                    }

                    uploadState.setStatus( UploadStatus.COMPLETED );
                }
                catch (IOException exp)
                {
                    // in the case of sendHTTPResponse() and sendResponseData()
                    // we handle a IOException as a aborted status
                    uploadState.setStatus( UploadStatus.ABORTED );
                    throw exp;
                }

                followUpRequestAvailable = false;
                if ( uploadHandler.isPersistentConnection() )
                {
                    // in case of readNextHTTPRequest() we handle a IOException
                    // not as a aborted status since it could mean the connection is
                    // not kept alive.
                    try
                    {
                        readNextHTTPRequest();
                        followUpRequestAvailable = true;
                    }
                    catch ( IOException exp )
                    {
                        //NLogger.debug( UploadEngine.class, exp );
                        uploadState.addToUploadLog( exp.toString() + " - " + exp.getMessage() );
                    }
                }
            }
            while ( followUpRequestAvailable );
            uploadState.setStatus( UploadStatus.COMPLETED );
        }
        catch ( Exception exp )
        {// catch all thats left...
            uploadState.setStatus( UploadStatus.ABORTED );
            //NLogger.error(UploadEngine.class, exp, exp);
            uploadState.addToUploadLog( exp.toString() + " - " + exp.getMessage() );
        }
        finally
        {
            assert !uploadState.isUploadRunning() :  
                "Upload state should not be in running status anymore. Request: "
                + httpRequest.buildHTTPRequestString();
            stopUpload();
            uploadMgr.releaseUploadAddress( uploadState.getHostAddress() );
            
            // set to null to give free for gc
            uploadState.setUploadEngine(null);

            if ( uploadHandler.isQueued() )
            {
                uploadMgr.removeQueuedUpload( uploadState );
            }
        }
    }

    public void stopUpload()
    {
        // disconnecting this connection will cause a IOException in the upload
        // thread and will result in cleaning up the download.
        connection.disconnect();
    }
    
    private void sendResponseData( UploadResponse response ) throws IOException
    {
        //NLogger.debug(UploadEngine.class, 
        //    "About to send response data: " + response.remainingBody() + " bytes." );
        uploadState.addToUploadLog( 
            "About to send response data: " + response.remainingBody() + " bytes." );

        // the upload is actually starting now..
        // if not yet done we are counting this upload.
        if ( !isUploadCounted )
        {
            // count upload even if upload is just starting and not finished
            // yet. swarming uploads fail often anyway.
            response.countUpload();
            
            // Increment the completed uploads count
            StatisticsManager statMgr = Servent.getInstance().getStatisticsService();
            SimpleStatisticProvider provider = (SimpleStatisticProvider) statMgr
                .getStatisticProvider(StatisticProviderConstants.SESSION_UPLOAD_COUNT_PROVIDER);
            provider.increment(1);
            isUploadCounted = true;
        }
        
        uploadState.setStatus( UploadStatus.UPLOADING_DATA );
        // open file
        BandwidthController throttleController = uploadMgr.getUploadBandwidthController();
        ByteBuffer byteBuffer = null;
        try
        {
            byteBuffer = ByteBuffer.allocate( BufferSize._16K );

            long lengthUploaded = 0;
            while ( response.remainingBody() > 0 )
            {
                // make sure we dont send more then requested
                int likeToSend = Math.min( response.remainingBody(), byteBuffer.capacity() );
                // we may be throttled to less than this amount
                int ableToSend = throttleController.getAvailableByteCount( likeToSend, true, false );
                byteBuffer.clear();
                byteBuffer.limit( ableToSend );
                int lengthRead = response.fillBody( byteBuffer );
                byteBuffer.flip();
                if ( !byteBuffer.hasRemaining() )
                {
                    break;
                }
                connection.write( byteBuffer );
                
                lengthUploaded += lengthRead;
    
                uploadState.setTransferredDataSize(lengthUploaded);
            }
        }
        finally
        {
            byteBuffer = null;
            response.close();
        }
    }

    private void readNextHTTPRequest() throws IOException
    {
        try
        {
            httpRequest = HTTPProcessor.parseHTTPRequest( connection );
            if ( uploadHandler.isQueued() && System.currentTimeMillis() 
                < uploadHandler.getQueueMinNextPollTime() )
            {// the request came too soon. Disconnect faulty client...
                throw new IOException("Queued host is requesting too soon.");
            }
        }
        catch (HTTPMessageException exp)
        {
            throw new IOException("Invalid HTTP Message: " + exp.getMessage());
        }
    }

    private void sendHTTPResponse( String httpResponseStr ) throws IOException
    {
        String logMsg = "HTTP Response: " + httpResponseStr;
        //NLogger.debug( UploadEngine.class, logMsg);
        uploadState.addToUploadLog( logMsg );
        connection.write( ByteBuffer.wrap( StringUtils.getBytesInUsAscii( 
            httpResponseStr ) ) );
    }
}