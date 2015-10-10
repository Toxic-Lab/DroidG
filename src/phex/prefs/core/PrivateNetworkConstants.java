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
 *  Created on 04.10.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.prefs.core;



public interface PrivateNetworkConstants
{
    /** 
     * If you like to use a PRIVATE_NETWORK specify here your chosen network
     * name for a private Net by replacing null with "<Network Name>". 
     * If you do, please choose a short ID for your Network, which will 
     * be displaed in the Vendor-String. 
     * You might want to choose a name, which can last, for you can't know, 
     * how your network will exist. 
     * For better appearance, put a space in front of it 
     * and write it in lowercase. (Your privateversion.number will be appended) 
     */
    public final static String PRIVATE_NETWORK = null;
    public final static String PRIVATE_BUILD_ID = ""; 
    
    /**
     * The default value to indicate if this node is forced to be ultrapeer.
     * In a small private network you might want to make everyone an Ultrapeer. 
     */
    public static final boolean DEFAULT_FORCE_TOBE_ULTRAPEER = false;
    
    /**
     * Assign here the default network to use. This can be the 
     * GENERAL_GNUTELLA_NETWORK or the self defined PRIVATE_NETWORK.
     * If you want to change something, do so above. 
     */
    public static String DEFAULT_NETWORK_TO_USE = PRIVATE_NETWORK == null ? 
        NetworkPrefs.GENERAL_GNUTELLA_NETWORK : PRIVATE_NETWORK;    

}
