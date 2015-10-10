/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2009 Phex Development Group
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
package phex.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.collections.map.LinkedMap;

import phex.common.Environment;
import phex.common.Phex;

public class HTTPHeaderGroup
{
    public static final HTTPHeader[] EMPTY_HTTPHEADER_ARRAY = new HTTPHeader[0];

    /**
     * A empty HTTPHeaderGroup object. Don't modify this object!
     */
    public static final HTTPHeaderGroup EMPTY_HEADERGROUP;
    public static final String COLON_SEPARATOR = ": ";

    public static final HTTPHeaderGroup COMMON_HANDSHAKE_GROUP;
    public static final HTTPHeaderGroup ACCEPT_HANDSHAKE_GROUP;

    static
    {
        EMPTY_HEADERGROUP  = new EmptyHTTPHeaders();
        COMMON_HANDSHAKE_GROUP = new HTTPHeaderGroup( false );
        COMMON_HANDSHAKE_GROUP.addHeader( new HTTPHeader( HTTPHeaderNames.USER_AGENT,
            Phex.getFullPhexVendor() ) );

        ACCEPT_HANDSHAKE_GROUP = new HTTPHeaderGroup( COMMON_HANDSHAKE_GROUP );
        ACCEPT_HANDSHAKE_GROUP.addHeader( new HTTPHeader( GnutellaHeaderNames.GGEP,
            "0.5" ) );
        ACCEPT_HANDSHAKE_GROUP.addHeader( new HTTPHeader( GnutellaHeaderNames.VENDOR_MESSAGE,
            "0.1" ) );
        ACCEPT_HANDSHAKE_GROUP.addHeader( new HTTPHeader( GnutellaHeaderNames.X_REQUERIES,
            "false" ) );
        ACCEPT_HANDSHAKE_GROUP.addHeader( new HTTPHeader( GnutellaHeaderNames.PONG_CACHING,
            "0.1" ) );
         
        // TODO3 for testing only
        //COMMON_HANDSHAKE_HEADERS.addHeader( new HTTPHeader( "Bye-Packet", "0.1" ) );
    }

    protected Map<String, List<HTTPHeader>> headerFields;

    /**
     * States if the header names are converted to lower case to ensure a lenient
     * header field retrieval.
     */
    protected boolean lenient;

    /**
     *
     * @param lenient States if the header names are converted to lower case to
     * ensure a lenient header field retrieval.
     */
    public HTTPHeaderGroup( boolean lenient )
    {
        this.lenient = lenient;
        headerFields = new LinkedMap();
    }

    public HTTPHeaderGroup( HTTPHeaderGroup headers )
    {
        headerFields = new LinkedMap( headers.headerFields );
        lenient = headers.lenient;
    }

    public HTTPHeaderGroup( int size, boolean lenient )
    {
        this.lenient = lenient;
        headerFields = new LinkedMap( size );
    }
    
    private HTTPHeaderGroup()
    {
    }

    /**
     * Adds a header field. If lenient is set to true
     * the header name is lower cased for easier and lenient retrieval with
     * getHeaderField()
     *
     * @param header the header to add.
     */
    public void addHeader( HTTPHeader header )
    {
        String name;
        if ( lenient )
        {
            name = header.getName().toLowerCase();
        }
        else
        {
            name = header.getName();
        }

        List<HTTPHeader> values = headerFields.get( name );
        if (values == null)
        {
            values = new ArrayList<HTTPHeader>();
            headerFields.put( name, values );
        }
        values.add( header );
    }

    /**
     * Adds a header array. If lenient is set to true
     * the header name is lower cased for easier and lenient retrieval with
     * getHeaderField()
     *
     * @param headers the header array to add.
     */
    public void addHeaders( HTTPHeader[] headers )
    {
        String name;
        List<HTTPHeader> values;
        for ( int i = 0; i < headers.length; i++ )
        {
            if ( lenient )
            {
                name = headers[i].getName().toLowerCase();
            }
            else
            {
                name = headers[i].getName();
            }

            values = headerFields.get( name );
            if (values == null)
            {
                values = new ArrayList<HTTPHeader>();
                headerFields.put( name, values );
            }
            values.add( headers[i] );
        }
    }

    /**
     * Adds or replaces headers in this HTTPHeaderGroup with all header in the
     * given HTTPHeaderGroup.
     * If lenient is set to true the header name is lower cased for easier and
     * lenient retrieval with getHeaderField()
     *
     * @param headers the HTTPHeaderGroup to add.
     */
    public void replaceHeaders( HTTPHeaderGroup headers )
    {
        // both have same lenient state
        if ( lenient == headers.lenient )
        {
            headerFields.putAll( headers.headerFields );
        }
        else
        {
            String name;
            List<HTTPHeader> values;
            for( String key : headers.headerFields.keySet())
            {
                values = headerFields.get( key );
                name = values.get( 0 ).getName();
                if ( lenient )
                {
                    name = name.toLowerCase();
                }
                headerFields.put( name, values );
            }
        }
    }


    /**
     * Returns the header field value for the given name. If not available it
     * returns null. If the lenient flag is set the header name is converted to
     * lower case for retrieval.
     *
     * @param name the header field name.
     * @return the value of the header field.
     */
    public HTTPHeader getHeader( String name )
    {
        if ( lenient )
        {
            name = name.toLowerCase();
        }

        List<HTTPHeader> values = null;
        values = headerFields.get(name);
        if (values != null)
        {
            return values.get( 0 );
        }
        else
        {
            return null;
        }
    }

