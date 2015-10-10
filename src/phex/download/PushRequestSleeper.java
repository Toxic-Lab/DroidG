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
import java.net.HttpURLConnection;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

import phex.common.address.DestAddress;
import phex.http.GnutellaHeaderNames;
import phex.http.HTTPHeaderNames;
import phex.http.HttpClientFactory;
import phex.msg.GUID;
import phex.msg.PushRequestMsg;
import phex.msghandling.MessageService;
import phex.net.repres.SocketFacade;
import phex.prefs.core.DownloadPrefs;
import phex.servent.Servent;
import phex.statistic.SimpleStatisticProvider;
import phex.statistic.StatisticsManager;

public class PushRequestSleeper
{
    private final StatisticsManager statsService;
    private final MessageService msgService;
    private final DestAddress serventAddress;
    private final GUID clientGUID;
    private final long fileIndex;
    private final DestAddress[] pushProxyAddresses;

    /**
     * The connection of the remote servent after he contacts us using the
     * PUSH request.
     */
    private SocketFacade givenSocket;

    public PushRequestSleeper( Servent servent, GUID aClientGUID, long aFileIndex, 
        DestAddress[] pushProxyAddresses )
    {
        this.statsService = servent.getStatisticsService();
        this.msgService = servent.getMessageService();
        this.serventAddress = servent.getLocalAddress();
        this.clientGUID = aClientGUID;
        this.fileIndex = aFileIndex;
        this.pushProxyAddresses = pushProxyAddresses;
    }

    public GUID getGUID()
    {
        return clientGUID;
    }

    /**
     * Returns the file index of the push request.
     */
    public long getFileIndex()
    {
        return fileIndex;
    }

    /**
     * we don't care about index or file name.. important is that we have a
     * open connection and we try to request what we want through it...
     * @param aGivenSocket
     * @param givenGUID
     * @return
     */
    public synchronized boolean acceptGIVConnection( SocketFacade aGivenSocket, GUID givenGUID )
    {
        if ( !clientGUID.equals( givenGUID ) )
        {
            return false;
        }

        // we have a give from the requested host with the correct id and file
        // name
        givenSocket = aGivenSocket;
        // wake up the sleeper
        notify();
        return true;
    }

    /**
     * Request the candidate socket via a push request. This call blocks until
     * the request times out or the requested host answers.
     * Null is returned if the connection can't be made.
     */
    public synchronized SocketFacade requestSocketViaPush()
    {
        boolean succ = false;
        try
        {
            if ( pushProxyAddresses != null && pushProxyAddresses.length > 0 )
            {
                succ = requestViaPushProxies();
            }
            
            if ( !succ )
            {
                succ = requestViaPushRoute();
            }
            
            if ( !succ )
            {
                return null;
            }
            
            try
            {
                // wait until the host connects to us or the timeout is reached
                wait( DownloadPrefs.PushRequestTimeout.get().intValue() );
            }
            catch ( InterruptedException exp )
            {// reset interruption
                Thread.currentThread().interrupt();
            }
            // no socket given during sleeping time.
            if ( givenSocket == null )
            {
                return null;
            }
            return givenSocket;
        }
        finally
        {
            PushHandler.unregisterPushRequestSleeper( this );
        }
    }
    
    private boolean requestViaPushProxies()
    {        
        // format: /gnet/push-proxy?guid=<ServentIdAsABase16UrlEncodedString>
        String requestPart = "/gnet/push-proxy?guid=" + clientGUID.toHexString();
        
        if ( pushProxyAddresses.length > 0 )
        {
            ((SimpleStatisticProvider)statsService.getStatisticProvider(
                StatisticsManager.PUSH_DLDPUSHPROXY_ATTEMPTS_PROVIDER)).increment(1);
        }
        
        for (int i = 0; i < pushProxyAddresses.length; i++)
        {
            String urlStr = "http://" + 
                    pushProxyAddresses[i].getFullHostName() + requestPart;
            //NLogger.debug( PushRequestSleeper.class, "PUSH via push proxy: " + urlStr );
            
            HttpClient httpClient = HttpClientFactory.createHttpClient();
            httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
                new DefaultHttpMethodRetryHandler( 1, false ) );
            
            httpClient.getParams().setSoTimeout( 10000 );
            httpClient.getHttpConnectionManager().getParams().setConnectionTimeout( 5000 );
            HeadMethod method = null;
            try
            {
                method = new HeadMethod( urlStr );
                method.addRequestHeader( GnutellaHeaderNames.X_NODE,
                    serventAddress.getFullHostName() );
                method.addRequestHeader( "Cache-Control", "no-cache");
                method.addRequestHeader( HTTPHeaderNames.CONNECTION,
                    "close" );
                
                int responseCode = httpClient.executeMethod( method );
                
                //NLogger.debug( PushRequestSleeper.class, "PUSH via push proxy response code: "
                //        + responseCode + " ("+urlStr+")" );
                
                // if 202
                if ( responseCode == HttpURLConnection.HTTP_ACCEPTED )
                {
                    ((SimpleStatisticProvider)statsService.getStatisticProvider(
                        StatisticsManager.PUSH_DLDPUSHPROXY_SUCESS_PROVIDER)).increment(1);
                    return true;
                }
            }
            catch ( IOException exp )
            {
                //NLogger.warn( PushRequestSleeper.class, exp );
            }
            finally
            {
                if ( method != null )
                {
                    method.releaseConnection();
                }
            }
        }
        return false;
    }
    
    /**
     * <p>Prepares and sends a push request via the push route.</p>
     *
     * <p>This will attempt to queue a push message to send back in response to
     * a query hit that needs push to fetch the file. This is used to help obtain
     * a socket to download a file from.</p>
     */
    private boolean requestViaPushRoute()
    {
        // pushing only works if we have a valid IP to use in the push message.
        if( serventAddress.getIpAddress() == null )
        {
            //NLogger.warn( PushRequestSleeper.class, "Local address has no IP to use for PUSH." );
            return false;
        }
        // according to the_gdf it is all right to send a push with a private
        // local address
        // http://groups.yahoo.com/group/the_gdf/message/14305
        PushRequestMsg push = new PushRequestMsg( clientGUID, fileIndex,
            serventAddress );
        
        // Route the PushRequestMsg.
        return msgService.routePushMessage( push );        
    }
}