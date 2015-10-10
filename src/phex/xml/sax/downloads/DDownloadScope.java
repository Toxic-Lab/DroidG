/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
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
 *  Created on 01.11.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.downloads;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public class DDownloadScope implements DElement
{
    public static final String FINISHED_SCOPE_ELEMENT_NAME = "finished-scopes";
    public static final String UNVERIFIED_SCOPE_ELEMENT_NAME = "unverified-scopes";
    
    private final String elementName;
    
    private boolean hasStart;
    private long start;
    
    private boolean hasEnd;
    private long end;
    
    public DDownloadScope( String elementName )
    {
        if ( elementName == null )
        {
            throw new NullPointerException();
        }
        this.elementName = elementName;
    }
    
    public long getEnd()
    {
        return end;
    }

    public void setEnd( long end )
    {
        this.end = end;
        hasEnd = true;
    }

    public long getStart()
    {
        return start;
    }

    public void setStart( long start )
    {
        this.start = start;
        hasStart = true;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        AttributesImpl attributes = new AttributesImpl();
        if( hasStart )
        {
            attributes.addAttribute( "", "", "start", "CDATA",
                String.valueOf( start ) );
        }
        if( hasEnd )
        {
            attributes.addAttribute( "", "", "end", "CDATA",
                String.valueOf( end ) );
        }
        writer.startElm( elementName, attributes );
        writer.endElm( elementName );
    }
}
