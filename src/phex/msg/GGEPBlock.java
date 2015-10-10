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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.DataFormatException;

import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.utils.IOUtil;

/**
 * Implementation for GGEP
 * Document Revision Version 0.51
 * Protocol Version 0.5
 */
public class GGEPBlock
{
    /**
     * This is a magic number is used to help distinguish GGEP extensions from
     * legacy data which may exist.  It must be set to the value 0xC3.
     */
    public static final byte MAGIC_NUMBER = (byte) 0xC3;

    /**
     * Browse host GGEP extension header ID.
     */
    public static final String BROWSE_HOST_HEADER_ID = "BH";
    public static final String ALTERNATE_LOCATIONS_HEADER_ID = "ALT";
    public static final String AVARAGE_DAILY_UPTIME = "DU";
    public static final String ULTRAPEER_ID = "UP";
    public static final String VENDOR_CODE_ID = "VC";
    public static final String PATH_INFO_HEADER_ID = "PATH";
    public static final String PUSH_PROXY_HEADER_ID = "PUSH";
    public static final String UDP_HOST_CACHE_UDPHC = "UDPHC";
    public static final String UDP_HOST_CACHE_IPP = "IPP";
    public static final String UDP_HOST_CACHE_SCP = "SCP";
    public static final String UDP_HOST_CACHE_PHC = "PHC";
    public static final String PHEX_EXTENDED_DESTINATION = "PHEX.EXDST";
    public static final String PHEX_EXTENDED_ORIGIN = "PHEX.EXORG";
    public static final String FEATURE_QUERY_HEADER_ID = "WH";
    public static final String CREATION_TIME_HEADER_ID = "CT";
    public static final String SECURE_OOB_ID = "SO";
    public static final String LARGE_FILE_HEADER_ID = "LF";

    private HashMap<String, byte[]> headerToDataMap;
    
    private boolean needsCobsFor0x00Byte;

    public GGEPBlock( boolean needsCobsFor0x00 )
    {
        this.needsCobsFor0x00Byte = needsCobsFor0x00;
        headerToDataMap = new HashMap<String, byte[]>( 3 );
    }
    
    public GGEPBlock( )
    {
        // unknown if we need cobs or not but to make sure useing it cant be bad.
        this( true );
    }

    public void debugDump()
    {
        System.out.println( "--------------------------------------" );
        for( String key : headerToDataMap.keySet() )
        {
            System.out.println( key + " = " + headerToDataMap.get( key ) );
        }
        System.out.println( "--------------------------------------" );
    }

    /**
     * Adds a GGEP extension to a extension block without a data segment.
     * @param header the header name of the extension
     */
    public void addExtension( String header )
    {
        addExtension( header, "".getBytes() );
    }

    /**
     * Adds a GGEP extension to a extension block with a data segment.
     * @param header the header name of the extension
     * @param data the data of the extension.
     */
    public void addExtension( String header, byte[] data )
    {
        headerToDataMap.put( header, data );
    }
    
    /** 
     * Adds a GGEP extension to a extension block with an integer value.
     * @param header the header name of the extension
     * @param value the integer data, it should be an unsigned integer value
     */
    public void addExtension( String header, int value )
    {
        addExtension( header, IOUtil.serializeInt2MinLE( value ) );
    }
    
    /** 
     * Adds a GGEP extension to a extension block with an long value.
     * @param header the header name of the extension
     * @param value the long data, it should be an unsigned long value
     */
    public void addExtension( String header, long value )
    {
        addExtension( header, IOUtil.serializeLong2MinLE( value ) );
    }
    
    /**
     * Adds multiple addresses as extension to this GGEP block in the common
     * standard IP format. Maximal maxAmount addresses are added.
     * @param header
     * @param addresses
     * @param maxAmount
     */
    public void addExtension( String header, DestAddress[] addresses,
        int maxAmount )
    {
        try
        {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            int count = Math.min( addresses.length, maxAmount );
            for ( int i = 0; i < count; i++ )
            {
                IpAddress ip = addresses[i].getIpAddress();
                if ( ip != null )
                {
                    outStream.write( ip.getHostIP() );
                    IOUtil.serializeShortLE( 
                        (short)addresses[i].getPort(), outStream );
                }
            }
            if ( outStream.size() > 0 )
            {
                addExtension( header, outStream.toByteArray() );
            }
        }
        catch ( IOException exp )
        {// this should never occur..
            //NLogger.error( GGEPBlock.class, exp, exp );
        }
    }
    
    public void addAllExtensions( GGEPBlock block )
    {
        headerToDataMap.putAll( block.headerToDataMap );
    }
    
