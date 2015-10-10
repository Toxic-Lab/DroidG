/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Arne Babenhauserheide ( arne_bab <at> web <dot> de )
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
 *  Created on 08.02.2005
 *  --- CVS Information ---
 *  $Id$
 */
 
package phex.utils;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

 
public class RSSParser
{

/** This Class reads out RSS-files passed to it and
 * collects them in the array "magnets[]". 
 * Check the usage in phex.gui.dialogs.NewDowloadDialog
 */	
	
    /**
     * List of possible EOL characters, not exactly according to YAML 4.1.4
     */

    private static final String EOL_CHARACTERS = "\r\n";

    private static final char[] AMPERSAND_AMP = new char[]
    { 'a','m','p',';' };

    private static final String START_OF_ELEMENT_CHAR = "<";
    
    private static final String END_OF_ELEMENT_CHAR = ">";
    
    private static final String END_OF_ELEMENT_CHARN = "/";

    private static final char[] XML_LINE = new char[]
    { '<','?','x','m','l' };
    
    private static final char[] MAGNET_PREFIX = new char[]
    { 'm', 'a', 'g', 'n', 'e', 't' };
    
    private static final char[] HTTP_PREFIX = new char[]
    { 'h', 't', 't', 'p', ':', '/' };

    private static final char[] MAGNET_TAG = new char[]
    { '<','m', 'a', 'g', 'n', 'e', 't','>' };

    private static final char[] ENCLOSURE_TAG_START = new char[]
    { '<','e', 'n', 'c', 'l', 'o', 's','u' };

    private static final char[] ENCLOSURE_TAG_MID = new char[]
    { 'r','e' };

    private static final char[] URL_IDENTIFIER = new char[]
    { 'u','r','l','=','"' }; //"

    private static final char[] ITEM_ELEMENT = new char[]
    { '<','i','t','e','m','>', };

    private static final char[] END_OF_ITEM_ELEMENT = new char[]
    { '<','/','i','t','e','m','>', };

    private static final char[] RSS_TAG = new char[]
    { '<','r','s','s','>', };

    private static final char[] END_OF_RSS_TAG = new char[]
    { '<','/','r','s','s','>', };


    private PushbackReader reader;

    private List<String> magnets;

