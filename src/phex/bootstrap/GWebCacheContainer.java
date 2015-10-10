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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import phex.common.Environment;
import phex.common.GeneralGnutellaNetwork;
import phex.common.Phex;
import phex.common.address.AddressUtils;
import phex.common.address.DestAddress;
import phex.connection.ProtocolNotSupportedException;
import phex.event.ChangeEvent;
import phex.event.EventHandler;
import phex.event.PhexEventTopics;
import phex.host.CaughtHostsContainer;
import phex.servent.Servent;
import phex.utils.IOUtil;
import phex.utils.NormalizableURL;
import phex.utils.StringUtils;

public class GWebCacheContainer implements EventHandler
{
//    private static final Logger logger = LoggerFactory.getLogger( 
//        GWebCacheContainer.class );
    
    private static int MIN_G_WEB_CACHES_SIZE = 5;
    private static int MAX_G_WEB_CACHES_SIZE = 1000;
    private static List<String> BLOCKED_WEB_CACHES;
    private static List<String> PHEX_WEB_CACHES;
    static
    {
        // This is a list of Phex only GWebCaches. Requests from other hosts are
        // ignored and return with ERROR.
        String[] arr =
        {
            /*"http://gc-phex02.rakjar.de/gcache.php", "http://gc-fust01.rakjar.de/gcache.php",*/
            "http://phexgwc.kouk.de/gcache.php"
        };
        PHEX_WEB_CACHES = Collections.unmodifiableList( Arrays.asList( arr ) );
        
        String[] blockedArr =
        {
            "gavinroy.com"
        };
        BLOCKED_WEB_CACHES = Collections.unmodifiableList( Arrays.asList(blockedArr) );
    }

    /**
     * Contains GWebCache objects.
     */
    private final List<GWebCacheHost> allGWebCaches;
    
    /**
     * Contains GWebCache objects.
     */
    private final List<GWebCacheHost> functionalGWebCaches;
    
    /**
     * Contains Phex GWebCaches only. Locking is done through allGWebCaches.
     */
    private final List<GWebCacheHost> phexGWebCaches;

    /**
     * Stores the URL.toExternalForm().toLowerCase(). The URL object itself is not stored
     * since the hash function of the URL is way slow, it could lead to doing a
     * IP lookup.
     * The lower case conversion is done to keep us from adding multiple equal URLs
     * of the same file, like they are often seen.
     */
    private final Set<String> uniqueGWebCacheURLs;
    
    /**
     * GWebCaches sorted by access order the last accessed GWebCaches first.
     */
    private final TreeSet<GWebCacheHost> sortedGWebCaches;
    private final Random random;
    
    private final Servent servent;

    public GWebCacheContainer( Servent servent )
    {
        if ( servent == null )
        {
            throw new NullPointerException( "Servent missing." );
        }
        //logger.debug( "Initializing GWebCacheContainer" );
        this.servent = servent;
        allGWebCaches = new ArrayList<GWebCacheHost>();
        phexGWebCaches = new ArrayList<GWebCacheHost>();
        functionalGWebCaches = new ArrayList<GWebCacheHost>();
        uniqueGWebCacheURLs = new HashSet<String>( );
        sortedGWebCaches = new TreeSet<GWebCacheHost>( BootstrapHostComparator.INSTANCE );
        random = new Random();
        
        initializeGWebCacheContainer();
        
        Phex.getEventService().register( this , new String[]{PhexEventTopics.Servent_GnutellaNetwork});
    }
    
    /**
     * Clears and reloads GWebCaches.
     */
    private void initializeGWebCacheContainer()
    {
        allGWebCaches.clear();
        phexGWebCaches.clear();
        functionalGWebCaches.clear();
        uniqueGWebCacheURLs.clear();
        sortedGWebCaches.clear();
        insertPhexGWebCaches();
        
        loadFromConfigInBackgrd();
    }
    
    /**
     * Reacts on gnutella network changes to initialize GWebCache.
     */
    public void onGnutellaNetworkEvent( String topic, ChangeEvent event )
    {
    	
        initializeGWebCacheContainer();
    }

