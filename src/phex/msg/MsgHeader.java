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
package phex.msg;

import phex.io.buffer.ByteBuffer;
import phex.prefs.core.MessagePrefs;
import phex.utils.IOUtil;

/**
 * <p>The header of a Gnutella message.</p>
 */
public class MsgHeader
{
    /**
     * <p>The length of a Gnutella message header in bytes.</p>
     *
     * <p>This will be 7 bytes greater than the size of a GUID.</p>
     */
    public static final int DATA_LENGTH = GUID.DATA_LENGTH + 7;

    // Function type constants
    /** Ping request for discovering network nodes. */
    public static final byte PING_PAYLOAD = (byte) 0x00;

    /** Pong response listing network nodes. */
    public static final byte PONG_PAYLOAD = (byte) 0x01;

    /**
     * Route table update message.
     */
    public static final byte ROUTE_TABLE_UPDATE_PAYLOAD = (byte) 0x30;

    /** Push */
    public static final byte PUSH_PAYLOAD = (byte) 0x40;

    /** Make a query request */
    public static final byte QUERY_PAYLOAD = (byte) 0x80;

    /** Response to a query */
    public static final byte QUERY_HIT_PAYLOAD = (byte) 0x81;

    public static final byte VENDOR_MESSAGE_PAYLOAD = (byte) 0x31;

    public static final byte STANDARD_VENDOR_MESSAGE_PAYLOAD = (byte) 0x32;

    public static final byte sUnknown = (byte) 0xFF;

    
    private GUID msgID;

    /**
     * The message payload.
     */
    private byte payload;

    private byte ttl;

    private byte hopsTaken;

    private int dataLength;

    private long arrivalTime;

    /**
     * Creates a new MsgHeader with the given payload and dataLength.
     * The GUID is newly generated, for ttl the application default is used,
     * hops is set to 0.
     * @param payload
     * @param dataLength
     */
    public MsgHeader( byte payload, int dataLength )
    {
        this(new GUID(), payload, MessagePrefs.TTL.get().byteValue(), (byte) 0,
            dataLength);
    }

    /**
     * Creates a new MsgHeader with the given payload, ttl and dataLength.
     * The GUID is newly generated, hops is set to 0.
     * @param payload
     * @param ttl
     * @param dataLength
     */
    public MsgHeader( byte payload, byte ttl, int dataLength )
    {
        this(new GUID(), payload, ttl, (byte) 0, dataLength);
    }
    
    public MsgHeader( GUID guid, byte payload, byte ttl, int dataLength )
    {
        this( guid, payload, ttl, (byte)0, dataLength );
    }

    /**
     * Creates a new MsgHeader with the given GUID, payload, ttl, hops and dataLength.
     * @param guid
     * @param payload
     * @param ttl
     * @param hops
     * @param dataLength
     */
    public MsgHeader( GUID guid, byte payload, byte ttl, byte hops,
        int dataLength )
    {
        msgID = guid;
        this.payload = payload;
        this.ttl = ttl;
        hopsTaken = hops;
        this.dataLength = dataLength;
    }

    /**
     * <p>Get the GUID of this message.</p>
     *
     * <p>Each message should have a different GUID. This allows messages
     * forwarded by different paths to be treated non-redundantly.</p>
     *
     * @return the GUID of the remote host
     */
    public GUID getMsgID()
    {
        return msgID;
    }

    /**
     * Set the GUID associated with this message.
     *
     * @param MsgID  the GUID of the servent originating this message
     */
    public void setMsgID(GUID MsgID)
    {
        this.msgID = MsgID;
    }

    /**
     * Get the function that defines how to interpret the payload of this message.
     *
     * @return an int encoding the function of this message
     */
    public byte getPayload()
    {
        return payload;
    }

    /**
     * Set the payload that defines how to interpret the data of this message.
     * @param payload  an int encoding the new function for this message
     */
    public void setPayloadType( byte payload )
    {
        this.payload = payload;
    }

    /**
     * Produce the name of the function type of this header.
     *
     * @return  a human-readable String stating the type of function this header
     *          has associated
     */
    private String getPayloadName()
    {
        switch (payload)
        {
        case PING_PAYLOAD:
            return "Ping";
        case PONG_PAYLOAD:
            return "Pong";
        case ROUTE_TABLE_UPDATE_PAYLOAD:
            return "RouteTableUpdate";
        case PUSH_PAYLOAD:
            return "Push";
        case QUERY_PAYLOAD:
            return "Query";
        case QUERY_HIT_PAYLOAD:
            return "QueryHit";
        case VENDOR_MESSAGE_PAYLOAD:
            return "Vendor";
        case STANDARD_VENDOR_MESSAGE_PAYLOAD:
            return "Vendor(st)";
            
        default:
            return "Unknown";
        }
    }

    /**
     * <p>Get the time to live for this message.</p>
     *
     * <p>This states the number of network hops this message can make before
     * being dropped from the network. Each time a message passes through a
     * network node, the ttl value will be decremented. Once zero, it will not be
     * forwarded, but will be silently dropped instead.</p>
     *
     * @return the time to live of this message
     */
    public byte getTTL()
    {
        return ttl;
    }

