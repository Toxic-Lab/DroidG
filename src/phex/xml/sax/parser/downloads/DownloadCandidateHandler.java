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
package phex.xml.sax.parser.downloads;

import java.io.CharArrayWriter;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import phex.xml.sax.downloads.DDownloadCandidate;

/**
 * 
 */
public class DownloadCandidateHandler extends DefaultHandler
{
    private CharArrayWriter text = new CharArrayWriter();

    private SAXParser parser;

    private DDownloadCandidate downloadCandidate;

    private DefaultHandler parent;

    public DownloadCandidateHandler( DDownloadCandidate downloadCandidate, 
        DefaultHandler parent, SAXParser parser )
    {
        this.downloadCandidate = downloadCandidate;
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
        return;
    }

    @Override
    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( qName.equals( "guid" ) )
        {
            downloadCandidate.setGuid( text.toString() );
        }
        else if ( qName.equals( "fileindex" ) )
        {
            try
            {
                downloadCandidate.setFileIndex( Long.parseLong( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( DownloadCandidateHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "last-connect" ) )
        {
            try
            {
                downloadCandidate.setLastConnectionTime( Long.parseLong( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( DownloadCandidateHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "filename" ) )
        {
            downloadCandidate.setFileName( text.toString() );
        }
        else if ( qName.equals( "download-uri" ) )
        {
            downloadCandidate.setDownloadUri( text.toString() );
        }
        else if ( qName.equals( "resource-urn" ) )
        {
            downloadCandidate.setResourceUrn( text.toString() );
        }
        else if ( qName.equals( "remotehost" ) )
        {
            downloadCandidate.setRemoteHost( text.toString() );
        }
        else if ( qName.equals( "connectionFailedRepetition" ) )
        {
            try
            {
                downloadCandidate.setConnectionFailedRepetition( Integer.parseInt( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( DownloadCandidateHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "vendor" ) )
        {
            downloadCandidate.setVendor( text.toString() );
        }
        else if ( qName.equals( "isPushNeeded" ) )
        {
            downloadCandidate.setPushNeeded( Boolean.getBoolean( text.toString() ) );
        }
        else if ( qName.equals( "isThexSupported" ) )
        {
            downloadCandidate.setThexSupported( Boolean.getBoolean( text.toString() ) );
        }
        else if ( qName.equals( "isChatSupported" ) )
        {
            downloadCandidate.setChatSupported( Boolean.getBoolean( text.toString() ) );
        }
        else if ( qName.equals( DDownloadCandidate.ELEMENT_NAME ) )
        {
            parser.getXMLReader().setContentHandler( parent );
        }
    }

    public InputSource resolveEntity( String publicId, String systemId )
    {
        return null;
    }

    public void characters( char[] ch, int start, int length )
    {
        text.write( ch, start, length );
    }
}
