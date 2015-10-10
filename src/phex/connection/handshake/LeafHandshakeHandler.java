/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Phex Development Group
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
package phex.connection.handshake;

import phex.connection.ConnectionConstants;
import phex.host.Host;
import phex.http.GnutellaHeaderNames;
import phex.http.HTTPHeader;
import phex.http.HTTPHeaderGroup;
import phex.http.HTTPHeaderNames;
import phex.prefs.core.ConnectionPrefs;
import phex.prefs.core.MessagePrefs;
import phex.servent.Servent;

public class LeafHandshakeHandler extends HandshakeHandler
    implements ConnectionConstants
{
    public LeafHandshakeHandler( Servent servent, Host connectedHost )
    {
        super( servent, connectedHost );
    }

    @Override
    protected HTTPHeaderGroup createDefaultHandshakeHeaders()
    {
        // create hash map based on common headers
        HTTPHeaderGroup openHeaders = super.createDefaultHandshakeHeaders();

        // add ultrapeer headers...
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_ULTRAPEER, "false" ) );
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_QUERY_ROUTING, "0.1" ) );
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_UP_QUERY_ROUTING, "0.1" ) );
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_DYNAMIC_QUERY, "0.1") );
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_DEGREE,
            ConnectionPrefs.Up2UpConnections.get().toString() ) );
        openHeaders.addHeader( new HTTPHeader(
            GnutellaHeaderNames.X_MAX_TTL,
            String.valueOf( MessagePrefs.DEFAULT_DYNAMIC_QUERY_MAX_TTL ) ) );

        return openHeaders;
    }

    @Override
    public HandshakeStatus createHandshakeResponse( HandshakeStatus hostResponse,
       boolean isOutgoing )
    {
        HTTPHeaderGroup headers = hostResponse.getResponseHeaders();
        if ( isOutgoing && isCrawlerConnection( headers ) )
        {
            return createCrawlerHandshakeStatus();            
        }
        
        // check ultrapeer header
        HTTPHeader header = headers.getHeader( GnutellaHeaderNames.X_ULTRAPEER );
        boolean isUltrapeer = header != null && Boolean.valueOf(
            header.getValue() ).booleanValue();
        if ( !isUltrapeer )
        {
            if ( isOutgoing )
            {
                // no additional headers on outgoing response...
                return new HandshakeStatus( STATUS_CODE_REJECTED,
                    STATUS_MESSAGE_ACCEPT_ONLY_UP );
            }
            else
            {
                return new HandshakeStatus( STATUS_CODE_REJECTED,
                    STATUS_MESSAGE_ACCEPT_ONLY_UP,
                    createRejectIncomingHeaders() );
            }
        }
        
        connectedHost.setConnectionType( Host.CONNECTION_LEAF_UP );

        if ( isOutgoing )
        {
            // in case of outgoing connection I try to accept every connection
            // type to get my slots full and be well connected...
            
            HTTPHeaderGroup myHeaders = new HTTPHeaderGroup(
                HTTPHeaderGroup.COMMON_HANDSHAKE_GROUP );
            
            // support for deflate... if accepted..
            if ( hostResponse.isDeflateAccepted() )
            {
                myHeaders.addHeader( new HTTPHeader(
                    HTTPHeaderNames.CONTENT_ENCODING, "deflate" ) );
            }            

            return new HandshakeStatus( STATUS_CODE_OK, STATUS_MESSAGE_OK,
                myHeaders );
        }
        else
        {
            if ( servent.isShieldedLeafNode() )
            {
                // a none ultrapeer incoming connections and I'm a shielded leaf..
                // we don't accept this on incoming...
                return new HandshakeStatus( STATUS_CODE_REJECTED,
                    STATUS_MESSAGE_SHIELDED_LEAF, createRejectIncomingHeaders() );
            }
            
            HTTPHeaderGroup myHeaders = createDefaultHandshakeHeaders();
            
            // support for deflate... if accepted..
            if ( hostResponse.isDeflateAccepted() )
            {
                myHeaders.addHeader( new HTTPHeader(
                    HTTPHeaderNames.CONTENT_ENCODING, "deflate" ) );
            }

            return new HandshakeStatus( STATUS_CODE_OK, STATUS_MESSAGE_OK,
                myHeaders );
        }
    }
}