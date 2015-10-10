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

import phex.common.QueryRoutingTable;
import phex.host.Host;
import phex.host.NetworkHostsContainer;
import phex.msg.QueryMsg;
import phex.query.LeafGuidedSearchProgress;
import phex.query.SearchProgress;
import phex.servent.Servent;
import phex.utils.QueryGUIDRoutingPair;


class QueryMsgRoutingHandler implements MessageSubscriber<QueryMsg>
{
    private final Servent servent;
    private final MessageRouting msgRouting;
    private final NetworkHostsContainer hostsContainer;
    

    public QueryMsgRoutingHandler( Servent servent, MessageRouting msgRouting )
    {
        this.servent = servent;
        this.msgRouting = msgRouting;
        this.hostsContainer = servent.getHostService().getNetworkHostsContainer();
    }

    /**
     * Routes received QueryMsg through this node.
     * <p>Called to forward a query to connected neighbors. This is only done 
     * under special conditions.<br>
     * When we are in Leaf mode we hold connections to Ultrapeers:<br>
     * - Never forward an incoming query to a ultrapeer.<br>
     * <br>
     * When we are in Ultrapeer mode we hold connections to Ultrapeers and Leafs. 
     * We know the leafs QRT and the Ultrapeers intra-UP QRT therefore:<br>
     * - Never forward a query that does not match a QRT entry.<br>
     * <br>
     * This strategy is used to separate the broadcast traffic of the peer
     * network from the Ultrapeer/Leaf network and is essential for a correct
     * Ultrapeer proposal support.</p>
     *
     * <p>This does not affect the TTL or hops fields of the message.</p>
     * @param sourceHost the Host this message was coming from.
     * @param queryMsg the QueryMsg to forward
     */
    public void onMessage(QueryMsg queryMsg, Host sourceHost)
    {
        // Never forward a message coming from a ultrapeer when in leaf mode!
        if ( servent.isShieldedLeafNode() )
        {
            return;
        }
        if ( sourceHost.isUltrapeerLeafConnection() )
        {// do dynamic query for my leaf.
            QueryGUIDRoutingPair routingPair = msgRouting.getQueryRouting( 
                queryMsg.getHeader().getMsgID(), 0 );
            SearchProgress searchProgress = new LeafGuidedSearchProgress( 
                routingPair, queryMsg.hasQueryURNs() );
            
            servent.getQueryService().sendDynamicQuery( queryMsg, sourceHost,
                searchProgress );
        }
        else
        {
            // only forward to ultrapeers if TTL > 0
            if ( queryMsg.getHeader().getTTL() > 0 )
            {
                forwardQueryToUltrapeers( queryMsg, sourceHost );
            }
            // Forward query to Leafs regardless of TTL
            // see section 2.4 Ultrapeers and Leaves Single Unit of
            // Gnutella Ultrapeer Query Routing v0.1
            forwardQueryToLeaves( queryMsg, sourceHost );
        }
    }
    
    
    
    /**
     * Forward query to Leafs regardless of TTL
     * see section 2.4 Ultrapeers and Leaves Single Unit of
     * Gnutella Ultrapeer Query Routing v0.1
     * @param msg query to forward.
     * @param fromHost the host the query comes from and
     *        query is not forwarded to
     */
    public void forwardQueryToLeaves( QueryMsg msg, Host fromHost )
    {
        Host[] hosts = hostsContainer.getLeafConnections();
        for ( int i = 0; i < hosts.length; i++ )
        {
            if ( hosts[i] == fromHost )
            {
                continue;
            }
            QueryRoutingTable qrt = hosts[i].getLastReceivedRoutingTable();
            if ( qrt != null && !qrt.containsQuery( msg ) )
            {
                continue;
            }
            hosts[i].queueMessageToSend( msg );
        }
    }

    /**
     * Forwards a query to the given hosts but never to the from Host.
     * @param msg the query to forward
     * @param fromHost the host the query came from.
     * @param hosts the hosts to forward to.
     */
    public void forwardQueryToUltrapeers( QueryMsg msg, Host fromHost )
    {
        Host[] ultrapeers = hostsContainer.getUltrapeerConnections();
        boolean lastHop = msg.getHeader().getTTL() == 1;
        for ( int i = 0; i < ultrapeers.length; i++ )
        {
            if ( ultrapeers[i] == fromHost )
            {
                continue;
            }
            // a query on last hop is forwarded to other Ultrapeers
            // with the use of a possibly available QRT.
            if ( lastHop && ultrapeers[i].isUPQueryRoutingSupported() )
            {
                QueryRoutingTable qrt = ultrapeers[i].
                    getLastReceivedRoutingTable();
                if ( qrt != null && !qrt.containsQuery( msg ) )
                {
                    continue;
                }
            }
            ultrapeers[i].queueMessageToSend( msg );
        }
    }
}
