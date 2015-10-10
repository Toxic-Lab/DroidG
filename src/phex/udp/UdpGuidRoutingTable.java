/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Phex Development Group
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
 *  Created on May 17, 2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.udp;

import java.util.Map;
import java.util.TreeMap;

import phex.common.address.DestAddress;
import phex.msg.GUID;

/**
 * @author Madhu
 */
public class UdpGuidRoutingTable
{
    /**
     * The max number of entries in a single route table. This could lead to have
     * a total of 2 * MAX_ROUTE_TABLE_SIZE entrys in total.
     */
    private static final int MAX_ROUTE_TABLE_SIZE = 50;
    
    protected Map<GUID, DestAddress> currentMap;
    protected Map<GUID, DestAddress> lastMap;
    
    /**
     * The lifetime of a map. After this number of millis passed the lastMap will be
     * replaced by the currentMap.
     */
    private long lifetime;

    /**
     * The time when the next replace is done and the lastMap will be
     * replaced by the currentMap.
     */
    private long nextReplaceTime;
    
    /**
     * The routing table will store at least lifetime to 2 * lifetime of GUID
     * mappings.
     * @param lifetime the lifetime in millis of a map. After this time is passed
     * the lastMap will be replaced by the currentMap.
     */
    public UdpGuidRoutingTable( long lifetime )
    {
        this.lifetime = lifetime;
        currentMap = new TreeMap<GUID, DestAddress>( new GUID.GUIDComparator() );
        lastMap = new TreeMap<GUID, DestAddress>( new GUID.GUIDComparator() );
    }
    
    /**
     * Checks if a routing for the GUID is already available. If the routing is
     * already available, we return false to indicate the message was already
     * routed. If the routing is not available it is added and true is returned.
     *
     * @param guid the GUID to check and add to the routing.
     * @param host address the route destination.
     * @return false, if the routing is
     * already available, to indicate the message was already
     * routed. If the routing is not available it is added and true is returned.
     */
    public synchronized boolean checkAndAddRouting( GUID guid, DestAddress address )
    {
        checkForSwitch();
        
//        // check if still connected.
//        NetworkHostsContainer netContainer = 
//            HostManager.getInstance().getNetworkHostsContainer();
//        if ( !( netContainer.isConnectedToHost( address ) ) )
//        {
//            return false;
//        }
        
        if ( !currentMap.containsKey( guid ) && !lastMap.containsKey( guid ) )
        {
            // update or add guid routing to new host in currentMap
            currentMap.put( guid, address );
            return true;
        }
        else
        {
            return false;
        }
    }
    
    /**
     * Check to delete old entries. If the lifetime has passed.
     */
    protected void checkForSwitch()
    {
        long currentTime = System.currentTimeMillis();
        // check if enough time has passed or the map size reached the max.
        if ( currentTime < nextReplaceTime && currentMap.size() < MAX_ROUTE_TABLE_SIZE )
        {
            return;
        }

        lastMap.clear();
        Map<GUID, DestAddress> temp = lastMap;
        lastMap = currentMap;
        currentMap = temp;
        nextReplaceTime = currentTime + lifetime;
        return;
    }
    
    /**
     * Trys to find the Host address for the given GUID.
     * @param guid the GUID for which the address is to be found.
     * @return the Hostaddress associated with the guid.
     */
    public synchronized DestAddress getRouting( GUID guid )
    {
        DestAddress address = null;
        
        // first check in the current map
        address = currentMap.get( guid );
        if( address != null )
        {
            return address;
        }
        
        // now check in the last map
        address = lastMap.get( guid );
        if( address != null )
        {
            return address;
        }
        
        //not found
        return null;
    }
    
    /**
     * Trys to find the Host address for the given GUID.
     * @param guid the GUID for which the address is to be found.
     * @return the Hostaddress associated with the guid if present 
     * or null
     */
    public synchronized DestAddress getAndRemoveRouting( GUID guid )
    {
        DestAddress address = null;
        
        // first check in the current map
        address = currentMap.remove( guid );
        if( address != null )
        {
            return address;
        }
        
        // now check in the last map
        address = lastMap.remove( guid );
        if( address != null )
        {
            return address;
        }
        
        //not found
        return null;
    }
}
