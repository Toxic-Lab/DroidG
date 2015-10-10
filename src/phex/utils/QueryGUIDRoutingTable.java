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
 *  --- CVS Information ---
 *  $Id$
 */
package phex.utils;

import phex.host.Host;
import phex.msg.GUID;

/**
 * 
 */
public class QueryGUIDRoutingTable extends GUIDRoutingTable
{
    /**
     * @param lifetime the lifetime in millis of a map. After this time is passed
     * the lastMap will be replaced by the currentMap.
     */
    public QueryGUIDRoutingTable( long lifetime )
    {
        super(lifetime);
    }
    
    public synchronized Host findRouting( GUID guid )
    {
        throw new UnsupportedOperationException( "Use findRoutingForQuerys()." );
    }
    
    /**
     * Returns the query routing pair with host for the given GUID or null
     * if no push routing is available or the host is not anymore
     * connected.
     * 
     * @param guid the GUID of the query reply route to find.
     * @param resultCount the number of results routed together with the query reply of
     *        this query GUID.
     * @return the QueryGUIDRoutingPair that contains the host and routed result count to 
     * 		route the reply or null.
     */
    public synchronized QueryGUIDRoutingPair findRoutingForQuerys( GUID guid, int resultCount )
    {
        QueryEntry entry = (QueryEntry)currentMap.get( guid );
        if ( entry == null )
        {
            entry = (QueryEntry)lastMap.get( guid );
        }
        if ( entry != null )
        {
            // returns null if there is no host for the id anymore.
            Host host = idToHostMap.get( entry.hostId );
            if ( host == null )
            {
                return null;
            }
            QueryGUIDRoutingPair returnPair = new QueryGUIDRoutingPair( host, entry.routedResultCount );
            // raise entries routed result count
            entry.routedResultCount += resultCount;
            return returnPair;
        }
        else
        {
            return null;
        }
    }
    
    protected Entry createNewEntry()
    {
        return new QueryEntry();
    }
    
    protected static class QueryEntry extends Entry
    {
        protected int routedResultCount;
    }
}
