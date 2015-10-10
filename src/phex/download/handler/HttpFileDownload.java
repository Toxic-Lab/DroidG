/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2008 Phex Development Group
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
package phex.download.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.httpclient.ChunkedInputStream;

import phex.common.AltLocContainer;
import phex.common.AlternateLocation;
import phex.common.URN;
import phex.common.address.AddressUtils;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.common.address.MalformedDestAddressException;
import phex.download.DataDownloadScope;
import phex.download.DownloadEngine;
import phex.download.FileNotAvailableException;
import phex.download.HostBusyException;
import phex.download.MemoryFile;
import phex.download.RangeUnavailableException;
import phex.download.ReconnectException;
import phex.download.RemotelyQueuedException;
import phex.download.ThexVerificationData;
import phex.download.WrongHTTPHeaderException;
import phex.download.swarming.SWDownloadCandidate;
import phex.download.swarming.SWDownloadConstants;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SWDownloadSegment;
import phex.download.swarming.SWDownloadSet;
import phex.download.swarming.SWDownloadCandidate.CandidateStatus;
import phex.host.UnusableHostException;
import phex.http.GnutellaHeaderNames;
import phex.http.GnutellaRequest;
import phex.http.HTTPCodes;
import phex.http.HTTPHeader;
import phex.http.HTTPHeaderNames;
import phex.http.HTTPMessageException;
import phex.http.HTTPProcessor;
import phex.http.HTTPRangeSet;
import phex.http.HTTPRequest;
import phex.http.HTTPResponse;
import phex.http.HTTPRetryAfter;
import phex.http.XQueueParameters;
import phex.io.buffer.ByteBuffer;
import phex.net.connection.Connection;
import phex.net.repres.PresentationManager;
import phex.prefs.core.DownloadPrefs;
import phex.prefs.core.NetworkPrefs;
import phex.prefs.core.UploadPrefs;
import phex.security.PhexSecurityManager;
import phex.servent.Servent;
import phex.utils.IOUtil;
import phex.utils.LengthLimitedInputStream;

public class HttpFileDownload extends AbstractHttpDownload
{
//    private static final Logger logger = LoggerFactory.getLogger( 
//        HttpFileDownload.class );
    private static final int BUFFER_LENGTH = 16 * 1024;
    
    private InputStream inStream;
    
    private ContentRange replyContentRange;
    
    private long replyContentLength;
    
    private boolean isDownloadSuccessful;
        
    public HttpFileDownload( DownloadEngine engine )
    {
    	super( engine );
    }
    
    /**
     * Performs download pre process operations.
     * @throws DownloadHandlerException in case no segment is found to allocate. 
     */
    public void preProcess() throws DownloadHandlerException
    {
        SWDownloadSet downloadSet = downloadEngine.getDownloadSet();
        SWDownloadCandidate candidate = downloadSet.getCandidate();
        candidate.setStatus( CandidateStatus.ALLOCATING_SEGMENT );
        SWDownloadSegment segment = downloadSet.allocateSegment( );
        int allocationAttempts = 1;
        while( segment == null )
        {
            if ( allocationAttempts > 80 )
            {// 80 attempts ~ 20 seconds - give up
                break;
            }
            try
            {
                Thread.sleep( 250 );
            }
            catch ( InterruptedException exp )
            {// reset interruption
                Thread.currentThread().interrupt();
            }
            segment = downloadSet.allocateSegment( );
            allocationAttempts ++;
        }
        if ( segment == null )
        {// no more segments found...
            //logger.debug( "No segment to allocate found.");
            downloadSet.getCandidate().addToCandidateLog(
                "No segment to allocate found." );
            
            // wait some time... and try again...
            
            
            downloadSet.getCandidate().setStatus( CandidateStatus.WAITING );
            throw new DownloadHandlerException( "No segment found to allocate." );
        }
    }

