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

import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;

/**
 * Represents the Udp Host Cache
 */
public class UdpHostCache extends BootstrapHost
{
    /**
     * Maximum permissible failure count on a cache
     */
    public static final int MAX_FAIL_COUNT = 3;

    private DestAddress address;

    public UdpHostCache( DestAddress addr )
    {
        this( addr, 0 );
    }

    public UdpHostCache( DestAddress addr, int failCount )
    {
        super( false );
        address = addr;
        setFailedInRowCount( failCount );
    }

    public UdpHostCache( String aHostName, int aPort, int failCount )
        throws IllegalArgumentException
    {
        this( new DefaultDestAddress( aHostName, aPort ), 
            failCount );
    }

    public DestAddress getHostAddress()
    {
        return this.address;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj instanceof UdpHostCache )
        {
            UdpHostCache objCache = (UdpHostCache) obj;
            return this.address.equals( objCache.address );
        }
        return false;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public int hashCode()
    {
        return 13 * 31 + this.address.hashCode();
    }

    @Override
    public String toString()
    {
        String str = " Host Address : " + address + " [ failure count : "
            + getFailedInRowCount() + " ] ";

        return str;
    }
}
