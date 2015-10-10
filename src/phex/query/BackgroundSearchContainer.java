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


import phex.common.URN;
import phex.common.address.DestAddress;
import phex.event.PhexEventTopics;
import phex.msg.GUID;
import phex.msg.QueryFactory;
import phex.servent.Servent;

public class BackgroundSearchContainer extends SearchContainer
{
    public BackgroundSearchContainer( QueryFactory queryFactory, 
        Servent servent )
    {
        super( queryFactory, servent );
    }

    /**
     * Method is unsupported.
     */
    @Override
    public synchronized Search createSearch( String queryStr )
    {
        throw new UnsupportedOperationException( );
    }
    
    /**
     * Method is unsupported.
     */
    @Override
    public synchronized Search createWhatsNewSearch( )
    {
        throw new UnsupportedOperationException( );
    }
    
    /**
     * Method is unsupported.
     */
    @Override
    public synchronized BrowseHostResults createBrowseHostSearch(
        DestAddress hostAddress, GUID hostGUID )
    {
        BrowseHostResults search = new BrowseHostResults( servent, hostAddress, hostGUID );
        int idx = searchList.size();
        insertToSearchList( search, idx );
        return search;
    }
    

    public synchronized Search createSearch( String queryStr, URN queryURN )
    {
        KeywordSearch search = new KeywordSearch( queryStr, queryURN, 
            queryFactory, servent );
        int idx = searchList.size();
        insertToSearchList( search, idx );
        return search;
    }
    
    public void onSearchDataEvent( String topic, SearchDataEvent event )
    {
    	if(topic.compareTo(PhexEventTopics.Search_Data)!=0) return;
        if ( event.getType() == SearchDataEvent.SEARCH_STOPED )
        {
            // the source not be a background search.. we try to remove
            // it anyway...
            Search source = (Search) event.getSource();
            removeSearch( source );
        }
    }
}