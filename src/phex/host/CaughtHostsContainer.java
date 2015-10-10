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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimerTask;

import org.apache.commons.collections.set.ListOrderedSet;
import org.apache.commons.lang.time.DateUtils;

import phex.common.Environment;
import phex.common.Phex;
import phex.common.address.AddressUtils;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.connection.ConnectionStatusEvent;
import phex.connection.ConnectionStatusEvent.Status;
import phex.event.ChangeEvent;
import phex.event.EventHandler;
import phex.event.PhexEventTopics;
import phex.host.HostFetchingStrategy.FetchingReason;
import phex.msg.PongMsg;
import phex.prefs.core.NetworkPrefs;
import phex.security.AccessType;
import phex.security.PhexSecurityManager;
import phex.servent.Servent;
import phex.utils.IPUtils;

/**
 * Responsible for holding all caught hosts.
 */
public class CaughtHostsContainer implements EventHandler
{
//    private static final Logger logger = LoggerFactory.getLogger( 
//        CaughtHostsContainer.class );
    
    public static final short HIGH_PRIORITY = 2;
    public static final short NORMAL_PRIORITY = 1;
    public static final short LOW_PRIORITY = 0;
    
    private static final int FREE_SLOT_SET_SIZE = 8;
        
    private phex.common.collections.PriorityQueue caughtHosts;
    private HashSet<CaughtHost> uniqueCaughtHosts;
    
    /**
     * The CatchedHostCache provides a container with a limited size.
     * The container stores CaughtHosts ordered by successful connection
     * probability. When the container is full, the element with the
     * lowest priority is dropped.
     * Access needs to be synchronized on this object.
     */
    private CatchedHostCache catchedHostCache;
    
    /**
     * All Hosts with free Leaf slots 
     * these are added by parsing the UP ggep extension in MsgPongs 
     */
    private Set<CaughtHost> freeLeafSlotSet;
    
    /**
     * All Hosts with free Ultrpeer slots
     * these are added by parsing the UP ggep extension in MsgPongs
     */
    private Set<CaughtHost> freeUltrapeerSlotSet;
    
    private boolean hasChangedSinceLastSave;
    
    private HostFetchingStrategy hostFetchingStrategy;
    private final Servent servent;

    public CaughtHostsContainer( Servent servent )
    {
        this.servent = servent;
        
        int[] capacities = new int[3];
        capacities[ HIGH_PRIORITY ] = (int)Math.round( NetworkPrefs.MaxHostInHostCache.get().doubleValue()
            * 2.0 / 3.0 );
        capacities[ NORMAL_PRIORITY ] = (int)Math.round( (NetworkPrefs.MaxHostInHostCache.get().doubleValue()
            - capacities[HIGH_PRIORITY]) * 2.0 / 3.0 );
        capacities[ LOW_PRIORITY ] = NetworkPrefs.MaxHostInHostCache.get().intValue()
            - capacities[HIGH_PRIORITY] - capacities[NORMAL_PRIORITY];
        caughtHosts = new phex.common.collections.PriorityQueue( capacities );

        uniqueCaughtHosts = new HashSet<CaughtHost>();
        catchedHostCache = new CatchedHostCache();
        
        Environment.getInstance().scheduleTimerTask(
            new SaveHostsContainerTimer(), SaveHostsContainerTimer.TIMER_PERIOD,
            SaveHostsContainerTimer.TIMER_PERIOD );
        
        freeUltrapeerSlotSet = new ListOrderedSet/*<CaughtHost>*/();
        freeLeafSlotSet = new ListOrderedSet/*<CaughtHost>*/();
        
        // initialize container 
        initializeCaughtHostsContainer();
        
        Phex.getEventService().register( this , new String[]{PhexEventTopics.Servent_GnutellaNetwork, PhexEventTopics.Net_ConnectionStatus});
    }
    
