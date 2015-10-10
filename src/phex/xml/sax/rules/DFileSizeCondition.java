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

import phex.rules.condition.Condition;
import phex.rules.condition.FileSizeCondition;
import phex.rules.condition.FileSizeCondition.Range;
import phex.xml.sax.PhexXmlSaxWriter;

/**
 * Filters all files matching the expression.
 */
public class DFileSizeCondition implements DCondition
{
    static public final String ELEMENT_NAME = "filesize-condition";
    private List<FileSizeCondition.Range> ranges;

    /**
     * @param expression
     * @param case1
     */
    public DFileSizeCondition( )
    {
        ranges = new ArrayList<FileSizeCondition.Range>();
    }
    
    public List<FileSizeCondition.Range> getRanges()
    {
        return ranges;
    }

    public void setRanges( List<FileSizeCondition.Range> ranges )
    {
        this.ranges = ranges;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );
        
        if ( ranges != null )
        {
            AttributesImpl attributes = null;
            Iterator iterator = ranges.iterator();
            while ( iterator.hasNext() )
            {
                FileSizeCondition.Range range = (Range) iterator.next();
                
                attributes = new AttributesImpl();
                attributes.addAttribute( "", "", "min", "CDATA", 
                    String.valueOf( range.min ) );
                attributes.addAttribute( "", "", "max", "CDATA", 
                    String.valueOf( range.max ) );
                
                writer.startElm( "range", attributes );
                writer.endElm( "range" );
            }
        }
        
        writer.endElm( ELEMENT_NAME );
    }
    
    public Condition createCondition()
    {
        FileSizeCondition cond = new FileSizeCondition( );
        Iterator iterator = ranges.iterator();
        while ( iterator.hasNext() )
        {
            cond.addRange( (Range) iterator.next() );
        }
        return cond;
    }
}
