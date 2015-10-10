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
package phex.security;

import java.lang.ref.SoftReference;

import phex.common.ExpiryDate;
import phex.common.address.AddressUtils;
import phex.xml.sax.security.DIpAccessRule;
import phex.xml.sax.security.DSecurityRule;

public class IpSystemSecurityRule implements IpSecurityRule
{
    private final HittingIpCidrPair ipCidrPair;
    
    private SoftReference<String> addressString;
    
    public IpSystemSecurityRule( int ip, byte cidr )
    {
        ipCidrPair = new HittingIpCidrPair( ip, cidr );
    }
    
    /**
     * Returns a string representation of the address
     * for displaying purposes on a GUI.
     * @return a string representation of the address.
     */
    public String getAddressString()
    {
        if ( addressString == null || addressString.get() == null)
        {
            if ( ipCidrPair.cidr == 32 )
            {
                addressString = new SoftReference<String>( AddressUtils.ip2string( ipCidrPair.ipAddr ) );
            }
            else
            {
                addressString = new SoftReference<String>( AddressUtils.ip2string( ipCidrPair.ipAddr ) + " / "
                    + String.valueOf( ipCidrPair.cidr ) );
            }
        }
        return addressString.get();
    }
    
    public HittingIpCidrPair getIpCidrPair()
    {
        return ipCidrPair;
    }
    
    public int getIp()
    {
        return ipCidrPair.ipAddr;
    }
    
    /**
     * Returns the number of times the rule was triggered. All checks that
     * match the rule will increment the trigger.
     * @return the number of times the rule was triggered.
     */
    public int getTriggerCount()
    {
        return ipCidrPair.getHits();
    }
    
    /**
     * Sets the number of times the rule was triggered. All checks that
     * match the rule will increment the trigger.
     */
    public void setTriggerCount( int count )
    {
        ipCidrPair.setHits( count );
    }
    
    public boolean isDenyingRule()
    {
        return true;
    }

    public boolean isDisabled()
    {
        return false;
    }

    public boolean isSystemRule()
    {
        return true;
    }
    
    public boolean isDeletedOnExpiry()
    {
        return false;
    }

    /**
     * Returns the expiry date that indicates when this rule expires. It can be a
     * at a time, at the end of the session or never.
     * @return the expiry date that indicates when this rule expires.
     */
    public ExpiryDate getExpiryDate()
    {
        return ExpiryDate.NEVER_EXPIRY_DATE;
    }

    /**
     * Returns the description of the rule.
     * @return the description of the rule.
     */
    public String getDescription()
    {
        return "System rule.";
    }
    
    public DSecurityRule createDSecurityRule()
    {
        DIpAccessRule dRule = new DIpAccessRule();
        dRule.setSystemRule( true );
        dRule.setIp( AddressUtils.intIp2ByteIp( ipCidrPair.ipAddr ) );
        dRule.setCidr( ipCidrPair.cidr );
        dRule.setTriggerCount( ipCidrPair.getHits() );
        return dRule;
    }
}