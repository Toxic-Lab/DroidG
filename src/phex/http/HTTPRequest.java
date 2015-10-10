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
package phex.http;

import phex.common.URN;

public class HTTPRequest
{
    public static final String GET_REQUEST = "GET";
    public static final String HEAD_REQUEST = "HEAD";
    public static final String HTTP_11 = "HTTP/1.1";

    public static final String CRLF = "\r\n";
    private static final char SP = ' ';

    /**
     * The request method of the http request.
     */
    private String requestMethod;

    /**
     * The request uri of the http request.
     */
    private String requestURI;

    /**
     * The http version information.
     */
    private String httpVersion;

    /**
     * The GnutellaRequest object if it was parsed out of the request URI. If
     * the request was parsed but remains null, the request uri is no Gnutella
     * request uri.
     */
    private GnutellaRequest gnutellaRequest;
    
    /**
     * Indicates whether the request uri was tried to be parsed into a GnutellaRequest.
     */
    private boolean isGnutellaRequestParsed;

    // parsed header fields
    private int contentLength;
    private String hostName;
    private int hostPort;

    /**
     * The http headers of the request.
     */
    private HTTPHeaderGroup httpHeaders;

    public HTTPRequest( String aRequestMethod, String aRequestURI,
        boolean isOutgoing )
    {
        this( aRequestMethod, aRequestURI, "HTTP/1.1", isOutgoing );
    }

    /**
     * For outgoing HTTP requests it is assumed that HTTP headers are not treatend
     * lenient, since we care for the correct header case. On incomming
     * (isOutgoing == false) we like to treat headers lenient to ignore case
     * differencies.
     *
     * @param aRequestMethod
     * @param aRequestURI
     * @param aHttpVersion
     * @param isOutgoing For outgoing HTTP requests it is assumed that HTTP
     * headers are not treatend lenient, since we care for the correct header
     * case. On incomming (isOutgoing == false) we like to treat headers lenient
     * to ignore case differencies.
     */
    public HTTPRequest( String aRequestMethod, String aRequestURI,
        String aHttpVersion, boolean isOutgoing )
    {
        requestMethod = aRequestMethod;
        requestURI = aRequestURI;
        httpVersion = aHttpVersion;
        if ( isOutgoing )
        {
            httpHeaders = HTTPHeaderGroup.createDefaultRequestHeaders();
        }
        else
        {
            httpHeaders = new HTTPHeaderGroup( true );
        }
        isGnutellaRequestParsed = false;
    }

    public void setContentLength( int length, boolean addHeader )
    {
        contentLength = length;
        if ( addHeader )
        {
            httpHeaders.addHeader( new HTTPHeader( HTTPHeaderNames.CONTENT_LENGTH,
                String.valueOf( length ) ) );
        }
    }

    public void setHost( String server, int port, boolean addHeader )
    {
        hostName = server;
        hostPort = port;
        if ( addHeader )
        {
            int bufLength = hostName.length() + 5;
            StringBuffer buffer = new StringBuffer( bufLength );
            buffer.append( hostName );
            if ( hostPort > 0 )
            {
                buffer.append( ':' );
                buffer.append( String.valueOf( hostPort ) );
            }
            httpHeaders.addHeader( new HTTPHeader( HTTPHeaderNames.HOST,
                buffer.toString() ) );
        }
    }

    /**
     * Adds a http header.
     * @param name the name of the header field.
     * @param value the value of the header field.
     */
    public void addHeader( HTTPHeader header )
    {
        httpHeaders.addHeader( header );
    }

    /**
     * Adds a http header array.
     * @param headers a HTTPHeader array to add.
     */
    public void addHeaders( HTTPHeader[] headers )
    {
        httpHeaders.addHeaders( headers );
    }

    /**
     * Returns the header field for the given name. If not available it
     * returns null.
     * @param name the header field name.
     * @return the value of the header field.
     */
    public HTTPHeader getHeader( String name )
    {
        return httpHeaders.getHeader( name );
    }

    /**
     * Returns all header fields for the given name. If not available it
     * returns null. If the lenient flag is set the header name is converted to
     * lower case for retrival.
     *
     * @param name the header field name.
     * @return the values of the header field.
     */
    public HTTPHeader[] getHeaders( String name )
    {
        return httpHeaders.getHeaders( name );
    }

    /**
     * Returns request method.
     * @return request method.
     */
    public String getRequestMethod()
    {
        return requestMethod;
    }
    
    public boolean isHeadRequest()
    {
        return HEAD_REQUEST.equals( requestMethod );
    }

    /**
     * Returns the URI of the request.
     * @return the URI of the request.
     */
    public String getRequestURI()
    {
        return requestURI;
    }

    public String getHTTPVersion()
    {
        return httpVersion;
    }

    /**
     * Returns the GnutellaRequest that is represented by the request uri.
     * If the request is no GnutellaRequest null is returned.
     * @return the GnutellaRequest that is represented by the request uri.
     * If the request is no GnutellaRequest null is returned.
     */
    public GnutellaRequest getGnutellaRequest()
    {
        if ( !isGnutellaRequestParsed )
        {
            parseGnutellaRequest();
        }
        return gnutellaRequest;
    }
		
    public boolean isGnutellaRequest()
    {
        if ( !isGnutellaRequestParsed )
        {
            parseGnutellaRequest();
        }
        return gnutellaRequest != null;
    }
    
    public String buildHTTPRequestString()
    {
        return requestMethod + SP + requestURI + SP + httpVersion + CRLF
            + httpHeaders.buildHTTPHeaderString() + CRLF;
    }
    
    private void parseGnutellaRequest()
    {
        gnutellaRequest = GnutellaRequest.parseGnutellaRequest( requestURI );
        if ( gnutellaRequest != null && gnutellaRequest.getURN() == null)
        {
            // learn from http headers if possible...
            HTTPHeader header = getHeader( 
                GnutellaHeaderNames.X_GNUTELLA_CONTENT_URN );
            if (header != null)
            {
                if (URN.isValidURN(header.getValue()))
                {
                    URN urn = new URN(header.getValue());
                    gnutellaRequest.setContentURN(urn);
                }
            }
        }
        isGnutellaRequestParsed = true;
    }
}