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
 *  Created on 12.09.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.prefs.core;

import phex.prefs.api.PreferencesFactory;
import phex.prefs.api.Setting;

public class UpdatePrefs extends PhexCorePrefs
{
    /** 
     * Update-Uris for the various Systems
     */
    public static final String UPDATE_URI_MAC_OSX = "magnet:?xs=http://draketo.de/magma/phex-update/phex_osx.magma&dn=phex_osx.magma";
    public static final String UPDATE_URI_WINDOWS = "magnet:?xs=http://draketo.de/magma/phex-update/phex_win.magma&dn=phex_win.magma";
    public static final String UPDATE_URI_OTHER = "magnet:?xs=http://draketo.de/magma/phex-update/phex_other.magma&dn=phex_other.magma";
    
    /**
     * Contains the version number of the last update check.
     */
    public static final Setting<String> LastUpdateCheckVersion;

    /**
     * Contains the version number of the last beta update check.
     */
    public static final Setting<String> LastBetaUpdateCheckVersion;

    /**
     * Contains the time in millis of the last update check.
     */
    public static final Setting<Long> LastUpdateCheckTime;
    
    /**
     * The id of the last shown update info from the Phex web server.
     */
    public static final Setting<Integer> LastShownUpdateInfoId;
    
    /**
     * This is introduced to maintain the current version of Phex.
     * After a update of Phex the version in the cfg and the Phex version differs.
     * In this case we know that we need to upgrade the cfg or other stuff to the
     * new Phex version
     */
    public static final Setting<String> RunningPhexVersion;
    
    /**
     * This is introduced to maintain the current build number of Phex.
     * After a update of Phex the build number in the cfg and the Phex build number
     * differs. In this case we know that we need to upgrade the cfg and maybe
     * also do some other stuff to reach the new Phex version.
     */
    public static final Setting<String> RunningBuildNumber;
    
    static
    {
        LastUpdateCheckVersion = PreferencesFactory.createStringSetting( 
            "Update.LastUpdateCheckVersion", "0", instance );
        
        LastBetaUpdateCheckVersion = PreferencesFactory.createStringSetting( 
            "Update.LastBetaUpdateCheckVersion", "0", instance );
        
        LastUpdateCheckTime = PreferencesFactory.createLongSetting( 
            "Update.LastUpdateCheckTime", 0, instance );
        
        LastShownUpdateInfoId = PreferencesFactory.createIntSetting( 
            "Update.LastShownUpdateInfoId", 0, instance );
        
        RunningPhexVersion = PreferencesFactory.createStringSetting( 
            "Update.RunningPhexVersion", "", instance );
        RunningPhexVersion.setAlwaysSaved( true );
        
        RunningBuildNumber = PreferencesFactory.createStringSetting( 
            "Update.RunningBuildNumber", "", instance );
        RunningBuildNumber.setAlwaysSaved( true );
    }
}