    public RSSParser(Reader reader)
    {
        magnets = new ArrayList<String>();
        this.reader = new PushbackReader(reader, 6);
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
             
            char buff[] = new char[5];
            int readBytes = 0;

            while (readBytes != 5)
            {
                int count = reader.read(buff, readBytes, 5);
                if (count == -1)
                {
                    throw new IOException("Input file is no XML-File ("
                        + String.valueOf(buff) + ").");
                }
                readBytes += count;
            }
            if (Arrays.equals(buff, XML_LINE))
            {
                parseXml();
            }
        }
        finally
        {
            reader.close();
        }
    }
    
    public List getMagnets()
    {
    // Can be called to get the included magnets
        return magnets;
    }

    private void parseXml()
        throws IOException
    {
        
        int pos = 0;
        int c;
        while ( true )
        {
            c = reader.read();
            if ( c == RSS_TAG[pos] )
            {
                pos ++;
                if ( pos == RSS_TAG.length )
                {
                    // found rss-tag.. find the first item.
                    parseList();
                    pos = 0;
                }
            }
            else if ( c == -1 )
            {
                // reached the end...
                return;
            }
            else
            {// next char of rss tag not found... skip line...
                pos = 0;
                
                
                //skipToEndOfObject();
                
                    parseList(); // ignore that this is careless
            }
        }
    }    


    private void parseList()
        throws IOException
    {
        
        int pos = 0;
        int c;
        while ( true )
        {
            c = reader.read();
            if ( c == ITEM_ELEMENT[pos] )
            {
                pos ++;
                if ( pos == ITEM_ELEMENT.length )
                {
                    // found list: element.. skip line and continue to parse body.
                    
                    
                    parseItemBody();
                    pos = 0;
                }
            }
            else if ( c == END_OF_RSS_TAG[pos] )
            {
                pos ++;
                if ( pos == END_OF_RSS_TAG.length )
                {
                    // RSS_TAG ended. 
                    
                    
                    pos = 0;
                    return;
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
            }
        }
    }    
    
    public void parseItemBody()
        throws IOException
    {
        int c;
        int pos = 0;
        while ( true )
        {
            c = reader.read();
            if ( c == MAGNET_TAG[pos] )
            {
            	pos ++;
                	if ( pos == MAGNET_TAG.length )
                	{
                    	// we found a magnet-element
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
            	        // reached quoted magnet 
            	        
            	        
                    	pos = 0; 
                	    parseMagnet();
	                }
	                else if (Arrays.equals(buff, HTTP_PREFIX))
        	        {
            	        // reached quoted magnet 
            	        
            	        
                    	pos = 0; 
                	    parseMagnet();
	                }
    	            else
        	        {
            	        // skip to the end of this magnet-tag, 
            	        // it doesn't contain a magnet nor a http-uri. 
            	        
            	        
	                }
    	            pos = 0; 
              	}
            }
            
            /** 
            * Code to read out enclosures with 
            * http- or magnet-uris doesn't work yet.
            */ 
            
            else if ( c == ENCLOSURE_TAG_START[pos] )
            {
            	pos ++;
                	if ( pos == ENCLOSURE_TAG_START.length )
                	{
                    	// we found an enclosure-tag
                    	// pre check if this contains a magnet or http-url..
            	        pos = 0; 
	                    while ( true )
    	                {
        	            	c = reader.read(); 
            	        	//go forward up to the end of the URL-identifier. 
                	    	if ( c == URL_IDENTIFIER[pos] )
                	    	{
                    				pos++; 
                    				if ( pos == URL_IDENTIFIER.length )
									{ //this containis an url-identifier. 
									  // check for magnet or http-start. 
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
           		    				    	// reached quoted magnet 
				                    		pos = 0; 
   					            	    	parseMagnet();
   					            	    	break; 
   		            		    		}
   		            		    		else if (Arrays.equals(buff, HTTP_PREFIX))
        					   	    	{
            		    			    	// reached quoted http-url 
					                   		pos = 0; 
    					           	    	parseMagnet();
    					           	    	break; 
    		            	    		}
		    		            	}
		    		         }
		    		         else if ( END_OF_ELEMENT_CHAR.indexOf( c ) != -1 )
		    		         { //return if we reached the end of the enclosure. 
		    		         	pos = 0; 
		    		            break; 
		    		         }
		    		         else if ( c == -1 )
		    		         {
		    		            pos = 0; 
		    		         	return; 
		    		         }
		    		         else 
		    		         { 
		    		         	pos = 0; 
		    		         }
		    			} // end of inner while (true)
					}
	                
    	            else // pos != ENCLOSURE_TAG_START.length
        	        {
            	        // next letter
	                }
	            
           	}
			
            else if ( c == -1 )
            {
                // reached the EOF
                pos = 0; 
                return;
            }
            
            /**
            * Commented it out, because it creaded an Array Out of Bounds error
           	* ToDo: Catch the Error and read out the titles of the items to use them along the magnets. 
           	* ToDo: Read out the content of the rss-items, so they can be shown alongside the magnet in the list (best in a tooltip). 
            */
            else if ( pos <= 6 && c == END_OF_ITEM_ELEMENT[pos] )
            {
            	pos ++;
                	if ( pos == END_OF_ITEM_ELEMENT.length )
                	{
                    	// the item ended. 
                    	
                    	
                    	pos = 0;
                    	return; 
                	}
            }
            /*
            **/
            else 
            {
            	pos = 0;  // didn't continue as magnet or enclosure tag, reset pos.
            }
            
		} //end of of while-loop
    }
    
    public void parseMagnet()
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
            else if ( c == '<' )
            {// found the end of the magnet. 
                break;
            }
            /**
            * only necessary when we are able to read out enclosures. 
            */
            else if ( c == '"' ) //"
            { // found the end of the magnet. 
                break;
            }
            else if ( c == -1 )
            {
                // unexpected end...
                return;
            }
            else if ( c == '&' )
            {
            char buff[] = new char[4];
    	            int readBytes = 0;
        	        while (readBytes != 4)
            	    {
                	    int count = reader.read(buff, readBytes, 4);
                    	if (count == -1)
	                    {
    	                    return;
        	            }
            	        readBytes += count;
                	}
    	            if (Arrays.equals(buff, AMPERSAND_AMP))
        	        {
            	        // reached quoted magnet 
            	        
            	        
                	magnetBuf.append( '&' );
	                }
	                else 
	                {
	                reader.unread(buff);
	                magnetBuf.append( (char)c );
	                }
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
    private void skipToEndOfObject() throws IOException
    {
    	// Only gets the next ending of any object. Could be improved to get 
    	// the end of a specific object supplied by the calling funktion. 
    	// At the moment ends either with "/>" or with "</"
        int c;
        while (true)
        {
            c = reader.read();
            if (c < 0)
            {// stream ended... a valid line could not be read... return
                return;
            }
            else if ( START_OF_ELEMENT_CHAR.indexOf( c ) != -1 )
            {// we found a possble end o the object... check if there are followups
            	c = reader.read();
            	if ( END_OF_ELEMENT_CHARN.indexOf( c ) != -1 )
            	{
            	
            	
                	return;
                }
                else {
                // the last character was no End of Element char... push it back
                reader.unread( c );
                }
            }
            else if ( END_OF_ELEMENT_CHARN.indexOf( c ) != -1 )
            {// we found a possble end o the object... check if there are followups
            	c = reader.read();
            	if ( END_OF_ELEMENT_CHAR.indexOf( c ) != -1 )
            	{
            	
            	
                	return;
                }
                else {
                // the last character was no End of Element char... push it back
                reader.unread( c );
                }
            }
        }
    }
}
