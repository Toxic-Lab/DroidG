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
package phex.host;

import phex.bootstrap.BootstrapManager;
import phex.bootstrap.UdpHostCacheContainer;
import phex.servent.Servent;

public class DefaultHostFetchingStrategy implements HostFetchingStrategy
{
    private final BootstrapManager gWebCacheMgr;
    private final UdpHostCacheContainer udpHostCacheContainer;
    
    public DefaultHostFetchingStrategy( Servent servent, UdpHostCacheContainer udpHostCacheContainer )
    {
        if ( udpHostCacheContainer == null )
        {
            throw new IllegalArgumentException( "UHC is null" );
        }
        this.gWebCacheMgr = new BootstrapManager( servent );
        this.udpHostCacheContainer = udpHostCacheContainer;
    }
    
    // temporary workaround method for post manager initialization
    public void postManagerInitRoutine()
    {
        gWebCacheMgr.postManagerInitRoutine();
    }

    public void fetchNewHosts( FetchingReason reason )
    {
//        NLogger.info( DefaultHostFetchingStrategy.class, "Fetch new Hosts: " +
//            reason.toString() );
     
        // Query udpHostCache for new hosts
        udpHostCacheContainer.invokeQueryCachesRequest();
        
        //if ( reason == FetchingReason.EnsureMinHosts )
        {
            // connect GWebCache for new hosts...
            gWebCacheMgr.invokeQueryMoreHostsRequest( true );
        }
    }
}