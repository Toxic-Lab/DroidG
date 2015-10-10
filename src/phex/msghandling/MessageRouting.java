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
package phex.msghandling;

import phex.host.Host;
import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.MsgHeader;
import phex.msg.PongMsg;
import phex.msg.QueryResponseMsg;
import phex.utils.GUIDRoutingTable;
import phex.utils.QueryGUIDRoutingPair;
import phex.utils.QueryGUIDRoutingTable;

class MessageRouting
{
    private static final int MAX_ROUTED_QUERY_RESULTS = 200;
    
    /**
     * Holds ping GUID routings to Host.
     */
    private final GUIDRoutingTable pingRoutingTable;

    /**
     * Holds query GUID routings to Host.
     */
    private final QueryGUIDRoutingTable queryRoutingTable;

    /**
     * Holds query reply GUID routings to Host.
     */
    private final GUIDRoutingTable pushRoutingTable;
    
    public MessageRouting()
    {
        // holds from 2-4 minutes of ping GUIDs
        pingRoutingTable = new GUIDRoutingTable( 2 * 60 * 1000 );
        // holds from 5-10 minutes of query GUIDs
        queryRoutingTable = new QueryGUIDRoutingTable( 5 * 60 * 1000 );
        // holds from 7-14 minutes of QueryReply GUIDs for push routes.
        pushRoutingTable = new GUIDRoutingTable( 7 * 60 * 1000 );
    }
    
    /**
     * <p>Checks if a route for the GUID is already available. If not associates
     * the Host with the GUID.</p>
     *
     * @param clientID  the GUID to route.
     * @param sender  the Host sending information
     */
    public synchronized boolean checkAndAddToPingRoutingTable( GUID pingGUID,
        Host sender )
    {
        boolean state = pingRoutingTable.checkAndAddRouting( pingGUID, sender );
        return state;
    }

    /**
     * <p>Checks if a route for the GUID is already available. If not associates
     * the Host with the GUID.</p>
     *
     * @param clientID  the GUID to route.
     * @param sender  the Host sending information
     */
    public synchronized boolean checkAndAddToQueryRoutingTable( GUID queryGUID,
        Host sender )
    {
        boolean state = queryRoutingTable.checkAndAddRouting( queryGUID, sender );
        return state;
    }

    /**
     * <p>Associate a Host with the GUID for the servent serving a file.</p>
     *
     * @param clientID  the GUID of a servent publishing a file
     * @param sender  the Host sending information
     */
    public synchronized void addToPushRoutingTable( GUID clientID, Host sender )
    {
        pushRoutingTable.addRouting( clientID, sender );
    }
    
    /**
     * Returns the push routing host for the given GUID or null
     * if no push routing is available or the host is not anymore
     * connected.
     */
    protected synchronized Host getPushRouting( GUID clientID )
    {
        return pushRoutingTable.findRouting( clientID );
    }

    /**
     * Returns the ping routing host for the given GUID or null
     * if no push routing is available or the host is not anymore
     * connected.
     */
    protected synchronized Host getPingRouting( GUID pingGUID )
    {
        return pingRoutingTable.findRouting( pingGUID );
    }
    
    public boolean routePongMessage( PongMsg pongMessage )
    {
        Host host = getPingRouting( pongMessage.getHeader().getMsgID() );
        if ( host == null || host == Host.LOCAL_HOST )
        { 
            // The PongMsg was for me or can't be routed.
            return false;
        }
        // I did forward the PingMsg on behalf of host.
        // Route the PongMsg back to pinging host.
        host.queueMessageToSend( pongMessage );
        return true;
    }

    /**
     * Returns the query routing pair with host for the given GUID or null
     * if no push routing is available or the host is not anymore
     * connected.
     * 
     * @param queryGUID the GUID of the query reply route to find.
     * @param resultCount the number of results routed together with the query reply of
     *        this query GUID.
     * @return the QueryGUIDRoutingPair that contains the host and routed result count to 
     *      route the reply or null.
     */
    protected synchronized QueryGUIDRoutingPair getQueryRouting( GUID queryGUID, int resultCount )
    {
        return queryRoutingTable.findRoutingForQuerys( queryGUID, resultCount );
    }
    
    public boolean routeQueryResponse( QueryResponseMsg queryResponseMsg, Host sourceHost )
        throws InvalidMessageException
    {
        MsgHeader header = queryResponseMsg.getHeader();
        
        // check if I forwarded a QueryMsg with the same message id as this QueryResponseMsg. 
        QueryGUIDRoutingPair routingPair = getQueryRouting( header.getMsgID(),
                queryResponseMsg.getUniqueResultCount() );
        if ( routingPair == null )
        {
            return false;
        }
        Host host = routingPair.getHost();
        if ( host == Host.LOCAL_HOST )
        {
            // The QueryResponseMsg was for me..
            return false;
        }
        if ( routingPair.getRoutedResultCount() >= MAX_ROUTED_QUERY_RESULTS )
        {
            // We have already routed enough results back to the host.
            return false;
        }
        
        // remember push routing
        addToPushRoutingTable( queryResponseMsg.getRemoteServentID(), sourceHost );
        
        host.queueMessageToSend( queryResponseMsg );
        return true;
    }
    
    public synchronized void removeRoutings( Host host )
    {
        pingRoutingTable.removeHost( host );
        queryRoutingTable.removeHost( host );
        pushRoutingTable.removeHost( host );
    }
}