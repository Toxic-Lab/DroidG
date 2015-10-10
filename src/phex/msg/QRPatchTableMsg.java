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
package phex.msg;

import phex.io.buffer.ByteBuffer;


public class QRPatchTableMsg extends RouteTableUpdateMsg
{
    /**
     * Constant when no compressor is used.
     */
    public static final byte COMPRESSOR_NONE = 0x00;

    /**
     * Constant when zlib compressor is used.
     */
    public static final byte COMPRESSOR_ZLIB = 0x01;

    /**
     * Constant that defines the max size of the message data.
     */
    public static final int MAX_MESSAGE_DATA_SIZE = 4 * 1024; // 4KB

    private byte sequenceNumber;
    private byte sequenceSize;
    private byte compressor;
    private byte entryBits;
    private byte[] data;
    private int dataOffset;
    private int dataLength;

    public QRPatchTableMsg( byte aSequenceNumber, byte aSequenceSize,
        byte aCompressor, byte aEntryBits, byte[] aData, int aDataOffset,
        int aDataLength )
    {
        // length = variant: 1 + SEQ_NO: 1 + SEQ_SIZE: 1 + COMPRESSOR: 1
        //    + ENTRY_BITS: 1 + data length
        super( PATCH_TABLE_VARIANT, 5 + aDataLength );
        sequenceNumber = aSequenceNumber;
        sequenceSize = aSequenceSize;
        compressor = aCompressor;
        entryBits = aEntryBits;
        data = aData;
        dataOffset = aDataOffset;
        dataLength = aDataLength;
    }

    public QRPatchTableMsg( MsgHeader header, byte[] aBody )
        throws InvalidMessageException
    {
        super( PATCH_TABLE_VARIANT, header );
        header.setDataLength( aBody.length );

        // since we dont forward this message we are not memorizing the body!

        sequenceNumber = aBody[1];
        sequenceSize = aBody[2];
        // validate
        if ( sequenceNumber == 0 || sequenceSize == 0 ||
            sequenceNumber > sequenceSize )
        {
            throw new InvalidMessageException(
                "Invalid sequence number or size: " + sequenceNumber + "/" +
                sequenceSize );
        }
        compressor = aBody[3];
        // validate
        if ( !(compressor == COMPRESSOR_NONE || compressor == COMPRESSOR_ZLIB) )
        {
            throw new InvalidMessageException(
                "Invalid compressor type: " + compressor );
        }
        entryBits = aBody[4];
        dataOffset = 0;
        dataLength = aBody.length - 5;
        data = new byte[ dataLength ];
        System.arraycopy( aBody, 5, data, 0, dataLength );
    }


    public byte getSequenceNumber()
    {
        return sequenceNumber;
    }

    public byte getSequenceSize()
    {
        return sequenceSize;
    }

    public byte getCompressor()
    {
        return compressor;
    }

    public byte getEntryBits()
    {
        return entryBits;
    }

    public byte[] getPatchData()
    {
        return data;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer createMessageBuffer()
    {
        ByteBuffer buffer = ByteBuffer.allocate( dataLength + 5 );
        buffer.put( variant )
              .put( sequenceNumber )
              .put( sequenceSize )
              .put( compressor )
              .put( entryBits )
              .put( data, dataOffset, dataLength );
        buffer.rewind();
        return buffer;
    }
}