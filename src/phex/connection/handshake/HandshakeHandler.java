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
 * 
 *  --- SVN Information ---
 *  $Id$
 */
package phex.connection.handshake;

import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.connection.ConnectionConstants;
import phex.host.Host;
import phex.http.GnutellaHeaderNames;
import phex.http.HTTPHeader;
import phex.http.HTTPHeaderGroup;
import phex.http.HTTPHeaderNames;
import phex.prefs.core.ConnectionPrefs;
import phex.servent.Servent;

public abstract class HandshakeHandler implements ConnectionConstants
{
    protected final Servent servent;
    protected final Host connectedHost;
    

    public HandshakeHandler( Servent servent, Host connectedHost )
    {
        this.servent = servent;
        this.connectedHost = connectedHost;
    }

    /**
     * The default handshake headers are used for incoming and outgoing
     * connections. They are usually extended by the specific incoming and
     * outgoing headers.
     * @return a default set of handshake headers.
     */
    protected HTTPHeaderGroup createDefaultHandshakeHeaders()
    {
        // create hash map based on common headers
        HTTPHeaderGroup openHeaders = new HTTPHeaderGroup(
            HTTPHeaderGroup.ACCEPT_HANDSHAKE_GROUP );

        // add Listen-IP even though it might be 127.0.0.1 the port is the
        // most important part...
        DestAddress myAddress = servent.getLocalAddress();
        openHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.LISTEN_IP,
            myAddress.getFullHostName() ) );

        // add remote-IP
        DestAddress remoteAddress = connectedHost.getHostAddress();
        IpAddress ipAddress = remoteAddress.getIpAddress();
        openHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.REMOTE_IP,
            ipAddress.getFormatedString() ) );
            
        // accepting deflate encoding
        if ( ConnectionPrefs.AcceptDeflateConnection.get().booleanValue() )
        {
            openHeaders.addHeader( new HTTPHeader(
                HTTPHeaderNames.ACCEPT_ENCODING, "deflate" ) );
        }

        return openHeaders;
    }
    
    public HTTPHeaderGroup createOutgoingHandshakeHeaders()
    {
        HTTPHeaderGroup outHeaders = createDefaultHandshakeHeaders();
        
        return outHeaders;
    }
    
    protected HandshakeStatus createCrawlerHandshakeStatus( )
    {
        // create hash map based on common headers
        HTTPHeaderGroup crawlerHeaders = new HTTPHeaderGroup( 
            HTTPHeaderGroup.COMMON_HANDSHAKE_GROUP );
        
        boolean isUltrapeer = servent.isUltrapeer();
        
        crawlerHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_ULTRAPEER, String.valueOf( isUltrapeer ) ) );
        
        if ( isUltrapeer )
        {
            // add connected leaves...
            Host[] leafs = servent.getHostService().getLeafConnections();
            if ( leafs.length > 0 )
            {
                String leafAddressString = buildHostAddressString(leafs, leafs.length );
                crawlerHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.LEAVES,
                    leafAddressString ) );
            }
        }
        
        // add connected ultrapeers        
        Host[] ultrapeers = servent.getHostService().getUltrapeerConnections();
        if ( ultrapeers.length > 0 )
        {
            String ultrapeerAddressString = buildHostAddressString(ultrapeers, ultrapeers.length );
            crawlerHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.PEERS,
                ultrapeerAddressString ) );
        }
        
        return new HandshakeStatus( STATUS_CODE_OK,
            STATUS_MESSAGE_OK, crawlerHeaders );
    }

    protected HTTPHeaderGroup createRejectOutgoingHeaders()
    {
        // create hash map based on common headers
        HTTPHeaderGroup openHeaders = new HTTPHeaderGroup(
            HTTPHeaderGroup.COMMON_HANDSHAKE_GROUP );

        return openHeaders;
    }
    
    // TODO2 add x-try-ultrapeer only with high hop (far) ultrapeers
    // to initial outgoing connection headers and to incoming accept headers.
    // currently we have no way to determine these far ultrapeers...
    protected HTTPHeaderGroup createRejectIncomingHeaders( )
    {
        // create hash map based on common headers
        HTTPHeaderGroup openHeaders = new HTTPHeaderGroup(
            HTTPHeaderGroup.COMMON_HANDSHAKE_GROUP );

        // add remote-IP
        openHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.REMOTE_IP,
            connectedHost.getHostAddress().getHostName() ) );
            
        // add X-Try-Ultrapeer
        Host[] ultrpeers = servent.getHostService().getUltrapeerConnections();
        String ultrapeerAddressString = buildHostAddressString(ultrpeers,
            10 );
        openHeaders.addHeader( new HTTPHeader( GnutellaHeaderNames.X_TRY_ULTRAPEERS,
            ultrapeerAddressString ) );

        return openHeaders;
    }

    public abstract HandshakeStatus createHandshakeResponse( HandshakeStatus hostResponse,
       boolean isOutgoing );
    
    public static HandshakeHandler createHandshakeHandler( Servent servent, Host connectedHost )
    {
        if ( servent.isAbleToBecomeUltrapeer() )
        {
            return new UltrapeerHandshakeHandler( servent, connectedHost );
        }
        // we dont support legacy peers anymore ( since 3.0 ) therefore we only
        // handle leaf mode here
        else
        {
            return new LeafHandshakeHandler( servent, connectedHost );
        }
    }
    
    protected String buildHostAddressString(Host[] hosts, int max )
    {
        StringBuffer buffer = new StringBuffer();
        max = Math.min( max, hosts.length );
        for ( int i = 0; i < max; i++ )
        {
            DestAddress address = hosts[i].getHostAddress();
            buffer.append( address.getFullHostName() );
            if( i < hosts.length - 1)
            {
                buffer.append(",");
            }
        }
        return buffer.toString();
    }

    protected boolean isCrawlerConnection(HTTPHeaderGroup headers)
    {
        HTTPHeader crawlerHeader = headers.getHeader( GnutellaHeaderNames.CRAWLER );
        if ( crawlerHeader == null )
        {
            return false;
        }
        float crawlerVersion;
        try
        {
            crawlerVersion = crawlerHeader.floatValue();
            if ( crawlerVersion >= 0.1f )
            {
                return true;
            }
            return false;
        }
        catch ( NumberFormatException exp )
        {
            return false;
        }
    }
}