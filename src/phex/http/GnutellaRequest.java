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
 *  --- CVS Information ---
 *  $Id$
 */
package phex.http;

import phex.common.URN;
import phex.utils.URLCodecUtils;


/**
 * <p>
 * Parse the request uri of a HTTPRequest to retrieve the Gnutella request
 * information, like fileIndex, fileName or URN if available.
 * </p>
 */
public class GnutellaRequest
{
    public static final String GNUTELLA_GET_PREFIX = "/get/";
    public static final String GNUTELLA_URI_RES_PREFIX = "/uri-res/N2R?";
    public static final String GNUTELLA_URI_RES_THEX_PREFIX = "/uri-res/N2X?";

    /**
     * The Gnutella file index from '/get/1/foo.txt' like requests.
     */
    private int fileIndex;

    /**
     * The Gnutella file name from '/get/1/foo.txt' like requests.
     */
    private String filename;

    /**
     * The Gnutella URN from '/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB'
     * like requests.
     */
    private URN urn;
    
    /**
     * Indicates if this is a tiger tree urn request or not.
     */
    private boolean isTigerTreeRequest;


    public GnutellaRequest( URN requestURN, boolean isTigerTreeRequest )
    {
        urn = requestURN;
        fileIndex = -1;
        filename = null;
        this.isTigerTreeRequest = isTigerTreeRequest;
    }

    public GnutellaRequest( int fileIndex, String fileName )
    {
        this.fileIndex = fileIndex;
        this.filename = fileName;
        urn = null;
        isTigerTreeRequest = false;
    }

    /**
     * Get the name of the file to manipulate.
     *
     * @return the name of the file to fetch
     */
    public String getFileName()
    {
        return filename;
    }

    /**
     * Get the index of the file to manipulate. If the request did not specify
     * an index, then this will be -1
     *
     * @return the index of the file to fetch
     */
    public int getFileIndex()
    {
        return fileIndex;
    }

    /**
     * Get the urn of the requested file. If the request did not specify
     * an uri-res request, then this will be null
     *
     * @return the urn of the file to fetch
     */
    public URN getURN()
    {
        return urn;
    }

    public void setContentURN( URN contentURN )
    {
        urn = contentURN;
    }
    
    public boolean isTigerTreeRequest()
    {
        return isTigerTreeRequest;
    }

    /**
     * <p>Create a new RequestURI object from a request URI line.</p>
     * Possible request URI lines are:<br>
     * /get/1/foo.txt
     * /foo.txt
     * /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB
     *
     * @param requestURI the request URI to parse
     */
    public static GnutellaRequest parseGnutellaRequest( String requestURI )
    {
        // check if standard Gnutella get request
        int index = requestURI.indexOf( GNUTELLA_GET_PREFIX );
        if ( index != -1 )
        {// this is a traditional /get/(index)/(file) request
            index += 5;
            // find end of index
            int indexEndIdx = requestURI.indexOf( '/', index );

            String indexStr = requestURI.substring( index, indexEndIdx );
            int fileIndex;
            try
            {
                fileIndex = Integer.parseInt( indexStr );
            }
            catch ( NumberFormatException exp )
            {
                // we need to try check for file name instead for index
                fileIndex = -1;
            }
            String filename = URLCodecUtils.decodeURL( requestURI.substring( indexEndIdx + 1 ) );
            return new GnutellaRequest( fileIndex, filename );
        }

        // check if uri-res request
        index = requestURI.indexOf( GNUTELLA_URI_RES_PREFIX );
        if ( index != -1 )
        {
            //parse uri-res...
            index += 13;
            String urnStr = URLCodecUtils.decodeURL( requestURI.substring( index ) );
            URN urn = new URN( urnStr );
            return new GnutellaRequest( urn, false );
        }
        
        index = requestURI.indexOf( GNUTELLA_URI_RES_THEX_PREFIX );
        if (index != -1)
        {
            //parse uri-res...
            index += GNUTELLA_URI_RES_THEX_PREFIX.length();
            String urnStr = requestURI.substring(index);
            URN urn = new URN(urnStr);
            return new GnutellaRequest( urn, true );
        }
        return null;
    }
}