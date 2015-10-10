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
 *  Created on 10.03.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.share;

import org.xml.sax.SAXException;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public class DAlternateLocation implements DElement
{
    public static final String ELEMENT_NAME = "AltLoc";
    
    private String hostAddress;
    
    private String urn;

    public String getHostAddress()
    {
        return hostAddress;
    }

    public void setHostAddress( String hostAddress )
    {
        this.hostAddress = hostAddress;
    }

    public String getUrn()
    {
        return urn;
    }

    public void setUrn( String urn )
    {
        this.urn = urn;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );
        
        if( hostAddress != null )
        {
            writer.startElm( "host-address", null );
            writer.elmText( hostAddress );
            writer.endElm( "host-address" );
        }
        
        if( urn != null )
        {
            writer.startElm( "URN", null );
            writer.elmText( urn );
            writer.endElm( "URN" );
        }
        
        writer.endElm( ELEMENT_NAME );
    }
}
