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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import phex.net.repres.PresentationManager;
import phex.security.AccessType;
import phex.security.IpCidrPair;
import phex.security.PhexSecurityManager;
import phex.utils.IOUtil;

public class AddressUtils
{
    private AddressUtils()
    {}
    
    /**
     * Array containing the int value network mask at the position
     * of its corresponding cidr value.
     */
    public static final int[] CIDR2MASK = new int[]
    {
        0x00000000, 0x80000000, 0xC0000000, 0xE0000000,
        0xF0000000, 0xF8000000, 0xFC000000, 0xFE000000,
        0xFF000000, 0xFF800000, 0xFFC00000, 0xFFE00000,
        0xFFF00000, 0xFFF80000, 0xFFFC0000, 0xFFFE0000,
        0xFFFF0000, 0xFFFF8000, 0xFFFFC000, 0xFFFFE000,
        0xFFFFF000, 0xFFFFF800, 0xFFFFFC00, 0xFFFFFE00,
        0xFFFFFF00, 0xFFFFFF80, 0xFFFFFFC0, 0xFFFFFFE0,
        0xFFFFFFF0, 0xFFFFFFF8, 0xFFFFFFFC, 0xFFFFFFFE,
        0xFFFFFFFF
    };
    
    /**
     * Converts the given bytes of an IP to a string representation.
     */
    public static String ip2string(byte[] ip)
    {
        if ( ip == null )
        {
            throw new NullPointerException("Ip is null!");
        }
        assert ip.length == 4;
        
        return (ip[0] & 0xff) + "." 
            + (ip[1] & 0xff) + "." 
            + (ip[2] & 0xff) + "." 
            + (ip[3] & 0xff);
    }
    
