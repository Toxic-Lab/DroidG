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
 *  Created on 18.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.parser;

import java.io.CharArrayWriter;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import phex.xml.sax.DPhex;
import phex.xml.sax.DSubElementList;
import phex.xml.sax.DUpdateResponse;
import phex.xml.sax.downloads.DDownloadFile;
import phex.xml.sax.favorites.DFavoriteHost;
import phex.xml.sax.gui.DGuiSettings;
import phex.xml.sax.parser.downloads.DownloadListHandler;
import phex.xml.sax.parser.favorites.FavoritesListHandler;
import phex.xml.sax.parser.gui.GuiSettingsHandler;
import phex.xml.sax.parser.rules.SearchRuleListHandler;
import phex.xml.sax.parser.security.SecurityHandler;
import phex.xml.sax.parser.share.SharedLibraryHandler;
import phex.xml.sax.rules.DSearchRule;
import phex.xml.sax.security.DSecurity;
import phex.xml.sax.share.DSharedLibrary;

/**
 * 
 */
public class PhexSAXHandler extends DefaultHandler
{
    private static final String PHEX_ELEMENT = "phex";
    
    private CharArrayWriter text = new CharArrayWriter();
    private SAXParser parser;
    private DPhex dPhex;
    
    public PhexSAXHandler( DPhex dPhex, SAXParser parser )
    {
        this.dPhex = dPhex;
        this.parser = parser;
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
        Attributes attributes)
        throws SAXException
    {
        text.reset();
        // this
        if ( PHEX_ELEMENT.equals( qName ) )
        {
            String phexVersion = attributes.getValue("phex-version");
            dPhex.setPhexVersion(phexVersion);
        }
        // childs
        else if ( qName.equals( "update-response" ) )
        {
            DUpdateResponse dResponse = new DUpdateResponse();
            dPhex.setUpdateResponse( dResponse );
            UpdateResponseHandler handler = new UpdateResponseHandler( 
                dResponse, attributes, this, parser );
            parser.getXMLReader().setContentHandler( handler );
        }
        else if ( qName.equals( "update-request" ) )
        {
            // we dont need to parse these... since we are only sending it.
            assert false : "We should not pare update-request";
        }
        else if ( qName.equals( SharedLibraryHandler.THIS_TAG_NAME ) )
        {
            DSharedLibrary sharedLib = new DSharedLibrary( );
            dPhex.setSharedLibrary( sharedLib );
            SharedLibraryHandler handler = new SharedLibraryHandler( 
                sharedLib, this, parser );
            parser.getXMLReader().setContentHandler( handler );
        }
        else if ( qName.equals( GuiSettingsHandler.THIS_TAG_NAME ) )
        {
            DGuiSettings dGui = new DGuiSettings();
            dPhex.setGuiSettings(dGui);
            GuiSettingsHandler handler = new GuiSettingsHandler( dGui, this,
                parser );
            parser.getXMLReader().setContentHandler( handler );
        }
        else if ( qName.equals( FavoritesListHandler.THIS_TAG_NAME ) )
        {
            DSubElementList<DFavoriteHost> favoritsList = new DSubElementList<DFavoriteHost>( 
                FavoritesListHandler.THIS_TAG_NAME );
            dPhex.setFavoritesList( favoritsList );
            FavoritesListHandler handler = new FavoritesListHandler( 
                favoritsList, this, parser );
            parser.getXMLReader().setContentHandler( handler );
        }
        else if ( qName.equals( SearchRuleListHandler.THIS_TAG_NAME ) )
        {
            DSubElementList<DSearchRule> ruleList = new DSubElementList<DSearchRule>( 
                SearchRuleListHandler.THIS_TAG_NAME );
            dPhex.setSearchRuleList( ruleList );
            SearchRuleListHandler handler = new SearchRuleListHandler( 
                ruleList, this, parser );
            parser.getXMLReader().setContentHandler( handler );
        }
        else if ( qName.equals( DownloadListHandler.THIS_TAG_NAME ) )
        {
            DSubElementList<DDownloadFile> downloadList = 
                new DSubElementList<DDownloadFile>( DownloadListHandler.THIS_TAG_NAME );
            dPhex.setDownloadList( downloadList );
            DownloadListHandler handler = new DownloadListHandler( 
                downloadList, this, parser );
            parser.getXMLReader().setContentHandler( handler );
        }
        else if ( qName.equals( SecurityHandler.THIS_TAG_NAME ) )
        {
            DSecurity security = new DSecurity( );
            dPhex.setSecurityList( security );
            SecurityHandler handler = new SecurityHandler( 
                security, this, parser );
            parser.getXMLReader().setContentHandler( handler );
        }
        
        
        return;
    }
    
    public void endElement(String uri, String localName, String qName) 
        throws SAXException
    {
        return;
    }
     
     public InputSource resolveEntity(String publicId,
        String systemId)
     {
         return null; 
     }
     
     public void characters(char[] ch, int start, int length)
     {
         text.write( ch,start,length );
     }
}
