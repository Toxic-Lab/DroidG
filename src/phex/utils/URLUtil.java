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
 */
package phex.utils;


import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import phex.common.URN;
import phex.common.address.DestAddress;
import phex.download.RemoteFile;
import phex.http.GnutellaRequest;


public class URLUtil
{
    
    
    /**
     * Returns a url to look up a URN over bitzi.com.
     * @param urn the URN to look up.
     * @return the bitzi url.
     */
    public static String buildBitziLookupURL( URN urn )
    {
        String url = "http://bitzi.com/lookup/" +
            urn.getNamespaceSpecificString() + "?detail&ref=phex";
        return url;
    }

    /**
     * Returns a magnet URL in the format:
     * magnet:?xt=urn:sha1:<sha1NSS>&dn=<filename>
     * @param sha1NSS the sha1 of the file.
     * @param filename the filename
     * @return the magnet URL string.
     */
    public static String buildMagnetURL( String sha1NSS, String filename )
    {
        String url = "magnet:?xt=urn:sha1:" + sha1NSS + "&dn="
            + URLCodecUtils.encodeURL( filename );
        return url;
    }
    
    public static String buildMagnetURLWithXS( String sha1NSS, String filename,
        DestAddress ha )
    {
        String url = "magnet:?xt=urn:sha1:" + sha1NSS + 
            "&dn=" + URLCodecUtils.encodeURL( filename ) +
            "&xs=" + buildHostURL( ha ) + GnutellaRequest.GNUTELLA_URI_RES_PREFIX + "urn:sha1:" + sha1NSS;
        return url;
    }

    public static String buildFileURL( RemoteFile file )
    {
        return "http://" + file.getHostAddress().getFullHostName() + "/get/"
            + file.getFileIndex() + "/" + URLCodecUtils.encodeURL( file.getFilename() );
    }

    public static String buildName2ResourceURL( URN urn )
    {
        return GnutellaRequest.GNUTELLA_URI_RES_PREFIX + urn.getAsString();
    }
    
    public static String buildName2ResThexURL( URN urn, String tigerTreeRoot )
    {
        return GnutellaRequest.GNUTELLA_URI_RES_THEX_PREFIX + urn.getAsString()
            + ";" + tigerTreeRoot;
    }

    public static String buildFullName2ResourceURL( DestAddress ha, URN urn )
    {
        return buildHostURL( ha ) + GnutellaRequest.GNUTELLA_URI_RES_PREFIX + urn.getAsString();
    }

    public static String buildHostURL( DestAddress address )
    {
        return "http://" + address.getFullHostName();
    }
    
    public static String getFileNameFromUri( URI uri )
        throws URIException
    {
        String path;
        path = uri.getPath();
        if ( path == null )
        {
            return uri.getHost();
        }
        int at = path.lastIndexOf("/");
        int to = path.length();
        return (at >= 0) ? path.substring(at + 1, to) : path;
    }
    
    public static String getPathQueryFromUri( URI uri )
        throws URIException
    {
        String path;
        String query;
        path = uri.getPath();
        query = uri.getQuery();
        
        uri.getCurrentHierPath();

        if ( query != null && query.length() > 0 )
        {
            return path + "?" + query;
        }
        
        if ( path == null )
        {
            return "/";
        }
        else
        {
            return path;
        }
    }
    
    /**
     * Tries to identify a URN in the query part of the uri.
     * @param uri
     * @return the urn if found otherwise null
     * @throws URIException
     */
    public static URN getQueryURN( URI uri )
        throws URIException
    {
        String query = uri.getQuery();
        if ( query != null && query.length() > 0 )
        {
            if ( URN.isValidURN(query) )
            {
                return new URN( query );
            }
        }
        return null;
    }
}