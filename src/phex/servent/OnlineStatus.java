/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2007 Phex Development Group
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
 *  --- SVN Information ---
 *  $Id$
 */
package phex.servent;

public enum OnlineStatus
{
    /**
     * Indicates we are offline, no network and no transfers.
     */
    OFFLINE(false, false),

    /**
     * Indicates we only allow gnutella network connections. Up-/Download
     * transfers are not allowed.
     */
    ONLY_NETWORK(true, false),

    /**
     * Indicates we only allow Up-/Download transfers. Network connections are
     * not allowed.
     */
    ONLY_TRANSFERS(false, true),

    /**
     * Indicates we are fully online, network and transfers.
     */
    ONLINE(true, true);

    private final boolean isNetworkOnline;

    private final boolean isTransfersOnline;

    OnlineStatus(boolean isNetworkConnected, boolean isTransferConnected)
    {
        this.isNetworkOnline = isNetworkConnected;
        this.isTransfersOnline = isTransferConnected;
    }

    public boolean isNetworkOnline()
    {
        return isNetworkOnline;
    }

    public boolean isTransfersOnline()
    {
        return isTransfersOnline;
    }
}