/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Phex Development Group
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
 */
package phex.utils;

import phex.common.address.DestAddress;
import phex.prefs.core.SecurityPrefs;

public final class IPUtils
{
    private IPUtils()
    {
    }

    /**
     * Search for the address, ip and port, in the invalid lists
     *
     * @param hostAddress is the address of the host, ip and port to be searched for
     * @return true if the host ip or host port was found in the invalid list
     */
    public static boolean isPortInUserInvalidList( DestAddress hostAddress )
    {
        boolean portFound ;

        //check if the port is in the invalid list
        String port = String.valueOf( hostAddress.getPort() );
        portFound = SecurityPrefs.FilteredPorts.get().contains( port );
        //if the port or the ip was found the host is in the invalid list
        if ( portFound )
        {
            return true;
        }

        return false;
    }
}