    /**
     * Set the time to live value for this message.
     * 
     * @param ttl the new ttl value
     */
    public void setTTL( byte ttl )
    {
        this.ttl = ttl;
    }

    /**
     * <p>The number of hops this message has taken so far through the network.
     * </p>
     *
     * <p>This value will be incremented each time that the message is
     * propogated.</p>
     *
     * @return the number of hops this message has taken so far
     */
    public byte getHopsTaken()
    {
        return hopsTaken;
    }

    /**
     * Set the number of hops taken by this message.
     *
     * @param HopsTaken  the number of hops this message has taken through the
     *                   network
     */
    public void setHopsTaken(byte HopsTaken)
    {
        this.hopsTaken = HopsTaken;
    }

    /**
     * Counts a hop by incrementing hops and decrementing ttl.
     */
    public void countHop()
    {
        if (ttl > 0)
        {
            ttl--;
        }
        hopsTaken++;
    }

    /**
     * <p>The length of the data following this message in bytes.</p>
     *
     * <p>This is the payload length. It shouldn't realy be greater than 4k. The
     * next message, if there is one, is located exactly this many bytes from
     * here, and will begin like this message with a message header.</p>
     *
     * @return the length of the data in this message
     */
    public int getDataLength()
    {
        return dataLength;
    }

    /**
     * <p>Set the length of the data associated with this header.</p>
     *
     * @param DataLen  the exact length (in bytes) of the message associated with
     *                 this header
     */
    public void setDataLength(int DataLen)
    {
        this.dataLength = DataLen;
    }

    /**
     * The time this message arrived.
     *
     * @return the arrival time
     */
    public long getArrivalTime()
    {
        return arrivalTime;
    }

    /**
     * Set the time this message arrived.
     *
     * @param arrivalTime  the new time of arrival
     */
    public void setArrivalTime(long arrivalTime)
    {
        this.arrivalTime = arrivalTime;
    }

    /**
     * Copy all the properties of a message header into this header.
     *
     * @param b  the MsgHeader instance to copy
     */
    public void copy(MsgHeader b)
    {
        msgID = b.msgID;
        payload = b.payload;
        ttl = b.ttl;
        hopsTaken = b.hopsTaken;
        dataLength = b.dataLength;
    }
    
    public ByteBuffer createHeaderBuffer()
    {
        ByteBuffer buffer = ByteBuffer.wrap( getBytes() );
        return buffer;
    }

    /**
     * gets the header in bytes form
     * @return
     * byte array containing the header
     * @author Madhu
     */
    public byte[] getBytes()
    {
        byte[] tmpArray = new byte[DATA_LENGTH];
        byte[] guid = msgID.getGuid();
        System.arraycopy(guid, 0, tmpArray, 0, GUID.DATA_LENGTH);
        tmpArray[16] = payload;
        tmpArray[17] = ttl;
        tmpArray[18] = hopsTaken;
        IOUtil.serializeIntLE(dataLength, tmpArray, 19);
        return tmpArray;
    }

    @Override
    public String toString()
    {
        return "Header[" + payload + "-" + getPayloadName() + ",TTL=" + ttl
            + ",Hop=" + hopsTaken + ",len=" + dataLength + ",GUID=" + msgID
            + " ]";
    }

    /**
     * creates a message header from a given buffer
     * sets almost everything, only thing you should set is fromHost
     * 
     * @param inbuf buffer array in bytes[]
     * @param offset from where to start reading
     * @return MsgHeader
     * @throws InvalidMessageException
     */
    public static MsgHeader createMsgHeader( byte[] inbuf, int offset ) 
    	throws InvalidMessageException
    {
        if( (inbuf.length - offset) < DATA_LENGTH )
        {
            throw new InvalidMessageException( "The byte array length is less then " +
            		"the message header length." );
        }
        
        // Copy input buffer to my content.
        byte[] guidBytes = new byte[GUID.DATA_LENGTH];
        System.arraycopy(inbuf, offset, guidBytes, 0, GUID.DATA_LENGTH);
        GUID guid = new GUID( guidBytes );
        offset += GUID.DATA_LENGTH;
        
        byte payload = inbuf[offset++];
        byte ttl = inbuf[offset++];
        byte hopsTaken = inbuf[offset++];
        int dataLen = IOUtil.deserializeIntLE(inbuf, offset);
        offset += 4;
        
        MsgHeader header = new MsgHeader( guid, payload, ttl, hopsTaken,
            dataLen );
       
        int length = header.getDataLength();
        
        if ( length < 0 )
        {
            throw new InvalidMessageException( "Negative body size when creating" +
            		" header from byte array.");
        }
        else if ( length > MessagePrefs.MaxLength.get().intValue() )
        {
//           NLogger.warn( MsgHeader.class, "Body too big("+length+"). Header: " + 
//               header + " Byte Array : " + new String( inbuf ) 
//                   );
           throw new InvalidMessageException("Package too big when creating Message " +
           		"from byte array: " + header.getDataLength() );
        }
        header.setArrivalTime( System.currentTimeMillis() );

        return header;
    }
}