    /**
     * Returns all header field values for the given name. If not available it
     * returns an empty header array. If the lenient flag is set the header name 
     * is converted to lower case for retrieval.
     *
     * @param name the header field name.
     * @return the values of the header field.
     */
    public HTTPHeader[] getHeaders( String name )
    {
        if ( lenient )
        {
            name = name.toLowerCase();
        }

        List<HTTPHeader> values = null;
        values = headerFields.get(name);
        if (values == null)
        {
            return EMPTY_HTTPHEADER_ARRAY;
        }
        else
        {
            HTTPHeader results[] = new HTTPHeader[ values.size() ];
            return values.toArray( results );
        }
    }
    
    /**
     * Returns if the header value is equal (ignoring case) with the given
     * compareValue.
     * @param name the name of the header to compare the value from
     * @param compareValue the value to compare the header value with
     * @return true if the header value is equal (ignoring case) with the 
     * compareValue, false otherwise.
     */
    public boolean isHeaderValueEqual(String name, String compareValue)
    {
        HTTPHeader header = getHeader( name );
        if ( header == null )
        {
            return false;
        }
        
        String value = header.getValue();
        return compareValue.equalsIgnoreCase( value );
    }
    
    /**
     * Returns if the header is containing a value
     * (values are separated by ',') that is equal (ignoring case) to
     * compareValue.
     * @param name the name of the header to compare the value from
     * @param compareValue the value to compare the header value with
     * @return true is containing a value
     * (values are separated by ',') that is equal (ignoring case) to
     * compareValue. false otherwise.
     */
    public boolean isHeaderValueContaining(String name, String compareValue)
    {
        HTTPHeader header = getHeader( name );
        if ( header == null )
        {
            return false;
        }
        
        String value = header.getValue();
        if ( value == null )
        {
            return false;
        }
        
        // first check for a single equal value...
        if ( compareValue.equalsIgnoreCase( value ) )
        {
            return true;
        }
        // multiple values in a HTTP header are separated by ','
        // therefore we tokenize by ',' 
        StringTokenizer tokenizer = new StringTokenizer( value, "," );
        while( tokenizer.hasMoreTokens() )
        {
            String token = tokenizer.nextToken();
            if( compareValue.equalsIgnoreCase( token ) )
            {
                return true;
            }
        }
        
        return false;
    }

    
    /**
     * Returns the header value in byte if available and parseable, 
     * otherwise defaultValue is returned.
     * @param name the name of the header to look for
     * @param defaultValue the default value to return when the header
     * is not available or can not be parsed.
     * @return the header value in byte if available and parseable, 
     * otherwise defaultValue is returned.
     */
    public byte getByteHeaderValue( String name, byte defaultValue )
    {
        HTTPHeader header = getHeader( name );
        if ( header == null )
        {
            return defaultValue;
        }
        
        try
        {
            byte value = header.byteValue();
            return value;
        }
        catch ( NumberFormatException e)
        {
            return defaultValue;
        }
    }
    
    /**
     * Returns the header value in int if available and parseable, 
     * otherwise defaultValue is returned.
     * @param name the name of the header to look for
     * @param defaultValue the default value to return when the header
     * is not available or can not be parsed.
     * @return the header value in int if available and parseable, 
     * otherwise defaultValue is returned.
     */
    public int getIntHeaderValue( String name, int defaultValue )
    {
        HTTPHeader header = getHeader( name );
        if ( header == null )
        {
            return defaultValue;
        }
        
        try
        {
            int value = header.intValue();
            return value;
        }
        catch ( NumberFormatException e)
        {
            return defaultValue;
        }
    }

    public String buildHTTPHeaderString()
    {
        StringBuffer buffer = new StringBuffer( 30 * headerFields.size() );
        List<HTTPHeader> values;
        for( String key : headerFields.keySet() )
        {
            values = headerFields.get( key );
            for ( HTTPHeader header : values )
            {
                buffer.append( header.getName() );
                buffer.append( COLON_SEPARATOR );
                buffer.append( header.getValue() );
                buffer.append( HTTPRequest.CRLF );
            }
        }
        return buffer.toString();
    }

    private static class EmptyHTTPHeaders extends HTTPHeaderGroup
    {
        EmptyHTTPHeaders()
        {
            super( );
            headerFields = Collections.emptyMap();
            lenient = false;
        }

        /**
         * Operation not supported
         */
        public void addHeader( HTTPHeader header )
        {
            throw new UnsupportedOperationException(
                "Modification of empty http headers not allowed" );
        }

        /**
         * Operation not supported
         */
        public void addHeaders( HTTPHeader[] headers )
        {
            throw new UnsupportedOperationException(
                "Modification of empty http headers not allowed" );
        }

        /**
         * Always returns null.
         */
        public HTTPHeader getHeader( String name )
        {
            return null;
        }

        /**
         * Always returns empty http header array.
         */
        public HTTPHeader[] getHeaders( String name )
        {
            return EMPTY_HTTPHEADER_ARRAY;
        }

        /**
         * Returns a empty string.
         * @return a empty string.
         */
        public String buildHTTPHeaderString()
        {
            return "";
        }
    }

    public static HTTPHeaderGroup createDefaultRequestHeaders()
    {
        HTTPHeaderGroup headers = new HTTPHeaderGroup( false );
        headers.addHeader( new HTTPHeader( HTTPHeaderNames.USER_AGENT, Phex.getFullPhexVendor() ) );
        return headers;
    }

    public static HTTPHeaderGroup createDefaultResponseHeaders()
    {
        HTTPHeaderGroup headers = new HTTPHeaderGroup( false );
        headers.addHeader( new HTTPHeader( HTTPHeaderNames.SERVER, Phex.getFullPhexVendor() ) );
        return headers;
    }
}