    /**
     * Converts the given bytes of an IP to a string representation.
     */
    public static String ip2string( int ip )
    {
        return ((ip >> 24) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." + 
               ((ip >>  8) & 0xFF) + "." + 
               ( ip        & 0xFF);
    }

    /**
     * Returns true if the given host name represents a IP address.
     * @param hostName
     * @return true if the given host name represents a IP address.
     */
    public static boolean isIPHostName( String hostName )
    {
        int portSeparatorIdx = hostName.indexOf( ':' );
        if ( portSeparatorIdx != -1 )
        {// cut of port.
            hostName = hostName.substring( 0, portSeparatorIdx );
        }
        char[] data = hostName.toCharArray();
        int hitDots = 0;
        for(int i = 0; i < data.length; i++)
        {
            char c = data[i];
            if (c < 48 || c > 57)
            { // !digit
                return false;
            }
            while(c != '.')
            {
                //      '0'       '9'
                if (c < 48 || c > 57)
                { // !digit
                    return false;
                }
                if (++i >= data.length)
                {
                    break;
                }
                c = data[i];
            }
            hitDots++;
        }
        if(hitDots != 4 || hostName.endsWith("."))
        {
            return false;
        }
        return true;
    }

    /**
     * Parses and validates a host address string in the format
     * ip:port. If no port is given 6346 is default.
     * Validation includes host ip access security check.
     * @param addressString in format ip:port or ip (port default 6346)
     * @return the parsed host address or null in case ther is any kind of error.
     * @throws MalformedDestAddressException
     */
    public static DestAddress parseAndValidateAddress( String addressString,
        boolean isPrivateIpAllowed, PhexSecurityManager securityService )
        throws MalformedDestAddressException
    {
        byte[] ip = AddressUtils.parseIP( addressString );
        if ( ip == null )
        {
            throw new MalformedDestAddressException( "Invalid IP: "
                + addressString );
        }
        int port = AddressUtils.parsePort( addressString );
        // Limewire is not setting default port...
        if ( port == -1 )
        {
            port = 6346;
        }
        else if ( !AddressUtils.isPortInRange( port ) )
        {
            throw new MalformedDestAddressException( "Port out of range: "
                + addressString );
        }
        IpAddress ipAddress = new IpAddress( ip );
        DestAddress hostAddress = 
            PresentationManager.getInstance().createHostAddress(ipAddress, port);
        if ( !hostAddress.isValidAddress() )
        {
            throw new MalformedDestAddressException( "Invalid IP: "
                + addressString );
        }
        if ( !isPrivateIpAllowed && hostAddress.isSiteLocalAddress() )
        {
            throw new MalformedDestAddressException( "Private IP: "
                + addressString );
        }
        
        AccessType access = securityService.controlHostIPAccess(
            hostAddress.getIpAddress().getHostIP() );
        switch ( access )
        {
            case ACCESS_DENIED:
            case ACCESS_STRONGLY_DENIED:
                throw new MalformedDestAddressException( "Host access denied: "
                    + addressString );
        }
        return hostAddress;
    }

    /**
     * Parses a unsigned int value to a byte array containing the IP
     * @param ip the IP in String format 111.111.111.111
     * @return byte array representation of the IP
     */
    public static byte[] parseIntIP( String ip )
    {
        long IP = Long.parseLong( ip );
        
        // create byte[] from int address
        byte[] addr = new byte[4];
        addr[0] = (byte) ((IP >>> 24) & 0xFF);
        addr[1] = (byte) ((IP >>> 16) & 0xFF);
        addr[2] = (byte) ((IP >>> 8) & 0xFF);
        addr[3] = (byte) (IP & 0xFF);
        return addr;
    }
    
    /**
     * Parses a netmask. This can either be in ip format (xxx.xxx.xxx.xxx)
     * or an int when using CIDR notation
     * @param netmask
     * @return
     */
    public static int parseNetmaskToInt( String netmask )
    {
        if ( netmask.indexOf( '.' ) == -1 )
        { // CIDR notation
            try 
            {
                int val = Integer.parseInt( netmask );
                if ( val == 32 )
                {
                    return 0xFFFFFFFF;
                }
                else if ( val >= 0)
                {
                    return ~(-1 >>> val);
                }
                throw new IllegalArgumentException( "Invalid netmask: " + netmask );
            }
            catch ( NumberFormatException exp )
            {
                throw new IllegalArgumentException( "Invalid netmask: " + netmask );
            }
        }
        else
        {// simple ip notation
            return parseDottedIpToInt( netmask );
        }
    }
    
    /**
     * Parses a netmask. This can either be in ip format (xxx.xxx.xxx.xxx)
     * or an int when using CIDR notation
     * @param netmask
     * @return
     */
    public static byte parseNetmaskToCidr( String netmask )
    {
        if ( netmask.indexOf( '.' ) == -1 )
        { // CIDR notation
            try 
            {
                byte val = Byte.parseByte( netmask );
                if ( val >= 0)
                {
                    return val;
                }
                throw new IllegalArgumentException( "Invalid netmask: " + netmask );
            }
            catch ( NumberFormatException exp )
            {
                throw new IllegalArgumentException( "Invalid netmask: " + netmask );
            }
        }
        else
        {// simple ip notation
            return calculateCidr( parseDottedIpToInt( netmask ) );
        }
    }
    
    /**
     * Parses an ip in the format xxx.xxx.xxx.xxx into an int value.
     * @param hostIp
     * @return
     */
    public static int parseDottedIpToInt( String hostIp )
    {
        /* The string (probably) represents a numerical IP address.
         * Parse it into an int, don't do uneeded reverese lookup,
         * leave hostName null, don't cache.  If it isn't an IP address,
         * (i.e., not "%d.%d.%d.%d") or if any element > 0xFF,
         * it is a hostname and we return null.
         * This seems to be 100% compliant to the RFC1123 spec.
         */
        String ipToParse;
        int portSeparatorIdx = hostIp.indexOf( ':' );
        if ( portSeparatorIdx != -1 )
        {// cut of port.
            ipToParse = hostIp.substring( 0, portSeparatorIdx );
        }
        else
        {
            ipToParse = hostIp;
        }
        
        char[] data = ipToParse.toCharArray();
        int IP = 0x00;
        int hitDots = 0;
    
        for(int i = 0; i < data.length; i++)
        {
            char c = data[i];
            if (c < 48 || c > 57)
            { // !digit
                throw new IllegalArgumentException( "IP contains character: " + ipToParse + " - org: " + hostIp);
            }
            int b = 0x00;
            while(c != '.')
            {
                //      '0'       '9'
                if (c < 48 || c > 57)
                { // !digit
                    throw new IllegalArgumentException( "IP contains character: " + ipToParse );
                }
                b = b * 10 + c - '0';
    
                if (++i >= data.length)
                {
                    break;
                }
                c = data[i];
            }
            if(b > 0xFF)
            { /* bogus - bigger than a byte */
                throw new IllegalArgumentException( "Bogus ip value: " + ipToParse );
            }
            IP = (IP << 8) + b;
            hitDots++;
        }
        if(hitDots != 4 || ipToParse.endsWith("."))
        {
            throw new IllegalArgumentException( "Bogus ip: " + ipToParse );
        }

        return IP;
    }

    /**
     * Trys to parse the given string. The String must represent a numerical IP
     * address in the format %d.%d.%d.%d. A possible attached port will be cut of.
     * @return the ip represented in a byte[] or null if not able to parse the ip.
     */
    public static byte[] parseIP( String hostIp )
    {
        
        try
        {
            int IP = parseDottedIpToInt( hostIp );
            // create byte[] from int address
            byte[] addr = new byte[4];
            addr[0] = (byte) ((IP >>> 24) & 0xFF);
            addr[1] = (byte) ((IP >>> 16) & 0xFF);
            addr[2] = (byte) ((IP >>> 8) & 0xFF);
            addr[3] = (byte) (IP & 0xFF);
            return addr;
        }
        catch ( IllegalArgumentException exp )
        {
            //NLogger.warn( AddressUtils.class, exp );
            return null;
        }
    }

    /**
     * Tries to parse the port of a host string. If no port could be parsed -1 is
     * returned.
     * @param hostName the full host string.
     * @return the port as an int or -1.
     */
    public static int parsePort( String hostName ) 
    {
        int portIdx = hostName.indexOf( ':' );
        if ( portIdx == -1 )
        {
            return -1;
        }
    
        String portString = hostName.substring( portIdx + 1);
        char[] data = portString.toCharArray();
        int port = 0;
        for ( char c : data )
        {
            //      '0'       '9'
            if (c < 48 || c > 57)
            { // !digit
                break;
            }
            // shift left and add value
            port = port * 10 + c - '0';
        }
        // no port or out of range
        if ( !isPortInRange( port ) )
        {
            return -1;
        }
        return port;
    }

    public static String toIntValueString( byte[] ip )
    {
        int v1 =  ip[3]        & 0xFF;
        int v2 = (ip[2] <<  8) & 0xFF00;
        int v3 = (ip[1] << 16) & 0xFF0000;
        int v4 = (ip[0] << 24);
        long ipValue = ((long)(v4|v3|v2|v1)) & 0x00000000FFFFFFFFL;
        return String.valueOf( ipValue );
    }
    
    public static int byteIpToIntIp( byte[] ip )
    {
        int v1 =  ip[3]        & 0xFF;
        int v2 = (ip[2] <<  8) & 0xFF00;
        int v3 = (ip[1] << 16) & 0xFF0000;
        int v4 = (ip[0] << 24);
        int ipValue = ((v4|v3|v2|v1)) & 0xFFFFFFFF;
        return ipValue;
    }
    
    public static byte[] intIp2ByteIp( int ip )
    {
        //  create byte[] from int address
        byte[] addr = new byte[4];
        addr[0] = (byte) ((ip >>> 24) & 0xFF);
        addr[1] = (byte) ((ip >>> 16) & 0xFF);
        addr[2] = (byte) ((ip >>> 8) & 0xFF);
        addr[3] = (byte) (ip & 0xFF);
        return addr;
    }

    /**
     * Validates a port value if it is in range ( 1 - 65535 )
     * 
     * @param port the port to verify in int value. Unsigned short ports must be
     * converted to singned int to let this function work correctly.
     * @return true if the port is in range, false otherwise.
     */
    public static boolean isPortInRange( int port )
    {
        return ( port & 0xFFFF0000 ) == 0 && port != 0;
    }
    
    /**
     * Calculates the cidr from an netmask IP.
     * @param ip the IP to calculate the cidr for.
     * @return the cidr value of the IP.
     */
    public static byte calculateCidr( int ip )
    {
        byte b = 0;
        
        b += cidrByteToNBits( (byte)((ip >>> 24) & 0xFF) );
        b += cidrByteToNBits( (byte)((ip >>> 16) & 0xFF) );
        b += cidrByteToNBits( (byte)((ip >>> 8) & 0xFF) );
        b += cidrByteToNBits( (byte)(ip & 0xFF) );
        
        return b;
    }
    
    private static byte cidrByteToNBits( byte val )
    {
        switch ( val )
        {
        case (byte)0xFF:
            return 8;
        case (byte)0xFE:
            return 7;
        case (byte)0xFC:
            return 6;
        case (byte)0xF8:
            return 5;
        case (byte)0xF0:
            return 4;
        case (byte)0xE0:
            return 3;
        case (byte)0xC0:
            return 2;
        case (byte)0x80:
            return 1;
        case (byte)0x00:
            return 0;
        }
        throw new IllegalArgumentException( "Invalid byte value" );
    }
    
    /**
     * Calculates the cidr from an netmask IP.
     * @param ip the IP to calculate the cidr for.
     * @return the cidr value of the IP.
     */
    public static byte calculateCidr( byte[] ip )
    {
        int intIp = byteIpToIntIp( ip );
        return calculateCidr( intIp );
    }
    
    public static List<IpCidrPair> range2cidr( byte[] startIp, byte[] endIp )
    {
        long start = IOUtil.unsignedInt2Long( byteIpToIntIp( startIp ) );
        long end = IOUtil.unsignedInt2Long( byteIpToIntIp( endIp ) );
        
        ArrayList<IpCidrPair> pairs = new ArrayList<IpCidrPair>();
        while ( end >= start )
        {
            byte maxsize = 32;
            while ( maxsize > 0)
            {
                long mask = CIDR2MASK[ maxsize -1 ];
                long maskedBase = start & mask;
                if ( maskedBase != start )
                {
                    break;
                }
                maxsize--;
            }
            
            // Java 6 - Math.getExponent() might perform faster.
            double x = Math.log( end - start + 1) / Math.log( 2 );
            byte maxdiff = (byte)( 32 - Math.floor( x ) );
            if ( maxsize < maxdiff)
            {
                maxsize = maxdiff;
            }
            pairs.add( new IpCidrPair( (int)start, maxsize ) );
            // Java 6 - Math.scalb( 1, (32 - maxsize) ) might perform faster.
            start += Math.pow( 2, (32 - maxsize) );
        }
        return pairs;
    }
    
    public static SocketAddress createSocketAddress( DestAddress address ) throws IOException
    {
        try
        {
            InetAddress inetAddress;
            if ( address.isIpHostName() )
            {
                IpAddress ip = address.getIpAddress();
                inetAddress = InetAddress.getByAddress( ip.getHostIP() );
            }
            else
            {
                inetAddress = InetAddress.getByName( address.getHostName() );
            }
            int port = address.getPort();
            return new InetSocketAddress( inetAddress, port );
        }
        catch ( UnknownHostException exp )
        {
            throw new IOException( "Can't resolve address." );
        }
    }
}