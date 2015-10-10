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
package phex.xml.sax.parser.rules;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import phex.xml.sax.DSubElementList;
import phex.xml.sax.rules.*;

/**
 * 
 */
public class ConsequencesListHandler extends DefaultHandler
{
    public static final String ELEMENT_NAME = DConsequencesList.ELEMENT_NAME;

    private SAXParser parser;

    private DSubElementList<DConsequence> consequencesListHolder;

    private DefaultHandler parent;

    public ConsequencesListHandler( DSubElementList<DConsequence> conditionListHolder, 
        DefaultHandler parent, SAXParser parser )
    {
        this.consequencesListHolder = conditionListHolder;
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
    public void startElement( String uri, String localName, String qName,
        Attributes attributes ) throws SAXException
    {
        if ( qName.equals( DFilterFromSearchConsequence.ELEMENT_NAME ) )
        {
            DFilterFromSearchConsequence consequence = new DFilterFromSearchConsequence();
            consequencesListHolder.getSubElementList().add( consequence );
            // no further content in this tag
        }
        else if ( qName.equals( DRemoveFromSearchConsequence.ELEMENT_NAME ) )
        {
            DRemoveFromSearchConsequence consequence = new DRemoveFromSearchConsequence();
            consequencesListHolder.getSubElementList().add( consequence );
            // no further content in this tag
        }
        else if ( qName.equals( DBanHostConsequence.ELEMENT_NAME ) )
        {
            DBanHostConsequence consequence = new DBanHostConsequence();
            consequencesListHolder.getSubElementList().add( consequence );
            // no further content in this tag
        }
        else if ( qName.equals( DDownloadFileConsequence.ELEMENT_NAME ) )
        {
            DDownloadFileConsequence consequence = new DDownloadFileConsequence();
            consequencesListHolder.getSubElementList().add( consequence );
            // no further content in this tag
        }
        return;
    }

    public void endElement( String uri, String localName, String qName )
        throws SAXException
    {
        if ( qName.equals( ELEMENT_NAME ) )
        {
            parser.getXMLReader().setContentHandler( parent );
        }
    }

    public InputSource resolveEntity( String publicId, String systemId )
    {
        return null;
    }
}
