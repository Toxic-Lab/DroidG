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
package phex.host;

import java.util.TimerTask;

import phex.bootstrap.UdpHostCacheContainer;
import phex.common.AbstractLifeCycle;
import phex.common.Environment;
import phex.connection.OutgoingConnectionDispatcher;
import phex.connection.PingWorker;
import phex.msg.PongMsg;
import phex.prefs.core.ConnectionPrefs;
import phex.prefs.core.NetworkPrefs;
import phex.servent.Servent;

/**
 * Responsible for managing caught host and the network neighborhood.
 */
final public class HostManager extends AbstractLifeCycle
{
    //private static final Logger logger = LoggerFactory.getLogger( HostManager.class );
    private static final int MAX_PARALLEL_CONNECTION_TRIES = 20;

    private final Servent servent;
    private final NetworkHostsContainer networkHostsContainer;
    private final CaughtHostsContainer caughtHostsContainer;
    private final UdpHostCacheContainer udpHostCacheContainer;
    private final FavoritesContainer favoritesContainer;

    public HostManager( Servent servent, boolean useUdpHostCache )
    {        
        this.servent = servent;
        networkHostsContainer = new NetworkHostsContainer( servent );
        caughtHostsContainer = new CaughtHostsContainer( servent );
        if ( useUdpHostCache )
        {
            udpHostCacheContainer = new UdpHostCacheContainer( servent );
        }
        else
        {
            udpHostCacheContainer = null;
        }
        favoritesContainer = new FavoritesContainer( servent );
        
    }
    
    @Override
    protected void doStart() throws Exception
    {
        networkHostsContainer.start();
        caughtHostsContainer.setHostFetchingStrategy( 
            servent.getHostFetchingStrategy() );
        PingWorker pingWorker = new PingWorker( servent );
        pingWorker.start();
        Environment.getInstance().scheduleTimerTask( 
            new HostCheckTimer(), HostCheckTimer.TIMER_PERIOD,
            HostCheckTimer.TIMER_PERIOD );
    }
    
    @Override
    public void doStop()
    {
        if ( udpHostCacheContainer != null )
        {
            udpHostCacheContainer.saveCachesToFile();
        }
        caughtHostsContainer.saveHostsContainer();
        favoritesContainer.saveFavoriteHosts();
    }

    public FavoritesContainer getFavoritesContainer()
    {
        return favoritesContainer;
    }

    public CaughtHostsContainer getCaughtHostsContainer()
    {
        return caughtHostsContainer;
    }
    
    /**
     * Returns the {@link UdpHostCacheContainer} of this HostManager.
     * Value might be null in case no UDP host cache is used.
     * @return the host cache container or null.
     */
    public UdpHostCacheContainer getUhcContainer()
    {
        return udpHostCacheContainer;
    }

    /////////////// START NetworkHostsContainer wrapper methods ///////////////////////

    public NetworkHostsContainer getNetworkHostsContainer()
    {
        return networkHostsContainer;
    }

    public boolean isShieldedLeafNode()
    {
        return networkHostsContainer.isShieldedLeafNode();
    }

    /**
     * Returns true if this node is currently a ultrapeer, false otherwise.
     * This node is currently a ultrapeer if it is forced to be a ultrapeer or
     * is not shielded and has connections.
     * @return true if the node is currently a ultrapeer, false otherwise.
     */
    public boolean isUltrapeer()
    {
        return ( ConnectionPrefs.AllowToBecomeUP.get().booleanValue() &&
            ConnectionPrefs.ForceToBeUltrapeer.get().booleanValue() ) 
            || !isShieldedLeafNode() && networkHostsContainer.getTotalConnectionCount() > 0;
    }

    /**
     * Indicates if the peer advertises through pongs that it has incoming slots
     * available.
     * @return true if it will advertise incoming slots. False otherwise.
     */
    public boolean areIncommingSlotsAdvertised()
    {
        if ( networkHostsContainer.isShieldedLeafNode() )
        {   // when shielded leaf we don't like many incoming request therefore
            // we claim to not have any slots available...
            return false;
        }
        return networkHostsContainer.hasUltrapeerSlotsAvailable() ||
            networkHostsContainer.hasLeafSlotsAvailable();
    }


