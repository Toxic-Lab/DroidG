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
package phex.bootstrap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import phex.common.Environment;
import phex.common.Phex;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.MalformedDestAddressException;
import phex.event.ChangeEvent;
import phex.event.EventHandler;
import phex.event.PhexEventTopics;
import phex.msg.PongMsg;
import phex.net.repres.PresentationManager;
import phex.servent.Servent;

/**
 * Container of collected udp host caches.
 */
public class UdpHostCacheContainer implements EventHandler
{
//    private static final Logger logger = LoggerFactory.getLogger( 
//        UdpHostCacheContainer.class );
    
    private static int MIN_UDP_HOST_CACHE_SIZE = 20;
    
    /**
     * List of Default udp Host Caches
     */
    private static final List<UdpHostCache> defaultCaches;
    static 
    {
        defaultCaches = new ArrayList<UdpHostCache>();
        //add a list of default caches to this list
        defaultCaches.add(  new UdpHostCache(new DefaultDestAddress( "gnutelladev1.udp-host-cache.com", 1234 )) );
        defaultCaches.add(  new UdpHostCache(new DefaultDestAddress( "gnutelladev2.udp-host-cache.com", 5678 )) );
        defaultCaches.add(  new UdpHostCache(new DefaultDestAddress( "gwc.ak-electron.eu", 12060 )) );
        defaultCaches.add(  new UdpHostCache(new DefaultDestAddress( "gwc.chickenkiller.com", 8080 )) );
        defaultCaches.add(  new UdpHostCache(new DefaultDestAddress( "yang.cloud.bishopston.net", 33558 )) );
        defaultCaches.add(  new UdpHostCache(new DefaultDestAddress( "yin.cloud.bishopston.net", 33558 )) );
        Collections.shuffle( defaultCaches );
    }
    
    private final Servent servent;
    
    /**
     * contains all the known functional caches in this session.
     * The list is always sorted in the ascending order of fail count.
     * So remember to sort it if we independently modify the list
     */
    private final List<UdpHostCache> functionalUdpCaches;
    
    /**
     * All the udpHostCaches not in functional udp caches container,
     * it contains all caches to be tried or which have failed.
     * The list is always sorted in the ascending order of fail count.
     * So remember to sort it if we independently modify the list
     */
    private final List<UdpHostCache> generalUdpCaches;
    
    /**
     * Lock to make sure not more then one thread request is running in parallel
     * otherwise it could happen that we create thread after thread while each
     * one takes a long time to come back.
     */
    private final AtomicBoolean isThreadRequestRunning;
    

    public UdpHostCacheContainer( Servent servent ) 
    {
        this.servent = servent;
        functionalUdpCaches = new ArrayList<UdpHostCache>();
        generalUdpCaches = new ArrayList<UdpHostCache>();
        isThreadRequestRunning = new AtomicBoolean( false );
        
        initialize();
        
        Phex.getEventService().register( this , new String[]{PhexEventTopics.Servent_GnutellaNetwork} );
    }
    
    /**
     * Reacts on gnutella network changes to initialize or save udp caches.
     */
    public void onGnutellaNetworkEvent( String topic, ChangeEvent event )
    {
    	
        saveCachesToFile();
        initialize();
    }
    
    private void initialize()
    {
        //first clear caches 
        functionalUdpCaches.clear();
        generalUdpCaches.clear();
        
        //first load from file 
        loadCachesFromFile();
        
        //now add missing default caches
        for ( UdpHostCache cache : defaultCaches )
        {
            addCache( cache );
        }
        
        //logger.debug( "Initialized UdpHostCacheContainer.");
    }
    
    /**
     * adds a cache, into the general container 
     */
    public boolean addCache( UdpHostCache cache )
    {
        //logger.info( "Adding a UDP Host Cache to the GENERAL container.");
        return addTo( cache, generalUdpCaches );
    }
    
    public String createPackedHostCaches()
    {
        final int PACKED_CACHES_SIZE = 8;
        
        int count = 0;
        StringBuffer packedCaches = new StringBuffer( PACKED_CACHES_SIZE * 21 );
        synchronized( functionalUdpCaches )
        {
            for ( Iterator<UdpHostCache> udphc = functionalUdpCaches.iterator(); 
            udphc.hasNext() && count < PACKED_CACHES_SIZE; count++ )
            {
                UdpHostCache cache = udphc.next();
                DestAddress address = cache.getHostAddress(); 
                String ipString  = address.getFullHostName();
                // add to the packed cache
                packedCaches.append( ipString );
                packedCaches.append( "\n" );
            }
        }
        return packedCaches.toString();
    }
    
