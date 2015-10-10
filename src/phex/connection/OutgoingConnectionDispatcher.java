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
package phex.connection;

import java.io.IOException;

import phex.common.Environment;
import phex.common.Phex;
import phex.common.address.DestAddress;
import phex.connection.ConnectionStatusEvent.Status;
import phex.event.PhexEventTopics;
import phex.host.CaughtHostsContainer;
import phex.host.Host;
import phex.host.HostManager;
import phex.host.HostStatus;
import phex.host.NetworkHostsContainer;
import phex.net.connection.Connection;
import phex.net.connection.ConnectionFactory;
import phex.servent.Servent;

/**
 * This class is responsible to dispatch an outgoing Gnutella network 
 * connection to a specific host or to the next best host from the host catcher.
 */
public class OutgoingConnectionDispatcher implements Runnable
{
    /**
     * Dispatches a outgoing Gnutella network connection to the next 
     * best host from the host catcher.
     */
    public static void dispatchConnectToNextHost( Servent servent )
    {
        dispatchConnectToNextHosts( 1, servent );
    }
    
    /**
     * Dispatches <tt>count</tt> number of outgoing Gnutella network connection 
     * to the next best hosts from the host catcher.
     */
    public static void dispatchConnectToNextHosts( int count, Servent servent )
    {        
        // creating OCDs in batches could cause unneeded thread use and heavy
        // HostFetchingStrategy requests in case no hosts are in host catcher.
        // Instead host lookup is now done before creating OCD and dispatching
        // is stopped in case no hosts are available.
        
        HostManager hostService = servent.getHostService();
        CaughtHostsContainer caughtHostsContainer = hostService.getCaughtHostsContainer();
        NetworkHostsContainer networkHostsCont = hostService.getNetworkHostsContainer();
        for ( int i = 0; i < count; i++ )
        {
            DestAddress caughtHost;
            do
            {
                caughtHost = caughtHostsContainer.getNextCaughtHost();
                if ( caughtHost == null )
                {
                    // no host is currently available...
                    // break out of dispatching
                    return;
                }
            }
            while ( networkHostsCont.isConnectedToHost( caughtHost ) );
            
            dispatchConnectToHost( caughtHost, servent );
        }
    }
    
    /**
     * Dispatches a outgoing Gnutella network connection to the specified
     * <tt>hostAddress</tt>
     * @param hostAddress the hostAddress to connect to.
     */
    public static void dispatchConnectToHost( DestAddress hostAddress, Servent servent )
    {
        OutgoingConnectionDispatcher dispatcher = new OutgoingConnectionDispatcher(
            hostAddress, servent );
        
        Environment.getInstance().executeOnThreadPool( dispatcher,
            "OutgoingConnectionDispatcher-" + Integer.toHexString( dispatcher.hashCode() ) );
    }
    
    private final Servent servent;
    private final DestAddress hostAddress;
    
    private OutgoingConnectionDispatcher( DestAddress hostAddress, Servent servent )
    {
        this.hostAddress = hostAddress;
        this.servent = servent;
    }
    
    public void run()
    {
        try
        {
            connectToHostAddress();
        }
        catch ( Throwable th )
        {
            //NLogger.error( OutgoingConnectionDispatcher.class, th, th);
        }
    }
    
    private void connectToHostAddress()
    {
        NetworkHostsContainer netHostsCont = servent.getHostService().getNetworkHostsContainer();
        Host host = netHostsCont.createOutgoingHost( hostAddress );
        host.setStatus( HostStatus.CONNECTING );
        
        Connection connection;
        try
        {
            connection = ConnectionFactory.createConnection( hostAddress,
                servent.getBandwidthService().getNetworkBandwidthController() );
        }
        catch ( IOException exp )
        {
            reportStatus( Status.CONNECTION_FAILED );
            host.setStatus( HostStatus.ERROR, exp.getMessage() );
            host.disconnect();
            //NLogger.debug( OutgoingConnectionDispatcher.class, exp);
            return;
        }
        catch (Exception exp)
        {
            reportStatus( Status.CONNECTION_FAILED );
            host.setStatus(HostStatus.ERROR, exp.getMessage());
            host.disconnect();
            //NLogger.warn( OutgoingConnectionDispatcher.class, exp, exp);
            return;
        }
        
        // I am connected to the remote host at this point.
        host.setConnection( connection );

        ConnectionEngine engine;
        try
        {
            engine = new ConnectionEngine( servent, host );
            engine.initHostHandshake();
        }
        catch ( ConnectionRejectedException exp )
        {
            reportStatus( Status.HANDSHAKE_REJECTED );
            host.setStatus( HostStatus.ERROR, exp.getMessage() );
            host.disconnect();
            //NLogger.debug( OutgoingConnectionDispatcher.class, exp);
            return;
        }
        catch ( IOException exp )
        {
            reportStatus( Status.HANDSHAKE_FAILED );
            host.setStatus( HostStatus.ERROR, exp.getMessage() );
            host.disconnect();
            //NLogger.debug( OutgoingConnectionDispatcher.class, exp);
            return;
        }
        catch (Exception exp)
        {
            reportStatus( Status.HANDSHAKE_FAILED );
            host.setStatus(HostStatus.ERROR, exp.getMessage());
            host.disconnect();
            //NLogger.warn( OutgoingConnectionDispatcher.class, exp, exp);
            return;
        }
        
        reportStatus( Status.SUCCESSFUL );
        
        try
        {
            engine.processIncomingData();
        }
        catch ( IOException exp )
        {
            if ( host.isConnected() )
            {
                host.setStatus( HostStatus.ERROR, exp.getMessage() );
                host.disconnect();
            }
            //NLogger.debug( OutgoingConnectionDispatcher.class, exp);
        }
        catch (Exception exp)
        {
            if ( host.isConnected() )
            {
                host.setStatus(HostStatus.ERROR, exp.getMessage());
                host.disconnect();
            }
            //NLogger.warn( OutgoingConnectionDispatcher.class, exp, exp);
        }
    }
    
    private void reportStatus( Status status )
    {
        Phex.getEventService().publish( 
            PhexEventTopics.Net_ConnectionStatus, 
            new ConnectionStatusEvent( hostAddress, status ) );
    }
}