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
package phex.query;

import phex.utils.QueryGUIDRoutingPair;

public class LeafGuidedSearchProgress extends DefaultSearchProgress
{
    /**
     * The number of results to get if we are starting the dynamic query
     * for a leaf.
     */
    public static final int DESIRED_LEAF_GUIDED_RESULTS = 50;
    
    private final QueryGUIDRoutingPair routingPair;
    
    public LeafGuidedSearchProgress( QueryGUIDRoutingPair routingPair,
        boolean isUrnQuery )
    {
        super( DEFAULT_QUERY_TIMEOUT,
            isUrnQuery ? DESIRED_HASH_RESULTS : DESIRED_LEAF_GUIDED_RESULTS );
        this.routingPair = routingPair;
    }
    
    @Override
    public int getReceivedResultsCount()
    {
        return routingPair.getRoutedResultCount();
    }
    
    @Override
    public void incReceivedResultsCount( int inc )
    {
        throw new UnsupportedOperationException();
    }
}
