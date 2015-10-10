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
 *  Created on 20.11.2006
 *  --- SVN Information ---
 *  $Id$
 */
package phex.xml.sax.parser.security;

import java.io.CharArrayWriter;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import phex.xml.sax.parser.ParserUtils;
import phex.xml.sax.security.DIpAccessRule;

public class IpAccessRuleHandler extends DefaultHandler
{
    public static final String THIS_TAG_NAME = DIpAccessRule.ELEMENT_NAME;
    
    private CharArrayWriter text = new CharArrayWriter();
    private SAXParser parser;
    private DIpAccessRule dIpAccessRule;
    private DefaultHandler parent;
    
    public IpAccessRuleHandler( DIpAccessRule dIpAccessRule, 
        DefaultHandler parent, SAXParser parser )
    {
        this.dIpAccessRule = dIpAccessRule;
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
        Attributes attributes)
        throws SAXException
    {
        text.reset();
        return;
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) 
        throws SAXException
    {
        if ( qName.equals( "description" ) )
        {
            dIpAccessRule.setDescription( text.toString() );
        }
        else if ( qName.equals( "isDenyingRule" ) )
        {
            dIpAccessRule.setDenyingRule( Boolean.valueOf( text.toString() )
                .booleanValue() );
        }
        else if ( qName.equals( "isDisabled" ) )
        {
            dIpAccessRule.setDisabled( Boolean.valueOf( text.toString() )
                .booleanValue() );
        }
        else if ( qName.equals( "isDeletedOnExpiry" ) )
        {
            dIpAccessRule.setDeletedOnExpiry( Boolean.valueOf( text.toString() )
                .booleanValue() );
        }
        else if ( qName.equals( "isSystemRule" ) )
        {
            dIpAccessRule.setSystemRule( Boolean.valueOf( text.toString() )
                .booleanValue() );
        }
        else if ( qName.equals( "triggerCount" ) )
        {
            try
            {
                dIpAccessRule.setTriggerCount( Integer.parseInt( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( DownloadFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "expiryDate" ) )
        {
            try
            {
                dIpAccessRule.setExpiryDate( Long.parseLong( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( DownloadFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( "ip" ) )
        {
            dIpAccessRule.setIp( ParserUtils.fromHexBinary( text.toString() ) );
        }
        else if ( qName.equals( "cidr" ) )
        {
            try
            {
                dIpAccessRule.setCidr( Byte.parseByte( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( DownloadFileHandler.class, exp, exp );
            }
        }
        else if ( qName.equals( THIS_TAG_NAME ) )
        {
            parser.getXMLReader().setContentHandler( parent );
        }
        else if ( qName.equals( "compareIP" ) )
        {
            dIpAccessRule.setCompareIp( ParserUtils.fromHexBinary( text.toString() ) );
        }
        else if ( qName.equals( "addressType" ) )
        {
            try
            {
                dIpAccessRule.setAddressType( Integer.parseInt( text.toString() ) );
            }
            catch (NumberFormatException exp)
            {
                //NLogger.error( DownloadFileHandler.class, exp, exp );
            }
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