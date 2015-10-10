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
public class HTTPHeaderNames
{
    ////// request headers
    public static final String ACCEPT = "Accept";

    public static final String ACCEPT_ENCODING ="Accept-Encoding";

    public static final String HOST = "Host";

    public static final String RANGE= "Range";

    public static final String USER_AGENT = "User-Agent";

    ////// response header
    public static final String SERVER = "Server";

    public static final String CONTENT_ENCODING = "Content-Encoding";

    ////// general header
    public static final String CONNECTION = "Connection";

    public static final String CONTENT_LENGTH = "Content-Length";

    public static final String CONTENT_RANGE = "Content-Range";

    public static final String CONTENT_TYPE = "Content-Type";

    public static final String RETRY_AFTER = "Retry-After";
    
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
}