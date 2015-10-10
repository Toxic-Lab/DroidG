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
 *  Created on 20.11.2006
 *  --- SVN Information ---
 *  $Id$
 */
package phex.xml.sax.security;

import org.xml.sax.SAXException;

import phex.xml.sax.PhexXmlSaxWriter;

public class DIpAccessRule extends DSecurityRule
{
    public static final String ELEMENT_NAME = "ip-access-rule";
    
    /**
     * @deprecated since build 301 (2007-05-24)
     */
    @Deprecated
    public static final byte SINGLE_ADDRESS = 1;
    /**
     * @deprecated since build 301 (2007-05-24)
     */
    @Deprecated
    public static final byte NETWORK_MASK = 2;
    /**
     * @deprecated since build 301 (2007-05-24)
     */
    @Deprecated
    public static final byte NETWORK_RANGE = 3;
    
    private byte[] ip;
    
    private boolean hasCidr;
    private byte cidr;
    
    /**
     * @deprecated since build 301 (2007-05-24)
     */
    @Deprecated
    private int addressType;
    
    /**
     * @deprecated since build 301 (2007-05-24)
     */
    @Deprecated
    private byte[] compareIp;
    
    
    
    
    public byte getCidr()
    {
        return cidr;
    }
    public void setCidr(byte cidr)
    {
        hasCidr = true;
        this.cidr = cidr;
    }
    public boolean hasCidr()
    {
        return hasCidr;
    }
    
    /**
     * @return the ip
     */
    public byte[] getIp()
    {
        return ip;
    }
    /**
     * @param ip the ip to set
     */
    public void setIp( byte[] ip )
    {
        this.ip = ip;
    }
    
    /**
     * @return the addressType
     * @deprecated since build 301 (2007-05-24)
     */
    @Deprecated
    public int getAddressType()
    {
        return addressType;
    }
    /**
     * @param addressType the addressType to set
     * @deprecated since build 301 (2007-05-24)
     */
    @Deprecated
    public void setAddressType( int addressType )
    {
        this.addressType = addressType;
    }
    /**
     * @return the compareIp
     * @deprecated since build 301 (2007-05-24)
     */
    @Deprecated
    public byte[] getCompareIp()
    {
        return compareIp;
    }
    /**
     * @param compareIp the compareIp to set
     * @deprecated since build 301 (2007-05-24)
     */
    @Deprecated
    public void setCompareIp( byte[] compareIp )
    {
        this.compareIp = compareIp;
    }
    
    
    
    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );
        
        serializeSecurityRuleElements( writer );
        
        if( ip != null )
        {
            writer.startElm( "ip", null );
            writer.elmHexBinary( ip );
            writer.endElm( "ip" );
        }
        
        if( hasCidr )
        {
            writer.startElm( "cidr", null );
            writer.elmByte( cidr );
            writer.endElm( "cidr" );
        }
        
        writer.endElm( ELEMENT_NAME );
    }
}