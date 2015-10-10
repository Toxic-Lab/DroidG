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

import java.io.IOException;
import java.net.SocketException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import phex.net.connection.Connection;


public class HTTPProcessor
{
    private static final char SP = ' ';
    private static final char HT = '\t';
    
    /**
     * 
     */
    private HTTPProcessor()
    {
    }

    /**
     * Parse the incoming HTTP request and set the corresponding HTTP request
     * properties.
     *
     * Code taken and modified from:
     * http://cvs.apache.org/viewcvs.cgi/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/connector/http10/HttpProcessor.java?rev=1.9&content-type=text/vnd.viewcvs-markup
     * @throws IOException when an IO error occurs
     * @throws HTTPMessageException when an error occurs while parsing the request.
     */
    public static HTTPRequest parseHTTPRequest( Connection connection )
        throws IOException, HTTPMessageException
    {
        // Parse the incoming request line
        String line = connection.readLine();
        if (line == null)
        {
            throw new IOException( "Stream unexpectedly ended." );
        }
        HTTPRequest request = parseHTTPRequest( line );
        parseHTTPHeaders( request, connection );
        return request;
    }

    public static HTTPRequest parseHTTPRequest( String requestLine )
        throws HTTPMessageException
    {
        StringTokenizer st = new StringTokenizer( requestLine );

        String method = null;
        try
        {
            method = st.nextToken();
        }
        catch (NoSuchElementException e)
        {
            // ignore, evalution is below
        }

        String uri = null;
        try
        {
            uri = st.nextToken();
            // TODO - URL decode the URI?
        } 
        catch (NoSuchElementException e)
        {
            // ignore, evalution is below
        }

        String protocol = null;
        try
        {
            protocol = st.nextToken();
        }
        catch (NoSuchElementException e)
        {
            protocol = "HTTP/0.9";
        }

        // Validate the incoming request line
        if (method == null)
        {
            throw new HTTPMessageException( "HTTPRequest has no method." );
        }
        else if (uri == null)
        {
            throw new HTTPMessageException( "HTTPRequest has no URI." );
        }

        // Parse any query parameters out of the request URI
/*        int question = uri.indexOf('?');
        if (question >= 0) {
            request.setQueryString(uri.substring(question + 1));
            if (debug >= 1)
                log(" Query string is " +
                    ((HttpServletRequest) request.getRequest()).getQueryString());
            uri = uri.substring(0, question);
        } else
            request.setQueryString(null);

        // Parse any requested session ID out of the request URI
        int semicolon = uri.indexOf(match);
        if (semicolon >= 0) {
            String rest = uri.substring(semicolon + match.length());
            int semicolon2 = rest.indexOf(';');
            if (semicolon2 >= 0) {
                request.setRequestedSessionId(rest.substring(0, semicolon2));
                rest = rest.substring(semicolon2);
            } else {
                request.setRequestedSessionId(rest);
                rest = "";
            }
            request.setRequestedSessionURL(true);
            uri = uri.substring(0, semicolon) + rest;
            if (debug >= 1)
                log(" Requested URL session id is " +
                    ((HttpServletRequest) request.getRequest()).getRequestedSessionId());
        } else {
            request.setRequestedSessionId(null);
            request.setRequestedSessionURL(false);
        }*/

        // Set the corresponding request properties
        HTTPRequest request = new HTTPRequest( method, uri, protocol, false );
        return request;
    }

    public static HTTPResponse parseHTTPResponse( Connection connection )
        throws IOException, HTTPMessageException
    {
        // Parse the incoming request line
        String line = connection.readLine();
        if (line == null)
        {
            throw new SocketException( "Stream unexpectedly ended." );
        }
        line = line.trim();

        int firstIdx = line.indexOf( ' ' );
        String httpVersion = null;
        try
        {
            httpVersion = line.substring( 0, firstIdx );
        }
        catch ( IndexOutOfBoundsException e )
        {
            httpVersion = null;
        }

        int secondIdx = -1;
        String statusCodeStr = null;
        try
        {
            secondIdx = line.indexOf( ' ', firstIdx + 1 );
            if ( secondIdx == -1 )
            {
                secondIdx = line.length();
            }
            statusCodeStr = line.substring( firstIdx + 1, secondIdx );
        }
        catch ( IndexOutOfBoundsException e )
        {
            statusCodeStr = null;
        }

        String statusReason = null;
        secondIdx ++;
        if ( secondIdx < line.length() )
        {
            try
            {
                statusReason = line.substring( secondIdx, line.length() );
            }
            catch ( IndexOutOfBoundsException e )
            {
                statusReason = "";
            }
        }
        else
        {
            statusReason = "";
        }

        // Validate the incoming request line
        if ( httpVersion == null)
        {
            throw new HTTPMessageException( "HTTP response has no version: " + line );
        }
        else if ( statusCodeStr == null )
        {
            throw new HTTPMessageException( "HTTP response has no status code: " + line );
        }
        else if ( statusCodeStr.length() != 3 )
        {
            throw new HTTPMessageException( "HTTP response status code has invalid lenth: "
                + statusCodeStr + " Line: " + line);
        }

        short statusCode = -1;
        try
        {
            statusCode = Short.parseShort( statusCodeStr );
        }
        catch ( NumberFormatException exp )
        {
            throw new HTTPMessageException( "Status code of HTTP response is not valid: "
                + statusCodeStr );
        }

        // Set the corresponding request properties
        HTTPResponse response = new HTTPResponse( httpVersion, statusCode,
            statusReason, false );
        parseHTTPHeaders( response, connection );
        return response;
    }

