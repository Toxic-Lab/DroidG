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
package phex.utils;


import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import phex.host.Host;
import phex.msg.GUID;

/**
 * This GUIDRoutingTable is used to route replies coming from the GNet
 * back to the requester. This is done by using the GUID of the reply
 * which matches the GUID of the request to identify the correct 
 * route back to the requester.
 */
public class GUIDRoutingTable
{
    /**
     * The max number of entries in a single route table. This could lead to have
     * a total of 2 * MAX_ROUTE_TABLE_SIZE entries in total.
     */
    private static final int MAX_ROUTE_TABLE_SIZE = 50000;
    
    /**
     * Maps a GUID to a integer that represents a id. The id can be used to
     * retrieve the host from the idToHostMap. This extra layer is used to solve
     * three problems:<br>
     * - When deleting a host because of disconnection we can delete its entry very
     *   fast and performant from the hostToIdMap and idToHostMap without the
     *   need to iterate over the GUID mappings.<br>
     * - We are still able to identify duplicate query GUIDs even though the host
     *   has already disconnected.<br>
     * - We are able to freeing up the Host object for garbage collections, since
     *   we are not holding it in the GUID mappings.<br>
     * <br>
     * To implement some kind of FIFO behavior for the GUID mappings we are using
     * to sets, currentMap and lastMap. After a certain time passed by we replace
     * the lastMap with the currentMap and create a new fresh currentMap. This
     * allows us to accomplish a very fast and efficient FIFO behavior that stores
     * at least n to 2n seconds of GUID mappings.
     */
    protected Map<GUID, Entry> currentMap;
    protected Map<GUID, Entry> lastMap;

    protected Map<Integer, Host> idToHostMap;
    protected Map<Host, Integer> hostToIdMap;

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
     * The next id to return for a host.
     */
    private int nextId;

    /**
     * The routing table will store at least lifetime to 2 * lifetime of GUID
     * mappings.
     * @param lifetime the lifetime in millis of a map. After this time is passed
     * the lastMap will be replaced by the currentMap.
     */
    public GUIDRoutingTable( long lifetime )
    {
        this.lifetime = lifetime;
        nextId = 0;
        currentMap = new TreeMap<GUID, Entry>( new GUID.GUIDComparator() );
        lastMap = new TreeMap<GUID, Entry>( new GUID.GUIDComparator() );
        idToHostMap = new HashMap<Integer, Host>();
        hostToIdMap = new HashMap<Host, Integer>();
    }

    /**
     * Adds a routing to the routing table.
     * @param guid the GUID to route for
     * @param host the route destination.
     */
    public synchronized void addRouting( GUID guid, Host host )
    {
        checkForSwitch();

        // check if still connected.
        if ( !host.isConnected() )
        {
            return;
        }

        Integer id = getIdForHost( host );
        
        // try to find and remove existing entry obj in currentMap and lastMap
		Entry entry = currentMap.remove( guid );
		if ( entry == null )
		{
			entry = lastMap.remove( guid );
	        if ( entry == null )
	        {
	        	entry = createNewEntry();
	        }
		}
		// update host id... there is a low chance it might have changed.
		entry.hostId = id;

        // update or add guid routing to new host in currentMap
        currentMap.put( guid, entry );
    }

    /**
     * Checks if a routing for the GUID is already available. If the routing is
     * already available, we return false to indicate the message was already
     * routed. If the routing is not available it is added and true is returned.
     * The check and set is done in one atomic operation because there are
     * multiple threads handling multiple connection at the same time.
     *
     * @param guid the GUID to check and add to the routing.
     * @param host the route destination.
     * @return false, if the routing is
     * already available, to indicate the message was already
     * routed. If the routing is not available it is added and true is returned.
     */
    public synchronized boolean checkAndAddRouting( GUID guid, Host host )
    {
        checkForSwitch();
        // check if still connected.
        if ( !host.isConnected() )
        {
            return false;
        }
        if ( !currentMap.containsKey( guid ) && !lastMap.containsKey( guid ) )
        {
            Integer id = getIdForHost( host );
			Entry entry = createNewEntry();
			entry.hostId = id;
            // update or add guid routing to new host in currentMap
            currentMap.put( guid, entry );
            
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Removes the host from the id mapping.
     * @param host the host to remove.
     */
    public synchronized void removeHost( Host host )
    {
        Integer id = hostToIdMap.get( host );
        if ( id != null )
        {
            idToHostMap.remove( id );
            hostToIdMap.remove( host );
        }
    }

    /**
     * Tries to find the reply route for the given GUID.
     * @param guid the GUID for the reply route to find.
     * @return the Host to route the reply for.
     */
    public synchronized Host findRouting( GUID guid )
    {
		Entry entry = currentMap.get( guid );
        if ( entry == null )
        {
			entry = lastMap.get( guid );
        }
        if ( entry != null )
        {
            // returns null if there is no host for the id anymore.
            return idToHostMap.get( entry.hostId );
        }
        else
        {
            return null;
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
        Map<GUID, Entry> temp = lastMap;
        lastMap = currentMap;
        currentMap = temp;
        nextReplaceTime = currentTime + lifetime;
        return;
    }


    /**
     * Returns the id for the host if there is a existing one already or creates
     * a new id otherwise.
     * @param host the host to get the id for.
     * @return the id of the host.
     */
    protected Integer getIdForHost( Host host )
    {
        Integer id = hostToIdMap.get( host );
        if ( id != null )
        {
            return id;
        }
        // find free id
        id = Integer.valueOf( nextId++ );
        while ( idToHostMap.get( id ) != null )
        {
            id = Integer.valueOf( nextId++ );
        }
        idToHostMap.put( id, host );
        hostToIdMap.put( host, id );
        return id;
    }
    
    protected Entry createNewEntry()
    {
        return new Entry();
    }
    
    protected static class Entry
    {
    	protected Integer hostId;
    }
}