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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import phex.common.PhexVersion;
import phex.common.address.DestAddress;
import phex.common.address.MalformedDestAddressException;
import phex.connection.ProtocolNotSupportedException;
import phex.http.HTTPHeaderNames;
import phex.http.HttpClientFactory;
import phex.net.repres.PresentationManager;
import phex.prefs.core.ProxyPrefs;
import phex.security.AccessType;
import phex.security.PhexSecurityManager;
import phex.utils.IOUtil;
import phex.utils.LengthLimitedInputStream;
import phex.utils.NormalizableURL;
import phex.utils.StringUtils;
import phex.utils.URLCodecUtils;

/**
 * This class implements the basic functionality of the Gnutella Web Cache (GWebCache)
 * invented from the Gnucleus Team.
 *
 * To access a GWebCache you should send a ping request first. To verify if it is
 * working.
 *
 * Supports Version 1.3 of GWebCache specification.
 */
public class GWebCacheConnection
{
    private static final String PING_QUERY = "ping=1";
    private static final String HOST_FILE_QUERY = "hostfile=1";
    private static final String URL_FILE_QUERY = "urlfile=1";
    private static final String IP_QUERY = "ip=";
    private static final String URL_QUERY = "url=";

    private static final String QUERY_POSTFIX =
        "&client=PHEX&version=" + PhexVersion.getFullVersion();
    
//    private static final Logger logger = LoggerFactory.getLogger( GWebCacheConnection.class );

    /**
     * The GWebCache base URL.
     */
    private GWebCacheHost gWebCache;

    /**
     * The connection to the GWebCache. The connection is opened when accessing
     * the GWebCache the first time.
     */
    private HttpMethod method;

    /**
     * The reader of the connection.
     */
    private BufferedReader reader;

    /**
     * Indicates if a cache is bad.
     */
    private boolean isCacheBad;

    /**
     * @param aGWebCache the GWebCache. Must be http protocol.
     * @throws ProtocolNotSupportedException in case a different protocol then http is used
     */
    public GWebCacheConnection( GWebCacheHost aGWebCache )
        throws ProtocolNotSupportedException
    {
        if ( !aGWebCache.getUrl().getProtocol().equals( "http" ) )
        {
            throw new ProtocolNotSupportedException(
                "Only http URLs are supported for a GWebCacheConnection" );
        }
        this.gWebCache = aGWebCache;
        isCacheBad = false;
    }

    /**
     * @return the {@link GWebCacheHost} behind this connection.
     */
    public GWebCacheHost getGWebCache()
    {
        return gWebCache;
    }

    /**
     * Sends a ping request to the GWebCache. The method returns true if a PONG
     * was received otherwise it returns false and set the cache to be a bad cache.
     */
    public boolean sendPingRequest()
    {
        try
        {
            URL requestURL = new URL( gWebCache.getUrl(), gWebCache.getUrl().getPath()
                + '?' + PING_QUERY + QUERY_POSTFIX );
            if ( requestURL.getHost() == null )
            {
                //logger.warn( "Host is null" );
                isCacheBad = true;
                return false;
            }
            openConnection( requestURL );
            String line = reader.readLine();
            if ( line != null && line.startsWith( "PONG" ) )
            {
                return true;
            }
            else
            {
                isCacheBad = true;
                return false;
            }
        }
        catch ( UnknownHostException exp )
        {
            //logger.debug( exp.getMessage(), exp);
            isCacheBad = true;
            return false;
        }
        catch ( IOException exp )
        {
            //logger.debug( exp.getMessage(), exp);
            isCacheBad = true;
            return false;
        }
        finally
        {
            closeConnection();
        }
    }