    public void processHandshake() 
        throws IOException, UnusableHostException, HTTPMessageException
    {
        isDownloadSuccessful = false;

        Connection connection = downloadEngine.getConnection();
        SWDownloadSet downloadSet = downloadEngine.getDownloadSet();
        Servent servent = downloadSet.getServent();
        PhexSecurityManager securityService = servent.getSecurityService();
        SWDownloadCandidate candidate = downloadSet.getCandidate();
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();
        SWDownloadSegment segment = downloadSet.getDownloadSegment();
        
        long downloadOffset = segment.getTransferStartPosition();

        OutputStreamWriter writer = new OutputStreamWriter(
            connection.getOutputStream() );
        // reset to default input stream
        inStream = connection.getInputStream();

        String requestUrl = candidate.getDownloadRequestUrl();
        
        HTTPRequest request = new HTTPRequest( "GET", requestUrl, true );
        request.addHeader( new HTTPHeader( HTTPHeaderNames.HOST,
            candidate.getHostAddress().getFullHostName() ) );
        
        long segmentEndOffset = segment.getEnd();
        if ( segmentEndOffset == -1 )
        {// create header with open end
            request.addHeader( new HTTPHeader( HTTPHeaderNames.RANGE,
                "bytes=" + downloadOffset + "-" ) );
        }
        else
        {
            request.addHeader( new HTTPHeader( HTTPHeaderNames.RANGE,
                "bytes=" + downloadOffset + "-" + segmentEndOffset ) );
        }
        request.addHeader( new HTTPHeader( GnutellaHeaderNames.X_QUEUE,
            "0.1" ) );
        // request a HTTP keep alive connection, needed for queuing to work.
        request.addHeader( new HTTPHeader( HTTPHeaderNames.CONNECTION,
            "Keep-Alive" ) );
        if ( candidate.isG2FeatureAdded() )
        {
            request.addHeader( new HTTPHeader( "X-Features",
                "g2/1.0" ) );
        }

        buildAltLocRequestHeader( downloadFile, candidate, request, 
            servent.getLocalAddress(), servent.isFirewalled() );

        DestAddress localAdress = servent.getLocalAddress();
        IpAddress myIp = localAdress.getIpAddress();
        if ( !servent.isFirewalled() && ( myIp == null || !myIp.isSiteLocalIP() ) )
        {
            request.addHeader( new HTTPHeader( GnutellaHeaderNames.X_NODE,
                localAdress.getFullHostName() ) );
            if ( NetworkPrefs.AllowChatConnection.get().booleanValue() )
            {
                request.addHeader( new HTTPHeader( "Chat", localAdress.getFullHostName() ) );
            }
        }

        String httpRequestStr = request.buildHTTPRequestString();

        //logger.debug( "HTTP Request to: {}\n{}", candidate.getHostAddress(), httpRequestStr );
        candidate.addToCandidateLog( "HTTP Request:\n" + httpRequestStr );
        // write request...
        writer.write( httpRequestStr );
        writer.flush();

        HTTPResponse response = HTTPProcessor.parseHTTPResponse( connection );
//        if ( //logger.isDebugEnabled( ) )
//        {
//            //logger.debug( "HTTP Response from: " + candidate.getHostAddress() + "\n" 
//                + response.buildHTTPResponseString() );
//        }
        if ( DownloadPrefs.CandidateLogBufferSize.get().intValue() > 0 )
        {
            candidate.addToCandidateLog( "HTTP Response:\n" 
                + response.buildHTTPResponseString() );
        }
        
        updateServerHeader( response );
        
        HTTPHeader header;

        header = response.getHeader( HTTPHeaderNames.TRANSFER_ENCODING );
        if ( header != null )
        {
            if ( header.getValue().equals("chunked") )
            {
                inStream = new ChunkedInputStream( connection.getInputStream() );
            }
        }

        replyContentRange = null;
        header = response.getHeader( HTTPHeaderNames.CONTENT_RANGE );
        if ( header != null )
        {
            replyContentRange = parseContentRange( header.getValue() );
            // startPos of -1 indicates '*' (free to choose)
            if ( replyContentRange.startPos != -1 && 
                 replyContentRange.startPos != downloadOffset )
            {
                throw new IOException( "Invalid 'CONTENT-RANGE' start offset." );
            }
        }
        
        replyContentLength = -1;
        header = response.getHeader( HTTPHeaderNames.CONTENT_LENGTH );
        if ( header != null )
        {
            try
            {
                replyContentLength = header.longValue();
            }
            catch ( NumberFormatException exp )
            { //unknown 
            }
        }
        
        header = response.getHeader( GnutellaHeaderNames.X_THEX_URI );
        handleThexUriHeader( downloadFile, candidate, header );
        
        List<HTTPHeader> contentURNHeaders = new ArrayList<HTTPHeader>();
        header = response.getHeader( GnutellaHeaderNames.X_GNUTELLA_CONTENT_URN );
        if ( header != null )
        {
            contentURNHeaders.add( header );
        }
        // Shareaza 1.8.10.4 send also a bitprint urn in multiple X-Content-URN headers!
        HTTPHeader[] headers = response.getHeaders( GnutellaHeaderNames.X_CONTENT_URN );
        CollectionUtils.addAll( contentURNHeaders, headers );
        handleContentUrnHeaders( downloadFile, contentURNHeaders );

        // check Limewire chat support header.
        header = response.getHeader( GnutellaHeaderNames.CHAT );
        if ( header != null )
        {
            candidate.setChatSupported( true );
        }
        // read out REMOTE-IP header... to update my IP
        header = response.getHeader( GnutellaHeaderNames.REMOTE_IP );
        if ( header != null )
        {
            byte[] remoteIP = AddressUtils.parseIP( header.getValue() );
            if ( remoteIP != null )
            {
                IpAddress ip = new IpAddress( remoteIP );
                DestAddress address = PresentationManager.getInstance().createHostAddress(ip, -1);
                servent.updateLocalAddress( address );                
            }
        }
        
        handleAvailableRangesHeader( candidate, downloadFile, response );
        
        URN downloadFileURN = downloadFile.getFileURN();
        // collect alternate locations...
        List<AlternateLocation> altLocList = new ArrayList<AlternateLocation>();
        headers = response.getHeaders( GnutellaHeaderNames.ALT_LOC );
        List<AlternateLocation> altLocTmpList = AltLocContainer.parseUriResAltLocFromHeaders( headers, 
            securityService );
        altLocList.addAll( altLocTmpList );
        
        headers = response.getHeaders( GnutellaHeaderNames.X_ALT_LOC );
        altLocTmpList = AltLocContainer.parseUriResAltLocFromHeaders( headers, 
            securityService );
        altLocList.addAll( altLocTmpList );
        
        headers = response.getHeaders( GnutellaHeaderNames.X_ALT );
        altLocTmpList = AltLocContainer.parseCompactIpAltLocFromHeaders( headers,
            downloadFileURN, securityService );
        altLocList.addAll( altLocTmpList );
        
        // TODO1 huh?? dont we pare X-NALT???? 
        
        Iterator<AlternateLocation> iterator = altLocList.iterator();
        while( iterator.hasNext() )
        {
            downloadFile.addDownloadCandidate( iterator.next() );
        }
        
        // collect push proxies.
        // first the old headers..
        headers = response.getHeaders( "X-Pushproxies" );
        handlePushProxyHeaders( candidate, headers, securityService );
        headers = response.getHeaders( "X-Push-Proxies" );
        handlePushProxyHeaders( candidate, headers, securityService );
        // now the standard header
        headers = response.getHeaders( GnutellaHeaderNames.X_PUSH_PROXY );
        handlePushProxyHeaders( candidate, headers, securityService );
        
        updateKeepAliveSupport( response );

        int httpCode = response.getStatusCode();
        if ( httpCode >= 200 && httpCode < 300 )
        {// code accepted
            
            // check if we can accept the urn...
            if ( contentURNHeaders.size() == 0 
                && requestUrl.startsWith(GnutellaRequest.GNUTELLA_URI_RES_PREFIX) )
            {// we requested a download via /uri-res resource urn.
             // we expect that the result contains a x-gnutella-content-urn
             // or Shareaza X-Content-URN header.
                throw new IOException(
                    "Response to uri-res request without valid Content-URN header." );
            }
            
            // check if we need and can update our file and segment size.
            if ( downloadFile.getTotalDataSize() == SWDownloadConstants.UNKNOWN_FILE_SIZE )
            {
                // we have a file with an unknown data size. For aditional check assert
                // certain parameters
                assert( segment.getTotalDataSize() == -1 );
                // Now verify if we have the great chance to update our file data!
                if ( replyContentRange != null && 
                     replyContentRange.totalLength != SWDownloadConstants.UNKNOWN_FILE_SIZE )
                {
                    downloadFile.setFileSize( replyContentRange.totalLength );
                    // we learned the file size. To allow normal segment use 
                    // interrupt the download!
                    stopDownload();
                    throw new ReconnectException();
                }
            }
            
            // connection successfully finished
            //logger.debug( "HTTP Handshake successfull.");
            return;
        }
        // check error type
        else if ( httpCode == 503 )
        {// 503 -> host is busy (this can also be returned when remotely queued)
            header = response.getHeader( GnutellaHeaderNames.X_QUEUE );
            XQueueParameters xQueueParameters = null;
            if ( header != null )
            {
                xQueueParameters = XQueueParameters.parseXQueueParameters( header.getValue() );
            }
            // check for persistent connection (gtk-gnutella uses queuing with 'Connection: close')
            if ( xQueueParameters != null && isKeepAliveSupported )
            {
                // Immediately release segment to give free for others...
                downloadSet.releaseDownloadSegment();
                throw new RemotelyQueuedException( xQueueParameters );
            }
            else
            {
                header = response.getHeader( HTTPHeaderNames.RETRY_AFTER );
                if ( header != null )
                {
                    int delta = HTTPRetryAfter.parseDeltaInSeconds( header );
                    if ( delta > 0 )
                    {
                        throw new HostBusyException( delta );
                    }
                }
                throw new HostBusyException();
            }
        }
        else if ( httpCode == HTTPCodes.HTTP_401_UNAUTHORIZED 
               || httpCode == HTTPCodes.HTTP_403_FORBIDDEN )
        {
            if ( "Network Disabled".equals( response.getStatusReason() ) )
            {
                if ( candidate.isG2FeatureAdded() )
                {
                    // already tried G2 but no success.. better give up and don't hammer..
                    throw new UnusableHostException( "Request Forbidden" );
                }
                else
                {
                    // we have not tried G2 but we could..
                    candidate.setG2FeatureAdded( true );
                    throw new HostBusyException( 60 * 5 );
                }
            }
            else
            {
                throw new UnusableHostException( "Request Forbidden" );
            }
        }
        else if ( httpCode == 408 )
        {
            // 408 -> Time out. Try later?
            throw new HostBusyException();
        }
        else if ( httpCode == 404 || httpCode == 410 )
        {// 404: File not found / 410: Host not sharing
            throw new FileNotAvailableException();
        }
        else if ( httpCode == 416 )
        {// 416: Requested Range Unavailable
            // Immediately release segment to give free for others...
            downloadSet.releaseDownloadSegment();
            
            header = response.getHeader( HTTPHeaderNames.RETRY_AFTER );
            if ( header != null )
            {
                int delta = HTTPRetryAfter.parseDeltaInSeconds( header );
                if ( delta > 0 )
                {
                    throw new RangeUnavailableException( delta );
                }
            }
            throw new RangeUnavailableException();
        }
        else if ( httpCode == 500 )
        {
            throw new UnusableHostException( "Internal Server Error" );
        }
        else
        {
            throw new IOException( "Unknown HTTP code: " + httpCode );
        }
    }

