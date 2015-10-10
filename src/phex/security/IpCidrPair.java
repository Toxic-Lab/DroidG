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
 *  --- SVN Information ---
 *  $Id$
 */
package phex.security;

import phex.common.address.AddressUtils;


public class IpCidrPair
{
    // we access the field directly for optimal performance.
    public final int ipAddr;
    public final byte cidr;
    
    public IpCidrPair( int ip, byte cidr )
    {
        ipAddr = ip;
        this.cidr = cidr;
    }
    
    public IpCidrPair( int ip )
    {
        ipAddr = ip;
        cidr = 32;
    }
    
    public int getNetMask()
    {
        return AddressUtils.CIDR2MASK[ this.cidr ];
    }

    public boolean contains( IpCidrPair ip)
    {
        return (ip.ipAddr & getNetMask() ) == this.ipAddr
            && (ip.getNetMask() & getNetMask() ) == getNetMask();
    }
    
    public boolean contains( int ip, int netMask )
    {
        return (ip & getNetMask() ) == this.ipAddr
            && (netMask & getNetMask() ) == getNetMask();
    }
    
    public boolean equals(Object obj) 
    {
        if ( this == obj ) return true;
        if ( !(obj instanceof IpCidrPair) ) 
        {
            return false;
        }
        
        IpCidrPair pair = (IpCidrPair)obj;
        if ( cidr != pair.cidr )
        {
            return false;
        }
        if ( (ipAddr & getNetMask()) != (pair.ipAddr & pair.getNetMask()) )
        {
            return false;
        }
        return true;
    }

    public int hashCode() 
    {
        return ipAddr^getNetMask();
    }
    
    public String toString()
    {
        return AddressUtils.ip2string( ipAddr ) + "/" + cidr;
    }
}
