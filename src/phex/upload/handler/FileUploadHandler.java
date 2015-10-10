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
package phex.upload.handler;

import java.io.IOException;

import phex.common.URN;
import phex.common.address.DestAddress;
import phex.common.file.ManagedFileException;
import phex.http.GnutellaHeaderNames;
import phex.http.HTTPCodes;
import phex.http.HTTPHeader;
import phex.http.HTTPHeaderNames;
import phex.http.HTTPRangeSet;
import phex.http.HTTPRequest;
import phex.http.HTTPResponse;
import phex.http.Range;
import phex.http.Range.RangeAvailability;
import phex.servent.Servent;
import phex.share.PartialShareFile;
import phex.share.ShareFile;
import phex.share.SharedFilesService;
import phex.thex.ShareFileThexData;
import phex.upload.UploadState;
import phex.upload.response.ShareFileUploadResponse;
import phex.upload.response.UploadResponse;
import phex.utils.URLUtil;

public class FileUploadHandler extends AbstractUploadHandler
{   
    private final Servent servent;
    private Range uploadRangeEntry;
    
    private long startOffset;
    private long endOffset;
    
    public FileUploadHandler( SharedFilesService sharedFilesService, Servent servent )
    {
        super( sharedFilesService );
        this.servent = servent;
    }
    
    @Override
    protected UploadResponse determineFailFastResponse( HTTPRequest httpRequest, 
        UploadState uploadState, ShareFile requestedFile )
    {
        HTTPRangeSet uploadRange = null;
        
        HTTPHeader rangeHeader = httpRequest.getHeader(HTTPHeaderNames.RANGE);
        if (rangeHeader != null)
        {
            uploadRange = HTTPRangeSet.parseHTTPRangeSet(
                rangeHeader.getValue(), true);
            if (uploadRange == null)
            {
                // this is not 416 Requested Range Not Satisfiable since
                // we have a parsing error on the requested range.
                return UploadResponse.get500RangeNotParseable( requestedFile, uploadState );
            }
        }
        else
        {
            uploadRange = new HTTPRangeSet(0, HTTPRangeSet.NOT_SET);
        }
        uploadRangeEntry = uploadRange.getFirstRange();
        RangeAvailability rangeAvail = requestedFile.getRangeAvailableStatus(uploadRangeEntry);

        if ( rangeAvail != RangeAvailability.RANGE_AVAILABLE )
        {
            HTTPResponse httpResponse;
            if ( rangeAvail == RangeAvailability.RANGE_NOT_AVAILABLE )
            {
                httpResponse = new HTTPResponse((short) HTTPCodes.HTTP_416_Requested_Range_Not_Available,
                    "Requested Range Not Available", true);
            }
            else
            {
                assert rangeAvail == RangeAvailability.RANGE_NOT_SATISFIABLE;
                httpResponse = new HTTPResponse(
                    (short)HTTPCodes.HTTP_416_Requested_Range_Not_Available,
                    "Requested Range Not Satisfiable", true);
            }
            if ( requestedFile instanceof PartialShareFile )
            {
                PartialShareFile pShareFile = (PartialShareFile)requestedFile;
                httpResponse.addHeader( new HTTPHeader(
                    GnutellaHeaderNames.X_AVAILABLE_RANGES, pShareFile
                        .buildXAvailableRangesString()));
            }
            
            UploadResponse uploadResponse = new UploadResponse( httpResponse );

            // append alt locs
            UploadResponse.appendAltLocs( uploadResponse, requestedFile, uploadState );
            
            return uploadResponse;
        }
        
        return null;
    }
    

    @Override
    public UploadResponse finalizeUploadResponse( HTTPRequest httpRequest, 
        UploadState uploadState, ShareFile requestedFile ) throws IOException
    {
        HTTPHeader availRangesHeader = null;
        
        if (requestedFile instanceof PartialShareFile)
        {
            PartialShareFile pShareFile = (PartialShareFile) requestedFile;

            // call adjusts uploadRangeEntry to fit...
            pShareFile.findFittingPartForRange(uploadRangeEntry);
            availRangesHeader = new HTTPHeader(
                GnutellaHeaderNames.X_AVAILABLE_RANGES, pShareFile
                    .buildXAvailableRangesString());
        }

        startOffset = uploadRangeEntry.getStartOffset(requestedFile
            .getFileSize());
        endOffset = uploadRangeEntry.getEndOffset(requestedFile
            .getFileSize());
        
        long contentLength = endOffset - startOffset + 1;
        URN sharedFileURN = requestedFile.getURN();
        
        uploadState.update(requestedFile.getFileName(), sharedFileURN, 
            contentLength);

        // form ok response...

        ShareFileUploadResponse response;
        try
        {
            response = new ShareFileUploadResponse( requestedFile, 
                startOffset, contentLength );
        } 
        catch ( ManagedFileException exp )
        {
            IOException ioExp = new IOException( "ManagedFileException: "
                + exp.getMessage() );
            ioExp.initCause( exp );
            throw ioExp;
        }

        if (availRangesHeader != null)
        {
            response.addHttpHeader(availRangesHeader);
        }
        

        // TODO for browser request we might like to return explicit content
        // types:
        // contentType = MimeTypeMapping.getMimeTypeForExtension( ext );
        response.addHttpHeader(new HTTPHeader(HTTPHeaderNames.CONTENT_TYPE,
            "application/binary"));

        response.addHttpHeader(new HTTPHeader(
            HTTPHeaderNames.CONTENT_LENGTH, String.valueOf(contentLength)));

        response.addHttpHeader(new HTTPHeader(
            HTTPHeaderNames.CONTENT_RANGE, "bytes " + startOffset + "-"
                + endOffset + "/" + requestedFile.getFileSize()));
        
        response.addHttpHeader( new HTTPHeader( HTTPHeaderNames.CONNECTION,
            "Keep-Alive" ) );

        if ( sharedFileURN != null )
        {
            response.addHttpHeader(new HTTPHeader(
                GnutellaHeaderNames.X_GNUTELLA_CONTENT_URN, sharedFileURN
                    .getAsString()));
        }
        UploadResponse.appendAltLocs( response, requestedFile, uploadState );
        DestAddress[] pushProxies = servent.getHostService().
            getNetworkHostsContainer().getPushProxies();
        UploadResponse.addPushProxyResponseHeader( pushProxies, response );
        
        if ( sharedFileURN != null )
        {
            handleAltLocRequestHeader( httpRequest, uploadState, requestedFile,
                sharedFileURN, servent.getSecurityService() );

            // add thex download url
            ShareFileThexData thexData = requestedFile.getThexData( sharedFilesService );
            if ( thexData != null )
            {
                String thexRootHash = thexData.getRootHash();
                HTTPHeader thexHeader = new HTTPHeader( GnutellaHeaderNames.X_THEX_URI, 
                    URLUtil.buildName2ResThexURL( sharedFileURN, thexRootHash ) );
                response.addHttpHeader( thexHeader );
            }
        }
        
        return response;
    }
}