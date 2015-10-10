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
package phex.msg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.StringTokenizer;

import phex.common.URN;
import phex.common.address.DestAddress;
import phex.security.PhexSecurityManager;
import phex.share.ShareFile;
import phex.utils.IOUtil;

/**
 * A single response record in a QueryResponse message.
 */
public class QueryResponseRecord
{
//    private static final Logger logger = LoggerFactory.getLogger( 
//        QueryResponseRecord.class );
    private static final int DROP_PACKAGE_ALT_LOCATION_LIMIT = 20;
    
    private int fileIndex = 0;
    private String fileName;
    private byte[] fileNameBytes;
    private String pathInfo;
    private long fileSize;
    private URN urn;
    private String metaData;
    private DestAddress[] alternateLocations;
    private long creationTime;

    /**
     * Create a new MsgResRecord.
     */
    public QueryResponseRecord()
    {
        pathInfo = "";
        creationTime = -1;
    }

    /**
     * Create a new MsgResRecord with all its properties populated.
     *
     * @param fileIndex  the index of the file
     * @param fileSize   the file size (bytes)
     * @param aFileName  a String representation of the file name
     */
    public QueryResponseRecord(int fileIndex, URN fileURN, long fileSize, String aFileName,
        long creationTime, Set<DestAddress> altLocSet )
    {
        if ( fileSize < 0)
        {
            throw new IllegalArgumentException( "Invalid file size: " + fileSize );
        }
        
        this.fileIndex = fileIndex;
        this.fileSize = fileSize;
        this.fileName = aFileName;
        this.creationTime = creationTime;
        if ( altLocSet != null && altLocSet.size() > 0 )
        {
            assert altLocSet.size() <= 10;
            alternateLocations = new DestAddress[ altLocSet.size() ];
            altLocSet.toArray( alternateLocations);
        }
        try
        {
            this.fileNameBytes = fileName.getBytes( "UTF-8" );
        }
        catch (UnsupportedEncodingException exp)
        {// should never happen
            //logger.error( exp.toString(), exp );
            throw new RuntimeException( exp );
        }
        this.urn = fileURN;
        pathInfo = "";
    }
    
    public static QueryResponseRecord createFromShareFile( ShareFile shareFile, DestAddress localAddress )
    {
        int fileIndex = shareFile.getFileIndex();
        URN urn = shareFile.getURN();
        long fileSize = shareFile.getFileSize();
        if ( fileSize < 0 )
        {
            //logger.error( "Invalid file size: " + fileSize );
            throw new IllegalArgumentException( "Invalid file size: " + fileSize );
        }
        String fileName = shareFile.getFileName();
        long networkCreateTime = shareFile.getNetworkCreateTime().longValue();
        Set<DestAddress> altLocSet = null;
        if ( shareFile.getAltLocCount() > 0 )
        {
            altLocSet = shareFile.getAltLocContainer().getAltLocForQueryResponseRecord(
                localAddress );
        }
        QueryResponseRecord record = new QueryResponseRecord( fileIndex, urn,
            fileSize, fileName, networkCreateTime, altLocSet );
        return record;
    }

    /**
     * Get the current file index.
     *
     * @return the file index
     */
    public int getFileIndex()
    {
        return fileIndex;
    }

    /**
     * Get the file size (bytes).
     *
     * @return the current file size
     */
    public long getFileSize()
    {
        return fileSize;
    }

    /**
     * Get the current file name.
     *
     * @return the current file name
     */
    public String getFilename()
    {
        return fileName;
    }
    
    public String getPathInfo()
    {
        return pathInfo;
    }

    public URN getURN()
    {
        return urn;
    }
    
    public DestAddress[] getAlternateLocations()
    {
        return alternateLocations;
    }
    
    public long getCreationTime()
    {
        return creationTime;
    }

    public String getMetaData()
    {
        return metaData;
    }

    /**
     * Copy the information from another MsgResRecord into this record.
     *
     * @param b  the MsgResRecord to copy
     */
    public void copy(QueryResponseRecord b)
    {
        fileIndex = b.fileIndex;
        fileSize = b.fileSize;
        fileName = b.fileName;
        fileNameBytes = b.fileNameBytes;
        pathInfo = b.pathInfo;
    }
    
    public void write( OutputStream outStream )
        throws IOException
    {
        // Convert to Intel little-endian
        IOUtil.serializeIntLE(fileIndex, outStream);

        // handle large files through GGEP..
        if (fileSize > Integer.MAX_VALUE) 
        {
            IOUtil.serializeIntLE( 0xFFFFFFFF, outStream );
        }
        else
        {
            IOUtil.serializeIntLE( (int)fileSize, outStream );
        }

        outStream.write( fileNameBytes );
        // write first ending 0
        outStream.write( 0 );
        // TODO return meta data
        
        byte[] ggepData = GGEPBlock.getQueryReplyRecordGGEPBlock( creationTime, 
            alternateLocations, fileSize );
        if ( urn != null )
        {
            outStream.write( urn.getAsString().getBytes() );
            if ( ggepData.length > 0 )
            {
                outStream.write( 0x1c );
            }
        }
        if ( ggepData.length > 0 )
        {
            outStream.write( ggepData );
        }
        
        // write second ending 0
        outStream.write( 0 );
    }

