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
package phex.xml.sax;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import phex.xml.sax.parser.PhexSAXHandler;

/**
 * 
 */
public class PhexXmlSaxParser
{
    public static DPhex parsePhexXml( InputStream inStream )   
        throws IOException
    {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try
        {
            SAXParser saxParser = spf.newSAXParser();
        
            DPhex dPhex = new DPhex();
            saxParser.parse( new InputSource( inStream ),
                new PhexSAXHandler( dPhex, saxParser ) );
            return dPhex;
        }
        catch ( ParserConfigurationException exp )
        {
            //NLogger.error( PhexXmlSaxParser.class, exp, exp );
            throw new IOException( "Parsing Phex XML failed." );
        }
        catch ( SAXException exp )
        {
            //NLogger.error( PhexXmlSaxParser.class, exp, exp );
            throw new IOException( "Parsing Phex XML failed." );
        }
    }
}
