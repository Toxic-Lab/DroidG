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
 * 
 *  Created on 12.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.host;

import phex.utils.Localizer;

public class HostInfo
{
    /**
     * Returns a localized string of the status for the given host.
     */
    public static String getHostStatusString( Host host )
    {
        String lastStatusMsg = host.getLastStatusMsg();
        switch ( host.getStatus() )
        {
        case NOT_CONNECTED:
            return Localizer.getString( "NetHostStatus_NotConnected" );

        case ERROR:
        {
            Object[] args =
            { lastStatusMsg };
            return Localizer.getFormatedString( "NetHostStatus_Error", args );
        }
        case CONNECTING:
        {
            Object[] args = { lastStatusMsg != null ? lastStatusMsg : "" };
            return Localizer.getFormatedString( "NetHostStatus_Connecting",
                args );
        }
        case ACCEPTING:
        {
            Object[] args =
            { lastStatusMsg };
            return Localizer
                .getFormatedString( "NetHostStatus_Accepting", args );
        }
        case CONNECTED:
        {
            Object[] args = 
            { host.isSendQueueInRed() ? Integer.valueOf( 1 ) : Integer.valueOf( 0 ) };
            return Localizer.getFormatedString( "NetHostStatus_Connected", args );
        }
        case DISCONNECTED:
            return Localizer.getString( "NetHostStatus_Disconnected" );
        }

        return Localizer.getString( "NetHostStatus_Unknown" );
    }
    
    public static String getHostStatusString2( Host host )
    {
        String lastStatusMsg = host.getLastStatusMsg();
        switch ( host.getStatus() )
        {
        case NOT_CONNECTED:
            return "Not Connected";

        case ERROR:
        {
            Object[] args =
            { lastStatusMsg };
            return String.format( "Error %s", args );
        }
        case CONNECTING:
        {
            Object[] args = { lastStatusMsg != null ? lastStatusMsg : "" };
            return String.format( "Connecting %s",
                args );
        }
        case ACCEPTING:
        {
            Object[] args =
            { lastStatusMsg };
            return String.format( "Accepting %s", args );
        }
        case CONNECTED:
        {
            Object[] args = 
            { host.isSendQueueInRed() ? Integer.valueOf( 1 ) : Integer.valueOf( 0 ) };
            return String.format( "Connected %d", args );
        }
        case DISCONNECTED:
            return "Disconnected";
        }

        return "Unknown";
    }
    
}