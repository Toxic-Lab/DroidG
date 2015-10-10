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
 *  Created on 05.03.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.net.connection;

import java.io.IOException;

import phex.common.address.DestAddress;
import phex.common.bandwidth.BandwidthController;
import phex.net.repres.SocketFacade;

/**
 *
 */
public class ConnectionFactory
{
    private ConnectionFactory()
    {}
    
    /**
     * Creates a connection to the given host and port with Cfg.socketConnectTimeout.
     * The connection returned will use the given bandwidth controller 
     * and the Cfg.socketRWTimeout as the read/write timeout.
     * 
     * @param address the host address to connect.
     * @param bwController the bandwidth controller to initialize the connection with.
     * @return a connection
     * @throws IOException in case an connection error occurs.
     */
    public static Connection createConnection( DestAddress address, BandwidthController bwController )
        throws IOException
    {
        SocketFacade socket = SocketFactory.connect( address );        
        Connection connection = new Connection( socket, bwController );
        
        return connection;
    }
    
    /**
     * Creates a connection to the given host and port with Cfg.socketConnectTimeout.
     * The connection returned will use the given bandwidth controller 
     * and the Cfg.socketRWTimeout as the read/write timeout.
     * 
     * @param address the host address to connect.
     * @param connectTimeout the connect timeout in millis
     * @param bwController the bandwidth controller to initialize the connection with.
     * @return a connection
     * @throws IOException in case an connection error occurs.
     */
    public static Connection createConnection( DestAddress address, int connectTimeout, 
        BandwidthController bwController )
        throws IOException
    {
        SocketFacade socket = SocketFactory.connect( address, connectTimeout );
        Connection connection = new Connection( socket, bwController );
        
        return connection;
    }
}