    /**
     * @param hostFetchingStrategy the hostFetchingStrategy to set
     */
    public void setHostFetchingStrategy(HostFetchingStrategy hostFetchingStrategy)
    {
        this.hostFetchingStrategy = hostFetchingStrategy;
    }
    
    /**
     * Clears and reloads caught hosts
     */
    private void initializeCaughtHostsContainer()
    {
        caughtHosts.clear();
        catchedHostCache.clear();
        hasChangedSinceLastSave = false;
        loadHostsFromFile();
    }

    
    /**
     * Adds a caught host based on the information from a pong message.
     * @param pongMsg the pong message to add the caught host from.
     */
    public boolean catchHosts( PongMsg pongMsg )
    {        
        DestAddress pongAddress = pongMsg.getPongAddress();
        boolean valid = isValidCaughtHostAddress( pongAddress );
        boolean isNew = false;
        if ( valid )
        {
            CaughtHost caughtHost = new CaughtHost( pongAddress );
            int dailyUptime = pongMsg.getDailyUptime();
            if ( dailyUptime > 0 )
            {
                caughtHost.setDailyUptime( dailyUptime );
            }
            
            String vendor = pongMsg.getVendor();
            if ( vendor != null )
            {
                caughtHost.setVendor( vendor, pongMsg.getVendorVersionMajor(), 
                    pongMsg.getVendorVersionMinor() );
            }
            
            caughtHost.setUltrapeer( pongMsg.isUltrapeerMarked() );
            
            short priority;
            if ( pongMsg.isUltrapeerMarked() )
            {
                priority = CaughtHostsContainer.HIGH_PRIORITY;
                
                if ( pongMsg.hasFreeLeafSlots() )
                {
                    addToFreeLeafSlotSet( caughtHost );
                }
                if ( pongMsg.hasFreeUPSlots() )
                {
                    addToFreeUltrapeerSlotSet( caughtHost );
                }
            }
            else
            {
                priority = CaughtHostsContainer.LOW_PRIORITY;
            }
            addPersistentCaughtHost( caughtHost );
            isNew = addToCaughtHostFetcher( caughtHost, priority );
        }
        
        // also add packed ip port pairs 
        Set<DestAddress> ipPortPairs = pongMsg.getIPPDestAddresses();
        if ( ipPortPairs != null )
        {
            for ( DestAddress ipPortAdd : ipPortPairs )
            {
                if ( isValidCaughtHostAddress( ipPortAdd ) )
                {
                    // we expect these hosts are UPs
                    addToCaughtHostFetcher( new CaughtHost( ipPortAdd ), 
                        CaughtHostsContainer.HIGH_PRIORITY );
                }
            }
        }
        return isNew;
    }
    
    /**
     * Adds a host address with given priority to the caught hosts.
     */
    public void addCaughtHost( DestAddress address, short priority )
    {
        boolean valid = isValidCaughtHostAddress( address );
        if ( !valid )
        {
            return;
        }        
        CaughtHost caughtHost = new CaughtHost( address );
        addPersistentCaughtHost( caughtHost );
        addToCaughtHostFetcher( caughtHost, priority );
    }
    
    /**
     * Adds the caught host to the host catcher that is used for
     * upcoming network connections.
     * The host is added to the top of its priority slot in the host catcher.
     * @param caughtHost the CaughtHost to add.
     * @param priority the priority slot to add the host to.
     */
    private synchronized boolean addToCaughtHostFetcher( CaughtHost caughtHost,
        short priority )
    {
        boolean isNew = false;
        // if host is not already in the list
        if ( !uniqueCaughtHosts.contains( caughtHost ) )
        {
            isNew = uniqueCaughtHosts.add( caughtHost );

            Object removed = caughtHosts.addToHead( caughtHost, priority );
            if ( removed != null )
            {// cause aging of dropped elements.
                uniqueCaughtHosts.remove( removed );
            }
            //logger.debug( "Added to host fetcher: {} - {}", caughtHost, priority );
        }
        return isNew;
    }
    
