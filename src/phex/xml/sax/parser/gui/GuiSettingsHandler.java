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
 *  Created on 18.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.parser.gui;

import java.io.CharArrayWriter;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import phex.xml.sax.gui.DGuiSettings;
import phex.xml.sax.gui.DTab;
import phex.xml.sax.gui.DTableList;

/**
 * 
 */
public class GuiSettingsHandler extends DefaultHandler
{
    public static final String THIS_TAG_NAME = DGuiSettings.ELEMENT_NAME;

    private CharArrayWriter text = new CharArrayWriter();

    private SAXParser parser;

    private DGuiSettings dGui;

    private DefaultHandler parent;

    public GuiSettingsHandler( DGuiSettings dGui, DefaultHandler parent, 
        SAXParser parser )
    {
        this.dGui = dGui;
        this.parser = parser;
        this.parent = parent;
    }

    /**
     * Receive notification of the start of an element.
     *
     * @param name The element type name.
     * @param attributes The specified or defaulted attributes.
     * @exception org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see org.xml.sax.ContentHandler#startElement
     */
    @Override
    public void startElement( String uri, String localName, String qName,
        Attributes attributes ) throws SAXException
    {
        text.reset();
        if ( qName.equals( "tab" ) )
        {
            DTab tab = new DTab();
            dGui.getTabList().add( tab );

            GuiTabHandler handler = new GuiTabHandler( tab, attributes, this,
                parser );
            parser.getXMLReader().setContentHandler( handler );
        }
        else if ( qName.equals( GuiTableListHandler.THIS_TAG_NAME ) )
        {
            DTableList tables = new DTableList();
            dGui.setTableList( tables );

            GuiTableListHandler handler = new GuiTableListHandler( tables, attributes,
                this, parser );
            parser.getXMLReader().setContentHandler( handler );
        }
        return;
    }

    @Override
    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( qName.equals( "look-and-feel-class" ) )
        {
            dGui.setLookAndFeelClass( text.toString() );
        }
        else if ( qName.equals( "icon-pack" ) )
        {
            dGui.setIconPackName( text.toString() );
        }
        else if ( qName.equals( "is-toolbar-visible" ) )
        {
            dGui.setToolbarVisible( Boolean.valueOf( text.toString() )
                .booleanValue() );
        }
        else if ( qName.equals( "is-statusbar-visible" ) )
        {
            dGui.setStatusbarVisible( Boolean.valueOf( text.toString() )
                .booleanValue() );
        }
        else if ( qName.equals( "is-searchbar-visible" ) )
        {
            dGui.setSearchBarVisible( Boolean.valueOf( text.toString() )
                .booleanValue() );
        }
        else if ( qName.equals( "is-searchlist-visible" ) )
        {
            dGui.setSearchListVisible( Boolean.valueOf( text.toString() )
                .booleanValue() );
        }
        else if ( qName.equals( "is-searchfilterpanel-visible" ) )
        {
            dGui.setSearchFilterPanelVisible( Boolean.valueOf( text.toString() )
                .booleanValue() );
        }
        else if ( qName.equals( "is-log-bandwidth-slider-used" ) )
        {
            dGui.setLogBandwidthSliderUsed( Boolean.valueOf( text.toString() )
                .booleanValue() );
        }
        else if ( qName.equals( "show-respect-copyright-notice" ) )
        {
            dGui.setShowRespectCopyrightNotice( Boolean.valueOf(
                text.toString() ).booleanValue() );
        }
        else if ( qName.equals( "window-posX" ) )
        {
            try
            {
                dGui.setWindowPosX( Integer.parseInt( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( SharedFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "window-posY" ) )
        {
            try
            {
                dGui.setWindowPosY( Integer.parseInt( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( SharedFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "window-width" ) )
        {
            try
            {

                dGui.setWindowWidth( Integer.parseInt( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( SharedFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "window-height" ) )
        {
            try
            {
                dGui.setWindowHeight( Integer.parseInt( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( SharedFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( THIS_TAG_NAME ) )
        {
            parser.getXMLReader().setContentHandler( parent );
        }
    }

    @Override
    public InputSource resolveEntity( String publicId, String systemId )
    {
        return null;
    }

    @Override
    public void characters( char[] ch, int start, int length )
    {
        text.write( ch, start, length );
    }
}
