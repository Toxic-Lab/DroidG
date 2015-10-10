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

import java.io.IOException;

import phex.connection.ConnectionConstants;
import phex.connection.ProtocolNotSupportedException;
import phex.http.HTTPHeaderGroup;
import phex.http.HTTPHeaderNames;
import phex.http.HTTPProcessor;
import phex.net.connection.Connection;
import phex.prefs.core.ConnectionPrefs;

public class HandshakeStatus implements ConnectionConstants
{
    private int statusCode;
    private String statusMessage;
    private HTTPHeaderGroup responseHeaders;

    public HandshakeStatus( int statusCode, String statusMessage )
    {
        this( statusCode, statusMessage, HTTPHeaderGroup.EMPTY_HEADERGROUP );
    }

    public HandshakeStatus( int statusCode, String statusMessage,
        HTTPHeaderGroup responseHeaders )
    {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.responseHeaders = responseHeaders;
    }

    public HandshakeStatus( HTTPHeaderGroup responseHeaders )
    {
        this( STATUS_CODE_OK, STATUS_MESSAGE_OK, responseHeaders );
    }


    public int getStatusCode()
    {
        return statusCode;
    }

    public String getStatusMessage()
    {
        return statusMessage;
    }

    public HTTPHeaderGroup getResponseHeaders()
    {
        return responseHeaders;
    }
    
    /**
     * Return true is the we accept deflate connections and the remote host
     * accepts a deflate encoding.
     * @return true if deflate is accepted, false otherwise.
     */
    public boolean isDeflateAccepted()
    {
        if ( ConnectionPrefs.AcceptDeflateConnection.get().booleanValue() &&
            responseHeaders.isHeaderValueContaining(  
            HTTPHeaderNames.ACCEPT_ENCODING, "deflate" ) )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static HandshakeStatus parseHandshakeResponse(
        Connection connection )
        throws ProtocolNotSupportedException, IOException
    {
        String response = connection.readLine();
        if ( response == null )
        {
            throw new IOException(
                "Disconnected from remote host during initial handshake" );
        }
        if ( !response.startsWith( GNUTELLA_06 ) )
        {
            throw new ProtocolNotSupportedException(
                "Bad protocol response: " + response );
        }

        // read response headers
        HTTPHeaderGroup responseHeaders = HTTPProcessor.parseHTTPHeaders( connection );

        int statusCode;
        String statusMessage;
        try
        {
            String statusString = response.substring( GNUTELLA_06.length() ).trim();
            int statusMsgIdx = statusString.indexOf( ' ' );
            if ( statusMsgIdx == -1 )
            {
                statusMsgIdx = statusString.length();
            }
            statusCode = Integer.parseInt( statusString.substring( 0, statusMsgIdx ) );
            statusMessage = statusString.substring( statusMsgIdx ).trim();
        }
        catch ( Exception exp )
        {
//            NLogger.warn(HandshakeStatus.class, "Error parsing response: '"
//                + response + "'.", exp );
            throw new IOException( "Error parsing response: '"
                + response + "': " + exp );
        }

        return new HandshakeStatus( statusCode, statusMessage, responseHeaders );
    }
}