    /**
     * Adds the caught host to the caught host cache that is stored in the
     * phex.hosts file. The position where the host is added depends on its
     * prioity in the cache.
     * If the host is already part of the cache the daily uptime is updated. 
     * @param caughtHost the caught host to add or update with.
     */
    private void addPersistentCaughtHost( CaughtHost caughtHost )
    {        
        synchronized ( catchedHostCache )
        {
            DestAddress address = caughtHost.getHostAddress();
            CaughtHost existingHost = catchedHostCache.getCaughHost( address );
            if ( existingHost == null )
            {
                catchedHostCache.add( caughtHost );
            }
            else
            {
                // update daily uptime..
                // to maintain correct order first remove... 
                catchedHostCache.remove( existingHost );
                // then modify...
                if ( caughtHost.getDailyUptime() > 0 )
                {
                    existingHost.setDailyUptime( caughtHost.getDailyUptime() );
                }
                // then add...
                catchedHostCache.add( existingHost );
            }
            hasChangedSinceLastSave = true;
        }
    }
    
    /**
     * The number of caught hosts that are ready to be used for upcomming
     * network connections.
     * @return the number of caught hosts.
     */
    public synchronized int getCaughtHostsCount()
    {
        return caughtHosts.getSize();
    }
    
    /**
     * Removes and returns the next caught host for network connection.
     * A call to this method also tryes to ensure that there are always
     * enought hosts availble for networks connections. 
     * @return the netxt top most host ready for network connection. 
     */
    public synchronized DestAddress getNextCaughtHost()
    {
        CaughtHost host;
        ensureMinCaughHosts();
        if ( !caughtHosts.isEmpty() )
        {
            host = (CaughtHost)caughtHosts.removeMaxPriority();
            uniqueCaughtHosts.remove( host );
            return host.getHostAddress();
        }
        // host list is empty
        // In this case we need to wait until a GWebCache reports IP's hopefully.
        return null;
    }
    
    /**
     * Handles the ConnectionStatusEvent of a just tried network connection to a
     * HostAddress, this is used to update the CatchedHostCache.
     * @param event the event containing the DestAddress to report the connection status
     *        and its connection status.
     */
    public void onConnectionStatusEvent( String topic, ConnectionStatusEvent event )
    {
    	
        DestAddress hostAddress = event.getHostAddres();
        boolean valid = isValidCaughtHostAddress( hostAddress );
        if ( !valid )
        {
            return;
        }
        synchronized ( catchedHostCache )
        {
            CaughtHost existingHost = catchedHostCache.getCaughHost( hostAddress );
            if ( existingHost == null )
            {
                existingHost = new CaughtHost( hostAddress ); 
            }
            else
            {
                // to maintain correct order first remove... 
                catchedHostCache.remove( existingHost );
            }
            
            // then modify...
            if ( event.getStatus() == Status.SUCCESSFUL ||
                 event.getStatus() == Status.HANDSHAKE_REJECTED )
            {
                existingHost.setLastSuccessfulConnection( System.currentTimeMillis() );
            }
            else
            {
                existingHost.setLastFailedConnection( System.currentTimeMillis() );
            }
    
            // then add...
            catchedHostCache.add( existingHost );
            hasChangedSinceLastSave = true;
        }
    }
    
    /**
     * Reacts on gnutella network changes to initialize or save caught hosts.
     */
    public void onGnutellaNetworkEvent( String topic, ChangeEvent event )
    {
        saveHostsContainer();
        initializeCaughtHostsContainer();
    }

    /**
     * Makes sure that at least one percent of the max number of hosts
     * are in the host catcher otherwise a host fetching strategy is queried.
     */
    private void ensureMinCaughHosts()
    {
        int minCount = (int)Math.ceil(
            NetworkPrefs.MaxHostInHostCache.get().doubleValue()/100.0 );
        if ( caughtHosts.getSize() < minCount && hostFetchingStrategy != null )
        {
            hostFetchingStrategy.fetchNewHosts( FetchingReason.EnsureMinHosts );
        }
    }
    
