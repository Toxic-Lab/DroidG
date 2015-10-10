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
package phex.share;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import phex.common.Phex;
import phex.common.address.DestAddress;
import phex.common.format.NumberFormatUtils;
import phex.http.HTTPHeader;
import phex.http.HTTPHeaderGroup;
import phex.http.HTTPHeaderNames;
import phex.http.HTTPRequest;
import phex.http.HTTPResponse;
import phex.io.buffer.ByteBuffer;
import phex.msg.GUID;
import phex.msg.MsgHeader;
import phex.msg.QueryResponseMsg;
import phex.msg.QueryResponseRecord;
import phex.net.connection.Connection;
import phex.prefs.core.BandwidthPrefs;
import phex.prefs.core.LibraryPrefs;
import phex.security.PhexSecurityManager;
import phex.servent.Servent;
import phex.share.export.ExportEngine;
import phex.statistic.SimpleStatisticProvider;
import phex.statistic.StatisticsManager;
import phex.utils.StringUtils;

public class HttpRequestDispatcher
{
    // Called by ReadWorker to handle a HTTP GET request from the remote host.
    public void httpRequestHandler(Connection connection,
        HTTPRequest httpRequest)
    {
        try
        {
            // GET / HTTP/1.1 (Browse Host request)
            if ( httpRequest.getRequestMethod().equals( "GET" ) )
            {
                String requestURI = httpRequest.getRequestURI();
                if ( requestURI.equals( "/" ) )
                {
                    // The remote host just want the index.html.
                    // Return a list of shared files.
                    sendFileListing( httpRequest, connection );
                    return;
                }
                else if ( requestURI.equals( "/robots.txt" ) )
                {// this appears to be a lost search engine... reject it...
                    HTTPResponse response = new HTTPResponse( (short)200, "OK", true );
                    response.addHeader( new HTTPHeader(HTTPHeaderNames.CONNECTION, "close") );
                    response.addHeader( new HTTPHeader(HTTPHeaderNames.CONTENT_TYPE, "text/plain") );
                    response.addHeader( new HTTPHeader(HTTPHeaderNames.CONTENT_LENGTH, "") );
                    String httpData = response.buildHTTPResponseString();
                    String robotsText = "User-agent: *\r\nDisallow: /";
                    connection.write( ByteBuffer.wrap( 
                        StringUtils.getBytesInUsAscii( httpData ) ) );
                    connection.write( ByteBuffer.wrap( 
                        StringUtils.getBytesInUsAscii( robotsText ) ) );
                    return;
                }
            }
        
            sendErrorHTTP( connection, "404 Not Found",
                "File not found." );
        }
        catch (IOException exp)
        {
            //NLogger.debug(HttpRequestDispatcher.class, exp, exp);
        }
    }

    private void sendErrorHTTP( Connection connection, String statusStr, String errMsg)
        throws IOException
    {
        StringBuffer content = new StringBuffer( 300 );
        content.append( "<html><head><title>PHEX</title></head><body>" );
        content.append( errMsg );
        content.append( "<hr>Visit the Phex website at " );
        content.append( "<a href=\"http://phex.sourceforge.net\">http://phex.sourceforge.net</a>." );
        content.append( "</body>" );
        content.append( "</html>" );

        StringBuffer buf = new StringBuffer( 300 );
        buf.append( "HTTP/1.1 " ).append( statusStr ).append( HTTPRequest.CRLF );
        buf.append( "Server: " ).append( Phex.getFullPhexVendor() ).append( HTTPRequest.CRLF );
        buf.append( "Connection: close" ).append( HTTPRequest.CRLF );
        buf.append( "Content-Type: text/plain" ).append( HTTPRequest.CRLF );
        buf.append( "Content-Length: " ).append( content.length() ).append( HTTPRequest.CRLF );
        buf.append( "\r\n" );
        
        connection.write( ByteBuffer.wrap( 
            StringUtils.getBytesInUsAscii( buf.toString() ) ) );
        
        connection.write( ByteBuffer.wrap( 
            StringUtils.getBytesInUsAscii( content.toString() ) ) );
    }

