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

import phex.connection.ConnectionConstants;
import phex.host.Host;
import phex.host.NetworkHostsContainer;
import phex.http.GnutellaHeaderNames;
import phex.http.HTTPHeader;
import phex.http.HTTPHeaderGroup;
import phex.http.HTTPHeaderNames;
import phex.prefs.core.ConnectionPrefs;
import phex.prefs.core.MessagePrefs;
import phex.servent.Servent;

public class UltrapeerHandshakeHandler extends HandshakeHandler
    implements ConnectionConstants
{
    public UltrapeerHandshakeHandler( Servent servent, Host connectedHost )
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
            GnutellaHeaderNames.X_ULTRAPEER, "true" ) );
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
        if ( isOutgoing )
        {
            return createOutgoingResponse( hostResponse );
        }
        else
        {
            return createIncomingResponse( hostResponse );
        }
    }

    private HandshakeStatus createIncomingResponse(HandshakeStatus hostResponse)
    {
        HTTPHeaderGroup headers = hostResponse.getResponseHeaders();
        
        boolean isCrawler = isCrawlerConnection( headers );
        if ( isCrawler )
        {
            return createCrawlerHandshakeStatus();
        }
        
        // check ultrapeer header
        HTTPHeader upHeader = headers.getHeader( GnutellaHeaderNames.X_ULTRAPEER );
        
        if ( !isConnectionAccepted( upHeader ) )
        {
            return new HandshakeStatus( STATUS_CODE_REJECTED,
                STATUS_MESSAGE_BUSY, createRejectIncomingHeaders() );
        }
        HTTPHeaderGroup myHeaders = createDefaultHandshakeHeaders();
    
        // add ultrapeer needed header for leaf guidance
        if ( upHeader != null && Boolean.valueOf( upHeader.getValue() ).booleanValue() )
        {
            boolean isUltrapeerNeeded = servent.getHostService().
                getNetworkHostsContainer().hasUltrapeerSlotsAvailable();
            String isUltrapeedNeededStr = isUltrapeerNeeded ? "true" :
                "false";
            myHeaders.addHeader( new HTTPHeader(
                GnutellaHeaderNames.X_ULTRAPEER_NEEDED, isUltrapeedNeededStr ) );
        }
        
        // support for deflate... if accepted..
        if ( hostResponse.isDeflateAccepted() )
        {
            myHeaders.addHeader( new HTTPHeader(
                HTTPHeaderNames.CONTENT_ENCODING, "deflate" ) );
        }
        
        return new HandshakeStatus( STATUS_CODE_OK, STATUS_MESSAGE_OK,
            myHeaders );
    }

    private HandshakeStatus createOutgoingResponse(
        HandshakeStatus hostResponse )
    {
        HTTPHeaderGroup headers = hostResponse.getResponseHeaders();
        
        // check ultrapeer header
        HTTPHeader upHeader = headers.getHeader( GnutellaHeaderNames.X_ULTRAPEER );
        
        // can we accept the connection??
        if ( !isConnectionAccepted( upHeader ) )
        {
            return new HandshakeStatus( STATUS_CODE_REJECTED,
                STATUS_MESSAGE_BUSY, createRejectOutgoingHeaders() );
        }
        
        HTTPHeaderGroup myHeaders = new HTTPHeaderGroup(
            HTTPHeaderGroup.COMMON_HANDSHAKE_GROUP );
        
        HTTPHeader upNeededHeader = headers.getHeader(
            GnutellaHeaderNames.X_ULTRAPEER_NEEDED );
        // if no ultrapeer is needed and we are able to become a leaf node
        // and we are not talking to a bearshare, since bearshare will not
        // accept us as a leaf.
        if ( upNeededHeader != null && !upNeededHeader.booleanValue() &&
            !isBearshare( headers ) && servent.allowDowngradeToLeaf() )
        {
            // create new HTTPHeaderGroup since we used empty headers earlier.
            myHeaders = new HTTPHeaderGroup( false );
            myHeaders.addHeader( new HTTPHeader(
               GnutellaHeaderNames.X_ULTRAPEER, "false" ) );
        }
        
        // support for deflate... if accepted..
        if ( hostResponse.isDeflateAccepted() )
        {
            myHeaders.addHeader( new HTTPHeader(
                HTTPHeaderNames.CONTENT_ENCODING, "deflate" ) );
        }
        
        return new HandshakeStatus( STATUS_CODE_OK, STATUS_MESSAGE_OK,
            myHeaders );
    }

    private boolean isConnectionAccepted( HTTPHeader upHeader )
    {
        // this is a legacy peer connection
        if ( upHeader == null )
        {
            return false;
        }

        NetworkHostsContainer netHostContainer = servent.getHostService().getNetworkHostsContainer();
        // this is an Ultrapeer connection
        // we accept it if we have ultrapeer slots or leaf slots for ultrapeers
        // (for leaf guidance) unfortunately we don't know if leaf guidance is accepted
        // though..
        if ( Boolean.valueOf( upHeader.getValue() ).booleanValue() )
        {
            if ( netHostContainer.hasUltrapeerSlotsAvailable() ||
                netHostContainer.hasLeafSlotForUltrapeerAvailable() )
            {
                return true;
            }
        }
        // this is a Leaf connection
        else if ( netHostContainer.hasLeafSlotsAvailable() ) // upHeader == false
        {
            return true;
        }

        return false;
    }

    private boolean isBearshare( HTTPHeaderGroup headers )
    {
        HTTPHeader header = headers.getHeader( HTTPHeaderNames.USER_AGENT );
        if ( header != null && header.getValue().startsWith( "BearShare" ) )
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}