    private static void handleAvailableRangesHeader(SWDownloadCandidate candidate,
        SWDownloadFile downloadFile, HTTPResponse response)
    {
        int httpCode = response.getStatusCode();

        // read available ranges
        HTTPHeader header = response.getHeader( GnutellaHeaderNames.X_AVAILABLE_RANGES );
        
        if (   header == null && httpCode >= 200 && httpCode < 300 
            && downloadFile.getTotalDataSize() != SWDownloadConstants.UNKNOWN_FILE_SIZE )
        {
            // OK httpCode and no available range header.. we assume candidate
            // shares the whole file
            candidate.setAvailableRangeSet( new HTTPRangeSet(
                0, downloadFile.getTotalDataSize() - 1) );
            return;
        }
        
        if ( header == null )
        {
            return;
        }
        
        HTTPRangeSet availableRanges = HTTPRangeSet.parseHTTPRangeSet( 
            header.getValue(), false );
        if ( availableRanges == null )
        {// failed to parse... give more detailed error report
            //logger.error( "Failed to parse X-Available-Ranges in {} request: {}", 
//                candidate.getVendor(),
//                response.buildHTTPResponseString() );
            return;
        }
        
        header = response.getHeader( GnutellaHeaderNames.X_AVAILABLE );
        if ( header == null )
        {
            // we assume the whole range was sent to us.
            candidate.setAvailableRangeSet( availableRanges );
            return;
        }

        // parse available size...
        String availStr = header.getValue();
        if ( availStr.startsWith( "bytes" ) )
        {
            try
            {
                availStr = availStr.substring( 6 ).trim();
            }
            catch ( StringIndexOutOfBoundsException exp )
            {
                // the best we can do is to assume we know the full range
                candidate.setAvailableRangeSet( availableRanges );
                //logger.error( "Invalid X-Available value: '{}'", header.getValue(), exp );
                return;
            }
        }
        long availVal;
        try
        {
            availVal = Long.parseLong( availStr );
        }
        catch ( NumberFormatException exp )
        {
            // the best we can do is to assume we know the full range
            candidate.setAvailableRangeSet( availableRanges );
            //logger.error( "Invalid X-Available value: '{}'", header.getValue(), exp );
            return;
        }
        candidate.addToAvailableRangeSet( availableRanges, availVal );
    }

