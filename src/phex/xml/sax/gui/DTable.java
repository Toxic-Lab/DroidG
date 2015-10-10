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

import org.xml.sax.SAXException;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public class DTable implements DElement
{
    public static final String ELEMENT_NAME = "table";

    private String tableIdentifier;

    private DTableColumnList tableColumnList;
    
    

    public DTableColumnList getTableColumnList()
    {
        return tableColumnList;
    }

    public void setTableColumnList( DTableColumnList tableColumnList )
    {
        this.tableColumnList = tableColumnList;
    }

    public String getTableIdentifier()
    {
        return tableIdentifier;
    }

    public void setTableIdentifier( String tableIdentifier )
    {
        this.tableIdentifier = tableIdentifier;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );

        if ( tableIdentifier != null )
        {
            writer.startElm( "tableIdentifier", null );
            writer.elmText( tableIdentifier );
            writer.endElm( "tableIdentifier" );
        }

        tableColumnList.serialize( writer );

        writer.endElm( ELEMENT_NAME );
    }
}
