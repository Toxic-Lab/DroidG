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
 *  Created on 01.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.common.address;


public interface DestAddressFactory
{
    /**
     * Creates a DestAddress object from a given address representation.
     * The implementation can decide if it takes the default port into account.
     * 
     * @param address a address representation.
     * @param defaultPort the default port to use for the resulting DestAddress.
     *        If a parsed port is required from the address, a defaultPort can
     *        be used that causes DestAddress.isValidAddress() to fail (-1).
     * @return a destination address.
     */
    public DestAddress createHostAddress( String address, int defaultPort )
        throws MalformedDestAddressException;
    
    /**
     * Creates a DestAddress object from a given IpAddress and port.
     * @param ipAddress the IpAddress of the new DestAddress.
     * @param port the port of the new DestAddress.
     * @return a destination address.
     */
    public DestAddress createHostAddress( IpAddress ipAddress, int port );
}