    /**
     * Connects to a random GWebCache to fetch more hosts. This should be triggered
     * on startup and when there are not enough hosts in the catcher. The queried
     * hosts are directly put into the CaughtHostsContainer with high priority.
     */
    public boolean queryMoreHosts( boolean preferPhex )
    {
        int retrys = 0;
        boolean succ = false;
        do
        {
            retrys ++;
            GWebCacheConnection connection = getRandomGWebCacheConnection( preferPhex );
            // continue if no connection...
            if ( connection == null )
            {
                continue;
            }
            DestAddress[] hosts = connection.sendHostFileRequest( servent.getSecurityService() );
            // continue if cache is bad or data is null...
            if ( !verifyGWebCache( connection ) || hosts == null )
            {
                continue;
            }
            // everything looks good add data..
            CaughtHostsContainer container = servent.getHostService().
                getCaughtHostsContainer();
            for ( int i = 0; i < hosts.length; i++ )
            {
                // gwebcache should only return Ultrapeers therefore we have
                // high priority.
                container.addCaughtHost( hosts[i], CaughtHostsContainer.HIGH_PRIORITY );
            }
            succ = true;
        }
        // do this max 5 times or until we where successful
        while ( !succ && retrys < 5 );
        return succ;
    }

    /**
     * Updates the remote GWebCache. By the specification clients should only
     * send updates if they accept incoming connections - i.e. clients behind
     * firewalls should not send updates. Also, if supported by clients, only
     * Ultrapeers/Supernodes should send updates. After a client has been up for
     * an hour, it should begin sending an Update request periodically - every
     * 60 minutes. It sends its own IP address and port in the "ip" parameter
     * and a the URL of a random cache in the "url" parameter.
     * Clients should only submit the URLs of caches that they know are functional!
     */
    public boolean updateRemoteGWebCache( DestAddress myHostAddress, boolean preferPhex )
    {
        String fullHostName = null;
        if ( myHostAddress != null )
        {
            fullHostName = myHostAddress.getFullHostName();
        }

        int retrys = 0;
        boolean succ = false;
        do
        {
            retrys ++;
            GWebCacheConnection connection = getRandomGWebCacheConnection( preferPhex );
            // continue if no connection...
            if ( connection == null )
            {
                continue;
            }
            
            String urlString = null;
            GWebCacheHost gWebCache = getGWebCacheForUpdate( connection.getGWebCache() );
            // there might be no gwebcache for update available.
            if ( gWebCache != null )
            {
                assert !gWebCache.isPhexCache() && !gWebCache.equals( connection.getGWebCache() )
                    : "isPhexCache: " + gWebCache.isPhexCache() + ",equals " + gWebCache.getUrl() 
                    + " - " + connection.getGWebCache().getUrl();
                urlString = gWebCache.getUrl().toExternalForm();
            }

            if ( fullHostName == null && urlString == null )
            {
                // no data to update... try again to determine random GWebCache in loop
                continue;
            }

            succ = connection.updateRequest( fullHostName, urlString );
            verifyGWebCache( connection );
        }
        // do this max 5 times or until we where successful
        while ( !succ && retrys < 5 );
        return succ;
    }

    /**
     * Connects to a random GWebCache to fetch more GWebCaches.
     */
    public boolean queryMoreGWebCaches( boolean preferPhex )
    {
        int retrys = 0;
        boolean succ = false;
        do
        {
            retrys ++;
            GWebCacheConnection connection = getRandomGWebCacheConnection( preferPhex );
            // continue if no connection...
            if ( connection == null )
            {
                continue;
            }
            URL[] urls = connection.sendURLFileRequest();
            // continue if cache is bad or data is null...
            if ( !verifyGWebCache( connection ) || urls == null )
            {
                continue;
            }
            // everything looks good add data..
            for ( int i = 0; i < urls.length; i++ )
            {
                try
                {
                    boolean isPhexCache = isPhexGWebCache( urls[i].toExternalForm() );
                    GWebCacheHost gWebCache = new GWebCacheHost( urls[i], isPhexCache );
                    if( isCacheAccessAllowed(gWebCache) )
                    {
                        insertGWebCache( gWebCache );
                    }
                }
                catch ( IOException exp )
                {// invalid GWebCache... ignore
                   // logger.debug( exp.toString(), exp );
                }
            }
            succ = true;
        }
        // do this max 5 times or until we where successful
        while ( !succ && retrys < 5 );
        return succ;
    }

    public int getGWebCacheCount()
    {
        return allGWebCaches.size();
    }