    public void processDownload() throws IOException
    {
        SWDownloadSet downloadSet = downloadEngine.getDownloadSet();
        SWDownloadCandidate candidate = downloadSet.getCandidate();
        SWDownloadFile downloadFile = downloadSet.getDownloadFile();
        SWDownloadSegment segment = downloadSet.getDownloadSegment();
        
        String snapshotOfSegment;
        //logger.debug( "Download Engine starts download." );
        LengthLimitedInputStream downloadStream = null;
        try
        {            
            segment.downloadStartNotify();
            snapshotOfSegment = segment.toString();
            
            // determine the length to download, we start with the MAX
            // which would cause a download until the stream ends.
            long downloadLengthLeft = Long.MAX_VALUE;
            // maybe we know the file size
            if ( replyContentRange != null && replyContentRange.totalLength != -1 )
            {
                downloadLengthLeft = replyContentRange.totalLength;
            }
            // maybe we know a reply content length
            if ( replyContentLength != -1 )
            {
                downloadLengthLeft = Math.min( replyContentLength, downloadLengthLeft );
            }
            // maybe the segment has a smaller length (usually not the case)
            long segmentDataSizeLeft = segment.getTransferDataSizeLeft();
            if ( segmentDataSizeLeft != -1 )
            {
                downloadLengthLeft = Math.min( segmentDataSizeLeft, downloadLengthLeft );
            }
            
            downloadStream = new LengthLimitedInputStream( 
                inStream, downloadLengthLeft );
            MemoryFile memoryFile = downloadFile.getMemoryFile();
            long fileOffset = segment.getStart() + segment.getTransferredDataSize();
            long lengthDownloaded = segment.getTransferredDataSize();
            int len;
            byte[] buffer = new byte[BUFFER_LENGTH];
            while( (len = downloadStream.read( buffer, 0, BUFFER_LENGTH )) > 0 )
            {
//                if ( //logger.isDebugEnabled( ) )
//                {
//                    //logger.debug( "Reading in {} bytes.", len );
//                    candidate.addToCandidateLog( "Reading in " + len + "bytes." );
//                }
                synchronized (segment)
                {
                    long tmpCheckLength = lengthDownloaded + len;
                    if ( tmpCheckLength < segment.getTransferredDataSize() )
                    {
                        //logger.error( "TransferredDataSize would be going down! " + 
//                            " ll " + downloadLengthLeft + " l " + len + " ld "
//                            + lengthDownloaded + " gtds "
//                            + segment.getTransferredDataSize()
//                            + " seg: " + segment
//                            + " originally: " + snapshotOfSegment);
                        throw new IOException( "TransferredDataSize would be going down!" );
                    }
                    else if (segment.getTransferDataSize() > -1 
                        && tmpCheckLength > segment.getTransferDataSize())
                    {
                        //logger.error( "TransferredDataSize would be larger then segment! " +
//                            " ll " + downloadLengthLeft + " l " + len + " ld "
//                            + lengthDownloaded + " gtds "
//                            + segment.getTransferredDataSize()
//                            + " seg: " + segment
//                            + " originally: " + snapshotOfSegment);
                        throw new IOException( "TransferredDataSize would be larger then segment!" );
                    }
                    
                    // this will request buffers between 1KB and 16KB of length.
                    // don't release or clear directByteBuffer this will be done 
                    // by MemoryFile and DownloadDataWriter
                    ByteBuffer byteBuffer = ByteBuffer.allocate( len );
                    byteBuffer.put( buffer, 0, len );
                    byteBuffer.flip();
                    DataDownloadScope dataScope = new DataDownloadScope( fileOffset,
                        fileOffset + len - 1, byteBuffer );
//                    if ( //logger.isDebugEnabled( ) )
//                    {
//                        //logger.debug( "Buffering {} bytes.", len );
//                        candidate.addToCandidateLog( "Buffering in " + len + "bytes." );
//                    }
                    memoryFile.bufferDataScope(dataScope);
//                    if ( //logger.isDebugEnabled( ) )
//                    {
//                        //logger.debug( "Buffered {} bytes.", len );
//                        candidate.addToCandidateLog( "Buffered " + len + "bytes." );
//                    }
                    
                    
                    fileOffset += len;
                    lengthDownloaded += len;
                    segment.setTransferredDataSize( lengthDownloaded );
                    candidate.incTotalDownloadSize( len );

                    // get transfer size since it might have changed in the meantime.
                    segmentDataSizeLeft = segment.getTransferDataSizeLeft();
                    if ( segmentDataSizeLeft != -1 )
                    {
                        downloadLengthLeft = Math.min( segmentDataSizeLeft, downloadLengthLeft );
                        downloadStream.setLengthLimit(downloadLengthLeft);
                    }
                }
            }
            isDownloadSuccessful = true;
            // if we successful downloaded and we still don't know the total file size,
            // we can assume that the file was completely downloaded.
            if ( downloadFile.getTotalDataSize() == SWDownloadConstants.UNKNOWN_FILE_SIZE )
            {
                // we have a file with an unknown data size. For additional check assert
                // certain parameters
                assert( segment.getTotalDataSize() == -1 );
                downloadFile.setFileSize( segment.getTransferredDataSize() );
            }
        }
        finally
        {// don't close managed file since it might be used by parallel threads.
            segment.downloadStopNotify();
            boolean isAcceptingNextSegment = isAcceptingNextRequest();
            candidate.addToCandidateLog( "Is accepting next segment: " + isAcceptingNextSegment );
            // this is for keep alive support...
            if ( isAcceptingNextSegment && downloadStream != null )
            {
                // only need to close and consume left overs if we plan to
                // continue using this connection.
                downloadStream.close();
            }
            else
            {
                stopDownload();
            }
        }
    }
    
