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
package phex.connection;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.ChunkedInputStream;

import phex.common.address.DestAddress;
import phex.download.PushHandler;
import phex.http.HTTPHeader;
import phex.http.HTTPHeaderNames;
import phex.http.HTTPMessageException;
import phex.http.HTTPProcessor;
import phex.http.HTTPRequest;
import phex.http.HTTPResponse;
import phex.io.buffer.ByteBuffer;
import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.MessageProcessor;
import phex.msg.MsgHeader;
import phex.msg.QueryResponseMsg;
import phex.net.connection.Connection;
import phex.net.connection.SocketFactory;
import phex.net.repres.SocketFacade;
import phex.prefs.core.MessagePrefs;
import phex.query.BrowseHostResults;
import phex.servent.Servent;

/**
 * This class implements the basic functionality of the new Browse Host protocol.
 */
public class BrowseHostConnection
{
    private final Servent servent;
    private final BrowseHostResults results;
    private final DestAddress address;
    private final GUID hostGUID;

    public BrowseHostConnection( Servent servent, DestAddress aAddress, 
        GUID aHostGUID, BrowseHostResults results )
    {
        this.servent = servent;
        address = aAddress;
        hostGUID = aHostGUID;
        this.results = results;
    }

    public void sendBrowseHostRequest()
        throws IOException, BrowseHostException
    {
        //NLogger.debug(BrowseHostConnection.class,
           // "Connection for Browse Host to " + address );
        results.setBrowseHostStatus( BrowseHostResults.BrowseHostStatus.CONNECTING );
        SocketFacade socket;
        try
        {
            socket = SocketFactory.connect( address );
        }
        catch ( IOException exp )
        {// standard connection failed try push request, if we have a hostGUID
            if ( hostGUID == null )
            {
                throw exp;
            }

            socket = PushHandler.requestSocketViaPush( servent, hostGUID,
                // HEX for Phex
                50484558 );
            if ( socket == null )
            {
                throw new IOException( "Push request failed" );
            }
        }
        
        Connection connection = new Connection( socket, 
            servent.getBandwidthService().getNetworkBandwidthController() );


        HTTPRequest request = new HTTPRequest( "GET", "/", true );
        request.addHeader( new HTTPHeader( HTTPHeaderNames.HOST, address.getFullHostName() ) );
        request.addHeader( new HTTPHeader( HTTPHeaderNames.ACCEPT,
        //    "text/html, application/x-gnutella-packets" ) );
            "application/x-gnutella-packets" ) );
        request.addHeader( new HTTPHeader( HTTPHeaderNames.CONTENT_LENGTH, "0" ) );
        request.addHeader( new HTTPHeader( HTTPHeaderNames.CONNECTION, "close" ) );

        String httpRequestStr = request.buildHTTPRequestString();
        //NLogger.debug(BrowseHostConnection.class,
            //"Sending Browse Host request: " + httpRequestStr );
        connection.write( ByteBuffer.wrap( httpRequestStr.getBytes() ) );

        HTTPResponse response;
        try
        {
            response = HTTPProcessor.parseHTTPResponse( connection );
        }
        catch ( HTTPMessageException exp )
        {
            throw new BrowseHostException( "Invalid HTTP Response: " + exp.getMessage() );
        }
        //NLogger.debug(BrowseHostConnection.class,
            //"Received Browse Host response: " + response.buildHTTPResponseString() );

        if ( response.getStatusCode() < 200 || response.getStatusCode() > 299 )
        {
            throw new BrowseHostException( "Browse host request not successfull. StatusCode: " +
                response.getStatusCode() + " " + response.getStatusReason() );
        }

        HTTPHeader typeHeader = response.getHeader( HTTPHeaderNames.CONTENT_TYPE );
        if ( typeHeader == null )
        {
            throw new BrowseHostException( "Unknwon content-type." );
        }
        
        InputStream inStream = connection.getInputStream();
        HTTPHeader encHeader = response.getHeader( HTTPHeaderNames.TRANSFER_ENCODING );
        if ( encHeader != null )
        {
            if ( encHeader.getValue().equals("chunked") )
            {
                inStream = new ChunkedInputStream( inStream );
            }
        }

        if ( typeHeader.getValue().equals( "application/x-gnutella-packets" ) )
        {
            results.setBrowseHostStatus( BrowseHostResults.BrowseHostStatus.FETCHING );
            byte[] headerBuffer = new byte[ MsgHeader.DATA_LENGTH ];
            while( true )
            {
                MsgHeader header = MessageProcessor.parseMessageHeader( inStream,
                    headerBuffer );
                if ( header == null )
                {
                    break;
                }
                if ( header.getPayload() != MsgHeader.QUERY_HIT_PAYLOAD )
                {
                    throw new BrowseHostException( "Wrong header payload. Expecting query hit: " + header.getPayload() );
                }
                int length = header.getDataLength();
                if ( length < 0 )
                {
                    throw new IOException( "Negative body size. Drop." );
                }
                else if ( length > MessagePrefs.MaxLength.get().intValue() )
                {
                    throw new IOException("Packet too big ("+length+"). Drop.");
                }
                try
                {
                    QueryResponseMsg message = ( QueryResponseMsg )MessageProcessor.parseMessage(
                        header, inStream, servent.getSecurityService() );
                    // prevent further routing of query response message...
                    message.getHeader().setTTL( (byte) 0 );
                    
                    // Unfortunately we don't have a host instance..
                    servent.getMessageService().dispatchMessage( message, null );
                    results.processResponse( message );
                }
                catch ( InvalidMessageException exp )
                {
                    //NLogger.debug(BrowseHostConnection.class, exp, exp);
                    throw new IOException( "Invalid message returned: "
                        + exp.getMessage() );
                }
            }
        }
        else
        {
            throw new BrowseHostException( "Not supported content-type. " + typeHeader.getValue() );
        }
    }
}