    /**
     * Validates a host address if it is acceptable for the host catcher.
     * A valid address has a valid port and ip, has no localhost ip or 
     * private ip, and has a port that is not user banned.
     * @param address the address to validate
     * @return true if the address is valid, false otherwise.
     */
    private boolean isValidCaughtHostAddress( DestAddress address )
    {
        if (   address.isLocalHost( servent.getLocalAddress() ) 
            || !address.isValidAddress() 
            || address.isSiteLocalAddress() )
        {
            return false;
        }
        if ( IPUtils.isPortInUserInvalidList( address ) )
        {
            return false;
        }
        return true;
    }
    
    /**
     * adds to the container listing the hosts that have free 
     * ultrapeer slots.This is done in a synchronized fashion.
     * the maximum no of hosts that are allowed in this container 
     * is FREE_SLOT_SIZE.
     * if the container is full then new host is added into the container
     * and the host with the least uptime is removed from the container
     * @param host
     */
    private void addToFreeUltrapeerSlotSet( CaughtHost host )
    {
        synchronized ( freeUltrapeerSlotSet )
        {
            freeUltrapeerSlotSet.add( host );
            if( freeUltrapeerSlotSet.size() > FREE_SLOT_SET_SIZE )
            {
                ((ListOrderedSet)freeUltrapeerSlotSet).remove(0);
            }
            assert freeUltrapeerSlotSet.size() <= FREE_SLOT_SET_SIZE :
                "freeUltrapeerSlotSet grows over max size.";
        }                
    }
    
    /**
     * adds to the container listing the hosts that have free 
     * leaf slots.This is done in a synchronized fashion.
     * the maximum no of hosts that are allowed in this container 
     * is FREE_SLOT_SIZE.
     * if the container is full then new host is added into the container
     * and the host with the least uptime is removed from the container
     * @param host
     */
    private void addToFreeLeafSlotSet( CaughtHost host )
    {
        synchronized ( freeLeafSlotSet )
        {
            freeLeafSlotSet.add( host );

            if( freeLeafSlotSet.size() > FREE_SLOT_SET_SIZE )
            {
                ((ListOrderedSet)freeLeafSlotSet).remove(0);
            }
            assert freeLeafSlotSet.size() <= FREE_SLOT_SET_SIZE :
                "freeLeafSlotSet grows over max size.";
        }
    }
    
    public List<CaughtHost> getFreeUltrapeerSlotHosts( )
    {
        synchronized ( freeUltrapeerSlotSet )
        {
            ArrayList<CaughtHost> freeHosts = new ArrayList<CaughtHost>( freeUltrapeerSlotSet );
            freeHosts.trimToSize();
            return freeHosts;
        }
    }
    
    public List<CaughtHost> getFreeLeafSlotHosts( )
    {
        synchronized ( freeLeafSlotSet )
        {
            ArrayList<CaughtHost> freeHosts = new ArrayList<CaughtHost>( freeLeafSlotSet );
            freeHosts.trimToSize();
            return freeHosts;
        }
    }
    
