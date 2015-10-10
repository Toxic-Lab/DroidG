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
import phex.prefs.api.RangeSetting;
import phex.prefs.api.Setting;

public class ConnectionPrefs extends PhexCorePrefs
{
    public static final Setting<Boolean> AutoConnectOnStartup;
    
    /**
     * Indicates if this node is allowed to become a Ultrapeer.
     */
    public static final Setting<Boolean> AllowToBecomeUP;

    /**
     * Indicates if this node force to be a Ultrapeer.
     * This value must always be checked together with allowToBecomeUP. If
     * allowToBecomeUP is false, a forceToBeUltrapeer value of true must be
     * ignored.
     */
    public static final Setting<Boolean> ForceToBeUltrapeer;

    /**
     * The number of ultrapeer to ultrapeer connections the nodes is allowed to
     * have open.
     * This value is used for the X-Degree header. To reach high out degree
     * for dynamic query the value must be above 15.
     */
    public static final Setting<Integer> Up2UpConnections;

    /**
     * The number of ultrapeer to leaf connections the nodes is allowed to
     * have open.
     */
    public static final Setting<Integer> Up2LeafConnections;

    /**
     * The number of leaf to ultrapeer connections the nodes is allowed to
     * have open. The max should be 5.
     */
    public static final RangeSetting<Integer> Leaf2UpConnections;
    
    /**
     * Indicates if the peers has connected incoming the last time it was
     * shutdown. The value is only updated in case of a server shutdown, but  
     * the Server maintains and holds the state changes during runtime.
     */
    public static final Setting<Boolean> HasConnectedIncomming;
    
    /**
     * The number of consecutive failed connection after which the servent 
     * is called as offline.
     */
    public static final Setting<Integer> OfflineConnectionFailureCount;
    
    /**
     * Enables QueryHit Snooping.
     */
    public static final Setting<Boolean> EnableQueryHitSnooping;
    
    /**
     * Indicates if we accept deflated connections.
     */
    public static final Setting<Boolean> AcceptDeflateConnection;    
    
    static
    {
        AutoConnectOnStartup = PreferencesFactory.createBoolSetting(
            "Connection.AutoConnectOnStartup", true, instance );
        AllowToBecomeUP = PreferencesFactory.createBoolSetting(
            "Connection.AllowToBecomeUP", true, instance );
        ForceToBeUltrapeer = PreferencesFactory.createBoolSetting(
            "Connection.ForceToBeUltrapeer", 
            PrivateNetworkConstants.DEFAULT_FORCE_TOBE_ULTRAPEER, instance );
        Up2UpConnections = PreferencesFactory.createIntRangeSetting(
            "Connection.Up2UpConnections", 32, 16, 999, instance );
        Up2LeafConnections = PreferencesFactory.createIntRangeSetting(
            "Connection.Up2LeafConnections", 30, 0, 999, instance );
        Leaf2UpConnections = PreferencesFactory.createIntRangeSetting(
            "Connection.Leaf2UpConnections", 5, 1, 5, instance );
        HasConnectedIncomming = PreferencesFactory.createBoolSetting( 
            "Connection.HasConnectedIncomming", false, instance );
        OfflineConnectionFailureCount = PreferencesFactory.createIntSetting(
            "Connection.OfflineConnectionFailureCount", 61, instance );
        EnableQueryHitSnooping = PreferencesFactory.createBoolSetting(
            "Connection.EnableQueryHitSnooping", true, instance );
        AcceptDeflateConnection = PreferencesFactory.createBoolSetting(
            "Connection.AcceptDeflateConnection", true, instance );
    }
}
