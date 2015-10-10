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

public class DTableColumn implements DElement
{
    public static final String ELEMENT_NAME = "table-column";

    private boolean hasColumnID;
    private int columnID;

    private boolean hasVisible;
    private boolean isVisible;

    private boolean hasVisibleIndex;
    private int visibleIndex;

    private boolean hasWidth;
    private int width;
    
    

    public int getColumnID()
    {
        return columnID;
    }

    public void setColumnID( int columnID )
    {
        hasColumnID = true;
        this.columnID = columnID;
    }

    public boolean isVisible()
    {
        return isVisible;
    }

    public void setVisible( boolean isVisible )
    {
        hasVisible = true;
        this.isVisible = isVisible;
    }

    public int getVisibleIndex()
    {
        return visibleIndex;
    }

    public void setVisibleIndex( int visibleIndex )
    {
        hasVisibleIndex = true;
        this.visibleIndex = visibleIndex;
    }

    public int getWidth()
    {
        return width;
    }

    public void setWidth( int width )
    {
        hasWidth = true;
        this.width = width;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );

        if ( hasColumnID )
        {
            writer.startElm( "columnID", null );
            writer.elmInt( columnID );
            writer.endElm( "columnID" );
        }

        if ( hasVisible )
        {
            writer.startElm( "isVisible", null );
            writer.elmBol( isVisible );
            writer.endElm( "isVisible" );
        }

        if ( hasVisibleIndex )
        {
            writer.startElm( "visibleIndex", null );
            writer.elmInt( visibleIndex );
            writer.endElm( "visibleIndex" );
        }

        if ( hasWidth )
        {
            writer.startElm( "width", null );
            writer.elmInt( width );
            writer.endElm( "width" );
        }

        writer.endElm( ELEMENT_NAME );
    }
}