    public byte[] getExtensionData( String header )
    {
        return headerToDataMap.get( header );
    }
    
    /**
     * Returns defaultValue in case no data is found or data is not valid.
     * @param header
     * @param defaultValue
     * @return the value of the extension or the given defaultValue in case
     *         data is not valid. 
     */
    public long getLongExtensionData( String header, long defaultValue )
    {
        byte[] data = getExtensionData( header );
        if (data == null || data.length < 1 || data.length > 8)
        {
            return defaultValue;
        }
        return IOUtil.deserializeLongLE( data, 0, data.length );
    }
    
    /**
     * Returns defaultValue in case no data is found or data is not valid.
     * @param header
     * @param defaultValue
     * @return the value of the extension or the given defaultValue in case
     *         data is not valid.
     */
    public byte getByteExtensionData( String header, byte defaultValue )
    {
        byte[] data = getExtensionData( header );
        if (data == null || data.length != 1 )
        {
            return defaultValue;
        }
        return data[0];
    }
    
    /**
     * Checks if the data associated with a header is in compressed form.
     * If it is compressed then the bit 5 ( starting from 0 )
     * of the header Flags is set
     * @author Madhu
     */
    private int checkIfCompressed( String header, int headerFlags )
    {
        if( header.equals( UDP_HOST_CACHE_PHC ) )
        {
            headerFlags |= 0x20;
        }
        return headerFlags;
    }
    
    private boolean checkIfNeedsCobsEncoding( byte[] data )
    {
        if ( needsCobsFor0x00Byte && contains0x00Byte( data ))
        {
            return true;
        }
        return false;
    }
    
