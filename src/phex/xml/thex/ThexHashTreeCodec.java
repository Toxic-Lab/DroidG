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
 *  Created on 02.11.2003
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.thex;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 */
public class ThexHashTreeCodec
{
    public static ThexHashTree parseThexHashTreeXML( InputStream inStream )   
        throws IOException
    {
        ThexHashTree hashTree = new ThexHashTree();
        
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try
        {
            SAXParser saxParser = spf.newSAXParser();
            InputSource is = new InputSource(inStream);
            ThexHashTreeSAXHandler handler = new ThexHashTreeSAXHandler(hashTree);
            
            saxParser.parse( is, handler );
            return hashTree;
        }
        catch ( ParserConfigurationException exp )
        {
            //NLogger.error( ThexHashTreeCodec.class, exp, exp );
            throw new IOException( "Parsing Thex HashTree failed." );
        }
        catch ( SAXException exp )
        {
            //NLogger.error( ThexHashTreeCodec.class, exp, exp );
            throw new IOException( "Parsing Thex HashTree failed." );
        }
        catch(IOException exp){
        	throw exp;
        }
    }
    
    public static byte[] generateThexHashTreeXML( ThexHashTree hashTree )
        throws IOException
    {
        StringWriter writer = new StringWriter();
        writer.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );
        writer.write( "<!DOCTYPE hashtree SYSTEM \"http://open-content.net/spec/thex/thex.dtd\">" );
        writer.write( "<hashtree>" );
        writer.write( "<file size=\"" );
        writer.write( hashTree.getFileSize() );
        writer.write( "\" segmentsize=\"" );
        writer.write( hashTree.getFileSegmentSize() );
        writer.write( "\"/>" );
        writer.write( "<digest algorithm=\"" );
        writer.write( hashTree.getDigestAlgorithm() );
        writer.write( "\" outputsize=\"" );
        writer.write( hashTree.getDigestOutputSize() );
        writer.write( "\"/>" );
        writer.write( "<serializedtree depth=\"" );
        writer.write( hashTree.getSerializedTreeDepth() );
        writer.write( "\" type=\"" );
        writer.write( hashTree.getSerializedTreeType() );
        writer.write( "\" uri=\"" );
        writer.write( hashTree.getSerializedTreeUri() );
        writer.write( "\"/>" );
        writer.write( "</hashtree>" );
        
        String output = writer.toString();

        return output.getBytes( "UTF-8" );
    }
}