    /**
     * Adds a caught host based on the information from a pong message.
     * @param pongMsg the pong message to add the caught host from.
     */
    public void catchHosts( PongMsg pongMsg )
    {
        // handle udp host caches and add them to the udp host cache.
        Set<UdpHostCache> udpHostCaches = pongMsg.getUdpHostCaches();
        if ( udpHostCaches != null )
        {
            //logger.debug( "Catch UDP Host Caches: {}.", udpHostCaches );
            for ( UdpHostCache cache : udpHostCaches )
            {
                addCache( cache );
            }
        }
        // handle available udp host cache
        UdpHostCache cache = pongMsg.getUdpHostCache();
        if ( cache != null )
        {
            //logger.debug( "Catch UDP Host Cache: {}.", cache );
            synchronized( generalUdpCaches )
            {
                int idx = generalUdpCaches.indexOf( cache );
                if ( idx >= 0 )
                {
                    cache = generalUdpCaches.get( idx );
                    generalUdpCaches.remove( idx );
                }
            }
            cache.resetFailedInRowCount();
            addTo( cache, functionalUdpCaches );
        }
    }
    
    /**
     * Starts a query for more hosts in an extra thread.
     */
    public synchronized void invokeQueryCachesRequest()
    {
        // we don't want multiple thread request to run at once. If one thread
        // request is running others are blocked.
        if ( !isThreadRequestRunning.compareAndSet( false, true ) )
        {
            return;
        }
        Runnable runner = new QueryCachesRunner();
        Environment.getInstance().executeOnThreadPool( runner,
            "UdpHostCacheQuery-" + Integer.toHexString(runner.hashCode()) );
    }
    
    private List<UdpHostCache> getUhcListToQuery( int count )
    {
        List<UdpHostCache> list = new ArrayList<UdpHostCache>( count );
        long now = System.currentTimeMillis();
        synchronized( functionalUdpCaches )
        {
            for ( UdpHostCache cache : functionalUdpCaches )
            {
                if ( now > cache.getEarliestReConnectTime() )
                {
                    list.add( cache );
                    if ( list.size() >= count )
                    {
                        return list;
                    }
                }
            }
        }
        
        synchronized( generalUdpCaches )
        {
            Collections.sort( generalUdpCaches, BootstrapHostComparator.INSTANCE );
            for ( UdpHostCache cache : generalUdpCaches )
            {
                if ( now > cache.getEarliestReConnectTime() )
                {
                    list.add( cache );
                    if ( list.size() >= count )
                    {
                        return list;
                    }
                }
            }
        }
        
        return list;
    }
    
    /**
     *	<p>Queries for hosts from udp host caches</p>
     */
    private void queryMoreHosts()
    {
        final int NO_OF_CACHES_TO_PING = 3;
        List<UdpHostCache> uhcListToQuery = getUhcListToQuery( NO_OF_CACHES_TO_PING );
        
        for ( UdpHostCache udpHostCache : uhcListToQuery )
        {
            queryCache( udpHostCache );
        }
    }
    
    
    
    /**
     * pings cache,
     * increments their failure count( assuming its going to fail )
     */
    private void queryCache( UdpHostCache cache )
    {
        //logger.debug( "Pinging UDP Host Cache: {}.", cache );
        
        cache.setLastRequestTime( System.currentTimeMillis() );
        
        // ping 
        servent.getMessageService().sendUdpPing( 
            cache.getHostAddress() );
        
        //assumed it has failed
        cache.incFailedInRowCount();
        synchronized( functionalUdpCaches )
        {
            functionalUdpCaches.remove( cache );
        }
        addCache( cache );
    }
    
    
    /**
     * adds a UdpHostcache to a HostCache Container in a thread safe manner
     * checks if already present and adds only if not present
     * <b>It then sorts the list in ascending order of fail count</b>  
     * @param cache
     * @param cacheContainer
     * @return
     * true if added successfully, 
     * false if not added
     */
    private boolean addTo( UdpHostCache cache, List<UdpHostCache> cacheContainer )
    {
        synchronized ( cacheContainer )
        {
            if ( ! ( cacheContainer.contains( cache )) )
            {
                cacheContainer.add( cache );
                Collections.sort( cacheContainer, BootstrapHostComparator.INSTANCE );
                //logger.info( "Added UdpHostCache: {}", cache ); 
                return true;
            }
        }
        return false;
    }
    
