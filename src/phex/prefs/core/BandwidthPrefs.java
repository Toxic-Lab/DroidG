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

public class BandwidthPrefs extends PhexCorePrefs
{
    public static final int UNLIMITED_BANDWIDTH = Integer.MAX_VALUE;
    
    /**
     * The total speed in kilo bits per second of the network connection the
     * user has available. This is not the bandwidth the user has available for
     * Phex.
     * The default of 6144 matches a DSL/Cable connection.
     */
    public static final Setting<Integer> NetworkSpeedKbps;
    
    /**
     * This is the maximal bandwidth in bytes per second Phex is allowed to use
     * in total. This means network, download and upload bandwidth combined.
     * The default is UNLIMITED_BANDWIDTH for full bandwidth usage.
     */
    public static final Setting<Integer> MaxTotalBandwidth;
    
    /**
     * This is the maximal bandwidth in bytes per second Phex is allowed to use
     * for Gnutella network connections.
     * The default is UNLIMITED_BANDWIDTH for full bandwidth usage.
     */
    public static final Setting<Integer> MaxNetworkBandwidth;
    
    /**
     * This is the maximal bandwidth in bytes per second Phex is allowed to use
     * for download connections.
     * The default is UNLIMITED_BANDWIDTH for full bandwidth usage.
     */
    public static final Setting<Integer> MaxDownloadBandwidth;
    
    /**
     * This is the maximal bandwidth in bytes per second Phex is allowed to use
     * for upload connections.
     * The default is UNLIMITED_BANDWIDTH for full bandwidth usage.
     */
    public static final Setting<Integer> MaxUploadBandwidth;
    
    
    static
    {
        NetworkSpeedKbps = PreferencesFactory.createIntSetting( 
            "Bandwidth.NetworkSpeedKbps", 6144, instance );
        
        MaxTotalBandwidth = PreferencesFactory.createIntSetting( 
            "Bandwidth.MaxTotalBandwidth", UNLIMITED_BANDWIDTH, instance );
        
        MaxNetworkBandwidth = PreferencesFactory.createIntSetting( 
            "Bandwidth.MaxNetworkBandwidth", UNLIMITED_BANDWIDTH, instance );
        
        MaxDownloadBandwidth = PreferencesFactory.createIntSetting( 
            "Bandwidth.MaxDownloadBandwidth", UNLIMITED_BANDWIDTH, instance );
        
        MaxUploadBandwidth = PreferencesFactory.createIntSetting( 
            "Bandwidth.MaxUploadBandwidth", UNLIMITED_BANDWIDTH, instance );
    }
}
