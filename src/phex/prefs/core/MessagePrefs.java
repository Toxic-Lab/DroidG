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

import phex.prefs.api.PreferencesFactory;
import phex.prefs.api.Setting;

public class MessagePrefs extends PhexCorePrefs
{
    /**
     * The default value of the X-Max-TTL header for dynamic queries. 
     */
    public static final int DEFAULT_DYNAMIC_QUERY_MAX_TTL = 4;
    
    /**
     * The number of consecutive failed connection after which the servent 
     * is called as offline.
     */
    public static final Setting<Integer> MaxLength;
    
    /**
     * The TTL Phex uses for messages.
     */
    public static final Setting<Integer> TTL;

    /**
     * The maximum number of hops allowed to be seen in messages otherwise a
     * message is dropped. Also the highest ttl allowed to be seen in messages
     * otherwise the ttl is limited to Cfg.maxNetworkTTL - hops.
     */
    public static final Setting<Integer> MaxNetworkTTL;
    
    /**
     * Indicates if outdated Clip2 index queries should be dropped.
     * @since 3.0.2.103
     */
    public static final Setting<Boolean> DropIndexQueries;
    
    /**
     *
     */
    public static final Setting<Boolean> UseExtendedOriginIpAddress;

    static
    {
        MaxLength = PreferencesFactory.createIntSetting( 
            "Message.MaxLength", 65536, instance );
        TTL = PreferencesFactory.createIntSetting( 
            "Message.TTL", 7, instance );
        MaxNetworkTTL = PreferencesFactory.createIntSetting( 
            "Message.MaxNetworkTTL", 7, instance );
        DropIndexQueries = PreferencesFactory.createBoolSetting( 
            "Message.DropIndexQueries", true, instance );
        UseExtendedOriginIpAddress = PreferencesFactory.createBoolSetting(
            "Message.UseExtendedOriginIpAddress", false, instance );        
    }
}
