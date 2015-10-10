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

import java.io.*;
import java.net.*;
import java.util.*;


import phex.common.address.IpAddress;
import phex.utils.*;


/**
 * <p><u>G</u>lobaly <u>U</u>nique <u>ID</u>entifier.</p>
 *
 * <p>GUIDs uniquely (we hope) identifie a gnutella node or message message.
 * It is composed from 16 bytes. The GUID of a phex host is randomly generated
 * by using the java.util.Random class and setting bytes 7 and 15 to OxFF and
 * Ox00 respectively as per
 * <a href="http://groups.yahoo.com/group/the_gdf/message/1397">
 * http://groups.yahoo.com/group/the_gdf/message/1397</a>.
 * </p>
 */
public class GUID implements Serializable
{
    public static final int DATA_LENGTH = 16;

    // Bearshare seems to send massive Pings with empty guid
    public static final GUID EMPTY_GUID;
    public static final Random randomizer;

    static
    {
        long time = System.currentTimeMillis();
        int ipValue;
        try
        {
            ipValue = IOUtil.deserializeIntLE(
                InetAddress.getLocalHost().getAddress(), 0 );
        }
        catch (Exception e)
        {
            ipValue = 0;
        }
        int shift = IOUtil.determineBitCount( ipValue );
        long seed = time << shift;
        seed = seed + ipValue;
        randomizer = new Random( seed );
        
        EMPTY_GUID = new GUID( 
            new byte[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
    }

    // Atributes
    private byte[] bytes;
    private String stringRepresentation = null;

    /**
     * Create a GUID for this server.
     */
    public GUID()
    {
        bytes = new byte[DATA_LENGTH];
        randomizer.nextBytes( bytes );

        // to meet current protocol standard set byte 9 to 0xFF and byte 16 to 0
        // see http://groups.yahoo.com/group/the_gdf/message/1397
        bytes[8] = (byte) 0xFF;
        bytes[15] = 0;
    }

    /**
     * <p>Factory method to create a new GUID from a hexadecimal string
     * image.</p>
     *
     * @param hexValue  a String representing a 16 byte hexadecimal value.
     * @return  a new GUID with a byte image taken from hexValue
     */
    public GUID( String hexValue )
    {
        fromHexString( hexValue );
    }

    /**
     * <p>Create a GUID from a byte array.</p>
     *
     * <p>The byte array passed in must be of length 16. The byte array becomes
     * owned by this GUID object. It should not be modified elsewhere.</p>
     *
     * @param guidBytes  the bytes to use
     * @throws IllegalArgumentException  if guidBytes is not 16 long
     */
    public GUID(byte[] guidBytes)
    {
        if(!(guidBytes.length == 16))
        {
            throw new IllegalArgumentException(
                "Attempted to construct a GUID from an array of bytes " +
                "not 16 long: " + guidBytes.length
            );
        }

        bytes = guidBytes;
        stringRepresentation = null;
    }

    /**
     * <p>Set the GUID image to a copy of the 16 bytes of guidBytes.</p>
     *
     * <p>guidBytes can be modified at will after calling this method as a copy
     * of the data is taken internaly. If guidBytes is shorter than 16 bytes
     * then System.arraycopy() will raise an exception.</p>
     *
     * @param guidBytes a 16 element byte array containing a GUID image to copy
     */
    public void setGuid(byte[] guidBytes)
    {
        System.arraycopy(guidBytes, 0, bytes, 0, DATA_LENGTH);
        stringRepresentation = null;
    }

    /**
     * <p>Return the 16 byte GUID image.</p>
     *
     * <p><em>Important:</em> Do not modify the return value.</p>
     *
     * @return the current 16 byte GUID image
     */
    public byte[] getGuid()
    {
        // The caller better not modified it.
        return bytes;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof GUID )
        {
            return Arrays.equals( bytes, ((GUID)obj).bytes );
        }
        return false;
    }

    /**
     * <p>Appears to be identical to setGuid().</p>
     *
     * @param guidBytes  the byte array to copy from
     */
    public void copy(byte[] guidBytes)
    {
        System.arraycopy(guidBytes, 0, bytes, 0, DATA_LENGTH);
        stringRepresentation = null;
    }

    /**
     * <p>Copy the byte image of this GUID into outbuf, starting at byte offset
     * in outbuf, returning the first index after the last byte written.</p>
     *
     * @param outbuf  the byte buffer to write to
     * @param offset  where to start writing to
     * @return  the index of the first byte after the last byte written
     */
    public int serialize(byte[] outbuf, int offset)
    {
        // Copy my content to output buffer.
        System.arraycopy(bytes, 0, outbuf, offset, DATA_LENGTH);

        // return new offset
        return offset + DATA_LENGTH;
    }
    
    /**
     * <p>Copy the byte image of this GUID into outbuf, starting at byte offset
     * in outbuf, returning the first index after the last byte written.</p>
     *
     * @param outbuf  the byte buffer to write to
     * @param offset  where to start writing to
     * @return  the index of the first byte after the last byte written
     */
    public void write( OutputStream stream )
        throws IOException
    {
        stream.write( bytes );
    }

    /**
     * <p>Copy a GUID image out of inbuf starting at offset, and return the
     * index of the first byte not read.</p>
     *
     * @param inbuf  a byte buffer to read
     * @param offset  where to start reading
     * @return  the index of the first byte not read
     */
    public int deserialize(byte[] inbuf, int offset)
    {
        // Copy input buffer to my content.
        System.arraycopy(inbuf, offset, bytes, 0, DATA_LENGTH);
        stringRepresentation = null;

        // return new offset
        return offset + DATA_LENGTH;
    }

    @Override
    public String toString()
    {
        if (stringRepresentation == null)
        {
            stringRepresentation = generateString();
        }
        return stringRepresentation;
    }


    private String generateString()
    {
        StringBuffer buffer = new StringBuffer( 20 );
        buffer.append( HexConverter.toHexString( bytes, 0, 4 ) );
        buffer.append( '-' );
        buffer.append( HexConverter.toHexString( bytes, 4, 2) );
        buffer.append( '-' );
        buffer.append( HexConverter.toHexString( bytes, 6, 2) );
        buffer.append( '-' );
        buffer.append( HexConverter.toHexString( bytes, 8, 2) );
        buffer.append( '-' );
        buffer.append( HexConverter.toHexString( bytes, 10, 6) );
        return buffer.toString();
    }

    /**
     * Return a view of this GUID as hexadecimal.
     *
     * @return a hex image of this GUID
     */
    public String toHexString()
    {
        return HexConverter.toHexString( bytes );
    }

    /**
     * <p>Set the byte image of this GUID using a hexadecimal string image.</p>
     *
     * <p><em>Fixme:</em> Do we need to be checking that this hex string
     * represents 16 bytes?</p>
     *
     * @param hexValue  a String representing a 16 byte hexadecimal value.
     */
    public void fromHexString( String hexValue )
    {
        bytes = HexConverter.toBytes( hexValue );
        stringRepresentation = null;
    }

    @Override
    public int hashCode()
    {
        int v1, v2, v3, v4;

        v1 = (0xFF & bytes[0]) | (0xFF00 & bytes[1] << 8)
            | (0xFF0000 & bytes[2] << 16) | ( bytes[3] << 24 );
        v2 = (0xFF & bytes[4]) | (0xFF00 & bytes[5] << 8)
            | (0xFF0000 & bytes[6] << 16) | ( bytes[7] << 24 );
        v3 = (0xFF & bytes[8]) | (0xFF00 & bytes[9] << 8)
            | (0xFF0000 & bytes[10] << 16) | ( bytes[11] << 24 );
        v4 = (0xFF & bytes[12]) | (0xFF00 & bytes[13] << 8)
            | (0xFF0000 & bytes[14] << 16) | ( bytes[15] << 24 );

        return v1^v2^v3^v4;
    }
    
    public static void applyOOBQueryMarkings( GUID guid, IpAddress ip, int port )
    {
        byte[] ipBytes = ip.getHostIP();
        for (int i = 0; i < 4; i++)
        {
            guid.bytes[i] = ipBytes[i];
        }
        IOUtil.serializeShortLE( (short)port, guid.bytes, 13 );
    }

    public static class GUIDComparator implements Comparator<GUID>
    {
        public int compare( GUID g1, GUID g2 )
        {
            int diff;
            for ( int i = 0; i < DATA_LENGTH; i++ )
            {
                diff = g1.bytes[i] - g2.bytes[i];
                if ( diff != 0 )
                {
                    return diff;
                }
            }
            return 0;
        }
    }
}