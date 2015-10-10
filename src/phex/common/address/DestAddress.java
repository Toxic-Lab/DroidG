/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2008 Phex Development Group
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
package phex.common.address;

public interface DestAddress
{
    /**
     * Returns a host name representation. No port should be appended and it should
     * never return null.
     * @return a host name.
     */
    public String getHostName();
    
    /**
     * Returns the full host name representation. Including possible port. It
     * should never return null.
     * @return the full host name.
     */
    public String getFullHostName();

    /**
     * Returns whether the host name returned by getHostName() is the string
     * representation of a IP or not.
     * 
     * @return Returns true if the host name is the string representation of 
     * the IP, false otherwise.
     */
    public boolean isIpHostName();

    /**
     * Returns the port of this destination address.
     * @return the port
     */
    public int getPort();

    /**
     * Returns the IP address of this destination address. In case the IP address
     * can't be resolved (which should be tried!) or there is no IP for this kind
     * of destination address null can be returned.
     * @return the IpAddress or null.
     */
    public IpAddress getIpAddress();

    public boolean equals( DestAddress address );
    public boolean equals( byte[] ipAddress, int port );

    public int hashCode();

    /**
     * Returns the country code of this destination address. In case the 
     * IP has not been resolved yet null can be returned. In case no country
     * code could be found "" can be returned.
     * @return the country code, empty string or null.
     */
    public String getCountryCode();

    /**
     * Checks if the DestAddress is a loopback address or the external address
     * of this localhost.
     *
     * @param localAddress the local address to compare against.
     * @return a <code>boolean</code> indicating if the DestAddress represents 
     *         localhost.
     */
    public boolean isLocalHost( DestAddress localAddress );
    
    /**
     * Checks if the DestAddress is a site local address. Meaning a address in 
     * the private LAN. 
     *
     * @return a <code>boolean</code> indicating if the DestAddress is 
     * a site local address; or false if address is not a site local address.
     */
    public boolean isSiteLocalAddress();
    
    /**
     * Checks if the DestAddress is completely valid. Each DestAddress must
     * by itself what is required to make up a valid address.
     * @return a <code>boolean</code> indicating if the DestAddress is 
     * valid; or false otherwise.
     */
    public boolean isValidAddress();
}
