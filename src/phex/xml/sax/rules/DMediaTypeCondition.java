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
 *  Created on 14.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.rules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import phex.common.MediaType;
import phex.rules.condition.Condition;
import phex.rules.condition.MediaTypeCondition;
import phex.xml.sax.PhexXmlSaxWriter;

/**
 * Filters all files matching the expression.
 */
public class DMediaTypeCondition implements DCondition
{
    static public final String ELEMENT_NAME = "mediatype-condition";
    private List<MediaType> types;
    
    /**
     * @param expression
     * @param case1
     */
    public DMediaTypeCondition( )
    {
        types = new ArrayList<MediaType>();
    }

    public List<MediaType> getTypes()
    {
        return types;
    }

    public void setTypes( List<MediaType> types )
    {
        this.types = types;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );
        
        if ( types != null )
        {
            AttributesImpl attributes = null;
            for( MediaType type : types )
            {
                attributes = new AttributesImpl();
                attributes.addAttribute( "", "", "name", "CDATA", 
                    String.valueOf( type.getName() ) );
                
                writer.startElm( "media", attributes );
                writer.endElm( "media" );
            }
        }
        
        writer.endElm( ELEMENT_NAME );
    }
    
    public Condition createCondition()
    {
        MediaTypeCondition cond = new MediaTypeCondition( );
        Iterator iterator = types.iterator();
        while ( iterator.hasNext() )
        {
            cond.addType( (MediaType) iterator.next() );
        }
        return cond;
    }
}
