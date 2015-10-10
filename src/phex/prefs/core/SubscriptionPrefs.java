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
 *  Created on 17.08.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.prefs.core;

import java.util.List;

import phex.prefs.api.PreferencesFactory;
import phex.prefs.api.Setting;

public class SubscriptionPrefs extends PhexCorePrefs
{
    public static final String default_subscriptionMagnets = null; /* new String("magnet:?xs=http://draketo.de/magma/filk-filme.magma&dn=filk-filme.magma"); */
    
    /**
     * Indicates if subscriptions should be downloaded silently.
     */
    public static final Setting<Boolean> DownloadSilently;
    
    /** 
     * Subscription-Uris
     * These should point to magma-files.  
     */
    public static final Setting<List<String>> SubscriptionMagnets;
    
    static
    {
        DownloadSilently = PreferencesFactory.createBoolSetting(
            "Subscription.DownloadSilently", false, instance );
        SubscriptionMagnets = PreferencesFactory.createListSetting(
            "Subscription.SubscriptionMagnets", instance );
    }
}
