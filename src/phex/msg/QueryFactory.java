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
package phex.msg;

import phex.common.URN;
import phex.common.address.DestAddress;
import phex.prefs.core.MessagePrefs;
import phex.servent.Servent;

public class QueryFactory
{
    private static final String WHAT_IS_NEW_QUERY_STRING = "WhatIsNewXOXO";
    
    /**
     * For outgoing queries, specifies whether we want to receive
     * Limewire-style XML metadata results.  For more info, see MsgQuery.java
     */
    public static final boolean IS_PHEX_CAPABLE_OF_XML_RESULTS = false;
    
    private final Servent servent;
    
    public QueryFactory( Servent servent )
    {
        this.servent = servent;
    }
    
    public QueryMsg createOOBKeywordQuery( String searchString, URN searchURN )
    {
        GUID guid = new GUID();
        DestAddress localAddress = servent.getLocalAddress();
        GUID.applyOOBQueryMarkings( guid, localAddress.getIpAddress(), localAddress.getPort() );
        
        return new QueryMsg(
            guid,
            MessagePrefs.TTL.get().byteValue(),
            searchString, searchURN, 
            IS_PHEX_CAPABLE_OF_XML_RESULTS,
            servent.isFirewalled(),
            true,
            QueryMsg.NO_FEATURE_QUERY_SELECTOR );
    }
    
    /**
     * Creates outgoing keyword query.
     */
    public QueryMsg createKeywordQuery( String searchString, URN searchURN )
    {
        return new QueryMsg(
            new GUID(),
            MessagePrefs.TTL.get().byteValue(),
            searchString, searchURN, 
            IS_PHEX_CAPABLE_OF_XML_RESULTS,
            servent.isFirewalled(),
            false,
            QueryMsg.NO_FEATURE_QUERY_SELECTOR );
    }
    
    /**
     * Creates a outgoing WhatIsNew query.
     */
    public QueryMsg createWhatsNewQuery( )
    {
        return new QueryMsg(
            new GUID(),
            MessagePrefs.TTL.get().byteValue(), 
            WHAT_IS_NEW_QUERY_STRING, null,
            IS_PHEX_CAPABLE_OF_XML_RESULTS,
            servent.isFirewalled(),
            false,
            QueryMsg.WHAT_IS_NEW_FEATURE_QUERY_SELECTOR );
    }
}