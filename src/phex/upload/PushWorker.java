/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2007 Phex Development Group
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
 */
package phex.upload;

import java.io.IOException;

import phex.common.Environment;
import phex.common.bandwidth.BandwidthController;
import phex.http.HTTPMessageException;
import phex.http.HTTPProcessor;
import phex.http.HTTPRequest;
import phex.io.buffer.ByteBuffer;
import phex.msg.PushRequestMsg;
import phex.net.connection.Connection;
import phex.net.connection.SocketFactory;
import phex.net.repres.SocketFacade;
import phex.servent.Servent;
import phex.share.HttpRequestDispatcher;
import phex.share.ShareFile;
import phex.statistic.SimpleStatisticProvider;
import phex.statistic.StatisticProviderConstants;
import phex.statistic.StatisticsManager;
import phex.utils.StringUtils;
import phex.utils.URLCodecUtils;

public class PushWorker implements Runnable
{
    private static final int PUSH_TIMEOUT = 45000;

    private final UploadManager uploadMgr;
    private final PushRequestMsg pushMsg;

    private Connection connection;

    public PushWorker(PushRequestMsg msg, UploadManager uploadMgr )
    {
        this.uploadMgr = uploadMgr;
        pushMsg = msg;
        Environment.getInstance().executeOnThreadPool( this,
            "PushWorker-" + Integer.toHexString( hashCode() ) );
        
        StatisticsManager statMgr = Servent.getInstance().getStatisticsService();
        ((SimpleStatisticProvider) statMgr.getStatisticProvider(
            StatisticProviderConstants.PUSH_UPLOAD_ATTEMPTS_PROVIDER)).increment( 1 );
    }

    public void run()
    {
        HTTPRequest httpRequest;
        try
        {
            httpRequest = connectAndGetRequest();
            if ( httpRequest == null )
            {
                StatisticsManager statMgr = Servent.getInstance().getStatisticsService();
                ((SimpleStatisticProvider) statMgr.getStatisticProvider(
                    StatisticProviderConstants.PUSH_UPLOAD_FAILURE_PROVIDER)).increment( 1 );
                return;
            }
            handleRequest( httpRequest );

            //NLogger.debug( PushWorker.class, 
            //    "PushWorker finished" );
        }
        catch (Exception exp)
        {
            //NLogger.error( PushWorker.class, exp, exp );
            return;
        }
        finally
        {
            if ( connection != null )
            {
                connection.disconnect();
            }
        }
    }

    /**
     * @param httpRequest
     * @throws IOException
     */
    private void handleRequest(HTTPRequest httpRequest)
    {
    	//NLogger.debug( PushWorker.class, "Handle PUSH request: "
        //    + httpRequest.buildHTTPRequestString() );
    	StatisticsManager statMgr = Servent.getInstance().getStatisticsService();
        ((SimpleStatisticProvider) statMgr.getStatisticProvider(
            StatisticProviderConstants.PUSH_UPLOAD_SUCESS_PROVIDER)).increment( 1 );
        if ( httpRequest.isGnutellaRequest() )
        {
            uploadMgr.handleUploadRequest( connection, httpRequest );
        }
        else
        {
            // Handle the HTTP GET as a normal HTTP GET to upload file.
            // This is most likely a usual browse host request.
            new HttpRequestDispatcher().httpRequestHandler( connection,
                httpRequest );
        }
    }

    /**
     * @return
     */
    private HTTPRequest connectAndGetRequest()
    {
        try
        {
            HTTPRequest httpRequest;
            //NLogger.debug( PushWorker.class,
            //    "Try PUSH connect to: " + pushMsg.getRequestAddress() );
            SocketFacade sock = SocketFactory.connect( pushMsg.getRequestAddress(),
                PUSH_TIMEOUT );
            BandwidthController bwController = uploadMgr.getUploadBandwidthController();
            connection = new Connection( sock, bwController );
            sendGIV( connection );
            httpRequest = HTTPProcessor.parseHTTPRequest( connection );
            return httpRequest;
        }
        catch (IOException exp)
        {
        	//NLogger.debug( PushWorker.class, exp );
            return null;
        }
        catch (HTTPMessageException exp)
        {
        	//NLogger.debug( PushWorker.class, exp );
            return null;
        }
    }

    /**
     * @param connection
     * @throws IOException
     */
    private void sendGIV(Connection connection) throws IOException
    {
        Servent servent = Servent.getInstance();
        // I only give out file indexes in the int range
        ShareFile sfile = servent.getSharedFilesService().getFileByIndex(
            (int) pushMsg.getFileIndex() );

        // Send the push greeting.
        // GIV <file-ref-num>:<ClientID GUID in hexdec>/<filename>\n\n
        StringBuffer buffer = new StringBuffer( 100 );
        buffer.append( "GIV " );
        buffer.append( pushMsg.getFileIndex() );
        buffer.append( ':' );
        buffer.append( pushMsg.getClientGUID().toHexString() );
        buffer.append( '/' );
        if ( sfile != null )
        {
            buffer.append( URLCodecUtils.encodeURL( sfile.getFileName() ) );
        }
        else
        {
            buffer.append( "file" );
        }
        buffer.append( "\n\n" );
        //NLogger.debug( PushWorker.class, "Send GIV: "
        //    + buffer.toString() );
        
        connection.write( ByteBuffer.wrap( 
            StringUtils.getBytesInUsAscii( buffer.toString() ) ) );

        connection.flush();
    }
}