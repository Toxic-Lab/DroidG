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
 *  Created on 22.09.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.prefs.core;

import java.util.*;

import phex.prefs.api.PreferencesFactory;
import phex.prefs.api.Setting;

// TODO changes to List or Set Settings do not cause a set call and a "saveRequiredNotify"
public class LibraryPrefs extends PhexCorePrefs
{
    /**
     * @since 2.1.5.80
     */
    public static final Setting<Set<String>> SharedDirectoriesSet;
    
    /**
     * @since 2.1.9.83
     */
    public static final Setting<List<String>> LibraryExclusionRegExList;
    
    /**
     * Determines the urn calculation speed mode. Values should range
     * from 0 for high speed (full CPU) calculation up to maybe 10.
     * A good value is 2 which is also the default. The value states
     * the wait cycles between each 64K segment. A value of 2 means
     * wait twice as long as you needed to calculate the last 64K. 
     */
    public static final Setting<Integer> UrnCalculationMode;
    
    /**
     * Determines the thex calculation speed mode. Values should range
     * from 0 for high speed (full CPU) calculation up to maybe 10.
     * A good value is 2 which is also the default. The value states
     * the wait cycles between each 128K segment. A value of 2 means
     * wait twice as long as you needed to calculate the last 128K. 
     */
    public static final Setting<Integer> ThexCalculationMode;
    
    public static final Setting<Boolean> AllowBrowsing;
    
    /**
     * The max of this value should be 255. The protocol is not able to handle
     * more.
     */
    public static final Setting<Integer> MaxResultsPerQuery;
    
    static
    {
        SharedDirectoriesSet = PreferencesFactory.createSetSetting(
            "Library.SharedDirectoriesSet", instance );
        LibraryExclusionRegExList = PreferencesFactory.createListSetting(
            "Library.LibraryExclusionRegExList", instance );
        UrnCalculationMode = PreferencesFactory.createIntSetting(
            "Library.UrnCalculationMode", 2, instance );
        ThexCalculationMode = PreferencesFactory.createIntSetting(
            "Library.ThexCalculationMode", 2, instance );
        AllowBrowsing = PreferencesFactory.createBoolSetting(
            "Library.AllowBrowsing", true, instance );
        MaxResultsPerQuery = PreferencesFactory.createIntSetting(
            "Library.MaxResultsPerQuery", 64, instance );
    }
}
