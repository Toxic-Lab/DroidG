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
 *  Created on 02.11.2003
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.thex;

import java.io.InputStream;

import org.xml.sax.*;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 */
public class ThexHashTreeSAXHandler extends DefaultHandler
{
    private static final String THEX_HASHTREE_DTD =
        "http://open-content.net/spec/thex/thex.dtd";
    private static final String FILE_ELEMENT = "file";
    private static final String DIGEST_ELEMENT = "digest";
    private static final String SERIALIZEDTREE_ELEMENT = "serializedtree";
    
    private ThexHashTree hashtree;
    
    public ThexHashTreeSAXHandler( ThexHashTree hashtree )
    {
        this.hashtree = hashtree;
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
        Attributes attributes)
        throws SAXException
    {
        if ( FILE_ELEMENT.equals( qName ) )
        {
            hashtree.setFileSize( attributes.getValue( "size" ) );
            hashtree.setFileSegmentSize( attributes.getValue( "segmentsize" ) );
        }
        else if ( DIGEST_ELEMENT.equals( qName ) )
        {
            hashtree.setDigestAlgorithm( attributes.getValue( "algorithm" ) );
            hashtree.setDigestOutputSize( attributes.getValue( "outputsize" ) );
        }
        else if ( SERIALIZEDTREE_ELEMENT.equals( qName ) )
        {
            hashtree.setSerializedTreeDepth( attributes.getValue( "depth" ) );
            hashtree.setSerializedTreeType( attributes.getValue( "type" ) );
            hashtree.setSerializedTreeUri( attributes.getValue( "uri" ) );
        }
    }
     
     public InputSource resolveEntity(String publicId,
        String systemId)
     {
         if ( systemId.equals( THEX_HASHTREE_DTD ) )
         {
             InputStream stream = ThexHashTreeSAXHandler.class.getResourceAsStream(
                "/phex/xml/thex/ThexHashTree.dtd");
             return new InputSource( stream );
         }
         return null; 
     }
}