    public void postProcess()
    {
        // segment download completed, release segment
        SWDownloadSet downloadSet = downloadEngine.getDownloadSet();
        SWDownloadSegment downloadSegment = downloadSet.getDownloadSegment();
        SWDownloadCandidate candidate = downloadSet.getCandidate();
        
        if ( downloadSegment == null )
        {
            candidate.addToCandidateLog( "No download segment available." );
            //logger.debug( "No download segment available." );
        }
        else
        {
            String logStr = "Completed a segment which started at " + downloadSegment.getStart() 
                + " and was downloaded at a rate of " + downloadSegment.getLongTermTransferRate();
            candidate.addToCandidateLog( logStr );
            //logger.debug( logStr);
        }
        //logger.debug( "Releasing DownloadSegment: {} - {}", downloadSet, this);
        
        downloadSet.releaseDownloadSegment();
    }

    public void stopDownload()
    {
        IOUtil.closeQuietly( inStream );
        SWDownloadSegment segment = downloadEngine.getDownloadSet().getDownloadSegment();
        if ( segment != null )
        {
            segment.downloadStopNotify();
        }
    }
    
    /**
     * Indicates whether the connection is kept alive and the next request can 
     * be send.
     * @return true if the next request can be send on this connection
     */
    public boolean isAcceptingNextRequest()
    {
        return isDownloadSuccessful && isKeepAliveSupported && replyContentLength != -1;
    }
    
