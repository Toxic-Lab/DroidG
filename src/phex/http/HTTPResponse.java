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
package phex.http;

public class HTTPResponse
{
    public static final String CRLF = "\r\n";
    private static final char SP = ' ';

    private String httpVersion;
    private short statusCode;
    private String statusReason;

    /**
     * The http headers of the response.
     */
    private HTTPHeaderGroup httpHeaders;

    /**
     * For outgoing HTTP responses it is assumed that HTTP headers are not treatend
     * lenient, since we care for the correct header case. On incomming
     * (isOutgoing == false) we like to treat headers lenient to ignore case
     * differencies.
     *
     * @param aRequestMethod
     * @param aRequestURI
     * @param isOutgoing For outgoing HTTP responses it is assumed that HTTP
     * headers are not treatend lenient, since we care for the correct header
     * case. On incomming (isOutgoing == false) we like to treat headers lenient
     * to ignore case differencies.
     */
    public HTTPResponse( short aStatusCode, String aStatusReason,
        boolean isOutgoing )
    {
        this( "HTTP/1.1", aStatusCode, aStatusReason, isOutgoing );
    }

    /**
     * For outgoing HTTP responses it is assumed that HTTP headers are not treatend
     * lenient, since we care for the correct header case. On incomming
     * (isOutgoing == false) we like to treat headers lenient to ignore case
     * differencies.
     *
     * @param aRequestMethod
     * @param aRequestURI
     * @param aHttpVersion
     * @param isOutgoing For outgoing HTTP responses it is assumed that HTTP
     * headers are not treatend lenient, since we care for the correct header
     * case. On incomming (isOutgoing == false) we like to treat headers lenient
     * to ignore case differencies.
     */
    public HTTPResponse( String aHTTPVersion, short aStatusCode,
        String aStatusReason, boolean isOutgoing )
    {
        httpVersion = aHTTPVersion;
        statusCode = aStatusCode;
        statusReason = aStatusReason;
        if ( isOutgoing )
        {
            httpHeaders = HTTPHeaderGroup.createDefaultResponseHeaders();
        }
        else
        {
            httpHeaders = new HTTPHeaderGroup( false );
        }
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getStatusReason()
    {
        return statusReason;
    }

    public String getHTTPVersion()
    {
        return httpVersion;
    }

    /**
     * Adds a http header.
     * @param header the a HTTPHeader to add.
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
     * Returns all header field values for the given name. If not available it
     * returns an empty header array. If the lenient flag is set the header name 
     * is converted to lower case for retrival.
     *
     * @param name the header field name.
     * @return the values of the header field.
     */
    public HTTPHeader[] getHeaders( String name )
    {
        return httpHeaders.getHeaders( name );
    }

    public String buildHTTPResponseString()
    {
        return httpVersion + SP + statusCode + SP + statusReason + CRLF
            + httpHeaders.buildHTTPHeaderString() + CRLF;
    }
}