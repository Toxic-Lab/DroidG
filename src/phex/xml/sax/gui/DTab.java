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
import org.xml.sax.helpers.AttributesImpl;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public class DTab implements DElement
{
    public static final String ELEMENT_NAME = "tab";
    
    private boolean hasTabId;
    private int tabId;
    
    private boolean isSetVisible;
    private boolean isVisible;
    
    
    
    public boolean isSetVisible()
    {
        return isSetVisible;
    }

    public boolean isVisible()
    {
        return isVisible;
    }

    public void setVisible( boolean isVisible )
    {
        isSetVisible = true;
        this.isVisible = isVisible;
    }

    public int getTabId()
    {
        return tabId;
    }

    public void setTabId( int tabId )
    {
        hasTabId = true;
        this.tabId = tabId;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        AttributesImpl attributes = null;
        if ( hasTabId )
        {
            attributes = new AttributesImpl();
            attributes.addAttribute( "", "", "tabID", "CDATA",
                String.valueOf( tabId ) );
        }
        writer.startElm( ELEMENT_NAME, attributes );

        if ( isSetVisible )
        {
            writer.startElm( "isVisible", null );
            writer.elmBol( isVisible );
            writer.endElm( "isVisible" );
        }

        writer.endElm( ELEMENT_NAME );
    }
}