    /**
     * Loads the hosts file phex.hosts.
     */
    private void loadHostsFromFile()
    {
        //logger.debug( "Loading hosts file." );
        try
        {
            File file = servent.getGnutellaNetwork().getHostsFile();
            BufferedReader br;
            if ( file.exists() )
            {
                br = new BufferedReader( new FileReader(file) );
            }
            else
            {
                //logger.debug( "Load default hosts file." );
                InputStream inStream = ClassLoader.getSystemResourceAsStream(
                    "phex/resources/phex.hosts" );
                if ( inStream != null )
                {
                    br = new BufferedReader( new InputStreamReader( inStream ) );
                }
                else
                {
                    //logger.debug( "Default Phex Hosts file not found." );
                    return;
                }
            }

            long now = System.currentTimeMillis();
            PhexSecurityManager securityMgr = servent.getSecurityService();
            String line;
            short usedPriority = LOW_PRIORITY;
            while ( (line = br.readLine()) != null )
            {
                if ( line.startsWith("#") )
                {
                    continue;
                }
                
                CaughtHost caughtHost = parseCaughtHostFromLine( line, now );
                if ( caughtHost == null )
                {
                    continue;
                }
                
                AccessType access = securityMgr.controlHostAddressAccess( 
                    caughtHost.getHostAddress() );
                switch ( access )
                {
                    case ACCESS_DENIED:
                    case ACCESS_STRONGLY_DENIED:
                        // skip host address...
                        continue;
                }
                if ( IPUtils.isPortInUserInvalidList( caughtHost.getHostAddress() ) )
                {
                    continue;
                }
                addPersistentCaughtHost( caughtHost );
                if ( usedPriority != HIGH_PRIORITY && caughtHosts.isFull( usedPriority ) )
                {
                    // goes from lowest priority (0) to highest (2)
                    usedPriority ++;
                }
                addToCaughtHostFetcher( caughtHost, usedPriority );
            }
            br.close();
        }
        catch ( IOException exp )
        {
            //logger.warn(exp.toString(), exp);
        }
    }
    
    /**
     * Returns the CaughtHost that can be parsed from the line, or 
     * null if parsing failed for some reason.
     * @param line
     * @return the parsed CaughtHost
     */
    private CaughtHost parseCaughtHostFromLine( String line, long now )
    {
        // tokenize line
        // line format can be:
        // IP:port         or:
        // IP:port,lastFailedConnection,lastSuccessfulConnection,dailyUptime
        // IP:port,lastFailedConnection,lastSuccessfulConnection,dailyUptime,vendor,vendorVersionMajor,vendorVersionMinor,isUltrapeer
        StringTokenizer tokenizer = new StringTokenizer( line, "," );
        int tokenCount = tokenizer.countTokens();
        
        String hostAddressStr;
        int dailyUptime;
        long lastFailedconnection;
        long lastSuccessfulConnection;
        String vendor = null;
        int vendorVersionMajor = -1;
        int vendorVersionMinor = -1;
        boolean isUltrapeer = false;
        if ( tokenCount == 1 )
        {
            hostAddressStr = line;
            dailyUptime = -1;
            lastFailedconnection = -1;
            lastSuccessfulConnection = -1;                    
        }
        else if ( tokenCount == 4 || tokenCount == 8 )
        {
            hostAddressStr = tokenizer.nextToken();
            try
            {
                lastFailedconnection = Long.parseLong( tokenizer.nextToken() );
            }
            catch ( NumberFormatException exp )
            {
                lastFailedconnection = -1;
            }
            try
            {
                lastSuccessfulConnection = Long.parseLong( tokenizer.nextToken() );
            }
            catch ( NumberFormatException exp )
            {
                lastSuccessfulConnection = -1;
            }
            try
            {
                dailyUptime = Integer.parseInt( tokenizer.nextToken() );
            }
            catch ( NumberFormatException exp )
            {
                dailyUptime = -1;
            }
            if ( tokenCount == 8 )
            {
                vendor = tokenizer.nextToken();
                try
                {
                    vendorVersionMajor = Integer.parseInt( tokenizer.nextToken() );
                }
                catch ( NumberFormatException exp )
                {
                    vendorVersionMajor = -1;
                }
                try
                {
                    vendorVersionMinor = Integer.parseInt( tokenizer.nextToken() );
                }
                catch ( NumberFormatException exp )
                {
                    vendorVersionMinor = -1;
                }
                isUltrapeer = Boolean.parseBoolean( tokenizer.nextToken() );
            }
        }
        else
        {// Unknown format
            //logger.warn( "Unknown HostCache line format: {}", line);
            return null;
        }
        byte[] ip = AddressUtils.parseIP( hostAddressStr );
        if ( ip == null )
        {
            return null;
        }
        int port = AddressUtils.parsePort( hostAddressStr );
        if ( port == -1 )
        {
            return null;
        }
        DestAddress hostAddress = new DefaultDestAddress( ip, port );
        CaughtHost caughtHost = new CaughtHost( hostAddress );
        caughtHost.setUltrapeer( isUltrapeer );
        if ( dailyUptime > 0 )
        {
            caughtHost.setDailyUptime( dailyUptime );
        }
        // check if lastFailedconnection is available and was during the last 30 days.
        if ( lastFailedconnection > 0 && (now - lastSuccessfulConnection) < 30*DateUtils.MILLIS_PER_DAY )
        {
            caughtHost.setLastFailedConnection( lastFailedconnection );
        }
        // check if lastSuccessfulConnection is available and was during the last 30 days.
        if ( lastSuccessfulConnection > 0 && (now - lastSuccessfulConnection) < 30*DateUtils.MILLIS_PER_DAY )
        {
            caughtHost.setLastSuccessfulConnection( lastSuccessfulConnection );
        }
        if ( vendor != null && !vendor.equals( "-" ) )
        {
            caughtHost.setVendor( vendor, vendorVersionMajor, vendorVersionMinor );
        }
        
        return caughtHost;
    }
    