    private static void handleContentUrnHeaders(SWDownloadFile downloadFile, List<HTTPHeader> headerList)
        throws IOException
    {
        URN downloadFileURN = downloadFile.getFileURN();
        if ( downloadFileURN != null )
        {
            for ( HTTPHeader urnHeader : headerList )
            {
                String contentURNStr = urnHeader.getValue();
                // check if I can understand urn.
                if ( URN.isValidURN( contentURNStr ) )
                {
                    URN contentURN = new URN( contentURNStr );
                    if ( contentURN.isBitprintNid() )
                    {
                        ThexVerificationData thexVerificationData = downloadFile.getThexVerificationData();
                        String expectedRootHash = thexVerificationData.getRootHash();
                        if ( expectedRootHash != null && 
                            !expectedRootHash.equals( contentURN.getTigerTreeRootNss() ) )
                        {
                            throw new IOException(
                                "Required TTR and content URN TTR do not match:" + 
                                expectedRootHash + " " + contentURN.getTigerTreeRootNss() );
                        }
                    }
                    if ( !downloadFileURN.equals( contentURN ) )
                    {
                        throw new IOException(
                            "Required URN and content URN do not match." );
                    }
                }
            }
        }
    }

    private static void handleThexUriHeader(SWDownloadFile downloadFile, 
        SWDownloadCandidate candidate, HTTPHeader header)
    {
        if ( header == null )
        {
            return;
        }
        String value = header.getValue();
        int idx = value.indexOf( ";" );
        if ( idx < 0 )
        {
            return;
        }
        String thexUri = value.substring( 0, idx );
        String root = value.substring( idx + 1 );
        
        ThexVerificationData thexVerificationData = downloadFile.getThexVerificationData();
        String expectedRootHash = thexVerificationData.getRootHash();
        if ( expectedRootHash == null )
        {// learn the hash for this download
            thexVerificationData.setRootHash( root );
        }
        candidate.setThexUriRoot( thexUri, root );
    }
    