    private void sendFileListing(HTTPRequest httpRequest, Connection connection)
        throws IOException
    {        
        if ( !LibraryPrefs.AllowBrowsing.get().booleanValue() )
        {
            HTTPHeaderGroup headers = HTTPHeaderGroup.createDefaultResponseHeaders();
            String response = createHTTPResponse( "403 Browsing disabled", headers );
            connection.write( ByteBuffer.wrap( 
                StringUtils.getBytesInUsAscii( response ) ) );
            connection.flush();
            connection.disconnect();
            return;
        }
        
        HTTPHeader acceptHeader = httpRequest.getHeader( "Accept" );
        if ( acceptHeader == null )
        {
            HTTPHeaderGroup headers = HTTPHeaderGroup.createDefaultResponseHeaders();
            String response = createHTTPResponse( "406 Not Acceptable", headers );
            connection.write( ByteBuffer.wrap( 
                StringUtils.getBytesInUsAscii( response ) ) );
            connection.flush();
            connection.disconnect();
            return;
        }
        Servent servent = Servent.getInstance();
        String acceptHeaderStr = acceptHeader.getValue();
        if ( acceptHeaderStr.indexOf( "application/x-gnutella-packets" ) != -1 )
        {// return file listing via gnutella packages...
            HTTPHeaderGroup headers = HTTPHeaderGroup.createDefaultResponseHeaders();
            headers.addHeader( new HTTPHeader( HTTPHeaderNames.CONTENT_TYPE,
                "application/x-gnutella-packets" ) );
            headers.addHeader( new HTTPHeader( HTTPHeaderNames.CONNECTION,
                "close" ) );
            String response = createHTTPResponse( "200 OK", headers );
            connection.write( ByteBuffer.wrap( 
                StringUtils.getBytesInUsAscii( response ) ) );
            connection.flush();

            // now send QueryReplys...
            PhexSecurityManager securityService = servent.getSecurityService();
            List<ShareFile> shareFiles = servent.getSharedFilesService().getSharedFiles();
            ArrayList<ShareFile> eligibleShareFiles = new ArrayList<ShareFile>();
            try {
                for (ShareFile shareFile : shareFiles) {
                    SharedDirectory dir = servent.getSharedFilesService().getSharedDirectory(shareFile.getSystemFile().getParentFile());
                    if (!securityService.isEligibleIpAddress(connection.getSocket().getRemoteAddress().getIpAddress().getHostIP(), dir))
                        continue;
                    eligibleShareFiles.add(shareFile);
                }
            }
            catch (Exception exp)
            {
                //NLogger.warn( HttpRequestDispatcher.class, exp, exp );
            }
            shareFiles = eligibleShareFiles;

            MsgHeader header = new MsgHeader( new GUID(),
                MsgHeader.QUERY_HIT_PAYLOAD, (byte) 2, (byte) 0, -1 );

            QueryResponseRecord record;
            ShareFile sfile;
            int sendCount = 0;
            int toSendCount = shareFiles.size();
            while ( sendCount < toSendCount )
            {
                int currentSendCount = Math.min( 255, toSendCount - sendCount );
                QueryResponseRecord[] records = new QueryResponseRecord[currentSendCount];
                for (int i = 0; i < currentSendCount; i++)
                {
                    sfile = shareFiles.get( sendCount + i );
                    record = QueryResponseRecord.createFromShareFile( sfile,
                        servent.getLocalAddress() );
                    records[i] = record;
                }

                DestAddress hostAddress = servent.getLocalAddress();
                QueryResponseMsg queryResponse = new QueryResponseMsg( header,
                    servent.getServentGuid(), hostAddress,
                    Math.round( BandwidthPrefs.MaxUploadBandwidth.get().floatValue()
                        / NumberFormatUtils.ONE_KB ), 
                    records, servent.getHostService().getNetworkHostsContainer().getPushProxies(),
                    !servent.isFirewalled(), servent.isUploadLimitReached() );

                // send msg over the wire 
                ByteBuffer headerBuf = queryResponse.createHeaderBuffer();
                connection.write( headerBuf );
                ByteBuffer messageBuf = queryResponse.createMessageBuffer();
                connection.write( messageBuf );

                // and count message
                ((SimpleStatisticProvider)servent.getStatisticsService().getStatisticProvider( 
                    StatisticsManager.QUERYMSG_OUT_PROVIDER )).increment( 1 );

                sendCount += currentSendCount;
            }
            connection.flush();
        }
        else if ( acceptHeaderStr.indexOf( "text/html" ) != -1
            || acceptHeaderStr.indexOf( "*/*" ) != -1 )
        {// return file listing via html page...
            HTTPHeaderGroup headers = HTTPHeaderGroup.createDefaultResponseHeaders();
            headers.addHeader( new HTTPHeader( HTTPHeaderNames.CONTENT_TYPE,
                "text/html; charset=iso-8859-1" ) );
            headers.addHeader( new HTTPHeader( HTTPHeaderNames.CONNECTION,
                "close" ) );
            String response = createHTTPResponse( "200 OK", headers );
            connection.write( ByteBuffer.wrap( 
                StringUtils.getBytesInUsAscii( response ) ) );
            connection.flush();

            // now send html
            ExportEngine exportEngine = new ExportEngine( 
                servent.getLocalAddress(),
                connection.getOutputStream(),
                servent.getSharedFilesService().getSharedFiles() );
            exportEngine.startExport();
            
            connection.flush();
        }
        // close connection as indicated in the header
        connection.disconnect();
    }

    private String createHTTPResponse(String code, HTTPHeaderGroup header)
    {
        StringBuffer buffer = new StringBuffer( 100 );
        buffer.append( "HTTP/1.1 " );
        buffer.append( code );
        buffer.append( "\r\n" );
        buffer.append( header.buildHTTPHeaderString() );
        buffer.append( "\r\n" );
        return buffer.toString();
    }

    
}