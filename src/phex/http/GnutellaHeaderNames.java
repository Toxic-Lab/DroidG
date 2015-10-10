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
 *  --- CVS Information ---
 *  $Id$
 */
package phex.http;

/**
 *
 */
public class GnutellaHeaderNames
{

    ////// gnutella headers
    public static final String LISTEN_IP = "Listen-IP";
    public static final String X_LISTEN_IP = "X-Listen-IP";

    public static final String VENDOR_MESSAGE = "Vendor-Message";

    public static final String CRAWLER = "Crawler";

    /**
     * Header informing the crawler about the connected leaves.
     */
    public static final String LEAVES = "Leaves";

    /**
     * Header informing the crawler about the connected ultrapeers.
     */
    public static final String PEERS = "Peers";

    public static final String X_MY_ADDRESS = "X-My-Address";

    public static final String REMOTE_IP = "Remote-IP";
    
    public static final String PONG_CACHING = "Pong-Caching";

    public static final String ALT_LOC = "Alt-Location";

    public static final String X_ALT_LOC = "X-Gnutella-Alternate-Location";

    public static final String X_ALT = "X-Alt";

    public static final String X_NALT = "X-NAlt";

    public static final String X_AVAILABLE_RANGES = "X-Available-Ranges";
    
    /**
     * Gtk-Gnutella header indicating that X-Available-Ranges is not complete.
     * See PFSP spec. section 5. 
     */
    public static final String X_AVAILABLE = "X-Available";

    public static final String X_GNUTELLA_CONTENT_URN = "X-Gnutella-Content-URN";

    public static final String X_CONTENT_URN = "X-Content-URN";

    public static final String X_QUEUE = "X-Queue";

    public static final String CHAT = "Chat";

    public static final String X_ULTRAPEER = "X-Ultrapeer";

    public static final String X_ULTRAPEER_NEEDED = "X-Ultrapeer-Needed";

    public static final String X_QUERY_ROUTING = "X-Query-Routing";

    public static final String X_UP_QUERY_ROUTING = "X-Ultrapeer-Query-Routing";

    public static final String GGEP = "GGEP";

    public static final String X_TRY = "X-Try";

    public static final String X_TRY_ULTRAPEERS = "X-Try-Ultrapeers";

    public static final String X_DYNAMIC_QUERY = "X-Dynamic-Querying";

    public static final String X_DEGREE = "X-Degree";

    public static final String X_MAX_TTL = "X-Max-TTL";
    
    public static final String X_THEX_URI = "X-Thex-URI";
    
    public static final String X_NODE = "X-Node";
    
    public static final String X_PUSH_PROXY = "X-Push-Proxy";
    
    public static final String X_REQUERIES = "X-Requeries";
}