    protected int deserialize(byte[] inbuf, int offset, PhexSecurityManager securityService )
    	throws InvalidMessageException
    {
        long tmpFileIndex = IOUtil.unsignedInt2Long( IOUtil.deserializeIntLE(inbuf, offset) );
        if( (tmpFileIndex & 0xFFFFFFFF00000000L) !=0 )
        {
            throw new InvalidMessageException( "Invalid file index: " + tmpFileIndex );
        }
        fileIndex = (int)tmpFileIndex;
        offset += 4;
        
        long tmpFileSize = IOUtil.unsignedInt2Long( IOUtil.deserializeIntLE(inbuf, offset) );
        // validate the file size
        if ( tmpFileSize < 0)
        {
            throw new InvalidMessageException( "Invalid file size: " + tmpFileSize );
        }
        fileSize = tmpFileSize;
        offset += 4;

        // Handle Gnotella termination of single null terminated file names right
        // this prevents strange results like a filename of 44khz which is meta
        // data of gnotella.
        // search for first null terminator
        int firstTerminatorIdx = offset;
        try
        {
            while ( inbuf[firstTerminatorIdx] != (byte) 0 )
            {
                firstTerminatorIdx++;
            }
        }
        catch ( IndexOutOfBoundsException exp )
        {// the cause seem to be Shareaza nods reporting a invalid number of 
         // records in the response.
            //logger.debug( exp.toString(), exp );
            //N//logger.warn( QueryResponseMsg.class, "Offset: " + offset + "- Buffer: " + HexConverter.toHexString( inbuf ) );
            throw new InvalidMessageException( exp.getMessage(), exp );
        }

        // extract the file name
        try
        {
            fileName = new String( inbuf, offset, firstTerminatorIdx - offset, "UTF-8" );
        } 
        catch ( UnsupportedEncodingException exp )
        {// Should never happen...
            //logger.error( exp.toString(), exp );
            throw new RuntimeException( exp );
        }
        
        int secondTerminatorIdx = firstTerminatorIdx + 1; // skip terminator
        //Find second null terminator.
        try
        {
            while ( inbuf[secondTerminatorIdx] != (byte) 0 )
            {
                secondTerminatorIdx++;
            }
        }
        catch ( IndexOutOfBoundsException exp )
        {// the cause seem to be Shareaza nods reporting a invalid number of 
         // records in the response.
            //logger.debug( exp.toString(), exp );
            //N//logger.warn( QueryResponseMsg.class, "Offset: " + offset + "- Buffer: " + HexConverter.toHexString( inbuf ) );
            throw new InvalidMessageException( exp.getMessage(), exp );
        }
        // parse out extension data
        byte[] extensionArea = new byte[ secondTerminatorIdx - firstTerminatorIdx - 1 ];
        System.arraycopy( inbuf, firstTerminatorIdx + 1, extensionArea, 0,
            secondTerminatorIdx - firstTerminatorIdx - 1 );
        parseExtensionArea( extensionArea, securityService );

        // skip second terminator
        offset = secondTerminatorIdx + 1;

        return offset;
    }

