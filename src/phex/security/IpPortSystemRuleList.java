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
  */
package phex.security;

import phex.common.address.AddressUtils;

import java.util.*;

public class IpPortSystemRuleList extends IpSystemRuleList
{
    private HashMap<IpPortAddress, IpSystemSecurityRule> IpPortMap;

    public IpPortSystemRuleList()
    {
        super();
        this.IpPortMap = new HashMap<IpPortAddress, IpSystemSecurityRule>(); 
    }

    public void add(IpPortAddress key, IpSystemSecurityRule value)
    {
        if (!this.IpPortMap.containsKey(key))
            this.IpPortMap.put(key, value);
        this.add(value);
    }

    public void removeAll()
    {
        this.IpPortMap.clear();
    }
    
    public boolean containsRuleAndPort(IpCidrPair key1, IpPortAddress key2)
    {       
        return (this.contains(key1) && this.IpPortMap.containsKey(key2));
    }
}