    private static void buildAltLocRequestHeader( SWDownloadFile downloadFile, 
        SWDownloadCandidate candidate, HTTPRequest request, 
        DestAddress serventAddress, boolean isFirewalled )
    {
        URN downloadFileURN = downloadFile.getFileURN();
        if ( downloadFileURN == null )
        {
            return;
        }
        
        // add good alt loc http header
        
        AltLocContainer altLocContainer = new AltLocContainer(
            downloadFileURN );
        // downloadFile.getGoodAltLocContainer() always returns a alt-loc container
        // when downloadFileURN != null
        altLocContainer.addContainer( downloadFile.getGoodAltLocContainer() );

        // create a temp copy of the container and add local alt location
        // if partial file sharing is active and we are not covered by a firewall
        if ( !isFirewalled && UploadPrefs.SharePartialFiles.get().booleanValue() )
        {
            // add the local peer to the alt loc on creation, but only if its
            // not a site local address.
            if ( !serventAddress.isSiteLocalAddress() )
            {
                AlternateLocation newAltLoc = new AlternateLocation( serventAddress,
                    downloadFileURN );
                altLocContainer.addAlternateLocation( newAltLoc );
            }
        }
        
        HTTPHeader header = altLocContainer.getAltLocHTTPHeaderForAddress(
            GnutellaHeaderNames.X_ALT, candidate.getHostAddress(),
            candidate.getSendAltLocsSet() );
        if ( header != null )
        {
            request.addHeader( header );
        }
        
        // add bad alt loc http header
        
        // downloadFile.getBadAltLocContainer() always returns a alt-loc container
        // when downloadFileURN != null
        altLocContainer = downloadFile.getBadAltLocContainer();
        header = altLocContainer.getAltLocHTTPHeaderForAddress(
            GnutellaHeaderNames.X_NALT, candidate.getHostAddress(),
            candidate.getSendAltLocsSet() );
        if ( header != null )
        {
            request.addHeader( header );
        }
        
    }
    