    public static HTTPHeaderGroup parseHTTPHeaders( Connection connection )
        throws IOException
    {
        HTTPHeaderGroup headers = new HTTPHeaderGroup( true );
        while ( true )
        {
            HTTPHeader header = parseHTTPHeader( connection );
            if ( header == null )
            {
                break;
            }
            headers.addHeader( header );
        }
        return headers;
    }

    /**
     * Parse the incoming HTTP request headers, and set the appropriate
     * request headers.
     * @throws IOException in case an IO error occurs
     */
    public static void parseHTTPHeaders( HTTPRequest httpRequest,
        Connection connection )
        throws IOException
    {
        // Some code parts taken and modified from:
        // http://cvs.apache.org/viewcvs.cgi/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/connector/http10/HttpProcessor.java

        String match;
        while ( true )
        {
            HTTPHeader header = parseHTTPHeader( connection );
            if ( header == null )
            {
                break;
            }
            match = header.getName().toLowerCase();
            if ( match.equals("content-length") )
            {
                int n = -1;
                try
                {
                    n = Integer.parseInt( header.getValue() );
                }
                catch ( NumberFormatException e )
                {
                    throw new IOException( "Error parsing content-length: " +
                        header.getName() + " - " + header.getValue() );
                }
                httpRequest.setContentLength( n, false );
                httpRequest.addHeader( header );
            }
            /*else if ( match.equals( "content-type" ) )
            {
                httpRequest.setContentType(value);
                httpRequest.addHeaderField(name, value);
            }*/
            else if ( match.equals( "host" ) )
            {
                int n = header.getValue().indexOf(':');
                if (n < 0)
                {
                    httpRequest.setHost( header.getValue(), -1, false );
                }
                else
                {
                    int port = -1;
                    try
                    {
                        port = Integer.parseInt( header.getValue().substring(n+1).trim() );
                    }
                    catch ( NumberFormatException e )
                    {
                        throw new IOException( "Error parsing host: " +
                            header.getName() + " - " + header.getValue() );
                    }
                    httpRequest.setHost( header.getValue().substring(0, n).trim(), port, false );
                }
                httpRequest.addHeader( header );
            }
            else
            {
                httpRequest.addHeader( header );
            }
        }
    }

    /**
     * Parse the incoming HTTP response headers, and set the appropriate
     * request headers.
     * @throws IOException in case an IO error occurs
     */
    private static void parseHTTPHeaders(HTTPResponse httpRequest,
        Connection connection )
        throws IOException
    {
        while ( true )
        {
            HTTPHeader header = parseHTTPHeader( connection );
            if ( header == null )
            {
                break;
            }
            httpRequest.addHeader( header );
        }
    }

    /**
     * Returns null if not headers are available...
     * @throws IOException in case an IO error occurs
     */
    private static HTTPHeader parseHTTPHeader( Connection connection )
        throws IOException
    {

        // Some code parts taken and modified from:
        // http://cvs.apache.org/viewcvs.cgi/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/connector/http10/HttpProcessor.java
        // http://cvs.apache.org/viewcvs.cgi/jakarta-tomcat-4.0/catalina/src/share/org/apache/catalina/connector/http/SocketInputStream.java

        // Read the next header line
        String line = connection.readLine( );
        if ( (line == null) || (line.length() < 1) )
        {
            return null;
        }

        // Parse the header name and value
        int colon = line.indexOf(':');
        if (colon < 0)
        {
            throw new IOException( "Invalid HTTP headers: " + line );
        }
        String name = line.substring( 0, colon ).trim();

        StringBuffer valueBuffer = new StringBuffer(
            line.substring( colon + 1 ).trim() );

        // peek on stream to check if the value might continue on the next line.
        char c = (char) connection.readPeek();
        while ( ( c == SP ) || ( c == HT) )
        {// value continues on next line... read next line...
            line = connection.readLine( );
            // trim leading but not trailing SP's and HT's
            int length = line.length();
            int st = 0;
            char[] val = line.toCharArray();
            while ( (st < length) && ( val[st] == SP || val[st] == HT ) )
            {
                st++;
            }
            if ( st > 0 )
            {
                line = line.substring( st, length );
            }
            valueBuffer.ensureCapacity( line.length() + 1 );
            valueBuffer.append( ' ' );
            valueBuffer.append( line );
            // peek on stream to check if the value might continue on the next line.
            c = (char) connection.readPeek();
        }
        String value = valueBuffer.toString();
        return new HTTPHeader( name, value );
    }
}