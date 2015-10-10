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
package phex.common;

import java.io.File;

import phex.prefs.core.NetworkPrefs;

/**
 * This class represents an abstract Gnutella network representation.
 * The network representation is used to distinguish between different
 * type of gnutella networks. This could be the General Gnutella Network
 * or a named private network the user likes to create or join.  
 * 
 */
public abstract class GnutellaNetwork
{
    public abstract String getName();
    
    public abstract File getHostsFile();
    public abstract File getGWebCacheFile();
    public abstract File getUdpHostCacheFile();
    public abstract File getFavoritesFile();
    public abstract File getSearchFilterFile();
    
    public abstract String getNetworkGreeting();
    
    public static GnutellaNetwork getGnutellaNetworkFromString( String networkName )
    {
        if ( networkName.equals( NetworkPrefs.GENERAL_GNUTELLA_NETWORK ) )
        {// use general gnutella network.
            return new GeneralGnutellaNetwork();
        }
        else
        {// use named gnutella network.
            return new NamedGnutellaNetwork( networkName );
        }
    }
}
