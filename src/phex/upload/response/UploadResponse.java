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
package phex.upload.response;

import java.io.IOException;

import phex.common.AltLocContainer;
import phex.common.address.DestAddress;
import phex.http.GnutellaHeaderNames;
import phex.http.HTTPCodes;
import phex.http.HTTPHeader;
import phex.http.HTTPResponse;
import phex.http.XQueueParameters;
import phex.io.buffer.ByteBuffer;
import phex.share.ShareFile;
import phex.upload.UploadState;

public class UploadResponse
{
    protected HTTPResponse httpResponse;

    public UploadResponse( HTTPResponse httpResponse )
    {
        this.httpResponse = httpResponse;
    }
    
    protected UploadResponse()
    {
    }
    
    @SuppressWarnings("unused")
    public int fillBody( ByteBuffer byteBuffer )
        throws IOException
    {
        throw new UnsupportedOperationException( "No Data." );
    }
    
    public int remainingBody()
    {
        return 0;
    }
    
    public void countUpload()
    {
    }
    
    public void close()
    {        
    }

    
    /////// Decorated HTTPResponse methods ///////
    public void addHttpHeader( HTTPHeader header )
    {
        httpResponse.addHeader( header );
    }
    
    public String buildHTTPResponseString()
    {
        return httpResponse.buildHTTPResponseString();
    }
    /////////////////////////////////////////////
    
    
    
    
    /////// Static factory methods to help create common response headers ///////

    public static final UploadResponse get404FileNotFound()
    {
        HTTPResponse response = new HTTPResponse( (short)HTTPCodes.HTTP_404_Not_Found, 
            "File not found", true );
        return new UploadResponse( response );
    }
    
    public static final UploadResponse get500RangeNotParseable( 
        ShareFile shareFile, UploadState uploadState )
    {
        HTTPResponse response = new HTTPResponse( (short)HTTPCodes.HTTP_500_Internal_Server_Error,
            "Requested Range Not Parseable", true);
        
        UploadResponse uploadResponse = new UploadResponse( response );

        // append alt locs
        appendAltLocs( uploadResponse, shareFile, uploadState );
        
        return new UploadResponse( response );
    }
    
    public static final UploadResponse get503Queued( int queuePosition, int queueLength,
        int uploadLimit, int pollMin, int pollMax, ShareFile shareFile, 
        UploadState uploadState )
    {
        HTTPResponse response = new HTTPResponse((short)HTTPCodes.HTTP_503_Service_Unavailable,
            "Remotely Queued", true);
        // raise index position by one when used as a http queue parameter
        XQueueParameters xQueueParas = new XQueueParameters( queuePosition+1,
            queueLength, uploadLimit, pollMin, pollMax);
        response.addHeader(new HTTPHeader(GnutellaHeaderNames.X_QUEUE,
            xQueueParas.buildHTTPString()));

        UploadResponse uploadResponse = new UploadResponse( response );

        // append alt locs
        appendAltLocs( uploadResponse, shareFile, uploadState );
        
        return new UploadResponse( response );
    }
    
    public static final UploadResponse get503UploadLimitReachedForIP()
    {
        HTTPResponse response = new HTTPResponse( (short)HTTPCodes.HTTP_503_Service_Unavailable, 
            "Upload Limit Reached for IP", true );
        return new UploadResponse( response );
    }

    public static UploadResponse get503UploadLimitReached( ShareFile shareFile, UploadState uploadState )
    {
        HTTPResponse response = new HTTPResponse( (short)HTTPCodes.HTTP_503_Service_Unavailable,
            "Upload Limit Reached", true );
        
        UploadResponse uploadResponse = new UploadResponse( response );
        
        // append alt locs
        appendAltLocs( uploadResponse, shareFile, uploadState );
        
        return uploadResponse;
    }
    
    public static void appendAltLocs( UploadResponse response, ShareFile shareFile, 
        UploadState uploadState )
    {
        if ( shareFile.getAltLocCount() == 0 )
        {
            return;
        }
        AltLocContainer altLocContainer = shareFile.getAltLocContainer();
        HTTPHeader header = altLocContainer.getAltLocHTTPHeaderForAddress(
            GnutellaHeaderNames.X_ALT, uploadState.getHostAddress(),
            uploadState.getSendAltLocSet() );
        if (header != null)
        {
            response.addHttpHeader(header);
        }
    }
    
    public static void addPushProxyResponseHeader( DestAddress[] pushProxyAddresses,
        UploadResponse response )
    {
        if ( pushProxyAddresses == null )
        {
            return;
        }
        StringBuffer headerValue = new StringBuffer();
        int count = Math.min( 4, pushProxyAddresses.length);
        for (int i = 0; i < count; i++)
        {
            if ( i > 0 )
            {
                headerValue.append( "," );
            }
            headerValue.append( pushProxyAddresses[i].getFullHostName() );
        }        
        if ( headerValue.length() > 0 )
        {
            HTTPHeader header = new HTTPHeader( GnutellaHeaderNames.X_PUSH_PROXY,
                headerValue.toString() );
            response.addHttpHeader( header );
        }
    }
}