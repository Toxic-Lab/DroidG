/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2009 Phex Development Group
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
package phex.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;

import org.xsocket.datagram.Endpoint;
import org.xsocket.datagram.IDatagramHandler;
import org.xsocket.datagram.IEndpoint;
import org.xsocket.datagram.UserDatagram;

import phex.common.Environment;
import phex.common.address.AddressUtils;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.utils.IOUtil;

public class UdpService implements IDatagramHandler
{
    private static final int RECV_SEND_BUFFER_SIZE = 64 * 1024;
    //private static final Logger logger = LoggerFactory.getLogger( UdpService.class );
    
    private final InetAddress bindAddress;
    private final int bindPort;
    
    private UdpDataHandler udpDataHandler;
    
    private volatile boolean isRunning;
    
    private IEndpoint endpoint;
    
    public UdpService( int bindPort )
    {
        this( null, bindPort, null );
    }
    
    public UdpService( int bindPort, UdpDataHandler udpDataHandler )
    {
        this( null, bindPort, udpDataHandler );
    }
    
    public UdpService( InetAddress bindAddress, int bindPort )
    {
        this( bindAddress, bindPort, null );
    }
    
    public UdpService( InetAddress bindAddress, int bindPort, UdpDataHandler udpDataHandler )
    {
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
        this.udpDataHandler = udpDataHandler;
    }
    
    /**
     * @param udpDataHandler the udpDataHandler to set
     */
    public void setUdpDataHandler(UdpDataHandler udpDataHandler)
    {
        this.udpDataHandler = udpDataHandler;
    }

    public synchronized void startup() throws IOException
    {
        if (isRunning)
        {
            return;
        }
        
        //Map<String, Object> options = new HashMap<String, Object>();
        //options.put( IEndpoint.SO_RCVBUF, RECV_SEND_BUFFER_SIZE );
        //options.put( IEndpoint.SO_SNDBUF, RECV_SEND_BUFFER_SIZE );
        endpoint = new Endpoint( RECV_SEND_BUFFER_SIZE, this,
            Environment.getInstance().getThreadPool(),
            bindAddress, bindPort );
        
        isRunning = true;
    }
    
    public synchronized void shutdown( )
    {
        // not running, already dead or been requested to die.
        if ( !isRunning )
        {
            return;
        }
        IOUtil.closeQuietly( endpoint );
        isRunning = false;
    }
    
    public void sendDatagram( byte[] data, DestAddress address ) throws IOException
    {
        if ( !isRunning )
        {
            return;
        }
        SocketAddress socketAddress = AddressUtils.createSocketAddress( address );
        UserDatagram datagram = new UserDatagram( socketAddress, data );
        //logger.debug( "Sending Datagram {}", datagram );
        endpoint.send( datagram );
    }

    ///////////////////////////////////////////////////////////////////////////
    ////////////////////////// IDatagramHandler ///////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    
    public boolean onDatagram( IEndpoint rcvEndpoint )
    {
        // Unfortunately xSocket 2.2 silently catches away all exception.
        // to see errors we need to handle them here...
        try
        {
            UserDatagram datagram = rcvEndpoint.receive();
            if ( udpDataHandler != null )
            {
                DefaultDestAddress addr = new DefaultDestAddress( 
                    datagram.getRemoteAddress().getAddress(),
                    datagram.getRemotePort() );
                udpDataHandler.handleUdpData( datagram, addr );
            }
            return true;
        }
        catch ( Exception exp )
        {
            //logger.error( exp.toString(), exp );
            throw new RuntimeException( exp );
        }
    }
}