    /**
     * The method checks if we are able to go into leaf state. This is
     * necessary to react accordingly to the "X-UltrapeerNeeded: false" header.
     *
     * @return true if we are able to switch to Leaf state, false otherwise.
     */
    public boolean allowDowngradeToLeaf()
    {
        if ( isUltrapeer() )
        {
            return false;
        }
        return true;
    }

    public void addConnectedHost( Host host )
    {
        networkHostsContainer.addConnectedHost( host );
    }

    public void removeNetworkHosts( Host[] hosts )
    {
        networkHostsContainer.removeNetworkHosts( hosts );
    }

    public void removeNetworkHost( Host host )
    {
        networkHostsContainer.removeNetworkHost( host );
    }
    
    public Host[] getLeafConnections()
    {
        return networkHostsContainer.getLeafConnections();
    }

    public Host[] getUltrapeerConnections()
    {
        return networkHostsContainer.getUltrapeerConnections();
    }

    /////////////// END NetworkHostsContainer wrapper methods ///////////////////////
    

    /**
     * Reads host informations from the given PongMsg.
     * @param pongMsg the PongMsg to catch hosts.
     */
    public boolean catchHosts( PongMsg pongMsg )
    {
        boolean isNew = caughtHostsContainer.catchHosts( pongMsg );
        if ( udpHostCacheContainer != null )
        {
            udpHostCacheContainer.catchHosts( pongMsg );
        }
        return isNew;
    }


    
    
    private class HostCheckTimer extends TimerTask
    {

        public static final long TIMER_PERIOD = 2000;

        /**
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run()
        {
            try
            {
                doAutoConnectCheck();
                networkHostsContainer.periodicallyCheckHosts();
            }
            catch ( Throwable th )
            {
                //logger.error( th.toString(), th );
            }
        }
        
        public void doAutoConnectCheck()
        {
            if ( !servent.getOnlineStatus().isNetworkOnline() )
            {
                return;
            }

            int hostCount;
            int requiredHostCount;

            if ( servent.isAbleToBecomeUltrapeer() )
            {
                // as a ultrapeer I'm primary searching for Ultrapeers only...
                // to make sure I'm well connected...
                hostCount = networkHostsContainer.getUltrapeerConnectionCount();
                requiredHostCount = ConnectionPrefs.Up2UpConnections.get().intValue();
            }
            // we don't support legacy peers anymore ( since 3.0 ) therefore we only
            // handle leaf mode here
            else
            {
                // as a leaf I'm primary searching for Ultrapeers only...
                hostCount = networkHostsContainer.getUltrapeerConnectionCount();
                requiredHostCount = ConnectionPrefs.Leaf2UpConnections.get().intValue();
            }

            // count the number of missing connection tries this is the required count
            // minus the available count. The result is multiplied by four to raise the
            // connection try count.
            int missingCount = ( requiredHostCount - hostCount ) * 4;
            
            // find out the number of hosts where a connection is currently tried...
            int allHostCount = networkHostsContainer.getNetworkHostCount( );
            int errorHostCount = networkHostsContainer.getNetworkHostCount(
                HostStatus.ERROR);
            // make sure the value is not negative.
            int totalCount = networkHostsContainer.getTotalConnectionCount();
            int currentTryCount = Math.max( 0, allHostCount - totalCount - errorHostCount);
            
            // we will never try more then a reasonable parallel tries..
            int upperLimit = Math.min( MAX_PARALLEL_CONNECTION_TRIES,
                NetworkPrefs.MaxConcurrentConnectAttempts.get().intValue() ) - currentTryCount;
            
            int outConnectCount = Math.min( missingCount-currentTryCount, 
                upperLimit );
            if ( outConnectCount > 0 )
            {
                //logger.debug( "Auto-connect to {} new hosts.", outConnectCount);
                OutgoingConnectionDispatcher.dispatchConnectToNextHosts( 
                    outConnectCount, servent );
            }
        }    
    }
}