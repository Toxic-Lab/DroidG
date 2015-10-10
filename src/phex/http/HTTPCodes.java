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
 *  Created on 22.07.2005
 *  --- SVN Information ---
 *  $Id$
 */
package phex.http;

public interface HTTPCodes
{
    public static final int HTTP_401_UNAUTHORIZED = 401;
    public static final int HTTP_404_Not_Found = 404;
    public static final int HTTP_403_FORBIDDEN = 403;
    public static final int HTTP_416_Requested_Range_Not_Available = 416; 
    
    public static final int HTTP_500_Internal_Server_Error = 500; 
    public static final int HTTP_503_Service_Unavailable = 503;
    
}
