package phex.security;

/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2007 Phex Development Group
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
 */

import phex.common.address.AddressUtils;
import phex.utils.IOUtil;

import java.util.Arrays;

public class IpPortAddress {

    /** Cache the hash code for the address */
    private int hash = 0;

    private byte[] ipAddress;
    private byte[] port;

    public IpPortAddress(byte[] hostIp)
    {
        int offset = 0;
        this.ipAddress = new byte[4];
        while (offset < 4)
        {
            this.ipAddress[offset] = hostIp[offset];
            offset++;
        }

        this.port = new byte[4];
        if (hostIp.length  > 6) {
            offset = 0;
            while (offset < 4)
            {
                this.port[offset] = hostIp[offset+4];
                offset++;                
            }
        }
        else {
            offset = 0;
            while (offset < 2)
            {
                this.port[offset] = hostIp[offset+4];
                offset++;
            }
        }
    }

    public IpPortAddress(String IpAddress, int port)
    {
        this.ipAddress = AddressUtils.parseIP(IpAddress);
        this.port = new byte[4];
        IOUtil.serializeShortLE((short)port, this.port, 0);
    }

    public byte[] getOrigIpAddress()
    {
        return this.ipAddress;
    }

    public byte[] getOrigPort()
    {
        return this.port;
    }

    public String getIpAddress()
    {
        return AddressUtils.ip2string(this.ipAddress);
    }

    public int getPort()
    {
        return IOUtil.deserializeInt(this.port, 0);
    }

    /**
     * Override hashCode() to ensure that two different instances with equal IpPortAddress
     * generate the same hashCode. This is necessary to find IpPortAddress in Maps.
     */
    public int hashCode()
    {
        if ( hash == 0 )
        {
            int ipVal1 = IOUtil.deserializeInt( ipAddress, 0 );            
            int ipVal2 = IOUtil.deserializeInt( port, 0 );
            hash = ipVal2 * 31;
            hash = hash + ipVal1 * 59;
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if( !(obj instanceof IpPortAddress) )
        {
            return false;
        }

        if( obj == this )
        {
            return true;
        }
        IpPortAddress ipPortAddr = (IpPortAddress)obj;
        return ipPortAddr.equals( ipPortAddr.getOrigIpAddress(), ipPortAddr.getOrigPort() );
    }

    public boolean equals( byte[] ip, byte[] port )
    {
        if ( ipAddress != null )
        {
            return Arrays.equals( this.ipAddress, ip ) && Arrays.equals( this.port, port );
        }
        return false;
    }
    
}
