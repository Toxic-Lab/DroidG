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
 *  Created on 16.03.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.gui;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public class DTableList implements DElement
{
    public static final String ELEMENT_NAME = "table-list";
    
    private List<DTable> tableList;
    
    private boolean isSetShowHorizontalLines;
    private boolean showHorizontalLines;
    
    private boolean isSetShowVerticalLines;
    private boolean showVerticalLines;
    
    

    public DTableList()
    {
        tableList = new ArrayList<DTable>();
    }
    
    public boolean isSetShowHorizontalLines()
    {
        return isSetShowHorizontalLines;
    }

    public boolean isSetShowVerticalLines()
    {
        return isSetShowVerticalLines;
    }
    
    public boolean isShowHorizontalLines()
    {
        return showHorizontalLines;
    }

    public void setShowHorizontalLines( boolean showHorizontalLines )
    {
        isSetShowHorizontalLines = true;
        this.showHorizontalLines = showHorizontalLines;
    }

    public boolean isShowVerticalLines()
    {
        return showVerticalLines;
    }

    public void setShowVerticalLines( boolean showVerticalLines )
    {
        isSetShowVerticalLines = true;
        this.showVerticalLines = showVerticalLines;
    }

    public List<DTable> getTableList()
    {
        return tableList;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        AttributesImpl attributes = new AttributesImpl();
        if( isSetShowHorizontalLines )
        {
            attributes.addAttribute( "", "", "showHorizontalLines", "CDATA",
                String.valueOf( showHorizontalLines ) );
        }
        if( isSetShowVerticalLines )
        {
            attributes.addAttribute( "", "", "showVerticalLines", "CDATA",
                String.valueOf( showVerticalLines ) );
        }
        writer.startElm( ELEMENT_NAME, attributes );
        
        for ( DTable dTable : tableList )
        {
            dTable.serialize(writer);
        }
        
        writer.endElm( ELEMENT_NAME );
    }
}