    private static void handlePushProxyHeaders( SWDownloadCandidate candidate, 
        HTTPHeader[] headers, PhexSecurityManager securityService )
    {
        if ( headers == null || headers.length == 0 )
        {
            return;
        }
        List<DestAddress> proxyList = new ArrayList<DestAddress>();
        StringTokenizer tokenizer;
        for ( int i = 0; i < headers.length; i++ )
        {
            HTTPHeader header = headers[i];
            tokenizer = new StringTokenizer( header.getValue(), ",");
            while( tokenizer.hasMoreTokens() )
            {
                String addressStr = tokenizer.nextToken().trim();
                DestAddress address;
                try
                {
                    // includes security validation.
                    address = AddressUtils.parseAndValidateAddress( 
                        addressStr, false, securityService );
                    proxyList.add( address );
                }
                catch (MalformedDestAddressException exp)
                {
                    //logger.debug( "Malformed alt-location URL: {}", exp.getMessage() );
                }
            }
        }
        if ( proxyList.size() == 0 )
        {
            return;
        }
        DestAddress[] pushProxyAddresses = new DestAddress[ proxyList.size() ];
        proxyList.toArray( pushProxyAddresses );
        candidate.setPushProxyAddresses( pushProxyAddresses );
    }
    
    /**
     * We only care for the start offset since this is the important point to
     * begin the download from. Wherever it ends we try to download as long as we
     * stay connected or until we reach our goal.
     *
     * Possible Content-Range Headers ( maybe not complete / header is upper
     * cased by Phex )
     *
     *   Content-range:bytes abc-def/xyz
     *   Content-range:bytes abc-def/*
     *   Content-range:bytes *\/xyz
     *   Content-range: bytes=abc-def/xyz (wrong but older Phex version and old clients use this)
     *
     * @param contentRangeLine the content range value
     * @throws WrongHTTPHeaderException if the content range line has wrong format.
     * @return the content range start offset.
     */
    private ContentRange parseContentRange( String contentRangeLine )
        throws WrongHTTPHeaderException
    {
        try
        {
            ContentRange range = new ContentRange();
            String line = contentRangeLine.toLowerCase( Locale.US );
            // skip over bytes plus extra char
            int idx = line.indexOf( "bytes" ) + 6;
            String rangeStr = line.substring( idx ).trim();
            
            int slashIdx = rangeStr.indexOf('/');
            String leadingPart = rangeStr.substring(0, slashIdx);
            String trailingPart = rangeStr.substring( slashIdx + 1 );
            
            // ?????/*
            if ( trailingPart.charAt(0) == '*' )
            {
                range.totalLength = -1;
            }
            else // ?????/789
            {
                long fileLength = Long.parseLong(trailingPart);
                range.totalLength = fileLength;
            }
            
            // */???
            if ( leadingPart.charAt( 0 ) == '*' )
            {
                // startPos of -1 indicates '*' (free to choose)
                range.startPos = -1;
                range.totalLength = range.totalLength;
            }
            else
            {
                // 123-456/???
                int dashIdx = rangeStr.indexOf( '-' );
                String startOffsetStr = leadingPart.substring( 0, dashIdx );
                long startOffset = Long.parseLong( startOffsetStr );
                String endOffsetStr = leadingPart.substring( dashIdx+1 );
                long endOffset = Long.parseLong( endOffsetStr );
                range.startPos = startOffset;
                range.endPos = endOffset;
            }
            return range;
        }
        catch ( NumberFormatException exp )
        {
            //logger.warn( exp.toString(), exp);
            throw new WrongHTTPHeaderException(
                "Number error while parsing content range: " + contentRangeLine );
        }
        catch ( IndexOutOfBoundsException exp )
        {
            throw new WrongHTTPHeaderException(
                "Error while parsing content range: " + contentRangeLine );
        }
    }


    private static class ContentRange
    {
        /**
         * startPos of -1 indicates '*' (free to choose)
         */
        long startPos;
        long endPos;
        long totalLength;
    }
}
