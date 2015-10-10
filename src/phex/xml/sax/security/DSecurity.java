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
 *  Created on 20.11.2006
 *  --- SVN Information ---
 *  $Id$
 */
package phex.xml.sax.security;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public class DSecurity implements DElement
{
    public static final String ELEMENT_NAME = "security";
    
    private List<DSecurityRule> ipAccessRuleList;
    
    /**
     * @return the ipAccessList
     */
    public List<DSecurityRule> getIpAccessRuleList()
    {
        if ( ipAccessRuleList == null )
        {
            ipAccessRuleList = new ArrayList<DSecurityRule>();
        }
        return ipAccessRuleList;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );
        if( ipAccessRuleList != null )
        {
            for ( DSecurityRule rule : ipAccessRuleList )
            {
                rule.serialize( writer );
            }
        }
        writer.endElm( ELEMENT_NAME );
    }
}