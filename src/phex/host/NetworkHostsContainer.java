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
package phex.host;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import phex.common.AbstractLifeCycle;
import phex.common.Phex;
import phex.common.address.DestAddress;
import phex.connection.ConnectionObserver;
import phex.event.ChangeEvent;
import phex.event.ContainerEvent;
import phex.event.EventHandler;
import phex.event.PhexEventTopics;
import phex.net.connection.Connection;
import phex.prefs.core.ConnectionPrefs;
import phex.servent.OnlineStatus;
import phex.servent.Servent;
import phex.utils.Localizer;

/**
 * Responsible for holding all hosts of the current network neighbor hood.
 */
public final class NetworkHostsContainer extends AbstractLifeCycle implements EventHandler
{
    private Servent servent;

    /**
     * The complete neighbor hood. Contains all connected and not connected
     * hosts independent from its connection type.
     * This collection is mainly used for GUI representation.
     */
    private final List<Host> networkHosts;

    /**
     * Contains a list of connected ultrapeer connections.
     */
    private final List<Host> ultrapeerConnections;

    /**
     * The number of connections that are leafUltrapeerConnections inside the
     * ultrapeerConnections list.
     */
    private int leafUltrapeerConnectionCount;

    /**
     * Contains a list of connected leaf connections, in case we act as there
     * Ultrapeer.
     */
    private final List<Host> leafConnections;
    
    public NetworkHostsContainer( Servent servent )
    {
        this.servent = servent;
        
        networkHosts = new ArrayList<Host>();
        ultrapeerConnections = new ArrayList<Host>();
        leafConnections = new ArrayList<Host>();
        
        Phex.getEventService().register( this , new String[]{PhexEventTopics.Servent_OnlineStatus, PhexEventTopics.Host_Disconnect});
    }
    
    @Override
    protected void doStart()
    {
        ConnectionObserver observer = new ConnectionObserver( this, 
            servent.getMessageService() );
        observer.start();
    }

    /**
     * Returns true if the local host is a shielded leaf node ( has a connection
     * to a ultrapeer).
     */
    public synchronized boolean isShieldedLeafNode()
    {
        return leafUltrapeerConnectionCount > 0;
    }

    /**
     * Indicates if connection to leafs are available.
     * @return true if connection to leafs are available, 
     *         false otherwise
     */
    public synchronized boolean hasLeafConnections()
    {
        // we are a ultrapeer if we have any leaf slots filled.
        return !leafConnections.isEmpty();
    }

    /**
     * Indicates if connection to ultrapeers are available.
     * @return true if connection to ultrapeers are available, 
     *         false otherwise
     */
    public synchronized boolean hasUltrapeerConnections()
    {
        return !ultrapeerConnections.isEmpty();
    }

    /**
     * Used to check if we have anymore ultrapeer slots. Usually this method
     * should only be used used as a Ultrapeer.
     * @return true if ultrapeer slots are available, false otherwise.
     */
    public boolean hasUltrapeerSlotsAvailable()
    {
        // Note: That we don't response on pings when the slots are full this
        // results in not getting that many incoming requests.
        return ultrapeerConnections.size() < ConnectionPrefs.Up2UpConnections.get().intValue();
    }
    
    /**
     * Returns the number of open slots for leaf nodes. Usually this method
     * should only be used used as a Ultrapeer.
     * @return the number of open slots for leaf nodes.
     */
    public int getOpenUltrapeerSlotsCount()
    {
        return ConnectionPrefs.Up2UpConnections.get().intValue() - ultrapeerConnections.size();
    }


    /**
     * Used to check if we would provide a Ultrapeer that will become a possible
     * leaf through leaf guidance a slot. This is only the case if we have not
     * already a ultrapeer too much and if we have a leaf slot available.
     * @return true if we have a leaf slot available to guide a ultrapeer, 
     *         false otherwise
     */
    public boolean hasLeafSlotForUltrapeerAvailable()
    {
        return hasLeafSlotsAvailable()
            // Allow one more up2up connection to accept this possibly leaf guided
            // ultrapeer
            && ultrapeerConnections.size() < ConnectionPrefs.Up2UpConnections.get().intValue() + 1;
    }

    /**
     * Used to check if we have anymore leaf slots. Usually this method
     * is only used used as a Ultrapeer.
     * @return true if we have leaf slots available, false otherwise.
     */
    public boolean hasLeafSlotsAvailable()
    {
        // Note: That we don't response on pings when the slots are full this
        // results in not getting that many incoming requests.
        return leafConnections.size() < ConnectionPrefs.Up2LeafConnections.get().intValue();
    }
    
    /**
     * Returns the number of open slots for leaf nodes.
     * @return the number of open slots for leaf nodes.
     */
    public int getOpenLeafSlotsCount()
    {
        if ( servent.isUltrapeer() )
        {
            return ConnectionPrefs.Up2LeafConnections.get().intValue() - leafConnections.size();
        }
        return 0;
    }
    
