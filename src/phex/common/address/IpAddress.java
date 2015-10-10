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

import java.util.Arrays;

import phex.common.Ip2CountryDB;
import phex.utils.IOUtil;

public class IpAddress
{
    public enum IPClass { CLASS_A, CLASS_B, CLASS_C, INVALID }

    public static final IpAddress LOCAL_HOST_IP = new IpAddress( 
        new byte[] { 127, 0, 0, 1 } );
    public static final IpAddress UNSET_IP = new IpAddress( 
        new byte[] { 0, 0, 0, 0 } );
    
    public static final String LOCAL_HOST_NAME = "127.0.0.1";

    /** Cache the hash code for the address */
    private int hash = 0;

    private final byte[] hostIP;
    
    public String countryCode;

    public IpAddress( byte[] aHostIP )
    {
        if ( aHostIP == null )
        {
            throw new NullPointerException( "Ip is null" );
        }
        hostIP = aHostIP;
    }
        
    /**
     * The method returns the IP of the host.
     */
    public byte[] getHostIP()
    {
        return hostIP;
    }
    
    public String getFormatedString()
    {
        return AddressUtils.ip2string(hostIP);
    }
    
    public long getLongHostIP( )
    {
        int v1 =  hostIP[3]        & 0xFF;
        int v2 = (hostIP[2] <<  8) & 0xFF00;
        int v3 = (hostIP[1] << 16) & 0xFF0000;
        int v4 = (hostIP[0] << 24);
        long ipValue = ((long)(v4|v3|v2|v1)) & 0x00000000FFFFFFFFL;
        return ipValue;
    }

    public boolean equals( IpAddress address )
    {
        if ( address == null )
        {
            return false;
        }
        return Arrays.equals( hostIP, address.hostIP );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof IpAddress )
        {
            return equals( (IpAddress) obj );
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        if ( hash == 0 )
        {
            int ipVal = IOUtil.deserializeInt( hostIP, 0 );
            hash = ipVal;
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
        if ( countryCode == null )
        {
            countryCode = Ip2CountryDB.getCountryCode( this );
        }
        return countryCode; 
    }

    /**
     * Checks if the host address is the local one with the local port
     */
    public boolean isLocalAddress( DestAddress localAddress )
    {
        if ( hostIP[0] == (byte) 127 )
        {
            return true;
        }
        else
        {
            return localAddress.getIpAddress().equals(this);
        }
    }

    /**
     * Checks if the IpAddress is a site local ip. Meaning a address in 
     * the private LAN. 
     *
     * @return a <code>boolean</code> indicating if the address is 
     * a site local ip; or false if address is not a site local ip.
     */
    public boolean isSiteLocalIP()
    {
        //10.*.*.* and 127.*.*.*
        if ( hostIP[0] == (byte)10 || hostIP[0] == (byte)127 )
        {
            return true;
        }
        //172.16.*.* - 172.31.*.*
        if ( hostIP[0] == (byte)172 && hostIP[1] >= (byte)16 && hostIP[1] <= (byte)31 )
        {
            return true;
        }
        //192.168.*.*
        if ( hostIP[0] == (byte)192 && hostIP[1] == (byte)168 )
        {
            return true;
        }
        return false;
    }
    
    public boolean isValidIP()
    {
        // Class A
        // |0|-netid-|---------hostid---------|
        //     7 bits                  24 bits
        //
        // Class B
        // |10|----netid-----|-----hostid-----|
        //            14 bits          16 bits
        //
        // Class C
        // |110|--------netid--------|-hostid-|
        //                    21 bits   8 bits
        boolean valid;
        switch( getIPClass() )
        {
            case CLASS_A:
                valid = ((hostIP[1]&0xFF) + (hostIP[2]&0xFF) + (hostIP[3]&0xFF)) != 0;
                break;
            case CLASS_B:
                valid = ((hostIP[2]&0xFF) + (hostIP[3]&0xFF)) != 0;
                break;
            case CLASS_C:
                valid = (hostIP[3]&0xFF) != 0;
                break;
            case INVALID:
            default:
                valid = false;
                break;
        }
        return valid;
    }

    public IPClass getIPClass()
    {
        // Class A
        // |0|-netid-|---------hostid---------|
        //     7 bits                  24 bits
        //
        // Class B
        // |10|----netid-----|-----hostid-----|
        //            14 bits          16 bits
        //
        // Class C
        // |110|--------netid--------|-hostid-|
        //                    21 bits   8 bits

        if ( (hostIP[0] & 0x80) == 0 )
        {
            return IPClass.CLASS_A;
        }
        else if ( (hostIP[0] & 0xC0) == 0x80 )
        {
            return IPClass.CLASS_B;
        }
        else if ( (hostIP[0] & 0xE0) == 0xC0 )
        {
            return IPClass.CLASS_C;
        }
        else
        {
            return IPClass.INVALID;
        }
    }

    @Override
	public String toString()
    {
        return AddressUtils.ip2string( hostIP );
    }
}
