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
package phex.common;

import java.util.HashMap;

import phex.common.address.DestAddress;


/**
 * The class is able to count connections to addresses. This is useful to track how
 * many parallel uploads or downloads to one address are tried.
 * Since different implementations might use/ignore different parts of the 
 * address (port), it is possible to specify whether the full DestAddress is used or 
 * just the IPAddress. 
 */
public class AddressCounter
{
    private final HashMap<Object, Integer> addressCountMap;

    /**
     * The max number of times a destination address is allowed.
     */
    private int maxCount;
    
    private boolean isFullAddressUsed;

    /**
     * Creates an AddressCounter instance allowing the specified number of
     * accesses and specifies if the full address or just the IP should be used
     * for tracking.
     * @param maxCount the max number of counts allowed per address.
     * @param isFullAddressUsed if true the full DestAddress is counted, if 
     *        false only its IPAddress is counted.
     */
    public AddressCounter( int maxCount, boolean isFullAddressUsed )
    {
        addressCountMap = new HashMap<Object, Integer>();
        this.maxCount = maxCount;
        this.isFullAddressUsed = isFullAddressUsed;
    }
    
    public synchronized int getMaxCount( )
    {
        return maxCount;
    }
    
    public synchronized void setMaxCount( int val )
    {
        maxCount = val;
    }

    /**
     * @param address the address to validate and count.
     * @return true if the address was counted, false if the address was rejected 
     * because the maxCount for this address was reached.
     */
    public synchronized boolean validateAndCountAddress( DestAddress address )
    {
        Object significantPart = provideSignificantAddressPart( address );
        Integer count = addressCountMap.get( significantPart );
        if ( count != null )
        {
            if ( count.intValue() == maxCount )
            {
                return false;
            }
            addressCountMap.put( significantPart, Integer.valueOf( count.intValue() + 1 ) );
        }
        else
        {
            addressCountMap.put( significantPart, Integer.valueOf( 1 ) );
        }
        return true;
    }

    public synchronized void relaseAddress( DestAddress address )
    {
        Object significantPart = provideSignificantAddressPart( address );
        Integer count = addressCountMap.get( significantPart );
        if ( count != null )
        {
            if ( count.intValue() == 1 )
            {
                addressCountMap.remove( significantPart );
                return;
            }
            addressCountMap.put( significantPart, Integer.valueOf( count.intValue() - 1 ) );
        }
    }
    
    protected Object provideSignificantAddressPart( DestAddress address )
    {
        if ( isFullAddressUsed )
        {
            return address;
        }
        else
        {
            return address.getIpAddress();
        }
    }
}