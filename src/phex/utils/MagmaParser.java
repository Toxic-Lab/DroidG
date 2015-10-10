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
package phex.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class MagmaParser
{

/** This Class reads out Magma-files passed to it and
 * collects them in the array "magnets[]". 
 * Check the usage in phex.gui.dialogs.NewDowloadDialog
 * 
 * You can find the specs for Magma at
 * http://gnuticles.gnufu.net/magma-specs.html
 */	

	/** 
	 * The update Uri for this Magma-List
	 */
	
    public String uuri; 
    
    /**
     * List of possible EOL characters, not exactly according to YAML 4.1.4
     */
    
    private static final String EOL_CHARACTERS = "\r\n";

    private static final char[] MAGMA_LINE = new char[]
    { '#', 'M', 'A', 'G', 'M', 'A'};

    private static final char[] MAGMA_LINE_0_2 = new char[]
    { '#', 'M', 'A', 'G', 'M', 'A' ,'v', '0', '.', '2'};
    
    private static final char[] MAGMA_LINE_0_4 = new char[]
    { '#', 'M', 'A', 'G', 'M', 'A' ,'v', '0', '.', '4'};
    
    private static final char[] MAGNET_PREFIX = new char[]
    { 'm', 'a', 'g', 'n', 'e', 't' };
    
    private static final char[] MAGMA_SUFFIX = new char[]
    { '.', 'm', 'a', 'g', 'm', 'a' };

    private static final char[] LIST_ELEMENT = new char[]
    { 'l', 'i', 's', 't', ':' };
    
    // The identifier of the download name in the header
    private static final char[] HEAD_DOWNLOAD_NAME = new char[]
    { 'd', 'n', '=' };
                                                        
    

    private PushbackReader reader;

    private List<String> magnets;
    
    private String magmaName; 

    public MagmaParser( InputStream inStream )
    {
        magnets = new ArrayList<String>();
        try
        {
            InputStreamReader reader = new InputStreamReader( inStream, "UTF-8" );
            this.reader = new PushbackReader(reader, 10);
        }
        catch ( UnsupportedEncodingException exp )
        {
            assert false;
        }
    }

    public void start()
        throws IOException
    {
        try
        {
            /* The FileReader checks, if the File begins with "#MAGMA"
             * and sends the characters following the "#MAGMA" to the 
             * listFinder. 
             */
            char buff[] = new char[10];
            int readBytes = 0;

            while (readBytes != 10)
            {
                int count = reader.read(buff, readBytes, 10);
                if (count == -1)
                {
                    throw new IOException("Input file is no MAGMA-File (EOF)");
                }
                readBytes += count;
            }
            if (Arrays.equals(buff, MAGMA_LINE_0_2))
            {
                // check for the name of the magma file, strip the .magma and use it as destination_dir
                parseMagmaHeader(); 
            	skipToEOL();
                parseList();
            }
            else if (Arrays.equals(buff, MAGMA_LINE_0_4))
            {
                // The file is a new style magma list in yaml format, so we should get it via the yaml interface. TODO: Read out 0.4 magmas via the yaml interface. 
		// We simply process it in the hope that it might have a 0.2 compatibility section. 
                // Still we need the magma header first. 
                

                parseMagmaHeader(); 
                skipToEOL();
                parseList();
                // but since this isn't implemented yet, we just throw an error. 
                // throw new IOException("Input file is a version 0.4 MAGMA file, which isn't yet supported.");

                /** Usage of jvyaml.YAML (just to avoid having to search again). 
                * 
                * import org.jvyaml.YAML;
                * 
                * Map configuration = (Map)YAML.load(new FileReader("c:/projects/ourSpecificConfig.yml"));
                * List values = (List)YAML.load("--- \n- A\n- b\n- c\n");
                * 
                * From http://ola-bini.blogspot.com/2006/06/announcing-jvyaml.html
                */
                
            }
            
        }
        finally 
        {
            reader.close();
        }
    }
    
    public List getMagnets()
    {
        return magnets;
    }
    
    public String getMagmaName()
    {
        return magmaName;
    }
    
    public void parseMagmaHeader()
        throws IOException
    {
        magmaName = ""; 
        // we are just after the magma header. 
        // We must now search for the HEAD_DOWNLOAD_NAME 
        int pos = 0; 
        int c; 
        while ( true )
        {
            c = reader.read(); 
            if ( c == HEAD_DOWNLOAD_NAME[pos] )
            {
                pos ++; 
                if ( pos == HEAD_DOWNLOAD_NAME.length )
                {
                    // we're at the beginning of the download name
                    parseMagmaName();
                    return; 
                }   
            }
            else if ( c == -1 )
            {
                // reached the end...
                return;
            }
            else if ( EOL_CHARACTERS.indexOf( c ) != -1 )
            {
                // We left the header line. 
                // unread the char so it can be checked again by other functions 
                reader.unread( c ); 
                return; 
            }
            else
            {// next char of list element not found... search on
                pos = 0;   
            }
        }
    }
    
    public void parseMagmaName()
        throws IOException
    {
        // we are just after the magma header. 
        // We must now search for the HEAD_DOWNLOAD_NAME 
        int pos = 0; 
        int c;
        StringBuffer nameBuffer = new StringBuffer(); 
        while ( true )
        {
            c = reader.read(); 
            if ( c == MAGMA_SUFFIX[pos] )
            {
                pos ++; 
                if ( pos == MAGMA_SUFFIX.length )
                {
                    // We read the whole name of the magma file
                    magmaName = nameBuffer.toString(); 
                    return; 
                }   
            }
            else if ( c == -1 )
            {
                // reached the end...
                return;
            }
            else if ( EOL_CHARACTERS.indexOf( c ) != -1 )
            {
                // We left the header line. 
                // unread the char so it can be checked again by other functions 
                reader.unread( c ); 
                return; 
            }
            else
            {
                // the char is part of the magmas name
                // We append it to the name. 
                nameBuffer.append( (char)c ); 
            }
        }
    }
    
    private void parseList()
        throws IOException
    {
        // we are at beginning of a line... we are searching for
        // the "list:" element in the stream it must be on the beginning of a line
        int pos = 0;
        int c;
        while ( true )
        {
            c = reader.read();
            if ( c == LIST_ELEMENT[pos] )
            {
                pos ++;
                if ( pos == LIST_ELEMENT.length )
                {
                    // found list: element.. skip line and continue to parse body.
                    skipToEOL();
                    parseListBody();
                    pos = 0;
                }
            }
            else if ( c == -1 )
            {
                // reached the end...
                return;
            }
            else
            {// next char of list element not found... skip line...
                pos = 0;
                skipToEOL();
            }
        }
    }    
    
    private void parseListBody()
        throws IOException
    {
        int c;
        boolean startOfLine = true;
        while ( true )
        {
            c = reader.read();
            
            if ( c == '-' )
            {// we found a list element
             // we could check if we are still on the start of line and do an error/list ending case
             // but we assume its just a list element here.
                if ( !startOfLine )
                {
                    parseListElement();
                    continue;
                }
                else
                {
                    // if we are on the start of line this is a faulty start for a 
                    // list element... but we try to work around possible user errors...
                    int pre = reader.read();
                    if ( pre == ' ' )
                    {// there is a good chance this is a magnet
                        reader.unread(pre);
                        parseListElement();
                    }
                    else
                    {// we are not in a list anymore... this must be the start of a next element.
                        reader.unread(pre);
                        reader.unread(c);
                        return;
                    }
                }
            }
            else if ( c == '#' )
            {// we reached a comment... skip rest of line... (YAML 4.2.3)
                skipToEOL();
                startOfLine = true;
            }
            else if ( c == ' ' || c == '\r' || c == '\n' )
            {// skip leading indetion whitespace..
                startOfLine = false;
            }
            else if ( EOL_CHARACTERS.indexOf( c ) != -1)
            {// skip leading indetion whitespace..
                startOfLine = true;
            }
            else if ( c == -1 )
            {
                // reached the EOF
                return;
            }
            else
            {
                if ( startOfLine )
                {// oups this indecates we jumped out of the list body..
                    reader.unread(c);
                    return;
                }
                else
                {
                    // most likley we found unwanted info between magnet elements in the list body.
                    // we could do more detail analysis of possible malformed YAML.. but we take it easy and
                    // just skip the line.
                    skipToEOL();
                    startOfLine = true;
                }
            }
        }
    }
    
    private void parseListElement()
        throws IOException
    {
        int c;
        while ( true )
        {
            c = reader.read();
            
            if ( c == ' ' )
            {// skip leading indetion whitespace..
                continue;
            }
            else if ( c == '#' )
            {// we reached a comment... skip rest of line... (YAML 4.2.3)
                skipToEOL();
            }
            else if ( c == '"' )
            {
                // pre check if this really is a magnet..
                char buff[] = new char[6];
                int readBytes = 0;
                while (readBytes != 6)
                {
                    int count = reader.read(buff, readBytes, 6);
                    if (count == -1)
                    {
                        return;
                    }
                    readBytes += count;
                }
                reader.unread(buff);
                if (Arrays.equals(buff, MAGNET_PREFIX))
                {
                    // reached quoted magnet "
                    parseMagnet();
                }
                else
                {
                    // skip rest of line and search for next list element
                    skipToEOL();
                }
                return;
            }
            else if ( c == -1 )
            {
                //reached the end
                return;
            }
            else
            {
                skipToEOL();
                return;
            }
        }
    }
    
    private void parseMagnet()
        throws IOException
    {
        StringBuffer magnetBuf = new StringBuffer();
        int c;
        while ( true )
        {
            c = reader.read();
            if ( c == ' ' || EOL_CHARACTERS.indexOf( c ) != -1 )
            {// skip all line folding characters.. and all spaces
                continue;
            }
            else if ( c == '"' )
            {// found the end of the quoted magnet. "
                break;
            }
            else if ( c == -1 )
            {
                // unexpected end...
                return;
            }
            else
            {
                magnetBuf.append( (char)c );
            }
        }
        magnets.add( magnetBuf.toString() );
    }
    
    
    /**
     * Skips all content till end of line.
     */
    private void skipToEOL() throws IOException
    {
        int c;
        while (true)
        {
            c = reader.read();
            if (c < 0)
            {// stream ended... a valid line could not be read... return
                return;
            }
            else if ( EOL_CHARACTERS.indexOf( c ) != -1 )
            {// we found a line ending... check if there are followups (YAML 4.1.4)
                while ( EOL_CHARACTERS.indexOf( c ) != -1 )
                {
                    c = reader.read();
                }
                // the last character was no EOL followup... push it back
                reader.unread( c );
                return;
            }
        }
    }
    
    
    
//    
//    /** Returns the Update Uri of a magma-List, ToDo */
//    public String getUpdateURI()
//    {
//        return uuri;
//    }
//
//
//    private void parseHeaderLine()
//        throws IOException
//    {
//        int c = reader.read();
//        while ( c != '"' &&  EOL_CHARACTERS.indexOf( c ) == -1 )
//        {
//        }
//        if ( c == '"' )
//        {
//            parseUpdateMagnet(); 
//        }
//    }
//    
//    public void parseUpdateMagnet()
//        throws IOException
//    {
//        StringBuffer magnetBuf = new StringBuffer();
//        int c;
//        while ( true )
//        {
//            c = reader.read();
//            if ( c == ' ' || EOL_CHARACTERS.indexOf( c ) != -1 )
//            {// skip all line folding characters.. and all spaces
//                continue;
//            }
//            else if ( c == '"' )
//            {// found the end of the quoted magnet. "
//                break;
//            }
//            else if ( c == -1 )
//            {
//                // unexpected end...
//                return;
//            }
//            else
//            {
//                magnetBuf.append( (char)c );
//            }
//        }
//        uuri = magnetBuf.toString();
//    }
}