    public synchronized Host[] getNetworkHosts()
    {
        Host[] hosts = new Host[ networkHosts.size() ];
        networkHosts.toArray( hosts );
        return hosts; 
    }

    /**
     * Returns all available connected ultrapeers.
     * @return all available connected ultrapeers.
     */
    public synchronized Host[] getUltrapeerConnections()
    {
        Host[] hosts = new Host[ ultrapeerConnections.size() ];
        ultrapeerConnections.toArray( hosts );
        return hosts;
    }

    /**
     * Returns all available connected leafs.
     * @return all available connected leafs.
     */
    public synchronized Host[] getLeafConnections()
    {
        Host[] hosts = new Host[ leafConnections.size() ];
        leafConnections.toArray( hosts );
        return hosts;
    }

    /**
     * The total number of connections (ultrapeers and leafs).
     * @return the total number of connections.
     */
    public synchronized int getTotalConnectionCount()
    {
        return ultrapeerConnections.size()
            + leafConnections.size();
    }
    
    public synchronized int getLeafConnectionCount()
    {
        return leafConnections.size();
    }

    public synchronized int getUltrapeerConnectionCount()
    {
        return ultrapeerConnections.size();
    }
    
    /**
     * Returns a array of push proxy addresses or null if 
     * this is not a shielded leaf node.
     * @return a array of push proxy addresses or null.
     */
    public DestAddress[] getPushProxies() 
    {
        if ( isShieldedLeafNode() )
        {
            HashSet<DestAddress> pushProxies = new HashSet<DestAddress>();
            for ( Host host : ultrapeerConnections )
            {
                DestAddress pushProxyAddress = host.getPushProxyAddress();
                if ( pushProxyAddress != null )
                {
                    pushProxies.add( pushProxyAddress );
                    if ( pushProxies.size() == 4 )
                    {
                        break;
                    }
                }
            }
            DestAddress[] addresses = new DestAddress[ pushProxies.size() ];
            pushProxies.toArray( addresses );
            return addresses;
        }
        return null;
    }

    /**
     * Adds a connected host to the connected host list. But only if its already
     * in the network host list.
     * @param host the host to add to the connected host list.
     */
    public synchronized void addConnectedHost( Host host )
    {
        // make sure host is still in network and not already removed
        if ( !networkHosts.contains( host ) )
        {// host is already removed by user action...
            host.disconnect();
            return;
        }

        if ( host.isUltrapeer() )
        {
            ultrapeerConnections.add( host );
            if ( host.isLeafUltrapeerConnection() )
            {
                leafUltrapeerConnectionCount++;
            }
        }
        else if ( host.isUltrapeerLeafConnection() )
        {
            leafConnections.add( host );
        }
        else
        {
            assert false : "Peer connections should not be used anymore";
        }
        //dump();
    }

    private synchronized void cleanupHost( Host host )
    {
        if ( host.isUltrapeer() )
        {
            boolean isRemoved = ultrapeerConnections.remove( host );
            if ( isRemoved && host.isLeafUltrapeerConnection() )
            {
                leafUltrapeerConnectionCount--;
            }
        }
        else if ( host.isUltrapeerLeafConnection() )
        {
            leafConnections.remove( host );
        }
        else
        {
            // There seem to be situations this is called? why?
            //assert false : "Peer connections should not be used anymore";
        }
    }

    /**
     * Checks for hosts that have a connection timeout...
     * Checks if a connected host is able to keep up...
     * if not it will be removed...
     */
    public synchronized void periodicallyCheckHosts()
    {
        HostStatus status;
        long currentTime = System.currentTimeMillis();

        Host[] badHosts = new Host[ networkHosts.size() ];
        int badHostsPos = 0;
        //boolean isShieldedLeafNode = isShieldedLeafNode();

        for( Host host : networkHosts )
        {
            status = host.getStatus();
            if ( status == HostStatus.CONNECTED )
            {
                String policyInfraction = null;
                if ( host.isSendQueueTooLong() )
                {
                    policyInfraction = Localizer.getString( "SendQueueTooLong" );
                }
                else if ( host.isNoVendorDisconnectApplying() )
                {
                    policyInfraction = Localizer.getString( "NoVendorString" );
                }
                // freeloaders are no real problem
                // else if ( host.isFreeloader( currentTime ) )
                // {
                //     policyInfraction = Localizer.getString( "FreeloaderNotSharing" );
                // }
                if ( policyInfraction != null )
                {
                    //Logger.logMessage( Logger.FINE, "log.core.msg",
                    //    "Applying disconnect policy to host: " + host +
                    //    " drops: " + host.tooManyDropPackets() +
                    //    " queue: " + host.sendQueueTooLong() );
                    host.setStatus( HostStatus.ERROR, policyInfraction, currentTime );
                    host.disconnect();
                }
            }
        }
        // kill all bad hosts...
        if ( badHostsPos > 0 )
        {
            removeNetworkHosts( badHosts );
        }
    }

    public synchronized Host getNetworkHostAt( int index )
    {
        if ( index < 0 || index >= networkHosts.size() )
        {
            return null;
        }
        return networkHosts.get( index );
    }

