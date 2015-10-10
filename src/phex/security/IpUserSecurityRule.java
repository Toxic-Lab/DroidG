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
 *  --- CVS Information ---
 *  $Id$
 */
package phex.security;

import java.lang.ref.SoftReference;

import phex.common.ExpiryDate;
import phex.common.address.AddressUtils;
import phex.xml.sax.security.DIpAccessRule;
import phex.xml.sax.security.DSecurityRule;

// http://www.telusplanet.net/public/sparkman/netcalc.htm
public class IpUserSecurityRule extends AbstractSecurityRule implements IpSecurityRule
{
    private final HittingIpCidrPair ipCidrPair;
    
    private SoftReference<String> addressString;

    /**
     * Creates a IPAccess rule.
     * @param description the description of the new rule.
     * @param ip the ip in byte[] format.
     * @param cidr the cidr to define the ip range.
     */
    public IpUserSecurityRule( String description, byte[] ip, byte cidr, boolean isDisabled, 
        boolean isDeletedOnExpiry, long expiryTime )
    {

        this( description, AddressUtils.byteIpToIntIp( ip ), cidr, isDisabled, 
            isDeletedOnExpiry, expiryTime );
    }
    
    /**
     * Creates a IPAccess rule.
     * @param description the description of the new rule.
     * @param ip the ip in byte[] format.
     * @param cidr the cidr to define the ip range.
     */
    public IpUserSecurityRule( String description, byte[] ip, byte cidr, boolean isDisabled, 
        boolean isDeletedOnExpiry, ExpiryDate expiryDate )
    {

        this( description, AddressUtils.byteIpToIntIp( ip ), cidr, isDisabled, 
            isDeletedOnExpiry, expiryDate );
    }

    /**
     * Creates a IPAccess rule.
     * 
     * @param description the description of the new rule.
     * @param ip the ip as an int.
     * @param cidr the cidr to define the ip range.
     * @param isDisabled defines if the rule is disabled.
     */
    public IpUserSecurityRule( String description,  int ip, byte cidr, boolean isDisabled,
        boolean isDeletedOnExpiry, long expiryTime )
    {
        this( description, ip, cidr, isDisabled, isDeletedOnExpiry, 
            ExpiryDate.getExpiryDate( expiryTime ) );
    }
    
    /**
     * Creates a IPAccess rule.
     * 
     * @param description the description of the new rule.
     * @param ip the ip as an int.
     * @param cidr the cidr to define the ip range.
     * @param isDisabled defines if the rule is disabled.
     */
    public IpUserSecurityRule( String description,  int ip, byte cidr, boolean isDisabled,
        boolean isDeletedOnExpiry, ExpiryDate expiryDate )
    {
        // to easier handle ip security rules for now all rules are deny rules.
        super( description, true, false, false, isDisabled );
        setDeleteOnExpiry( isDeletedOnExpiry );
        setExpiryDate( expiryDate );
        
        ipCidrPair = new HittingIpCidrPair( ip, cidr );
    }
    
    /**
     * Returns the IP, CIDR pair of this access rule.
     * @return th ip cidr hitting pair
     */
    public HittingIpCidrPair getIpCidrPair()
    {
        return ipCidrPair;
    }
    
    public int getIp()
    {
        return ipCidrPair.ipAddr;
    }
        
    public byte getCidr()
    {
        return ipCidrPair.cidr;
    }
    
    /**
     * Returns the number of times the rule was triggered. All checks that
     * match the rule will increment the trigger.
     * @return the number of times the rule was triggered.
     */
    @Override
    public int getTriggerCount()
    {
        return ipCidrPair.getHits();
    }

    /**
     * Increments the trigger count by one.
     */
    @Override
    protected void incrementTriggerCount()
    {
        ipCidrPair.countHit();
    }

    /**
     * Sets the number of times the rule was triggered. All checks that
     * match the rule will increment the trigger.
     */
    @Override
    public void setTriggerCount( int count )
    {
        ipCidrPair.setHits( count );
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

    @Override
    public int hashCode()
    {
        final int PRIME = 31;
        int result = super.hashCode();
        result = PRIME * result + ((ipCidrPair == null) ? 0 : ipCidrPair.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        final IpUserSecurityRule other = (IpUserSecurityRule) obj;
        if (ipCidrPair == null)
        {
            if (other.ipCidrPair != null)
                return false;
        } else if (!ipCidrPair.equals(other.ipCidrPair))
            return false;
        return true;
    }

    @Override
    public DSecurityRule createDSecurityRule()
    {
        DIpAccessRule dRule = new DIpAccessRule();
        if ( !isSystemRule )
        {
            dRule.setDescription( description );
            dRule.setExpiryDate( expiryDate.getTime() );
            dRule.setDeletedOnExpiry( isDeletedOnExpiry );
            dRule.setDenyingRule( isDenyingRule );
            dRule.setDisabled( isDisabled );
        }
        else
        {
            dRule.setSystemRule( true );
        }
        dRule.setIp( AddressUtils.intIp2ByteIp( ipCidrPair.ipAddr ) );
        dRule.setCidr( ipCidrPair.cidr );
        dRule.setTriggerCount( ipCidrPair.getHits() );

        return dRule;
    }
}