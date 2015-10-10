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
 *  Created on 18.09.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.prefs.core;

import phex.prefs.api.PreferencesFactory;
import phex.prefs.api.Setting;

public class ProxyPrefs extends PhexCorePrefs
{
    public static final int DEFAULT_HTTP_PORT = 80;
    public static final int DEFAULT_SOCKS5_PORT = 1080;
    
    public static final Setting<String> ForcedIp;
    
    public static final Setting<Boolean> UseSocks5;
    public static final Setting<String> Socks5Host;
    public static final Setting<Integer> Socks5Port;
    public static final Setting<Boolean> Socks5Authentication;
    public static final Setting<String> Socks5User;
    public static final Setting<String> Socks5Password;
    
    /**
     * Defines if a http proxy is used for HTTP connections (not Gnutella
     * connections.
     */
    public static final Setting<Boolean> UseHttp;
    
    /**
     * Defines the name of the http proxy host.
     */
    public static final Setting<String> HttpHost;
    
    /**
     * Defines the port of the http proxy host.
     */
    public static final Setting<Integer> HttpPort;
    
    static
    {
        ForcedIp = PreferencesFactory.createStringSetting( 
            "Proxy.ForcedIp", "", instance );
        UseSocks5 = PreferencesFactory.createBoolSetting(
            "Proxy.UseSocks5", false, instance );
        Socks5Host = PreferencesFactory.createStringSetting(
            "Proxy.Socks5Host", "", instance );
        Socks5Port = PreferencesFactory.createIntSetting(
            "Proxy.Socks5Port", DEFAULT_SOCKS5_PORT, instance );
        Socks5Authentication = PreferencesFactory.createBoolSetting(
            "Proxy.Socks5Authentication", false, instance );
        Socks5User = PreferencesFactory.createStringSetting(
            "Proxy.Socks5User", "", instance );
        Socks5Password = PreferencesFactory.createStringSetting(
            "Proxy.Socks5Password", "", instance );
        
        UseHttp = PreferencesFactory.createBoolSetting(
            "Proxy.UseHttp", false, instance );
        HttpHost = PreferencesFactory.createStringSetting(
            "Proxy.HttpHost", "", instance );
        HttpPort = PreferencesFactory.createIntSetting(
            "Proxy.HttpPort", DEFAULT_HTTP_PORT, instance );
    }
}
