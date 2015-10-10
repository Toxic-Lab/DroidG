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

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.time.DateUtils;

import phex.common.Environment;
import phex.common.Phex;
import phex.connection.ConnectionStatusEvent;
import phex.connection.ConnectionStatusEvent.Status;
import phex.event.ChangeEvent;
import phex.event.EventHandler;
import phex.event.PhexEventTopics;
import phex.host.HostFetchingStrategy;
import phex.host.NetworkHostsContainer;
import phex.host.HostFetchingStrategy.FetchingReason;
import phex.prefs.core.ConnectionPrefs;
import phex.servent.OnlineStatus;
import phex.servent.Servent;

/**
 * This class tries to observers the online status of a connection.
 * If a certain amount of connection fail due to socket connection 
 * failure the online observer assumes a missing online connection 
 * and disconnects from network.
 */
public class OnlineObserver implements EventHandler
{
    //private static final Logger logger = LoggerFactory.getLogger( OnlineObserver.class );
    
    /**
     * The number of failed connections in a row.
     */
    private AtomicInteger failedConnections;
    private final Servent servent;
    private final HostFetchingStrategy fetchingStrategy;
    private AutoReconnectTimer autoReconnectTimer;
    private long lastOfflineTime;
    
    /**
     * Delay to let host fetch operation take effect.
     */
    private long lastHostFetchTime;
    
    public OnlineObserver( Servent servent, HostFetchingStrategy fetchingStrategy )
    {
        this.fetchingStrategy = fetchingStrategy;
        this.servent = servent;
        failedConnections = new AtomicInteger(0);
        
        Phex.getEventService().register( this, new String[]{PhexEventTopics.Net_ConnectionStatus, PhexEventTopics.Servent_OnlineStatus} );
    }
    
    //@EventTopicSubscriber(topic=PhexEventTopics.Net_ConnectionStatus)
    public void onConnectionStatusEvent( String topic, ConnectionStatusEvent event )
    {
        if ( event.getStatus() == Status.CONNECTION_FAILED )
        {
            // only count if there are no active connections in the network
            NetworkHostsContainer networkHostsContainer = 
                servent.getHostService().getNetworkHostsContainer();
            if ( networkHostsContainer.getTotalConnectionCount() > 0 )
            {
                failedConnections.set( 0 );
                return;
            }
            
            int fc = failedConnections.incrementAndGet();
//            if ( //logger.isDebugEnabled( ) && fc % 5 == 0 )
//            {
//                //logger.debug( "Observed " + failedConnections + " failed connections.");
//            }
            
            // if we have 15 failed connections trigger host fetch operation, honor a delay between
            // host fetching to let last operation take effect.
            if( fc % 15 == 0 && System.currentTimeMillis() - lastHostFetchTime > 15*DateUtils.MILLIS_PER_SECOND )
            {
                //logger.info( "Started fetching new hosts due to increasing failed connections");
                lastHostFetchTime = System.currentTimeMillis();
            	fetchingStrategy.fetchNewHosts( FetchingReason.UpdateHosts );
            }
            
            if ( fc >= ConnectionPrefs.OfflineConnectionFailureCount.get().intValue() )
            {
                //logger.debug( "Too many connections failed.. disconnecting network.");
                // trigger timer to attempt to reconnect after some time...
                triggerReconnectTimer( );
            }
        }
        else
        {
            // for online status we don't care if handshake failed or not...
            resetAutoReconnect();
        }
    }
    
    private synchronized void triggerReconnectTimer()
    {
        OnlineStatus oldStatus = servent.getOnlineStatus();
        servent.setOnlineStatus( OnlineStatus.OFFLINE );
        if ( autoReconnectTimer != null )
        {
            autoReconnectTimer.setOfflineTime( lastOfflineTime );
            return;
        }
        
        autoReconnectTimer = new AutoReconnectTimer();
        autoReconnectTimer.setReconnectStatus( oldStatus );
        autoReconnectTimer.setOfflineTime( lastOfflineTime );
        Environment.getInstance().scheduleTimerTask( autoReconnectTimer, 
            1*DateUtils.MILLIS_PER_MINUTE,  2*DateUtils.MILLIS_PER_MINUTE );
    }
    
    private synchronized void autoReconnectTry( OnlineStatus status )
    {
        //logger.debug( "Triggering auto-reconnect" );
        failedConnections.set( 0 );
        servent.setOnlineStatus( status );
    }
    
    private synchronized void resetAutoReconnect()
    {
        if ( autoReconnectTimer != null )
        {
            //logger.debug( "Reset auto-reconnect" );
            autoReconnectTimer.cancel();
            autoReconnectTimer = null;
        }
        failedConnections.set( 0 );
    }
    
    /**
     * Reacts on online status changes to reset failed connection counter.
     */
    public void onOnlineStatusEvent( String topic, ChangeEvent event )
    {
        OnlineStatus oldStatus = (OnlineStatus) event.getOldValue();
        OnlineStatus newStatus = (OnlineStatus) event.getNewValue();
        if ( oldStatus == OnlineStatus.OFFLINE && 
             newStatus != OnlineStatus.OFFLINE )
        {// switch from offline to any online status
            failedConnections.set( 0 );
        }
        else if ( oldStatus != OnlineStatus.OFFLINE && 
             newStatus == OnlineStatus.OFFLINE )
        {
            // monitor last offline switch time to detect user interaction
            lastOfflineTime = System.currentTimeMillis();
        }
    }
    
    public class AutoReconnectTimer extends TimerTask
    {
        private OnlineStatus reconnectStatus;
        private long offlineTime;
        
        @Override
        public void run()
        {
            if ( lastOfflineTime != offlineTime )
            {
                cancel();
                return;
            }
            if ( !servent.getOnlineStatus().isNetworkOnline() )
            {
                autoReconnectTry( reconnectStatus );
            }
        }

        public void setReconnectStatus(OnlineStatus reconnectStatus)
        {
            this.reconnectStatus = reconnectStatus;
        }

        public void setOfflineTime(long offlineTime)
        {
            this.offlineTime = offlineTime;
        }

        public long getOfflineTime()
        {
            return offlineTime;
        }
    }

	@Override
	public void onEvent(String topic, Object event) {
		// TODO Auto-generated method stub
    	if(topic.compareTo(PhexEventTopics.Net_ConnectionStatus)== 0) 
    		onConnectionStatusEvent(  topic, (ConnectionStatusEvent) event );
    	if(topic.compareTo(PhexEventTopics.Servent_OnlineStatus)==0) 
    		onOnlineStatusEvent(  topic, (ChangeEvent) event );
	}
}