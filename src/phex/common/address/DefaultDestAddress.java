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
package phex.common.address;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.Arrays;

import org.apache.commons.lang.time.DateUtils;

/**
 * Represents a host address.
 */
public class DefaultDestAddress implements DestAddress
{
//    private static final Logger logger = LoggerFactory.getLogger( 
//        DefaultDestAddress.class );
    static
    {
        // set DNS cache time to 2 minutes
        Security.setProperty( "networkaddress.cache.ttl", "120");
        Security.setProperty( "networkaddress.cache.negative.ttl", "120");
    }
    
    public static final int DEFAULT_PORT = 6346;

    /** Cache the hash code for the address */
    private int hash = 0;

    /**
     * The host name is compared to the host ip very memory intensive.
     * Therefore if the host name represents a ip we only store the ip and
     * not the hostName itself.
     */
    private String hostName;
    
    /**
     * The ip address. This could be a dns lookup ip of the host name. In this
     * case we only store the ip for a limited time (60 seconds) for performance
     * reasons. The lastIpLookupTime indicates when the last ip lookup was
     * performed.
     */
    private IpAddress ipAddress;
    
    /**
     * The port of this address.
     */
    private final int port;
    
    /**
     * The last time a ip lookup was performed. If it is set to -1 and 
     * ipAddress is not null the ip is not a result of a host name lookup.
     */
    private long lastIpLookupTime = -1;

    public DefaultDestAddress( String aHostName, int aPort )
    {
        if ( aHostName == null )
        {
            throw new NullPointerException( "Host name is null" );
        }
        
        // try to store only ip in case of host name represents ip
        if ( AddressUtils.isIPHostName(aHostName) )
        {
            ipAddress = new IpAddress( AddressUtils.parseIP( aHostName ) );
        }
        else
        {
            hostName = aHostName;
        }
        port = aPort;
    }
    
    public DefaultDestAddress( IpAddress ipAddress, int port )
    {
        if ( ipAddress == null )
        {
            throw new NullPointerException( "Null ipAddress not allowed." );
        }
        
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public DefaultDestAddress( byte[] aHostIP, int aPort )
    {
        if ( aHostIP == null )
        {
            throw new NullPointerException( "Host ip is null" );
        }
        ipAddress = new IpAddress( aHostIP );
        port = aPort;
    }

    /**
     * If a host name is known the host name is retuned otherwise the IP is
     * returned. No port information is appended.
     */
    public String getHostName()
    {
        if ( hostName == null )
        {
            return ipAddress.getFormatedString();
        }
        return hostName;
    }
    
    /**
     * Returns whether the host name is the string representation of the IP or
     * not.
     * @return Returns true if the host name is the string representation of 
     * the IP, false otherwise.
     */
    public boolean isIpHostName()
    {
        if ( hostName == null )
        {
            return true;
        }
        if( ipAddress == null )
        {// otherwise host name would be parsed to IpAddress in constructor.
            return false;
        }
        return hostName.equals( ipAddress.getFormatedString() );        
    }
    
    /**
     * Returns the full host name with port in the format hostname:port.
     */
    public String getFullHostName()
    {
        StringBuffer buffer = new StringBuffer( 21 );
        buffer.append( getHostName() );
        buffer.append( ':' );
        buffer.append( port );
        return buffer.toString();
    }

    public int getPort()
    {
        return port;
    }
    
    public IpAddress getIpAddress()
    {
        if ( ipAddress == null || ( lastIpLookupTime > 0 && lastIpLookupTime 
              + DateUtils.MILLIS_PER_MINUTE < System.currentTimeMillis() ) )
        {
            // try to determine IP by lookup..
            try
            {
//                logger.debug( "Performing IP lookup {}", hostName );
                byte[] ip = InetAddress.getByName(hostName).getAddress();
                // we cache IpAddress for a limited time ( 60 seconds ) so that
                // a new IP lookup will occur.
                ipAddress = new IpAddress( ip );
                lastIpLookupTime = System.currentTimeMillis();
            }
            catch (UnknownHostException exp)
            {
            }
        }
        return ipAddress;
    }
    
    public boolean equals( Object obj )
    {
        if ( obj instanceof DefaultDestAddress )
        {
            return equals( (DestAddress) obj );
        }
        return false;
    }

    public boolean equals( DestAddress address )
    {
        if ( address == null )
        {
            return false;
        }
        if ( ipAddress != null && address.getIpAddress() != null)
        {
            return ipAddress.equals(address.getIpAddress()) && port == address.getPort();
        }
        else
        {
            return getHostName().equals( address.getHostName() )
               && port == address.getPort();
        }
    }
    
    public boolean equals( byte[] testIp, int testPort )
    {
        if ( ipAddress != null )
        {
            return Arrays.equals( ipAddress.getHostIP(), testIp ) && port == testPort;
        }
        else
        {
            return getHostName().equals( AddressUtils.ip2string( testIp ) )
               && port == testPort ;
        }
    }

    @Override
    public int hashCode()
    {
        if ( hash == 0 )
        {
            int h = 0;
            h = ((31 *h) + port);
            if ( ipAddress != null )
            {
                h = 31*h + ipAddress.hashCode();
            }
            else
            {
                h = ((127 *h)+hostName.hashCode());
            }
            hash = h;
        }
        return hash;
    }
    
    /**
     * Returns the country code of the HostAddress. But only in case the host ip
     * has already been resolved. Otherwise no country code is returned since
     * the country code lookup would cost high amount of time.
     * @return the country code or null.
     */
    public String getCountryCode()
    {
        if ( ipAddress != null )
        {
            return ipAddress.getCountryCode();
        }
        return null;
    }

    /**
     * Checks if the DestAddress is a loopback address or the external address
     * of this localhost.
     *
     * @return a <code>boolean</code> indicating if the DestAddress represents 
     *         the local host.
     */
    public boolean isLocalHost( DestAddress localAddress )
    {
        IpAddress ipAddr = getIpAddress();
        if ( ipAddr != null && ipAddr.isLocalAddress( localAddress ) )
        {
            return true;
        }
        else
        {
            return localAddress.equals( this );
        }
    }
    
    /**
     * Checks if the DestAddress is a site local address. Meaning a address in 
     * the private LAN. 
     *
     * @return a <code>boolean</code> indicating if the DestAddress is 
     * a site local address; or false if address is not a site local address
     * or the IP could not be resolved.
     */
    public boolean isSiteLocalAddress()
    {
        IpAddress ip = getIpAddress();
        if ( ip == null )
        {
            return false;
        }
        return ip.isSiteLocalIP();
    }
    
    /**
     * Checks if the DestAddress is completely valid. It checks if the port is in
     * range and if the IP is valid.
     * @return a <code>boolean</code> indicating if the DestAddress is 
     * valid; or false otherwise.
     */
    public boolean isValidAddress()
    {
        boolean validPort = AddressUtils.isPortInRange( port );
        boolean validAddress = getIpAddress() != null ? getIpAddress().isValidIP() : true;
        return validPort && validAddress;
    }

    public String toString()
    {
        return getFullHostName();
    }
}