    /**
     * Request a list of hosts from the GWebCache. The returned list is either
     * null if a problem occurred, empty if the GWebCache does not have any hosts
     * stored, or filled with IPs.
     */
    public DestAddress[] sendHostFileRequest( PhexSecurityManager securityService )
    {
        try
        {
            URL requestURL = new URL( gWebCache.getUrl(), gWebCache.getUrl().getPath()
                + '?' + HOST_FILE_QUERY + QUERY_POSTFIX );
            if ( requestURL.getHost() == null )
            {
               // logger.warn( "Host is null" );
                isCacheBad = true;
                return null;
            }
            openConnection( requestURL );
            String line = reader.readLine();
            // the first line might contain an error
            if ( line != null && line.startsWith( "ERROR" ) )
            {
                isCacheBad = true;
                return null;
            }
            
            DestAddress destAddress;
            boolean falseHostFound = false;
            List<DestAddress> hostFileList = new ArrayList<DestAddress>( 20 );
            PresentationManager presentationMgr = PresentationManager.getInstance();
            while ( line != null )
            {
                try
                {
                    // The line requires to have a valid port. If no port could
                    // be found the default port of -1 should ensure the
                    // DestAddress is invalid when no port is given.
                    destAddress = presentationMgr.createHostAddress( line, -1 );
                    if ( !destAddress.isIpHostName() || !destAddress.isValidAddress() )
                    {
                        throw new MalformedDestAddressException( "Invalid address." );
                    }
                    
                    AccessType access = securityService.controlHostAddressAccess( 
                        destAddress );
                    switch ( access )
                    {
                        case ACCESS_DENIED:
                        case ACCESS_STRONGLY_DENIED:
                            // skip host address...
                            break;
                        case ACCESS_GRANTED:
                            hostFileList.add( destAddress );
                            break;
                    }
                }
                catch (MalformedDestAddressException exp)
                {
                    //logger.debug( "Invalid host found: {}", line );
                    falseHostFound = true;
                    line = reader.readLine();
                    continue;
                }
                line = reader.readLine();
            }
            // all returned hosts where false, cache is bad.
            if ( hostFileList.size() == 0 && falseHostFound )
            {
                isCacheBad = true;
                return null;
            }
            DestAddress[] hostFileArr = new DestAddress[ hostFileList.size() ];
            hostFileList.toArray( hostFileArr );
            //logger.debug( "Hosts found: {}", hostFileArr.length );
            return hostFileArr;
        }
        catch ( IOException exp )
        {
            //logger.debug( exp.toString(), exp);
            isCacheBad = true;
            return null;
        }
        finally
        {
            closeConnection();
        }
    }

    /**
     * Request a list of GWebCache URLs from the GWebCache. The returned list is
     * either null if a problem occurred, empty if the GWebCache does not have any
     * URLs stored, or filled with URLs.
     */
    public URL[] sendURLFileRequest()
    {
        try
        {
            URL requestURL = new URL( gWebCache.getUrl(), gWebCache.getUrl().getPath()
                + '?' + URL_FILE_QUERY + QUERY_POSTFIX );
            if ( requestURL.getHost() == null )
            {
               // logger.warn( "Host is null" );
                isCacheBad = true;
                return null;
            }
            openConnection( requestURL );
            String line = reader.readLine();
            // the first line might contain an error
            if ( line != null && line.startsWith( "ERROR" ) )
            {
                isCacheBad = true;
                return null;
            }

            boolean falseURLFound = false;
            List<URL> urlFileList = new ArrayList<URL>( 20 );
            int counter = 0;
            // don't apply more then 20 URL
            while ( line != null && counter < 20 )
            {
                try
                {
                    NormalizableURL helpUrl = new NormalizableURL( line );
                    helpUrl.normalize();
                    URL url = new URL( helpUrl.toExternalForm() );
                    if ( !url.getProtocol().equals( "http" ) )
                    {
                        throw new ProtocolNotSupportedException(
                            "Only http URLs are supported for a GWebCacheConnection" );
                    }
                    urlFileList.add( url );
                }
                catch ( MalformedURLException exp )
                {//ignore false url
                    falseURLFound = true;
                }
                line = reader.readLine();
                counter ++;
            }
            // all returned urls where false, cache is bad.
            if ( urlFileList.size() == 0 && falseURLFound )
            {
                isCacheBad = true;
                return null;
            }

            URL[] urlFileArr = new URL[ urlFileList.size() ];
            urlFileList.toArray( urlFileArr );
           // logger.debug( "URLs found: {}", urlFileArr.length );
            return urlFileArr;
        }
        catch ( IOException exp )
        {
            //logger.debug( exp.getMessage(), exp);
            isCacheBad = true;
            return null;
        }
        finally
        {
            closeConnection();
        }
    }

