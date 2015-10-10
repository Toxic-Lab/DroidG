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
package phex.connection;

public interface ConnectionConstants
{
    public static final String PROTOCOL_06 = "0.6";
    public static final String GNUTELLA_06 = "GNUTELLA/0.6";
    public static final String GNUTELLA_06_503 = "GNUTELLA/0.6 503";
    public static final String GNUTELLA_OK_06 = "GNUTELLA/0.6 200 OK";

    public static final String GNUTELLA_CONNECT = "GNUTELLA CONNECT";
    public static final String GNUTELLA_PHEX_CONNECT = "GNUTELLA PCONNECT";

    public static final int STATUS_CODE_OK = 200;
    public static final int STATUS_CODE_REJECTED = 503;

    public static final String STATUS_MESSAGE_OK = "OK";
    public static final String STATUS_MESSAGE_BUSY = "I am busy.";
    public static final String STATUS_MESSAGE_SHIELDED_LEAF =
        "I am a shielded leaf node";
    public static final String STATUS_MESSAGE_ACCEPT_ONLY_UP =
        "I accept only Ultrapeers";
}