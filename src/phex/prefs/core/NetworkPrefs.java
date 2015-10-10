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

import org.apache.commons.lang.SystemUtils;

import phex.prefs.api.PreferencesFactory;
import phex.prefs.api.Setting;

public class NetworkPrefs extends PhexCorePrefs
{
    /**
     * The default number of maximum concurrent connection attempts allowed on
     * a XP system. (XP limits this to 10, leave 2 for other process)
     */
    private static final int DEFAULT_MAX_CONCURRENT_CONNECT_ATTEMPTS_XP = 8;
    
    /**
     * The default number of maximum concurrent connection attempts allowed on
     * a other systems then XP. (XP limits this to 10, leave 2 for other process)
     */
    private static final int DEFAULT_MAX_CONCURRENT_CONNECT_ATTEMPTS_OTHERS = 15;
    
    /**
     * Settings which have to be defined, before you can define your own ones...
     * DON'T CHANGE THESE!
     * ... 
     * ...
     * DON'T CHANGE THESE AGAIN! :)
     */
    public final static String GENERAL_GNUTELLA_NETWORK = "<General Gnutella Network>";
    
    /**
     * The GUID of the servent.
     */
    public static final Setting<String> ServentGuid;
    
    /**
     * The listening port the server binds to.
     */
    public static final Setting<Integer> ListeningPort;
    
    /**
     * The name of the used gnutella network.
     */
    public static final Setting<String> CurrentNetwork;
    
    /**
     * The history of networks.
     */
    public static final Setting<List<String>> NetworkHistory;
    
    /**
     * Indicates if the node is connected to a Local Area Network (LAN). This
     * is used to indicate if there is a chance to access a local IP when 
     * firewalled.
     */
    public static final Setting<Boolean> ConnectedToLAN;
    
    /**
     * The create socket default connect timeout.
     */
    public static final Setting<Integer> TcpConnectTimeout;
    
    /**
     * The sockets default read/write timeout.
     */
    public static final Setting<Integer> TcpRWTimeout;
    
    /**
     * The number of maximum concurrent connection attempts allowed.
     * (XP limits this to 10)
     */
    public static final Setting<Integer> MaxConcurrentConnectAttempts;
    
    /**
     * The max number of host that should be hold in HostCache.
     */
    public static final Setting<Integer> MaxHostInHostCache;
    
    /**
     * Automatically removes bad hosts from the connection container.
     */
    public static final Setting<Boolean> AutoRemoveBadHosts;
    
    /**
     * Indicates if the chat feature is enabled.
     */
    public static final Setting<Boolean> AllowChatConnection;
    
    static
    {
        ServentGuid = PreferencesFactory.createStringSetting( 
            "Network.ServentGuid", "", instance );
        
        ListeningPort = PreferencesFactory.createListeningPortSetting( 
            "Network.ListeningPort", instance );
        
        CurrentNetwork = PreferencesFactory.createStringSetting( 
            "Network.CurrentNetwork", 
            PrivateNetworkConstants.DEFAULT_NETWORK_TO_USE, instance );
        
        NetworkHistory = PreferencesFactory.createListSetting(
            "Network.NetworkHistory", instance );
        
        ConnectedToLAN = PreferencesFactory.createBoolSetting(
            "Network.ConnectedToLAN", true, instance );
        
        TcpConnectTimeout = PreferencesFactory.createIntSetting( 
            "Network.TcpConnectTimeout", 30 * 1000, instance );
        
        TcpRWTimeout = PreferencesFactory.createIntSetting( 
            "Network.TcpRWTimeout", 60 * 1000, instance );
        
        int maxConcConnectAtt;
//        if ( SystemUtils.IS_OS_WINDOWS_XP )
//        {
//            maxConcConnectAtt = DEFAULT_MAX_CONCURRENT_CONNECT_ATTEMPTS_XP;
//        }
//        else
//        {
            maxConcConnectAtt = DEFAULT_MAX_CONCURRENT_CONNECT_ATTEMPTS_OTHERS;
//        }
        MaxConcurrentConnectAttempts = PreferencesFactory.createIntSetting( 
            "Network.MaxConcurrentConnectAttempts", maxConcConnectAtt, instance );
        
        MaxHostInHostCache = PreferencesFactory.createIntSetting(
            "Network.MaxHostInHostCache", 750, instance );
        
        AutoRemoveBadHosts = PreferencesFactory.createBoolSetting(
            "Network.AutoRemoveBadHosts", true, instance );
        
        AllowChatConnection = PreferencesFactory.createBoolSetting(
            "Network.AllowChatConnection", true, instance );
    }
}
