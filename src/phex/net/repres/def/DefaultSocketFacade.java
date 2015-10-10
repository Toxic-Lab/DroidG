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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.ByteChannel;

import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.io.channels.StreamingByteChannel;
import phex.net.repres.SocketFacade;

public class DefaultSocketFacade implements SocketFacade
{
    private DestAddress remoteAddress;
    private Socket socket;
    private StreamingByteChannel channel;
    
    public DefaultSocketFacade( Socket aSocket )
    {
        socket = aSocket;
    }
    
    public ByteChannel getChannel() throws IOException
    {
        if ( channel == null )
        {
            channel = new StreamingByteChannel( socket );
        }
        return channel;
    }

    public void setSoTimeout( int socketRWTimeout )
        throws SocketException
    {
        socket.setSoTimeout(socketRWTimeout);
    }
    
    public void close() throws IOException
    {
        socket.close();
    }
    
    public DestAddress getRemoteAddress()
    {
        if ( remoteAddress == null )
        {
            remoteAddress = new DefaultDestAddress(
                socket.getInetAddress().getHostAddress(), socket.getPort() );
        }
        return remoteAddress;
    }
}