    private boolean contains0x00Byte(byte[] bytes) 
    {
        if (bytes != null)
        {
            for (int i = 0; i < bytes.length; i++)
            {                
                if (bytes[i] == 0x00)
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    /**
     * Returns the byte representation of the GGEP block.
     * @return the byte representation of the GGEP block.
     */
    public byte[] getBytes()
    {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream( 30 );
        outStream.write( MAGIC_NUMBER );

        Iterator<String> iterator = headerToDataMap.keySet().iterator();
        while( iterator.hasNext() )
        {
            String headerKey = iterator.next();
            byte[] dataBytes = headerToDataMap.get( headerKey );

            int headerFlags = 0x00;
            
            // needed if we add compressed data with a headerKey. 
            // For ex the PHC extension
            headerFlags = checkIfCompressed( headerKey, headerFlags );
            boolean needsCobsEncoding = checkIfNeedsCobsEncoding( dataBytes );
            if ( needsCobsEncoding )
            {
                headerFlags |= 0x40;
                dataBytes = IOUtil.cobsEncode( dataBytes );
            }
            if ( !iterator.hasNext() )
            {
                headerFlags |= 0x80;
            }
            byte[] headerBytes = headerKey.getBytes();
            headerFlags = headerFlags | headerBytes.length;
            outStream.write( headerFlags );
            try
            {
                outStream.write( headerBytes );
            }
            catch ( IOException exp )
            {
                assert false : "Exception occured which should never happen.";
                throw new RuntimeException( exp );
            }

            int dataLength = dataBytes.length;
            int tmp = dataLength & 0x3f000;
            // first byte...
            if ( tmp != 0 )
            {
                // shift left to drop of non relevant bytes...
                tmp = tmp >> 12;
                tmp = 0x80 | tmp;
                outStream.write( tmp );
            }
            tmp = dataLength & 0xFC0;
            if ( tmp != 0 )
            {
                // shift left to drop of non relevant bytes...
                tmp = tmp >> 6;
                tmp = 0x80 | tmp;
                outStream.write( tmp );
            }

            tmp = dataLength & 0x3F;
            tmp = 0x40 | tmp;
            outStream.write( tmp );

            if ( dataLength > 0 )
            {
                try
                {
                    outStream.write( dataBytes );
                }
                catch ( IOException exp )
                {
                    assert false : "Exception occured which should never happen.";
                    throw new RuntimeException( exp );
                }
            }
        }
        
        return outStream.toByteArray();
    }

    /**
     * Checks if the extension with the given headerID is available.
     * @param headerID the header to look for.
     * @return true if the extension is available, false otherwise.
     */
    public boolean isExtensionAvailable( String headerID )
    {
        return headerToDataMap.containsKey( headerID );
    }


    //////////////////////// Static helpers ////////////////////////////////////


    private static byte[] browseHostGGEPBlock;

    public static byte[] getQueryReplyGGEPBlock( 
        boolean isBrowseHostSupported, DestAddress[] pushProxyAddresses )
    {
        if ( pushProxyAddresses != null && pushProxyAddresses.length > 0 )
        {// we need to create the GGEP block in realtime.
            GGEPBlock ggepBlock = new GGEPBlock( true );
            if ( isBrowseHostSupported )
            {
                ggepBlock.addExtension( GGEPBlock.BROWSE_HOST_HEADER_ID );
            }
            ggepBlock.addExtension(PUSH_PROXY_HEADER_ID, 
                pushProxyAddresses, 4);

            byte[] data = ggepBlock.getBytes();
            return data;
        }
        else if ( isBrowseHostSupported )
        {
            if ( browseHostGGEPBlock == null )
            {
                GGEPBlock ggepBlock = new GGEPBlock( true );
                ggepBlock.addExtension( GGEPBlock.BROWSE_HOST_HEADER_ID );
                browseHostGGEPBlock = ggepBlock.getBytes();
            }
            return browseHostGGEPBlock;
        }
        else
        {
            return IOUtil.EMPTY_BYTE_ARRAY;
        }
    }
    
    /**
     * 
     * @param creationTime in millis
     * @param alternateLocations
     * @return the ggep block data
     */
    public static byte[] getQueryReplyRecordGGEPBlock( long creationTime,
        DestAddress[] alternateLocations, long fileSize )
    {
        if ( creationTime > 0 || 
           ( alternateLocations != null && alternateLocations.length > 0 ) ||
           fileSize > Integer.MAX_VALUE )
        {// we need to create the GGEP block in realtime.
            GGEPBlock ggepBlock = new GGEPBlock( true );
            if ( creationTime > 0 )
            {
                ggepBlock.addExtension( GGEPBlock.CREATION_TIME_HEADER_ID, 
                    creationTime / 1000 );
            }
            if ( alternateLocations != null && alternateLocations.length > 0 )
            {
                ggepBlock.addExtension(ALTERNATE_LOCATIONS_HEADER_ID, 
                    alternateLocations, 10);
            }
            if ( fileSize > Integer.MAX_VALUE )
            {
                ggepBlock.addExtension( LARGE_FILE_HEADER_ID, fileSize );
            }
            byte[] data = ggepBlock.getBytes();
            return data;
        }
        else
        {
            return IOUtil.EMPTY_BYTE_ARRAY;
        }
    }

    /**
     * Returns if the extension is available in any GGEP block.
     * @param ggepBlocks
     * @param headerID
     * @return true is extension is found, false otherwise.
     */
    public static boolean isExtensionHeaderInBlocks( GGEPBlock[] ggepBlocks,
        String headerID )
    {
        for (int i = 0; i < ggepBlocks.length; i++ )
        {
            if ( ggepBlocks[i].isExtensionAvailable( headerID ) )
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns the extension if available in any GGEP block or null if not available.
     * @param ggepBlocks
     * @param headerID
     * @return the extension if available in any GGEP block or null if not available.
     */
    public static byte[] getExtensionDataInBlocks( GGEPBlock[] ggepBlocks,
        String headerID )
    {
        for (int i = 0; i < ggepBlocks.length; i++ )
        {
            if ( ggepBlocks[i].isExtensionAvailable( headerID ) )
            {
                return ggepBlocks[i].getExtensionData( headerID );
            }
        }
        return null;
    }
    
    public static GGEPBlock mergeGGEPBlocks( GGEPBlock[] ggepBlocks )
    {
        GGEPBlock mergedBlock = new GGEPBlock();
        for ( int i = 0; i < ggepBlocks.length; i++ )
        {
            mergedBlock.addAllExtensions( ggepBlocks[i] );
        }
        return mergedBlock;
    }
    
    public static GGEPBlock[] parseGGEPBlocks( byte[] body, int offset )
    {
        GGEPParser parser = new GGEPParser();
        return parser.parseGGEPBlocks( body, offset );
    }
    
    public static GGEPBlock[] parseGGEPBlocks( PushbackInputStream inStream )
        throws InvalidGGEPBlockException, IOException
    {
        GGEPParser parser = new GGEPParser();
        return parser.parseGGEPBlocks( inStream );
    }
    
    public static void debugDumpBlocks( GGEPBlock[] ggepBlocks )
    {
        for (int i = 0; i < ggepBlocks.length; i++ )
        {
            ggepBlocks[i].debugDump();
        }
    }


    private static class GGEPParser
    {
        private int offset;
        private List<GGEPBlock> ggepList;

        public GGEPParser( )
        {
            ggepList = new ArrayList<GGEPBlock>( 3 );
        }
        
        public GGEPBlock[] parseGGEPBlocks( PushbackInputStream inStream )
            throws InvalidGGEPBlockException, IOException
        {
            // the ggep specification requires us to support more then one GGEP
            // extension:
            // 'Extension blocks may contain an arbitrary number of GGEP blocks
            // packed one against another.'
            byte b;
            while ( true )
            {
                b = (byte)inStream.read();
                if ( b == -1 )
                {
                    break;
                }
                else if ( b != MAGIC_NUMBER )
                {
                    // not ggep anymore
                    // push back and break...
                    inStream.unread(b);
                    break;
                }
                ggepList.add( parseGGEPBlock( inStream ) );
            }

            GGEPBlock[] ggepArray = new GGEPBlock[ ggepList.size() ];
            ggepList.toArray( ggepArray );
            return ggepArray;
        }
        
        private GGEPBlock parseGGEPBlock( InputStream inStream )
            throws InvalidGGEPBlockException, IOException
        {
            GGEPBlock ggepBlock = new GGEPBlock();

            boolean isLastExtension = false;
            int b;
            while ( !isLastExtension)
            {
                // parse the extension header flags. They must be in form:
                // - 7: Last Extension
                // - 6: Encoding
                // - 5: Compression
                // - 4: Reserved ( must be 0 )
                // - 3-0: ID Len ( 1-15 )

                // validate extension byte
                b = inStream.read();
                if ( (b & 0x10) != 0)
                {
                    throw new InvalidGGEPBlockException();
                }
                // last bit in header
                isLastExtension = (b & 0x80) != 0;
                boolean isEncoded = (b & 0x40) != 0;
                boolean isCompressed = (b & 0x20) != 0;

                // first 4 bit
                short headerLength = (short) (b & 0x0F);
                if ( headerLength == 0 )
                {// 0 not allowed...
                    throw new InvalidGGEPBlockException();
                }
                
                byte[] headerData = new byte[ headerLength ];
                inStream.read( headerData, 0, headerLength );
                
                // parse the rest of the extension header.
                String header = new String( headerData, 0, headerLength );

                // parse the data length
                int dataLength = parseDataLength( inStream );
                byte[] dataArr = null;
                try
                {
                    if ( dataLength > 0 )
                    {
                        // get data as byte array...
                        dataArr = new byte[ dataLength ];
                        inStream.read( dataArr, 0, dataLength );
                        
                        if ( isCompressed )
                        {
                            // use zlib inflator to decompress
                            dataArr = IOUtil.inflate( dataArr );
                        }
    
                        if ( isEncoded )
                        {
                            try
                            {
                                dataArr = IOUtil.cobsDecode( dataArr );
                            }
                            catch ( IOException exp )
                            {// set to null in case of parsing error
                                dataArr = null;
                            }
                        }
                    }
                    else
                    {
                        dataArr = new byte[0];
                    }
                    // check if there was a parsing failure
                    if ( dataArr != null )
                    {
                        ggepBlock.addExtension( header, dataArr );
                    }
                }
                catch ( DataFormatException exp )
                {// in case the inflate data format does not work.
//                    if ( //NLogger.isWarnEnabled( GGEPBlock.class ) ) 
//                    {
//                        //NLogger.warn(GGEPBlock.class,
//                            "Invalid GGEP data format. Header: '" +
//                            header + "' Data: '"
//                            + HexConverter.toHexString(dataArr) + "'.", exp );
//                    }
                }
            }
            return ggepBlock;
        }
        
        /**
         * Code taken form GGEP specification document.
         */
        private int parseDataLength( InputStream inStream )
            throws InvalidGGEPBlockException, IOException
        {
            int length = 0;
            int byteCount = 0;
            byte currentByte;
            do
            {
                byteCount ++;
                if ( byteCount > 3 )
                {
                    throw new InvalidGGEPBlockException();
                }
                currentByte = (byte)inStream.read();
                length = (length << 6) | ( currentByte & 0x3F );
            }
            while ( 0x40 != (currentByte & 0x40) );
            return length;
        }

        public GGEPBlock[] parseGGEPBlocks( byte[] body, int aOffset )
        {
            offset = aOffset;
            try
            {
                // the ggep specification requires us to support more then one GGEP
                // extension:
                // 'Extension blocks may contain an arbitrary number of GGEP blocks
                // packed one against another.'
                // It could happen that the GUID appended to a query hit has
                // the MAGIC_NUMBER as its first byte. This might result in a
                // InvalidGGEPBlockException, but should not further disrupt parsing.
                while ( body.length > offset && body[ offset ] == MAGIC_NUMBER )
                {
                    // skip magic number
                    offset ++;
                    ggepList.add( parseGGEPBlock( body ) );
                }
            }
            catch ( InvalidGGEPBlockException exp )
            {// failed to further parse GGEP block.. skipping block
             // and follow-up blocks. Sucessfully parsed blocks are
             // used as valid results.
                //NLogger.debug( GGEPBlock.class, exp, exp );
            }

            GGEPBlock[] ggepArray = new GGEPBlock[ ggepList.size() ];
            ggepList.toArray( ggepArray );
            return ggepArray;
        }

        private GGEPBlock parseGGEPBlock( byte[] body )
            throws InvalidGGEPBlockException
        {
            GGEPBlock ggepBlock = new GGEPBlock();

            boolean isLastExtension = false;
            while ( !isLastExtension)
            {
                // parse the extension header flags. They must be in form:
                // - 7: Last Extension
                // - 6: Encoding
                // - 5: Compression
                // - 4: Reserved ( must be 0 )
                // - 3-0: ID Len ( 1-15 )

                // validate extension byte
                if ( body.length > offset && (body[offset] & 0x10) != 0)
                {
                    throw new InvalidGGEPBlockException();
                }
                // last bit in header
                isLastExtension = (body[offset] & 0x80) != 0;
                boolean isEncoded = (body[offset] & 0x40) != 0;
                boolean isCompressed = (body[offset] & 0x20) != 0;
                boolean isReserved = (body[offset] & 0x10) != 0;
                if( isReserved )
                {
                    throw new InvalidGGEPBlockException( "Reserved bit set to 1" );
                }

                // first 4 bit
                short headerLength = (short) (body[offset] & 0x0F);
                if ( headerLength == 0 )
                {// 0 not allowed...
                    throw new InvalidGGEPBlockException();
                }
                offset ++;

                // parse the rest of the extension header.
                String header = new String( body, offset, headerLength );
                offset += headerLength;

                // parse the data length
                int dataLength = parseDataLength( body );
                byte[] dataArr = null;
                try
                {
                    if ( dataLength > 0 )
                    {
                        // get data as byte array...
                        dataArr = new byte[ dataLength ];
                        try
                        {
                            System.arraycopy( body, offset, dataArr, 0, dataLength);
                        }
                        catch ( IndexOutOfBoundsException exp )
                        {
//                            if ( //NLogger.isDebugEnabled( GGEPBlock.class ) )
//                            {
//                                //NLogger.warn( GGEPBlock.class, exp, exp );
//                                //NLogger.warn( GGEPBlock.class, "Offset: " + offset 
//                                 //   + "- Buffer: " + HexConverter.toHexString( body ) );
//                            }
                            throw new InvalidGGEPBlockException( exp );
                        }
                        offset += dataLength;
                        
                        if ( isCompressed )
                        {
                            // use zlib inflator to decompress
                            dataArr = IOUtil.inflate( dataArr );
                        }
    
                        if ( isEncoded )
                        {
                            try
                            {
                                dataArr = IOUtil.cobsDecode( dataArr );
                            }
                            catch ( IOException exp )
                            {// set to null in case of parsing error
                                dataArr = null;
                            }
                        }
                    }
                    else
                    {
                        dataArr = new byte[0];
                    }
                    // check if there was a parsing failure
                    if ( dataArr != null )
                    {
                        ggepBlock.addExtension( header, dataArr );
                    }
                }
                catch ( DataFormatException exp )
                {// in case the inflate data format does not work.
//                    if ( //NLogger.isWarnEnabled( GGEPBlock.class ) ) 
//                    {
//                        //NLogger.warn(GGEPBlock.class,
////                            "Invalid GGEP data format. Header: '" +
////                            header + "' Data: '"
////                            + HexConverter.toHexString(dataArr) + "'.", exp );
//                        //NLogger.warn( GGEPBlock.class, "Offset: " + offset 
//                         //   + "- Buffer: " + HexConverter.toHexString( body ) );
//                    }
                }
            }
            return ggepBlock;
        }

        /**
         * Code taken form GGEP specification document.
         */
        private int parseDataLength( byte[] body )
            throws InvalidGGEPBlockException
        {
            int length = 0;
            int byteCount = 0;
            byte currentByte;
            do
            {
                byteCount ++;
                if ( byteCount > 3 )
                {
                    throw new InvalidGGEPBlockException();
                }
                currentByte = body[ offset ];
                offset ++;
                length = (length << 6) | ( currentByte & 0x3F );
            }
            while ( 0x40 != (currentByte & 0x40) );
            return length;
        }
    }
}