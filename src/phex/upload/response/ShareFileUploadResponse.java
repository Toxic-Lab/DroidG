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

import phex.common.Phex;
import phex.common.file.ManagedFileException;
import phex.common.file.ReadOnlyManagedFile;
import phex.http.HTTPResponse;
import phex.io.buffer.ByteBuffer;
import phex.share.ShareFile;
import phex.utils.IOUtil;

public class ShareFileUploadResponse extends UploadResponse
{
    private ShareFile shareFile;
    private ReadOnlyManagedFile uploadFile;

    private long startOffset;
    private long currentOffset;
    
    private long length;

    public ShareFileUploadResponse( ShareFile shareFile, long offset, long length ) 
        throws ManagedFileException
    {
        super();
        this.shareFile = shareFile;
        uploadFile = Phex.getFileManager().
            getReadOnlyManagedFile( shareFile.getSystemFile() );
        
        startOffset = offset;
        currentOffset = startOffset;
        this.length = length;
        
        if ( offset == 0 && length == uploadFile.getLength() )
        {
            httpResponse = new HTTPResponse((short) 200, "OK", true);
        }
        else
        {
            httpResponse = new HTTPResponse((short) 206, "Partial Content",
                true);
        }
    }
    
    public int remainingBody()
    {
        return (int)(startOffset + length - currentOffset);
    }
    
    @Override
    public int fillBody( ByteBuffer byteBuffer ) 
        throws IOException
    {
        try
        {
//            if ( NLogger.isDebugEnabled( ShareFileUploadResponse.class ) )
//            {
//                String log = "Reading in " + byteBuffer.remaining() + " bytes at " + 
//                    currentOffset + " from " + uploadFile;
//                NLogger.debug( ShareFileUploadResponse.class, log );
//            }
            int read = uploadFile.read( byteBuffer, currentOffset );
            currentOffset += read;
            return read;
        } 
        catch ( ManagedFileException exp )
        {
            IOException ioExp = new IOException( "ManagedFileException: "
                + exp.getMessage() );
            ioExp.initCause( exp );
            throw ioExp;
        }
    }
    
    public void countUpload()
    {
        shareFile.incUploadCount();
    }
    
    public void close()
    {
        IOUtil.closeQuietly( uploadFile );
    }
}
