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
 * 
 *  Created on 29.10.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.net.repres.def;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import phex.common.address.*;
import phex.net.repres.PresentationManager;
import phex.net.repres.SocketFacade;

public class DefaultPresentationManager extends PresentationManager
{
    @Override
    public SocketFacade createSocket( DestAddress address, int connectTimeout )
        throws IOException
    {
        Socket socket = new Socket();
        socket.connect( new InetSocketAddress( 
            address.getHostName(), address.getPort() ), connectTimeout );
        return new DefaultSocketFacade( socket );
    }

    /**
     * Creates a host address object from a given address representation.
     * The address representation we expect is hostname:port.
     * 
     * @param address a address representation.
     * @return a destination address.
     */
    public DestAddress createHostAddress( String address, int defaultPort ) 
        throws MalformedDestAddressException
    {
        int idx = address.indexOf(':');
        if ( idx == 0 )
        {
            throw new MalformedDestAddressException( "No host name: "
                + address );
        }
        String hostName;
        int port;
        if ( idx < 0 )
        {
            hostName = address;
            port = DefaultDestAddress.DEFAULT_PORT;
        }
        else
        {
            hostName = address.substring( 0, idx );
            port = AddressUtils.parsePort( address );
            if ( port < 0 )
            {
                throw new MalformedDestAddressException( "Invalid port: " +
                    address );
            }
        }
        
        DestAddress hostAddress = new DefaultDestAddress( hostName, port );
        return hostAddress;
    }
    
    /**
     * Creates a DestAddress object from a given IpAddress and port.
     * @param ipAddress the IpAddress of the new DestAddress.
     * @param port the port of the new DestAddress.
     * @return a destination address.
     */
    public DestAddress createHostAddress( IpAddress ipAddress, int port )
    {
        DestAddress destAddress = new DefaultDestAddress( ipAddress, port );
        return destAddress;
    }
}