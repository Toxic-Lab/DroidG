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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import phex.http.HTTPResponse;
import phex.io.buffer.ByteBuffer;
import phex.share.ShareFile;
import phex.thex.FileHashCalculationHandler;
import phex.thex.ShareFileThexData;
import phex.utils.StringUtils;
import phex.xml.thex.ThexHashTree;
import phex.xml.thex.ThexHashTreeCodec;

import com.onionnetworks.dime.DimeGenerator;
import com.onionnetworks.dime.DimeRecord;

public class ThexUploadResponse extends UploadResponse
{
    private byte[] dimeData;
    private int offset;

    public ThexUploadResponse( ShareFile shareFile, FileHashCalculationHandler fileHashCalcQueue ) throws IOException
    {        
        super( new HTTPResponse((short) 200, "OK", true) );
        
        // I have to select the serialization of this shareFile
        ShareFileThexData thexData = shareFile.getThexData( fileHashCalcQueue );
        String uuidStr = StringUtils.generateRandomUUIDString();
        
        // We get the THEX metadata
        ThexHashTree hashTree = new ThexHashTree();
        hashTree.setFileSize( String.valueOf( shareFile.getFileSize() ) );
        hashTree.setFileSegmentSize("1024");
        hashTree.setDigestAlgorithm("http://open-content.net/spec/digest/tiger");
        hashTree.setDigestOutputSize("24");
        hashTree.setSerializedTreeDepth( String.valueOf( thexData.getTreeDepth() ) );
        hashTree.setSerializedTreeType("http://open-content.net/spec/thex/breadthfirst");
        hashTree.setSerializedTreeUri("uuid:" + uuidStr);

        String type ="http://open-content.net/spec/thex/breadthfirst";
        byte[] metadata = ThexHashTreeCodec.generateThexHashTreeXML( hashTree );
        byte[] serialization = thexData.getSerializedTreeNodes();
        
        // add 1024 overhead...
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( 
            metadata.length + serialization.length + 1024 );
        DimeGenerator dg = new DimeGenerator( outputStream );
        DimeRecord dr = new DimeRecord(metadata, DimeRecord.TypeNameFormat.MEDIA_TYPE, type, null);
        dg.addRecord(dr, false);
        DimeRecord dr2 = new DimeRecord(serialization, DimeRecord.TypeNameFormat.URI, type, "uuid:" + uuidStr);
        dg.addRecord(dr2, true);
        
        dimeData = outputStream.toByteArray();
        offset = 0;
    }
    
    public int remainingBody()
    {
        return dimeData.length - offset;
    }
    
    @Override
    public int fillBody( ByteBuffer directBuffer ) 
        throws IOException
    {
        int remaining = directBuffer.remaining();
        directBuffer.put( dimeData, offset, remaining );
        offset += remaining;
        return remaining;
    }
    
    public void countUpload()
    {
    }
    
    public void close()
    {
        dimeData = null;
    }
}