    // we dont send update urls to other gwebcaches... too much trash...
    private GWebCacheHost getGWebCacheForUpdate( GWebCacheHost ignore )
    {
        // we dont send update urls to other gwebcaches... too much trash...
        /*
        GWebCacheHost gWebCache = null;
        int count = functionalGWebCaches.size();
        if ( count == 0 )
        {
            return null;
        }
        else if ( count == 1 )
        {
            gWebCache = (GWebCacheHost)functionalGWebCaches.get( 0 );
            if ( gWebCache.equals( ignore ) )
            {
                return null;
            }
            else
            {
                assert( !gWebCache.isPhexCache() );
                return gWebCache;
            }
        }

        int tries = 0;
        do
        {
            int randomIndex = random.nextInt( count - 1 );
            gWebCache = (GWebCacheHost)functionalGWebCaches.get( randomIndex );
            if ( !gWebCache.equals( ignore ) )
            {
                assert ( !gWebCache.isPhexCache() );
                return gWebCache;
            }
            tries ++;
        }
        while ( tries < 10 );
        // no valid cache found...
        
        */
        return null;
    }

    private GWebCacheHost getRandomGWebCache( boolean preferPhex )
    {
        ensureMinGWebCaches();
        synchronized ( allGWebCaches )
        {
            GWebCacheHost cache = null;
            boolean usePhexCache = preferPhex && random.nextInt( 8 ) == 0;
            if ( usePhexCache && phexGWebCaches.size() > 0 )
            {
                Collections.sort( phexGWebCaches, BootstrapHostComparator.INSTANCE );
                cache = phexGWebCaches.get( 0 );
            }
            else
            {
                int count = allGWebCaches.size();
                if ( count == 0 )
                {
                    return null;
                }
                cache = sortedGWebCaches.first();
            }
            long now = System.currentTimeMillis();
            if ( now > cache.getEarliestReConnectTime() )
            {
                return cache;
            }
            else
            {
                return null;
            }
        }
    }

    /**
     * Single trys to get a random and pinged GWebCacheConnection or null if
     * connection could not be obtained.
     */
    private GWebCacheConnection getRandomGWebCacheConnection( boolean preferPhex )
    {
        GWebCacheConnection connection = null;
        GWebCacheHost gWebCache = null;
        try
        {
            gWebCache = getRandomGWebCache( preferPhex );
            // Usually this URL is always != null but in case there is no standard 
            // GWebCache file or a different default gnutella network is chosen the
            // URL might be null
            if ( gWebCache == null )
            {
                return null;
            }
            
            connection = new GWebCacheConnection( gWebCache );
            // we stop pinging GWebCache... this is not necessary since we
            // find out anyway if cache is working on first contact.
            //if ( !connection.sendPingRequest() )
            //{
            //    removeGWebCache( url, false );
            //    return null;
            //}
        }
        catch ( ProtocolNotSupportedException exp )
        {// not valid url.. throw away...
            removeGWebCache( gWebCache, true );
            return null;
        }
        
        return connection;
    }

    private boolean verifyGWebCache( GWebCacheConnection connection )
    {
        GWebCacheHost gWebCache = connection.getGWebCache();
        // resort cache.
        sortedGWebCaches.remove(gWebCache);
        gWebCache.countConnectionAttempt( connection.isCacheBad() );
        sortedGWebCaches.add(gWebCache);
        
        // cache is working add it to functional list if not phex cache.
        if ( !connection.isCacheBad() && !gWebCache.isPhexCache() && 
             !functionalGWebCaches.contains( gWebCache ) )
        {
            functionalGWebCaches.add( gWebCache );
        }
        saveGWebCacheToFile();
        
        return true;
    }
    