    private void loadCachesFromFile()
    {
        try
        {
            File file = servent.getGnutellaNetwork().getUdpHostCacheFile();
            if ( !file.exists() )
            {
                //logger.debug( "No UDP host cache file found." );
                return;
            }
            BufferedReader br = new BufferedReader( new FileReader(file) );
            String line;
            synchronized( generalUdpCaches )
            {
                while ( (line = br.readLine()) != null)
                {
                    if ( line.startsWith("#") )
                    {
                        continue;
                    }
                    UdpHostCache cache = parseUhcFromLine( line );
                    
                    // no security manager IP check.. because DNS resolve
                    // can make startup very very slow... 
                    
                    addCache( cache );
                }
            }
            br.close();
        }
        catch ( IOException e )
        {
            //logger.warn( "Loading Udp Host Caches from file FAILED", e );
        }
    }
    
    private UdpHostCache parseUhcFromLine( String line ) 
    {
        // tokenize line
        // line format can be:
        // host         or:
        // host lastRequestTime failedInRowCount
        StringTokenizer tokenizer = new StringTokenizer( line, " " );
        int tokenCount = tokenizer.countTokens();
        
        String hostStr;
        long lastRequestTime;
        int failedInRowCount;
        if ( tokenCount == 1 )
        {
            hostStr = line;
            lastRequestTime = -1;
            failedInRowCount = -1;                    
        }
        else if ( tokenCount == 3 )
        {
            hostStr = tokenizer.nextToken();
            try
            {
                lastRequestTime = Long.parseLong( tokenizer.nextToken() );
            }
            catch ( NumberFormatException exp )
            {
                lastRequestTime = -1;
            }
            try
            {
                failedInRowCount = Integer.parseInt( tokenizer.nextToken() );
            }
            catch ( NumberFormatException exp )
            {
                failedInRowCount = -1;
            }
        }
        else
        {// Unknown format
            //logger.warn( "Unknown HostCache line format: {}", line );
            return null;
        }
        
        DestAddress hostCacheAdr;
        try
        {
            hostCacheAdr = PresentationManager.getInstance().createHostAddress(
                hostStr, DefaultDestAddress.DEFAULT_PORT);
        }
        catch ( MalformedDestAddressException e )
        {
            //logger.warn( "Could not create cache from host string: {}", line, e );
            return null;
        }
        
        UdpHostCache cache = new UdpHostCache( hostCacheAdr );
        if ( lastRequestTime > 0 )
        {
            cache.setLastRequestTime( lastRequestTime );
        }
        if ( failedInRowCount > 0 )
        {
            cache.setFailedInRowCount( failedInRowCount );
        }
        
        return cache;
    }
    
    
    
    public void saveCachesToFile()
    {
        try
        {
            File file = servent.getGnutellaNetwork().getUdpHostCacheFile();
            BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
            
            writeCachesToFile( writer, functionalUdpCaches );
            writeCachesToFile( writer, generalUdpCaches );
            
            writer.flush();
            writer.close();
        }
        catch ( IOException exp )
        {
            //logger.warn( "Saving Udp Host Caches to file FAILED ", exp );
        }
    }
    
    private void writeCachesToFile(  BufferedWriter writer, List<UdpHostCache> cacheContainer ) 
        throws IOException
    {
        synchronized( cacheContainer )
        {
            for ( UdpHostCache cache : cacheContainer )
            {
                DestAddress address = cache.getHostAddress(); 
                String ipString  = address.getFullHostName();
                writer.write( ipString );
                writer.write( ' ' );
                writer.write( String.valueOf( cache.getLastRequestTime() ) );
                writer.write( ' ' );
                writer.write( String.valueOf( cache.getFailedInRowCount() ) );
                writer.newLine();
            }
        }
    }
        
    private final class QueryCachesRunner implements Runnable
    {
        public void run()
        {
            queryMoreHosts( );
            isThreadRequestRunning.set( false );
        }
    }

	@Override
	public void onEvent(String topic, Object event) {
		// TODO Auto-generated method stub
		if(topic.compareTo(PhexEventTopics.Servent_GnutellaNetwork) == 0)
			onGnutellaNetworkEvent(  topic, (ChangeEvent) event );
	}
}