    /**
     * Sends a update request to update the GWebCache. You can provide a fullHostName
     * and a cacheURL or only one of them by settings the other one to null.
     */
    public boolean updateRequest( String fullHostName, String cacheURL )
    {
        if ( fullHostName == null && cacheURL == null )
        {
            throw new IllegalArgumentException(
                "Must provide at least one of hostIP or cacheURL." );
        }

        try
        {
            StringBuffer queryBuffer = new StringBuffer();
            if ( fullHostName != null )
            {
                queryBuffer.append( IP_QUERY );
                queryBuffer.append( URLCodecUtils.encodeURL( fullHostName ) );
            }
            if ( cacheURL != null )
            {
                if ( fullHostName != null )
                {
                    queryBuffer.append( '&' );
                }
                queryBuffer.append( URL_QUERY );
                queryBuffer.append( URLCodecUtils.encodeURL( cacheURL ) );
            }

            URL requestURL = new URL( gWebCache.getUrl(), gWebCache.getUrl().getPath()
                + '?' + queryBuffer.toString() + QUERY_POSTFIX );
            if ( requestURL.getHost() == null )
            {
                //logger.warn( "Host is null" );
                isCacheBad = true;
                return false;
            }
            openConnection( requestURL );
            String line = reader.readLine();
            if ( line != null && line.startsWith( "OK" ) )
            {
                return true;
            }
            else
            {
                isCacheBad = true;
                return false;
            }
        }
        catch ( IOException exp )
        {
            //logger.debug( exp.getMessage(), exp);
            isCacheBad = true;
            return false;
        }
        finally
        {
            closeConnection();
        }
    }

    /**
     * Returns if a cache is bad.
     *
     * Bad caches are those that return:<br>
     * - nothing - those that cannot be accessed at all (timeouts, invalid hostnames, etc.)<br>
     * - HTTP error codes (400-599)<br>
     * - responses that cannot be parsed by a client<br>
     * - ERROR response<p>
     * @return true if the cache is bad, false otherwise
     */
    public boolean isCacheBad()
    {
        return isCacheBad;
    }

    /**
     * Opens a connection to the request url and checks the response code.
     * 3xx are automatically redirected.
     * 400 - 599 response codes will throw a ConnectException.
     * @throws IOException if an IO error occurs
     */
    private void openConnection( URL requestURL )
        throws IOException
    {
        HttpClient client = HttpClientFactory.createHttpClient();
        
        if ( ProxyPrefs.UseHttp.get().booleanValue()  
            && !StringUtils.isEmpty( ProxyPrefs.HttpHost.get() ) )
        {
            client.getHostConfiguration().setProxy( ProxyPrefs.HttpHost.get(), 
                ProxyPrefs.HttpPort.get().intValue() );
        }
            
        method = new GetMethod( requestURL.toExternalForm() );
        method.setFollowRedirects(false);
        method.addRequestHeader("Cache-Control", "no-cache");
        // be HTTP/1.1 compliant
        method.addRequestHeader( HTTPHeaderNames.CONNECTION,
            "close" );
        
        //logger.debug( "Open GWebCache connection to {}.", requestURL );
        int responseCode = client.executeMethod(method);      
        //logger.debug( "GWebCache {} returned response code: {}", requestURL, responseCode );
        
        // we only accept the status codes 2xx all others fail...
        if ( responseCode < 200 || responseCode > 299 )
        {
            throw new ConnectException( "GWebCache service not available, response code: "
                + responseCode );
        }
        
        InputStream bodyStream = method.getResponseBodyAsStream();
        if ( bodyStream != null )
        {
            // ensure we never read a body size larger then 100K to reduce misuse 
            reader = new BufferedReader( new InputStreamReader(
                new LengthLimitedInputStream( bodyStream, 100000 ) ) );
        }
        else
        {
            throw new ConnectException( "Empty response." );
        }
    }
    
    private void closeConnection()
    {
        IOUtil.closeQuietly( reader );
        reader = null;
        
        if ( method != null )
        {
            method.releaseConnection();
            method = null;
        }
        
        //logger.debug( "Releasing HTTP method." );
    }
}