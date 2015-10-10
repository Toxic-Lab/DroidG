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
 *  --- SVN Information ---
 *  $Id$
 */

// File from:
// Rev.: Revision 62 - hu Feb 8 12:55:39 2007 UTC 
// http://metalinks.svn.sourceforge.net/viewvc/metalinks/extra-tools/java/saxparser/MetalinkSAXHandler.java?view=log

/*
    This file is part of the saxparser for Java from the Metalinks tools project
    Copyright (C) 2007  A. Bram Neijt <bneijt@gmail.com>

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/


package phex.metalink;

/*

 */
import java.io.CharArrayWriter;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 */

public class MetalinkSAXHandler extends DefaultHandler
{
    private CharArrayWriter text = new CharArrayWriter();
    private DMetalink dMetalink;
    private Attributes atr;
    private boolean inFiles = false;

    /**
     * @param dMetalink The metalink data container
     */
    MetalinkSAXHandler( DMetalink dMetalink )
    {
        this.dMetalink = dMetalink;
    }

    @Override
    public void startElement(String uri, String localName, String qName, 
        Attributes attributes)
    {
        if ( qName.equals("files") )
        {
            inFiles = true;
        }
        if ( inFiles ) 
        {
            //Set current name and copy attributes
            if ( qName.equals("file") )
            {
                dMetalink.newFile( attributes.getValue("name") );
            }

            //copy attributes
            atr = attributes;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
    {
        if ( qName.equals("files") )
        {
            inFiles = false;
        }

        if ( inFiles )
        {
            if( qName.equals("hash") )
            {
                dMetalink.addHash( atr.getValue("type"), text.toString().trim() );
            }

            if( qName.equals("url") )
            {
                dMetalink.addURL( atr.getValue("type"), text.toString().trim() );
            }
            text.reset();
        }
    }

    @Override
    public void characters( char[] ch, int start, int length )
    {
        text.write(ch,start,length );
    }
}
