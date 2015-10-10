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
package phex.net.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import phex.common.Environment;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.connection.IncomingConnectionDispatcher;
import phex.host.NetworkHostsContainer;
import phex.net.repres.PresentationManager;
import phex.net.repres.SocketFacade;
import phex.net.repres.def.DefaultSocketFacade;
import phex.prefs.core.NetworkPrefs;
import phex.security.AccessType;
import phex.security.PhexSecurityException;
import phex.servent.Servent;

public class OIOServer extends Server
{
    public OIOServer( Servent servent )
    {
        super( servent );
    }

    // The listening thread.
    public void run()
    {
//        if (////NLogger.isDebugEnabled( OIOServer.class ))
//            ////NLogger.debug( OIOServer.class,
//                "Listener started. Listening on: "
//                    + serverSocket.getInetAddress().getHostAddress() + ':'
//                    + serverSocket.getLocalPort());
        
        try
        {
            while ( serverSocket != null && !serverSocket.isClosed() )
            {  
                try
                {
                    Socket incoming = serverSocket.accept();
                    // create facade...
                    DefaultSocketFacade incomingFacade = new DefaultSocketFacade( 
                        incoming );
                    handleIncomingSocket( incomingFacade );
                }
                catch ( SocketException exp )
                {
                    //NLogger.debug( OIOServer.class, exp );
                }
                catch ( PhexSecurityException exp )
                {
                    //NLogger.debug( OIOServer.class, exp );
                }
                catch (IOException exp)
                {
                    //NLogger.error( OIOServer.class, exp, exp);
                }
            }
        }
        catch ( Exception exp )
        {
            //NLogger.error( OIOServer.class, exp, exp );
        }

        isRunning = false;
        //NLogger.debug( OIOServer.class, "Listener stopped.");
        DestAddress newAddress = PresentationManager.getInstance().createHostAddress(
            IpAddress.LOCAL_HOST_IP, DefaultDestAddress.DEFAULT_PORT );
        localAddress.updateLocalAddress( newAddress );
        synchronized(this)
        {
            notifyAll();
        }
    }

    /**
     * @param socketChannel
     * @throws PhexSecurityException
     * @throws SocketException
     */
    private void handleIncomingSocket(SocketFacade clientSocket )
        throws IOException, PhexSecurityException
    {        
        clientSocket.setSoTimeout( NetworkPrefs.TcpRWTimeout.get().intValue() );

        DestAddress address = clientSocket.getRemoteAddress();
        NetworkHostsContainer netHostsContainer = servent.getHostService()
            .getNetworkHostsContainer();

        // if not already connected and connection is not from a private address.
        IpAddress remoteIp = address.getIpAddress();
        assert remoteIp != null;
        if (!netHostsContainer.isConnectedToHost(address)
            && !remoteIp.isSiteLocalIP() )
        {
            hasConnectedIncomming = true;
            lastInConnectionTime = System.currentTimeMillis();
        }
        
        // Set this will defeat the Nagle Algorithm, making short bursts of
        // transmission faster, but will be worse for the overall network.
        // incoming.setTcpNoDelay(true);

        // Create a Host object for the incoming connection
        // and hand it off to a ReadWorker to handle.
        AccessType access = servent.getSecurityService()
            .controlHostAddressAccess(address);
        switch (access)
        {
            case ACCESS_DENIED:
            case ACCESS_STRONGLY_DENIED:
                throw new PhexSecurityException("Host access denied: " + address );
        }

//        //NLogger.debug( OIOServer.class, 
//            "Accepted incoming connection from: "
//                + address.getFullHostName());

        IncomingConnectionDispatcher dispatcher = new IncomingConnectionDispatcher(
            clientSocket, servent );
        Environment.getInstance().executeOnThreadPool( dispatcher,
            "IncomingConnectionDispatcher-" + Integer.toHexString(hashCode()));
    }

    @Override
    protected synchronized void bind( int initialPort ) throws IOException
    {
        assert (serverSocket == null);

        serverSocket = new ServerSocket();

        // Create a listening socket at the port.
        int tries = 0;
        boolean error;
        int tryingPort = initialPort;
        // try to find new port if port not valid
        do
        {
            error = false;

            try
            {
                //NLogger.debug( OIOServer.class, "Binding to port " + tryingPort );
                serverSocket.bind(new InetSocketAddress( tryingPort ));
            }
            catch (SocketException exp)
            {
                //NLogger.debug( OIOServer.class, "Binding failed to port " + tryingPort );
                if (tries > 50)
                {
                    throw new BindException( "Failed to bind to port (" + initialPort + " - " 
                        + tryingPort + "). Last reason was: " + exp.getMessage() );
                }
                error = true;
                tryingPort++;
                tries++;
            }
        }
        while (error == true);

        IpAddress hostIP = resolveLocalHostIP();
        tryingPort = serverSocket.getLocalPort();
        DestAddress newAddress = PresentationManager.getInstance().createHostAddress(
            hostIP, tryingPort );
        localAddress.updateLocalAddress( newAddress );
    }
    
    @Override
    protected synchronized void closeServer()
    {
        if ( serverSocket != null )
        {
            try
            {
                serverSocket.close();
            }
            catch (IOException exp)
            {// ignore
            }
            serverSocket = null;
        }
    }
}