    /**
     * Returns the GWebCache that can be parsed from the line, or 
     * null if parsing failed for some reason.
     * @param line
     * @return the parsed GWebCache instance or null.
     * @throws IOException 
     */
    private GWebCacheHost parseGWebCacheFromLine( String line ) 
        throws IOException
    {
        // tokenize line
        // line format can be:
        // URL         or:
        // URL lastRequestTime failedInRowCount
        StringTokenizer tokenizer = new StringTokenizer( line, " " );
        int tokenCount = tokenizer.countTokens();
        
        String urlStr;
        long lastRequestTime;
        int failedInRowCount;
        if ( tokenCount == 1 )
        {
            urlStr = line;
            lastRequestTime = -1;
            failedInRowCount = -1;                    
        }
        else if ( tokenCount == 3 )
        {
            urlStr = tokenizer.nextToken();
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
           // logger.warn( "Unknown HostCache line format: {}", line );
            return null;
        }
        
        NormalizableURL helpUrl = new NormalizableURL( urlStr );
        helpUrl.normalize();
        URL url = new URL( helpUrl.toExternalForm() );
        boolean isPhexCache = isPhexGWebCache( url.toExternalForm() );
        GWebCacheHost cache = new GWebCacheHost( url, isPhexCache );
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

    /**
     * Inserts a GWebCache...
     */
    private void insertGWebCache( GWebCacheHost gWebCache )
    {
        synchronized( allGWebCaches )
        {
            if ( allGWebCaches.size() >= MAX_G_WEB_CACHES_SIZE )
            {
                //logger.error( "Limit of 1000 GWebCaches reached." );
                removeGWebCache( sortedGWebCaches.last(), true );
                return;
            }
            
            // The URL object itself is not stored
            // since the hash function of the URL is way slow, it could lead to
            // doing a IP lookup.
            String uniqueString = gWebCache.getHostDomain();
            if ( !uniqueGWebCacheURLs.contains( uniqueString ) )
            {
                allGWebCaches.add( gWebCache );
                sortedGWebCaches.add( gWebCache );
                uniqueGWebCacheURLs.add( uniqueString );
                
                if ( gWebCache.isPhexCache() )
                {
                    phexGWebCaches.add( gWebCache );
                }
            }
        }
    }

    /**
     * Removes a GWebCache..
     */
    private void removeGWebCache( GWebCacheHost gWebCache, boolean force )
    {
        synchronized( allGWebCaches )
        {
            // maintain a min number of GWebCaches even if bad.
            if ( allGWebCaches.size() > MIN_G_WEB_CACHES_SIZE || force )
            {
                allGWebCaches.remove( gWebCache );
                functionalGWebCaches.remove( gWebCache );
                sortedGWebCaches.remove( gWebCache );
                
                String uniqueString = gWebCache.getHostDomain();
                uniqueGWebCacheURLs.remove( uniqueString );
                
                if ( gWebCache.isPhexCache() )
                {
                    phexGWebCaches.remove( gWebCache );
                }

                // save file...
                saveGWebCacheToFile();
            }
        }
    }

    /**
     * Inserts a GWebCache...
     */
    private void insertGWebCacheFromLine( String gWebCacheLine )
    {
        // verify URL
        try
        {
            GWebCacheHost gWebCache = parseGWebCacheFromLine( gWebCacheLine );
            if( gWebCache != null && isCacheAccessAllowed( gWebCache ) )
            {
                insertGWebCache( gWebCache );
            }
        }
        catch ( IOException exp )
        {
            //logger.debug( exp.toString(), exp );
        }
    }

    /**
     * Tries to ensure that there is a minimum number of GWebCaches available.
     * This is done by loading GWebCaches from a districuted GWebCache default
     * file and 1 hard coded emergency GWebCache.
     * If we are not on the General Gnutella Network there is no way to ensure
     * a minimum set of GWebCaches and this call returns without actions.
     *
     */
    private void ensureMinGWebCaches()
    {
        if ( allGWebCaches.size() >= 10 )
        {
            return;
        }
        if ( !( servent.getGnutellaNetwork() instanceof GeneralGnutellaNetwork ) )
        {// not on general gnutella network... cant use default list
            return;
        }
        
        //logger.debug( "Load default GWebCache file." );
        InputStream inStream = ClassLoader.getSystemResourceAsStream(
            "phex/resources/gwebcache.cfg" );
        if ( inStream != null )
        {
            InputStreamReader reader = new InputStreamReader( inStream );
            try
            {
                loadGWebCacheFromReader( reader );
                saveGWebCacheToFile();
            }
            catch ( IOException exp )
            {
                //logger.warn( exp.toString(), exp );
            }
        }
        else
        {
           // logger.warn( "Default GWebCache file not found." );
        }
        if ( allGWebCaches.size() < 1 )
        {// emergency case which should never happen since the gwebcache.cfg
         // should contain enough caches.
            //insertGWebCache( "http://gwebcache.bearshare.net/gcache.php" );
            insertPhexGWebCaches();
            saveGWebCacheToFile();
        }
    }
    
    private void insertPhexGWebCaches()
    {
        if ( !( servent.getGnutellaNetwork() instanceof GeneralGnutellaNetwork ) )
        {// not on general gnutella network... can't use default list
            return;
        }
        URL url;
        for ( String phexCachesURL : PHEX_WEB_CACHES )
        {
            try
            {
                url = new URL( phexCachesURL );
                GWebCacheHost cache = new GWebCacheHost( url, true );
                insertGWebCache( cache );
            }
            catch (IOException e)
            {
                //logger.error( e.toString(), e);
            }
        }
    }
    
    private boolean isCacheAccessAllowed( GWebCacheHost gWebCache )
    {
        // check access by IP
//    Looking up host ip turns out to be a very slow solution...
//        byte[] hostIP = gWebCache.getHostIp();
//        byte access = PhexSecurityManager.getInstance().controlHostIPAccess(hostIP);
//        if ( access != PhexSecurityManager.ACCESS_GRANTED )
//        {
//            // ignore GWebCache
//            NLogger.debug( NLoggerNames.GWEBCACHE, "GWebCache IP blocked." );
//            return false;
//        }
        
        // check if this is a IP only gWebCache.. IP only GWebCache are mostly
        // spam distributed by gavinroy.com
        URL cacheURL = gWebCache.getUrl();
        String hostName = cacheURL.getHost();
        if (   AddressUtils.isIPHostName( hostName ) 
            && StringUtils.isEmpty( cacheURL.getPath() ) )
        {
            return false;
        }
        
        // check access by host name
        for ( String blocked : BLOCKED_WEB_CACHES )
        {
            if ( hostName.indexOf(blocked) != -1 )
            {
                // ignore GWebCache
                //logger.debug( "GWebCache host blocked." );
                return false;
            }
        }
        
        return true;
    }
    
    public boolean isPhexGWebCache( String url )
    {
        return PHEX_WEB_CACHES.indexOf(url) != -1;
    }

    private void loadFromConfigInBackgrd( )
    {
        Runnable runner = new Runnable()
        {
            public void run()
            {
                try
                {
                    File gWebCacheFile = servent.getGnutellaNetwork().getGWebCacheFile();
                    if ( !gWebCacheFile.exists() )
                    {
                        return;
                    }
                    loadGWebCacheFromReader( new FileReader( gWebCacheFile ) );
                }
                catch ( IOException exp )
                {
                    //logger.error( "Failed loading GWebCache file.", exp );
                }
                finally
                {
                    ensureMinGWebCaches();
                }
            }
        };
        Environment.getInstance().executeOnThreadPool(runner, "LoadGWebCacheRunner" );
    }

    /**
     * Reads the contents of the reader and closes the reader when done.
     * @param reader the reader to read from.
     * @throws IOException thrown when there are io errors.
     */
    private void loadGWebCacheFromReader( Reader reader )
        throws IOException
    {
        BufferedReader br = new BufferedReader( reader );
        String line;
        synchronized( allGWebCaches )
        {
            while ( (line = br.readLine()) != null)
            {
                if ( line.startsWith("#") )
                {
                    continue;
                }
                insertGWebCacheFromLine( line );
            }
        }
        br.close();
    }

    private void saveGWebCacheToFile()
    {
        //logger.debug( "Saving GWebCaches.");
        BufferedWriter writer = null;
        try
        {
            File file = servent.getGnutellaNetwork().getGWebCacheFile();
            writer = new BufferedWriter( new FileWriter( file ) );

            synchronized( allGWebCaches )
            {
                for ( GWebCacheHost gWebCache : allGWebCaches )
                {
                    if ( gWebCache.isPhexCache() )
                    {
                        continue;
                    }
                    // line format can be:
                    // URL         or:
                    // URL lastRequestTime failedInRowCount
                    writer.write( gWebCache.getUrl().toExternalForm() +
                        " " + gWebCache.getLastRequestTime() +
                        " " + gWebCache.getFailedInRowCount()  );
                    writer.newLine();
                }
            }
        }
        catch ( IOException exp )
        {
            //logger.error( exp.toString(), exp );
        }
        finally
        {
            IOUtil.closeQuietly( writer );
        }
    }

	@Override
	public void onEvent(String topic, Object event) {
		// TODO Auto-generated method stub
		if(topic.compareTo(PhexEventTopics.Servent_GnutellaNetwork)== 0)
			onGnutellaNetworkEvent(  topic, (ChangeEvent) event );
	}
}