    public synchronized Host[] getNetworkHostsAt( int[] indices )
    {
        int length = indices.length;
        Host[] hosts = new Host[ length ];
        for ( int i = 0; i < length; i++ )
        {
            if ( indices[i] < 0 || indices[i] >= networkHosts.size() )
            {
                hosts[i] = null;
            }
            else
            {
                hosts[i] = networkHosts.get( indices[i] );
            }
        }
        return hosts;
    }
    
    public synchronized Host getNetworkHost( DestAddress address )
    {
        for ( Host networkHost : networkHosts )
        {
            DestAddress networkAddress = networkHost.getHostAddress();
            if ( networkAddress.equals( address ) )
            {
                return networkHost;
            }            
        }
        //not found
        return null;
    }

    /**
     * Returns the count of the complete neighbour hood, containing all 
     * connected and not connected hosts independent from its connection type.
     */
    public synchronized int getNetworkHostCount()
    {
        return networkHosts.size();
    }

    /**
     * Returns the count of the networks hosts with the given status.
     */
    public synchronized int getNetworkHostCount( HostStatus status )
    {
        int count = 0;
        for( Host host : networkHosts )
        {
            if ( host.getStatus() == status )
            {
                count++;
            }
        }
        return count;
    }
    
    public synchronized Host createOutgoingHost( DestAddress hostAddress )
    {
        Host host = new Host( hostAddress );
        host.setType( Host.Type.OUTGOING );
        addNetworkHost( host );
        return host;
    }
    
    public synchronized Host createIncomingHost( DestAddress hostAddress, 
        Connection connection )
    {
        Host host = new Host( hostAddress, connection );
        host.setType( Host.Type.INCOMING );
        addNetworkHost( host );
        return host;
    }
    
    /**
     * Adds a host to the network host list.
     * @param host the host to add to the network host list.
     */
    private synchronized void addNetworkHost( Host host )
    {
        int position = networkHosts.size();
        networkHosts.add( host );
        fireNetworkHostAdded( host, position );
        //dump();
    }
    
    public synchronized boolean isConnectedToHost( DestAddress address )
    {
        // Check for duplicate.
        for (int i = 0; i < networkHosts.size(); i++)
        {
            Host host = networkHosts.get( i );
            if ( host.getHostAddress().equals( address ) )
            {// already connected
                return true;
            }
        }
        return false;
    }

    public synchronized void removeAllNetworkHosts()
    {
        Host host;
        while ( networkHosts.size() > 0 )
        {
            host = networkHosts.get( 0 );
            internalRemoveNetworkHost( host );
        }
    }

    public synchronized void removeNetworkHosts( Host[] hosts )
    {
        Host host;
        int length = hosts.length;
        for ( int i = 0; i < length; i++ )
        {
            host = hosts[ i ];
            internalRemoveNetworkHost( host );
        }
    }

    public synchronized void removeNetworkHost( Host host )
    {
        internalRemoveNetworkHost( host );
    }

    /**
     * Disconnects from host.
     */
    private synchronized void internalRemoveNetworkHost( Host host )
    {
        if ( host == null )
        {
            return;
        }
        host.disconnect();
        int position = networkHosts.indexOf( host );
        if ( position >= 0 )
        {
            networkHosts.remove( position );
            fireNetworkHostRemoved( host, position );
        }
        //dump();
    }

    ///////////////////// START event handling methods ////////////////////////
    private void fireNetworkHostAdded( Host host, int position )
    {
        Phex.getEventService().publish( PhexEventTopics.Net_Hosts, 
            new ContainerEvent( ContainerEvent.Type.ADDED, host, this, position ) );
    }

    private void fireNetworkHostRemoved( Host host, int position )
    {
        Phex.getEventService().publish( PhexEventTopics.Net_Hosts, 
            new ContainerEvent( ContainerEvent.Type.REMOVED, host, this, position ) );
    }
    
    /**
     * Reacts on online status changes to initialize or save caught hosts.
     */
    public void onOnlineStatusEvent( String topic, ChangeEvent event )
    {
        OnlineStatus oldStatus = (OnlineStatus) event.getOldValue();
        OnlineStatus newStatus = (OnlineStatus) event.getNewValue();
        if (    newStatus == OnlineStatus.OFFLINE 
             && oldStatus != OnlineStatus.OFFLINE )
        {// switch from any online to offline status
         // Disconnect all hosts
            removeAllNetworkHosts();
        }
    }
    
    public void onHostDisconnectEvent( String topic, Host host )
    {
        cleanupHost( host );
    }
    ///////////////////// END event handling methods ////////////////////////

	@Override
	public void onEvent(String topic, Object event) {
		// TODO Auto-generated method stub

    	if(topic.compareTo(PhexEventTopics.Servent_OnlineStatus)== 0) 
    		onOnlineStatusEvent(  topic, (ChangeEvent) event );

    	if(topic.compareTo(PhexEventTopics.Host_Disconnect)== 0) 
    		onHostDisconnectEvent(  topic, (Host) event );
	}
}