    /**
     * Blocking operation which saves the caught hosts and auto connect hosts
     * if they changed since the last save operation.
     */
    void saveHostsContainer( )
    {
        if ( !hasChangedSinceLastSave )
        {
            return;
        }
        saveCaughtHosts();
        hasChangedSinceLastSave = false;
    }

    /**
     * The caught hosts are saved from the persistent host cache container in 
     * reverse order. Since when loaded back the last element will be on top of
     * the caught host priority queue. 
     */
    private void saveCaughtHosts()
    {
        //logger.debug( "Start saving caught hosts." );
        try
        {
            File file = servent.getGnutellaNetwork().getHostsFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            
            synchronized( catchedHostCache )
            {
                Iterator<CaughtHost> iterator = catchedHostCache.iterator();
                while( iterator.hasNext() )
                {
                    CaughtHost host = iterator.next();
                    DestAddress hostAddress = host.getHostAddress();
                    String vendor = host.getVendor();
                    // line format is:
                    // IP:port,lastFailedConnection,lastSuccessfulConnection,dailyUptime,
                    // vendor,vendorVersionMajor,vendorVersionMinor,isUltrapeer
                    bw.write( hostAddress.getFullHostName() +
                        "," + host.getLastFailedConnection() +
                        "," + host.getLastSuccessfulConnection() +
                        "," + host.getDailyUptime() +
                        "," + (vendor == null ? "-" : vendor) +
                        "," + host.getVendorVersionMajor() +
                        "," + host.getVendorVersionMinor() +
                        "," + host.isUltrapeer() );
                    
                    bw.newLine();
                }
            }
            bw.close();
        }
        catch ( IOException exp )
        {
            //logger.error( exp.toString(), exp );
        }
        //logger.debug( "Finish saving caught hosts." );
    }
    
    ////////////////////// START inner classes //////////////////////////
    
    private class SaveHostsContainerRunner implements Runnable
    {
        public void run()
        {
            saveHostsContainer();
        }
    }
    
    private class SaveHostsContainerTimer extends TimerTask
    {
        // once per minute
        public static final long TIMER_PERIOD = 1000 * 60;
        
        public void run()
        {
            // trigger the save inside a background job to not
            // slow down the timer too much
            Environment.getInstance().executeOnThreadPool( new SaveHostsContainerRunner(),
                "SaveHostsContainer" );
        }
    }

	@Override
	public void onEvent(String topic, Object event) {
		// TODO Auto-generated method stub
    	if(topic.compareTo(PhexEventTopics.Servent_GnutellaNetwork)== 0)
    		onGnutellaNetworkEvent(  topic, (ChangeEvent) event );
    	if(topic.compareTo(PhexEventTopics.Net_ConnectionStatus) == 0)
    		onConnectionStatusEvent(  topic, (ConnectionStatusEvent) event );
	}    
}
