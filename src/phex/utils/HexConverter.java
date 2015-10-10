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
package phex.utils;

import phex.io.buffer.ByteBuffer;

/**
 * This class is able to convert byte arrays to hex strings and hex strings to
 * byte arrays.
 * You could do this with the standard J2SE calls Integer.toHexString( int ) and
 * Byte.valueOf( String, 16 ). But this code is highly optimized and outperforms
 * the standard J2SE 1.3 algorithem by the factor 4:1.
 */
public final class HexConverter
{
    /**
     * The shortcut array to convert from decimal to hex value.
     */
    private static final char[] hexValueArray;

    static
    {
        hexValueArray = new char[16];
        hexValueArray[0] = '0';
        hexValueArray[1] = '1';
        hexValueArray[2] = '2';
        hexValueArray[3] = '3';
        hexValueArray[4] = '4';
        hexValueArray[5] = '5';
        hexValueArray[6] = '6';
        hexValueArray[7] = '7';
        hexValueArray[8] = '8';
        hexValueArray[9] = '9';
        hexValueArray[10] = 'A';
        hexValueArray[11] = 'B';
        hexValueArray[12] = 'C';
        hexValueArray[13] = 'D';
        hexValueArray[14] = 'E';
        hexValueArray[15] = 'F';
    }
    
    /**
     * Converts the bytes in the ByteBuffer into a hex string. 
     * No separator is used between hex values.
     * @param buffer the byte buffer to generate a hex string from.
     * @param length the number of bytes to use.
     * @return a hex representation.
     */
    public static final String toHexString( ByteBuffer buffer, int length )
    {
        return toHexString( buffer, length, null );
    }
    
    /**
     * Converts the bytes in the ByteBuffer into a hex string. 
     * No separator is used between hex values.
     * @param buffer the byte buffer to generate a hex string from.
     * @param length the number of bytes to use.
     * @param separator the separator to use between each hex value.
     * @return a hex representation.
     */
    public static final String toHexString( ByteBuffer buffer, int length, String separator )
    {
        if ( length < 1 )
        {
            throw new IllegalArgumentException( "Length " + length + " < 1 " );
        }

        boolean isLimited = buffer.remaining() > length;
        int size;
        if ( isLimited ) 
        {
            size = length;
        } 
        else 
        {
            size = buffer.remaining();
        }

        if( size == 0 )
        {
            return "empty";
        }

        StringBuffer out = new StringBuffer( ( buffer.remaining() * 3 ) - 1 );

        int mark = buffer.position();

        int byteValue = buffer.get() & 0xFF;
        out.append( (byteValue >> 4) & 0x0F );
        out.append( byteValue & 0x0F );
        size --;

        while( size > 0 )
        {
            if ( separator != null )
            {
                out.append( separator );
            }
            byteValue = buffer.get();
            out.append( (byteValue >> 4) & 0x0F );
            out.append( byteValue & 0x0F );
            size --;
        }

        buffer.position( mark );
        
        if ( isLimited ) 
        {
            out.append("...");
        }

        return out.toString();
    }

    /**
     * Converts the data byte array into a hex string.
     */
    public static final String toHexString( byte[] data )
    {
        if ( data.length == 0 )
        {
            return "";
        }
        return toHexString( data, 0, data.length );
    }

    /**
     * Converts length bytes from the data byte array starting from offset into
     * a hex string. No separator is used between hex values.
     */
    public static final String toHexString( byte[] data, int offset, int length )
    {
        return toHexString( data, offset, length, null );
    }
    
    /**
     * Converts length bytes from the data byte array starting from offset into
     * a hex string. A separator can be provided.
     */
    public static final String toHexString( byte[] data, int offset, int length, String separator )
    {
        if ( length < 1 )
        {
            throw new IllegalArgumentException( "Length " + length + " < 1 " );
        }
        int end = offset + length;
        if ( offset > data.length || end > data.length )
        {
            throw new IndexOutOfBoundsException( "Data length: " + data.length +
                " offset: " + offset + " length: " + length );
        }

        StringBuffer buffer = new StringBuffer( length * 2 );
        buffer.append( hexValueArray[ (data[offset] >> 4) & 0x0F ] );
        buffer.append( hexValueArray[  data[offset] & 0x0F] );
        for ( int i = offset + 1; i < end; i++ )
        {
            if ( separator != null )
            {
                buffer.append( separator );
            }
            buffer.append( hexValueArray[ (data[i] >> 4) & 0x0F ] );
            buffer.append( hexValueArray[  data[i] & 0x0F] );
        }
        return buffer.toString();
    }

    /**
     * Converts a single byte into a hex string.
     */
    public static final String toHexString( byte data )
    {
        StringBuffer buffer = new StringBuffer( 2 );
        buffer.append( hexValueArray[ (data >> 4) & 0x0F ] );
        buffer.append( hexValueArray[  data & 0x0F] );
        return buffer.toString();
    }

    /**
     * Converts hexString into a byte array.
     */
    public static final byte[] toBytes( String hexString )
    {
        if ( hexString == null )
        {
            throw new NullPointerException( "HexString is null" );
        }
        int length = hexString.length();
        if ( length % 2 != 0 )
        {
            throw new NumberFormatException( "Hex string has odd characters: " + hexString );
        }

        byte[] data = new byte[ length / 2 ];
        char highChar, lowChar;
        byte highNibble, lowNibble;
        for (int i = 0, offset = 0; i < length; i += 2, offset ++ )
        {
            highChar = hexString.charAt( i );
            if ( highChar >= '0' && highChar <= '9')
            {
                highNibble = (byte)(highChar - '0');
            }
            else if (highChar >= 'A' && highChar <= 'F')
            {
                highNibble = (byte)(10 + highChar - 'A');
            }
            else if (highChar >= 'a' && highChar <= 'f')
            {
                highNibble = (byte)(10 + highChar - 'a');
            }
            else
            {
                throw new NumberFormatException( "Invalid hex char: " + highChar );
            }

            lowChar = hexString.charAt( i + 1 );
            if ( lowChar >= '0' && lowChar <= '9')
            {
                lowNibble = (byte)(lowChar - '0');
            }
            else if (lowChar >= 'A' && lowChar <= 'F')
            {
                lowNibble = (byte)(10 + lowChar - 'A');
            }
            else if (lowChar >= 'a' && lowChar <= 'f')
            {
                lowNibble = (byte)(10 + lowChar - 'a');
            }
            else
            {
                throw new NumberFormatException( "Invalid hex char: " + lowChar );
            }

            data[ offset ] = (byte)(highNibble << 4 | lowNibble);
        }
        return data;
    }
}