    private void parseExtensionArea(byte[] extensionArea, PhexSecurityManager securityService )
        throws InvalidMessageException
    {
        try
        {
            PushbackInputStream inStream = new PushbackInputStream( 
                new ByteArrayInputStream( extensionArea ) );
            byte b;
            StringBuffer buffer = new StringBuffer();
            GGEPBlock[] ggepBlocks = null;
            GGEPBlock ggepBlock = null;
            
            while ( true )
            {
                b = (byte)inStream.read();
                if ( b == -1 )
                {
                    evaluateExtensionToken( buffer.toString() );
                    break;
                }
                else if ( b == GGEPBlock.MAGIC_NUMBER && buffer.length() == 0 )
                {
                    inStream.unread( b );
                    try
                    {
                        ggepBlocks = GGEPBlock.parseGGEPBlocks( inStream );
                        ggepBlock = GGEPBlock.mergeGGEPBlocks(ggepBlocks);
                    }
                    catch ( InvalidGGEPBlockException exp )
                    {// try to continue even though parsing of message might now be completely screwed!
                        //logger.error( exp.toString(), exp );
                    }
                    continue;
                }
                else if ( b == 0x1c )
                {// evaluate buffer and check for 3c
                    evaluateExtensionToken( buffer.toString() );
                    buffer.setLength(0);
                    continue;
                }
                buffer.append( (char)b );
            }
            
            if ( ggepBlocks != null )
            {
                alternateLocations = GGEPExtension.parseAltExtensionData( ggepBlocks, securityService );
                if ( alternateLocations != null &&
                     alternateLocations.length > DROP_PACKAGE_ALT_LOCATION_LIMIT )
                {
                    throw new InvalidMessageException( 
                        "Number of query response record alt-locs exceed the acceptable maximum: "
                        + alternateLocations.length + "/" + DROP_PACKAGE_ALT_LOCATION_LIMIT );
                }
                
                byte[] pathInfoArr = GGEPBlock.getExtensionDataInBlocks(
                    ggepBlocks, GGEPBlock.PATH_INFO_HEADER_ID );
                if ( pathInfoArr != null )
                {
                    pathInfo = new String( pathInfoArr );
                }
                creationTime = ggepBlock.getLongExtensionData( 
                    GGEPBlock.CREATION_TIME_HEADER_ID, -1 ) * 1000;
                
                if ( ggepBlock.isExtensionAvailable( GGEPBlock.LARGE_FILE_HEADER_ID ) ) 
                {
                    long tmpFileSize = ggepBlock.getLongExtensionData( GGEPBlock.LARGE_FILE_HEADER_ID, -1 );
                    // validate the file size
                    if ( tmpFileSize < 0)
                    {
                        throw new InvalidMessageException( "Invalid file size: " + tmpFileSize );
                    }
                    fileSize = tmpFileSize;
                }
            }
        }
        catch ( IOException exp )
        {// should never happen!!
            //logger.error( exp.toString(), exp );
        }
    }
    
    /**
     * Evaluates the extension tokens except GGEP extensions.
     * @param extension
     */
    private void evaluateExtensionToken( String extension ) 
    {
        // first check if this is the URN of the file
        if ( URN.isValidURN( extension ) )
        {
            urn = new URN( extension );
        }
        // otherwise is must be meta data or other extension... or??
        // meta data description ( like 44kHZ for mp3 )
        else
        {
            if ( metaData == null || metaData.length() == 0 )
            {// only parse metaData if not already found...
                metaData = parseMetaData( extension );
            }
        }
    }

    @Override
    public String toString()
    {
        return	"[" +
                "FileIndex=" + fileIndex + ", " +
                "FileSize=" + fileSize + ", " +
                "Filename=" + fileName +
                "]";
    }

    private String parseMetaData( String metaDataString )
    {
        // This is modified Limewire code... they seem to know what they are
        // doing in most cases... but I extended stuff a bit.

        StringTokenizer tokenizer = new StringTokenizer( metaDataString );
        if( tokenizer.countTokens() < 2 )
        {
            return "";
        }
        String first  = tokenizer.nextToken();
        String second = tokenizer.nextToken();
        String length="";
        String frequency = "";
        String bitrate="";
        boolean isVBR = false;
        boolean bearShare1 = false;
        boolean bearShare2 = false;
        boolean gnotella = false;
        if( second.toLowerCase().startsWith( "kbps" ) )
        {
            bearShare1 = true;
            if ( second.indexOf( "VBR" ) > 0 )
            {
                isVBR = true;
            }
        }
        else if ( first.toLowerCase().endsWith( "kbps" ) )
        {
            bearShare2 = true;
        }
        if( bearShare1 )
        {
            bitrate = first;
        }
        else if ( bearShare2 )
        {
            int j = first.toLowerCase().indexOf( "kbps" );
            bitrate = first.substring(0,j);
        }
        if( bearShare1 || bearShare2 )
        {
            String prev = "";
            String token = "";
            while( tokenizer.hasMoreTokens() )
            {
                token = tokenizer.nextToken();
                if ( token.startsWith( "kHz" ) )
                {
                    frequency = prev;
                }
                prev = token;
            }
            // last token is length
            length = token;
            //OK we have the bitrate and the length
        }
        else if ( metaDataString.endsWith( "kHz" ) )
        {//Gnotella
            gnotella = true;
            length=first;
            //extract the bitrate from second
            int i = second.indexOf( "kbps" );
            if(i>-1)
            {//see if we can find the bitrate
                bitrate = second.substring(0,i);
            }
            else
            {//not gnotella, after all...some other format we do not know
                gnotella=false;
            }
        }
        if(bearShare1 || bearShare2 || gnotella)
        {//some metadata we understand
            StringBuffer buffer = new StringBuffer();
            buffer.append( bitrate );
            buffer.append( "Kbps" );
            if ( isVBR )
            {
                buffer.append( "(VBR)" );
            }
            buffer.append( " - " );
            if ( frequency != null && frequency.length() > 0 )
            {
                buffer.append( frequency );
                buffer.append( "kHz");
                buffer.append( " - " );
            }
            buffer.append( length );
            return buffer.toString();
        }
        return "";
    }
}

