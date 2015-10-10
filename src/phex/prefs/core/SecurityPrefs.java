/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
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
 *  Created on 29.10.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.prefs.core;

import java.util.List;

import phex.prefs.api.PreferencesFactory;
import phex.prefs.api.Setting;

public class SecurityPrefs extends PhexCorePrefs
{
    /**
     * Indicates if the internal hostile host list should be loaded.
     */
    public static final Setting<Boolean> LoadHostileHostList;
    
    /**
     * Indicates if the internal hostile sha1 urn list should be loaded.
     */
    public static final Setting<Boolean> LoadHostileSha1List;
    
    /**
     * Indicates if nodes with no vendor code are disconnected.
     */
    public static final Setting<Boolean> DisconnectNoVendorHosts;
    
    /**
     * When the HostCatcher finds hosts it will first use the port filter
     * to see if it is allowed to use the host
     */
    public static final Setting<List<String>> FilteredPorts;
    
    static
    {
        LoadHostileHostList = PreferencesFactory.createBoolSetting(  
            "Security.LoadHostileHostList", true, instance );
        
        LoadHostileSha1List = PreferencesFactory.createBoolSetting(  
            "Security.LoadHostileSha1List", true, instance );
        
        DisconnectNoVendorHosts = PreferencesFactory.createBoolSetting(  
            "Security.DisconnectNoVendorHosts", true, instance );
        
        FilteredPorts = PreferencesFactory.createListSetting(
            "Security.FilteredPorts", instance );
    }
}
