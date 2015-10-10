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
 *  --- SVN Information ---
 *  $Id$
 */
package phex.xml.sax;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 */
public class PhexXmlSaxWriter
{
    private AttributesImpl attributes = new AttributesImpl();
    private TransformerHandler transHandler;
    
    public static void serializePhexXml( OutputStream outStream, DPhex dPhex )
        throws IOException
    {
        PhexXmlSaxWriter writer = new PhexXmlSaxWriter();
        try
        {
            writer.writePhexXml(outStream, dPhex);
        }
        catch (SAXException exp)
        {
            //NLogger.error( PhexXmlSaxWriter.class, exp, exp );
            throw new IOException( "Serializing Phex XML." );
        }
    }
    
    public void writePhexXml( OutputStream outStream, DPhex dPhex ) 
        throws SAXException
    {
        StreamResult streamResult = new StreamResult( outStream );
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        try
        {
            transHandler = tf.newTransformerHandler();
        }
        catch (TransformerConfigurationException exp)
        {
            throw new SAXException( exp.getMessage(), exp );
        }
        Transformer transformer = transHandler.getTransformer();
        // idention only works till Java 1.4.2 and from Java 1.5.0_06
        // we dont work around this since its not critical.
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2"); 
        transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
        transHandler.setResult( streamResult );
        
        transHandler.startDocument();
        dPhex.serialize(this);
        transHandler.endDocument();
    }
        
    public void startElm( String name, AttributesImpl atts ) throws SAXException
    {
        if ( atts == null )
        {
            attributes.clear();
            atts = attributes;
        }
        transHandler.startElement( "", "", name, atts );
    }
    
    public void elmText( String text ) throws SAXException
    {
        transHandler.characters( text.toCharArray(), 0, text.length() );
    }
    
    public void elmByte( byte val ) throws SAXException
    {
        elmText( String.valueOf(val) );
    }
    
    public void elmInt( int val ) throws SAXException
    {
        elmText( String.valueOf(val) );
    }
    
    public void elmLong( long val ) throws SAXException
    {
        elmText( String.valueOf(val) );
    }
    
    public void elmBol( boolean val ) throws SAXException
    {
        elmText( String.valueOf(val) );
    }
    
    public void elmHexBinary( byte[] data ) throws SAXException
    {
        StringBuffer r = new StringBuffer(data.length * 2);
        for(int i = 0; i < data.length; i++)
        {
            r.append( hexBinaryEncode(data[i] >> 4));
            r.append( hexBinaryEncode(data[i] & 0xf));
        }
        elmText( r.toString() );
    }
    
    private char hexBinaryEncode( int ch )
    {
        ch &= 0xf;
        if(ch < 10)
            return (char)(48 + ch);
        else
            return (char)(65 + (ch - 10));
    }
    
    public void endElm( String name ) throws SAXException
    {
        transHandler.endElement( "", "", name );
    }
}
