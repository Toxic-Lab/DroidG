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
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import phex.common.Environment;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.connection.IncomingConnectionDispatcher;
import phex.host.NetworkHostsContainer;
import phex.net.repres.PresentationManager;
import phex.net.repres.def.DefaultSocketFacade;
import phex.prefs.core.NetworkPrefs;
import phex.security.AccessType;
import phex.security.PhexSecurityException;
import phex.servent.Servent;

//// NOT USED YET ////
public class NIOServer extends Server
{
    private ServerSocketChannel listeningChannel;
    private Selector selector;

    public NIOServer( Servent servent )
    {
        super( servent );
    }

    // The listening thread.
    public void run()
    {
//        if (NLogger.isDebugEnabled( NIOServer.class ))
//            NLogger.debug( NIOServer.class,
//                "Listener started. Listening on: "
//                    + serverSocket.getInetAddress().getHostAddress() + ':'
//                    + serverSocket.getLocalPort());

        try
        {
            while ( selector.isOpen() )
            {
                selector.select(10 * 1000);
                if ( !selector.isOpen() )
                {
                    break;
                }
    
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext())
                {
                    SelectionKey selKey = iterator.next();
                    // Remove it from the list to indicate that it is being processed
                    iterator.remove();
    
                    // Check if it's a connection request
                    if ( !selKey.isAcceptable() )
                    {
                        continue;
                    }
                    
                    // Get channel with connection request
                    ServerSocketChannel ssChannel = (ServerSocketChannel)selKey.channel();
                    try
                    {
                        SocketChannel socketChannel = ssChannel.accept();
                        handleIncomingClientChannel(socketChannel);
                    }
                    catch ( PhexSecurityException exp )
                    {
                        //NLogger.debug( NIOServer.class, exp );
                    }
                    catch ( IOException exp )
                    {
                        //NLogger.debug( NIOServer.class, exp, exp);
                    }
                }
            }
        }
        catch ( Exception exp )
        {
            //NLogger.error( NIOServer.class, exp, exp );
        }

        isRunning = false;
        //NLogger.debug( NIOServer.class, "Listener stopped.");
        
        PresentationManager presentationMgr = PresentationManager.getInstance();
        DestAddress newAddress = presentationMgr.createHostAddress( 
            IpAddress.LOCAL_HOST_IP, 
            NetworkPrefs.ListeningPort.get().intValue() );
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
    private void handleIncomingClientChannel(SocketChannel socketChannel)
        throws IOException, PhexSecurityException
    {
        socketChannel.configureBlocking(true);
        Socket clientSocket = socketChannel.socket();
        clientSocket.setSoTimeout( NetworkPrefs.TcpRWTimeout.get().intValue() );

        IpAddress ip = new IpAddress( clientSocket.getInetAddress().getAddress() );
        PresentationManager presentationMgr = PresentationManager.getInstance();
        DestAddress address = presentationMgr.createHostAddress(ip, clientSocket.getPort() );
        NetworkHostsContainer netHostsContainer = servent.getHostService()
            .getNetworkHostsContainer();

        // if not already connected and connection is not from a private address.
        // TODO we might like to accept more then two connection in some cases!
        if (!netHostsContainer.isConnectedToHost(address)
            && !address.getIpAddress().isSiteLocalIP())
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
            throw new PhexSecurityException("Host access denied: "
                + clientSocket.getInetAddress().getHostAddress());
        }

//        NLogger.debug( NIOServer.class, 
//            "Accepted incoming connection from: "
//                + address.getFullHostName());

        // facade socket
        DefaultSocketFacade clientFacade = new DefaultSocketFacade( clientSocket );
        IncomingConnectionDispatcher dispatcher = new IncomingConnectionDispatcher(
            clientFacade, servent );
        Environment.getInstance().executeOnThreadPool( dispatcher,
            "IncomingConnectionDispatcher-" + Integer.toHexString(hashCode()));
    }

    @Override
    protected synchronized void bind( int initialPort ) throws IOException
    {
        assert (listeningChannel == null);

        listeningChannel = ServerSocketChannel.open();
        serverSocket = listeningChannel.socket();
        listeningChannel.configureBlocking(false);

        // Create a listening socket at the port.
        int tries = 0;
        boolean error;
        // try to find new port if port not valid
        do
        {
            error = false;

            try
            {
                //NLogger.debug( NIOServer.class, "Binding to port " + initialPort);
                serverSocket.bind(new InetSocketAddress( initialPort ));
            }
            catch (BindException exp)
            {
                //NLogger.debug( NIOServer.class, "Binding failed to port " + initialPort );
                if (tries > 10)
                {
                    throw exp;
                }
                error = true;
                initialPort++;
                tries++;
            }
        }
        while (error == true);
        
        IpAddress hostIP = resolveLocalHostIP();
        initialPort = serverSocket.getLocalPort();
        DestAddress newAddress = PresentationManager.getInstance().createHostAddress(
            hostIP, initialPort);
        localAddress.updateLocalAddress( newAddress );
        
        selector = Selector.open();
        listeningChannel.register(selector, SelectionKey.OP_ACCEPT);
    }
    
    /**
     * @see phex.net.server.Server#closeServer()
     */
    @Override
    protected void closeServer()
    {
        try
        {
            listeningChannel.close();
            SelectionKey key = listeningChannel.keyFor(selector);
            key.cancel();
            selector.close();
        }
        catch (IOException exp)
        {// ignore
        }
        serverSocket = null;
        listeningChannel = null;
    }
}