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
package phex.utils;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.common.file.ReadOnlyManagedFile;



public class IOUtil
{
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    
    private IOUtil()
    {}

    public static int serializeIntLE(int value, byte[] outbuf, int offset)
    {
        outbuf[offset++] = (byte)(value);
        outbuf[offset++] = (byte)(value >> 8);
        outbuf[offset++] = (byte)(value >> 16);
        outbuf[offset++] = (byte)(value >> 24);

        // Return next offset.
        return offset;   
    }
    
    /**
     * Converts a int to a little-endian byte representation and writes it to the 
     * given stream.
     * @throws IOException if an IO error occurs
     */
    public static void serializeIntLE( int value, OutputStream outStream )
        throws IOException
    {
        outStream.write( (byte)(value) );
        outStream.write( (byte)(value >> 8) );
        outStream.write( (byte)(value >> 16) );
        outStream.write( (byte)(value >> 24) );
    }

    public static int deserializeIntLE(byte[] inbuf, int offset)
    {
        return  (inbuf[offset + 3]      ) << 24 |
                (inbuf[offset + 2] &0xff) << 16 |
                (inbuf[offset + 1] &0xff) <<  8 |
                (inbuf[offset]     &0xff);
    }
    
    public static int deserializeInt( byte[] inbuf, int offset )
    {
        return  (inbuf[offset]          ) << 24 |
                (inbuf[offset + 1] &0xff) << 16 |
                (inbuf[offset + 2] &0xff) << 8  |
                (inbuf[offset + 3] &0xff);
    }
    
    @SuppressWarnings( value="SF_SWITCH_FALLTHROUGH" )
    public static int deserializeIntLE(byte[] inbuf, int offset, int length)
    {
        int a,b,c,d;
        b = c = d = 0x00;
        switch ( length )
        {
            case 4:
                d = (inbuf[offset + 3]      ) << 24;
              // nobreak
            case 3:
                c = (inbuf[offset + 2] &0xff) << 16;
              // nobreak
            case 2:
                b = (inbuf[offset + 1] &0xff) <<  8;
             // nobreak
            case 1:
                a = (inbuf[offset]     &0xff);
                break;
            default:
                throw new IllegalArgumentException( "Wrong length: " + length);
        }
        return d|c|b|a;
    }
    
    @SuppressWarnings( value="SF_SWITCH_FALLTHROUGH" )
    public static long deserializeLongLE(byte[] inbuf, int offset, int length)
    {
        long a,b,c,d,e,f,g,h;
        b = c = d = e = f = g = h = 0x00;
        switch ( length )
        {
            case 8:
                h = (inbuf[offset + 7]&0xFFL) << 56;
             // nobreak
            case 7:
                g = (inbuf[offset + 6]&0xFFL ) << 48;
             // nobreak
            case 6:
                f = (inbuf[offset + 5]&0xFFL ) << 40;
             // nobreak
            case 5:
                e = (inbuf[offset + 4]&0xFFL ) << 32;
             // nobreak
            case 4:
                d = (inbuf[offset + 3]&0xFFL ) << 24;
             // nobreak
            case 3:
                c = (inbuf[offset + 2]&0xFFL ) << 16;
             // nobreak
            case 2:
                b = (inbuf[offset + 1]&0xFFL ) <<  8;
             // nobreak
            case 1:
                a = (inbuf[offset]    &0xFFL );
                break;
            default:
                throw new IllegalArgumentException( "Wrong length: " + length);
        }
        return h|g|f|e|d|c|b|a;
    }
    
    /**
     * Returns the minimum bytes needed for the given value to encoded it in
     * little-endian format. Value must be positive.
     * @param value
     * @return the minimum bytes needed for little-endian format
     */
    public static byte[] serializeInt2MinLE( int value )
    {
        if ( value < 0 )
        {
            throw new IllegalArgumentException( "Negative input value" );
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream( 4 );
        do
        {
            byteStream.write( value & 0xFF);
            value >>= 8;
        }
        while ( value !=0 );
        return byteStream.toByteArray();
    }
    
    /**
     * Returns the minimum bytes needed for the given value to encoded it in
     * little-endian format. Value must be positive.
     * @param value
     * @return
     */
    public static byte[] serializeInt2MinLE( long value )
    {
        if ( value <= 0xFFFF )
        {
            if ( value <= 0xFF )
            {
                if ( value < 0 )
                {
                    throw new IllegalArgumentException( "Negative input value" );
                }
                return new byte[] { (byte) value };
            }
            return new byte[] { 
                   (byte) value, 
                   (byte) (value >> 8) };
        }
        if ( value <= 0xFFFFFF )
        {
            return new byte[] { 
                (byte) value, 
                (byte) (value >> 8), 
                (byte) (value >> 16) };
        }
        return new byte[] { 
            (byte) value, 
            (byte) (value >> 8), 
            (byte) (value >> 16),
            (byte) (value >> 24) };
    }
    
    /**
     * Returns the minimum number of bytes needed to encode it in little-endian
     * format. Value must be positive.
     * @param value
     */
    public static byte[] serializeLong2MinLE( long value )
    {
        if ( value <= 0xFFFFFFFFFFFFFFL )
        {
            if ( value <= 0xFFFFFFFFFFFFL )
            {
                if ( value <= 0xFFFFFFFFFFL )
                {
                    if ( value <= 0xFFFFFFFFL )
                    {
                        if ( value <= 0xFFFFFFL )
                        {
                            if ( value <= 0xFFFFL )
                            {
                                if ( value <= 0xFFL )
                                {
                                    if ( value < 0 )
                                    {
                                        throw new IllegalArgumentException( "value negative" );
                                    }
                                    return new byte[] { (byte) value };
                                }
                                return new byte[] { 
                                    (byte) value, 
                                    (byte) (value >> 8) };
                            }
                            return new byte[] { 
                                (byte) value, 
                                (byte) (value >> 8),
                                (byte) (value >> 16) };
                        }
                        return new byte[] { 
                            (byte) value, 
                            (byte) (value >> 8),
                            (byte) (value >> 16), 
                            (byte) (value >> 24) };
                    }
                    return new byte[] { 
                        (byte) value, 
                        (byte) (value >> 8), 
                        (byte) (value >> 16),
                        (byte) (value >> 24), 
                        (byte) (value >> 32) };
                }
                return new byte[] { 
                    (byte) value, 
                    (byte) (value >> 8), 
                    (byte) (value >> 16),
                    (byte) (value >> 24), 
                    (byte) (value >> 32),
                    (byte) (value >> 40) };
            }
            return new byte[] { 
                (byte) value, 
                (byte) (value >> 8), 
                (byte) (value >> 16),
                (byte) (value >> 24), 
                (byte) (value >> 32),
                (byte) (value >> 40), 
                (byte) (value >> 48) };
        }
        return new byte[] { 
            (byte) value, 
            (byte) (value >> 8), 
            (byte) (value >> 16),
            (byte) (value >> 24), 
            (byte) (value >> 32), 
            (byte) (value >> 40),
            (byte) (value >> 48), 
            (byte) (value >> 56) };
    }
    
    public static byte[] serializeLong(long value) 
    {
        return new byte[] { 
            (byte) (value >> 56), 
            (byte) (value >> 48), 
            (byte) (value >> 40),
            (byte) (value >> 32), 
            (byte) (value >> 24), 
            (byte) (value >> 16),
            (byte) (value >> 8 ), 
            (byte) (value      ) };
    }
    
    public static long deserializeLong( byte[] inbuf, int offset )
    {
        return  ((inbuf[offset    ] & 0xFFL) << 56)
              | ((inbuf[offset + 1] & 0xFFL) << 48)
              | ((inbuf[offset + 2] & 0xFFL) << 40)
              | ((inbuf[offset + 3] & 0xFFL) << 32)
              | ((inbuf[offset + 4] & 0xFFL) << 24)
              | ((inbuf[offset + 5] & 0xFFL) << 16)
              | ((inbuf[offset + 6] & 0xFFL) <<  8)
              | ((inbuf[offset + 7] & 0xFFL)      );
    }

    /**
     * Serialize a short, and convert it to little endian in the process
     *
     * @param value
     * @param outbuf
     * @param offset
     * @return int indicating the next offset
     */
    public static int serializeShortLE(short value, byte[] outbuf, int offset)
    {
        outbuf[offset++] = (byte)(value);
        outbuf[offset++] = (byte)(value >> 8);

        // Return next offset.
        return offset;
    }
    
    /**
     * Converts a short to a little-endian byte representation and writes it to the 
     * given stream.
     */
    public static void serializeShortLE( short value, OutputStream outStream )
        throws IOException
    {
        outStream.write( (byte)(value&0xFF) );
        outStream.write( (byte)((value >> 8) &0xFF) );
    }


    /**
     * Serialize a short, but do not convert it to little endian in the process
     *
     * @param value
     * @param outbuf
     * @param offset
     * @return int indicating the next offset
     */
    public static int serializeShort(short value, byte[] outbuf, int offset)
    {
        outbuf[offset++] = (byte)(value >> 8);
        outbuf[offset++] = (byte)(value);

        // Return next offset.
        return offset;
    }
    
    /**
     * Serialize a short, but do not convert it to little endian in the process 
     * given stream.
     */
    public static void serializeShort( short value, OutputStream outStream )
        throws IOException
    {
        outStream.write( (byte)((value >> 8) &0xFF) );
        outStream.write( (byte)(value&0xFF) );
    }

    /**
     * Deserialize a short, but do not convert it from little endian in the process
     * @param inbuf
     * @param offset
     * @return short
     */
    public static short deserializeShort(byte[] inbuf, int offset)
    {
        return  (short) ((inbuf[offset] &0xff) <<  8 |
                         (inbuf[offset + 1]     &0xff));
    }
    
    /**
     * Deserialize a short, but do not convert it from little endian in the process
     * @param inStream
     * @return short
     */
    public static short deserializeShort( InputStream inStream )
        throws IOException
    {
        int a = (inStream.read() & 0xFF) <<8;
        int b = inStream.read() & 0xFF;
        return (short) ( a | b );
    }
    
    /**
     * Deserialize a short, and convert it from little endian in the process
     * @param inbuf
     * @param offset
     * @return short
     */
    public static short deserializeShortLE(byte[] inbuf, int offset)
    {
        return  (short) ((inbuf[offset + 1] &0xff) <<  8 |
                         (inbuf[offset]     &0xff));
    }
    
    /**
     * Deserialize a short, and convert it from little endian in the process
     * @param inbuf
     * @param offset
     * @return short
     */
    public static short deserializeShortLE( InputStream inStream )
        throws IOException
    {
        int a = inStream.read() & 0xFF;
        int b = (inStream.read() & 0xFF) <<8;
        return (short) ( b | a );
    }


    public static int unsignedByte2int( byte x )
    {
        return x & 0xFF;
    }

    public static int unsignedShort2Int( short x )
    {
        return x & 0xFFFF;
    }

    public static long unsignedInt2Long( int x )
    {
        return x & 0xFFFFFFFFL;
    }
    
    public static int castLong2Int( long x )
    {
        if ( x >= Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }
        else if ( x <= Integer.MIN_VALUE)
        {
            return Integer.MIN_VALUE;
        }
        return (int)x;
    }
    
    
    
    public static int serializeString(String str, byte[] outbuf, int offset)
    {
        char[] chars = str.toCharArray();
        // Strip off the hi-byte of the char.  No good.
        for (int i = 0; i < chars.length; i++)
        {
            outbuf[offset] = (byte)chars[i];
            offset++;
        }

        return offset;
    }

    public static int deserializeString(byte[] inbuf, int offset, StringBuffer outbuf)
    {
        int begin = offset;
        int maxLen = inbuf.length;

        while (offset < maxLen)
        {
            if (inbuf[offset] == 0)
            {
                // Note that the terminating 0 is not added in the returning offset.
                break;
            }
            offset++;
        }
        if (offset-begin > 0)
            outbuf.append(new String(inbuf, begin, offset-begin));

        return offset;
    }


    public static int serializeIP(String ip, byte[] outbuf, int offset)
    {
        InetAddress inet = null;
        byte[] addrBuf;
        try
        {
            inet = InetAddress.getByName(ip);
            addrBuf = inet.getAddress();
        }
        catch (Exception e)
        {
            addrBuf = new byte[4];
            addrBuf[0] = (byte)'\0';
            addrBuf[1] = (byte)'\0';
            addrBuf[2] = (byte)'\0';
            addrBuf[3] = (byte)'\0';
        }

        outbuf[offset++] = addrBuf[0];
        outbuf[offset++] = addrBuf[1];
        outbuf[offset++] = addrBuf[2];
        outbuf[offset++] = addrBuf[3];

        return offset;
    }
    
    /**
     *
     */
    public static byte serializeGUESSVersionFormat(int majorVersion, int minorVersion) 
        throws IllegalArgumentException
    {
        if ( majorVersion < 0 || majorVersion >= 16 ||
             minorVersion < 0 || minorVersion >= 16 )
        {
            throw new IllegalArgumentException( "Version out of range.");
        }
        int guessVersion = majorVersion << 4;
        guessVersion |= minorVersion;

        return (byte)guessVersion;
    }
    
    /**
     * Reads the bytes of the stream till a byte has the value 0.
     * @param inStream
     * @return
     * @throws IOException
     */
    public static byte[] readBytesToNull( InputStream inStream )
        throws IOException
    {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int b;
        while( inStream.available() > 0 && (b=inStream.read())!=0 )
        {
            outStream.write( b );
        }
        return outStream.toByteArray();
    }
    
    /**
     * Get the contents of an <code>InputStream</code> as a <code>byte[]</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * 
     * @param input  the <code>InputStream</code> to read from
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException if an I/O error occurs
     */
    public static byte[] toByteArray( InputStream input ) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy( input, output );
        return output.toByteArray();
    }
    
    /**
     * Get the contents of a <code>Reader</code> as a String.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedReader</code>.
     * 
     * @param input  the <code>Reader</code> to read from
     * @return the requested String
     * @throws NullPointerException if the input is null
     * @throws IOException if an I/O error occurs
     */
    public static String toString(Reader input) throws IOException {
        StringWriter reader = new StringWriter();
        copy(input, reader);
        return reader.toString();
    }    

    /**
     * Copy bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * 
     * @param input  the <code>InputStream</code> to read from
     * @param output  the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     * @since 1.1
     */
    public static int copy( InputStream input, OutputStream output )
        throws IOException
    {
        byte[] buffer = new byte[4 * 1024];
        int count = 0;
        int n = 0;
        while ( -1 != (n = input.read( buffer )) )
        {
            output.write( buffer, 0, n );
            count += n;
        }
        return count;
    }
    
    /**
     * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedReader</code>.
     *
     * @param input  the <code>Reader</code> to read from
     * @param output  the <code>Writer</code> to write to
     * @return the number of characters copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     * @since Commons IO 1.3
     */
    public static int copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[4 * 1024];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    
    
    /**
     * Returns the logarithm to the based 2 of value.
     * @param num the number to determine the log2 of. This is expceted to be a
     * power of 2
     */
    public static byte calculateCeilLog2( int value )
    {
        if      (value <=             0x10000)
            if      (value <=           0x100)
                if      (value <=        0x10)
                    if      (value <=     0x4)
                        if  (value <=     0x2)
                           if (value <=   0x1) return  0; else return  1;
                           else                return  2;
                    else if (value <=     0x8) return  3; else return  4;
                else if (value <=        0x40)
                    if      (value <=    0x20) return  5; else return  6;
                else if (value <=        0x80) return  7; else return  8;
            else if (value <=          0x1000)
                if      (value <=       0x400)
                    if      (value <=   0x200) return  9; else return 10;
                else if (value <=       0x800) return 11; else return 12;
            else if (value <=          0x4000)
                if       (value <=     0x2000) return 13; else return 14;
            else if (value <=          0x8000) return 15; else return 16;
        else if (value <=           0x1000000)
            if      (value <=        0x100000)
                if      (value <=     0x40000)
                    if      (value <= 0x20000) return 17; else return 18;
                else if (value <=     0x80000) return 19; else return 20;
            else if (value <=        0x400000)
                if      (value <=    0x200000) return 21; else return 22;
            else if (value <=        0x800000) return 23; else return 24;
        else if (value <=          0x10000000)
            if      (value <=       0x4000000)
                if      (value <=   0x2000000) return 25; else return 26;
            else if (value <=       0x8000000) return 27; else return 28;
        else if (value <=          0x40000000)
            if      (value <=      0x20000000) return 29; else return 30;
        else return 31;
    }

    /**
     * Returns the logarithm to the based 2 of value.
     * @param num the number to determine the log2 of. This is expceted to be a
     * power of 2
     */
    public static byte calculateLog2( int value )
    {
        if      (value <             0x10000)
            if      (value <           0x100)
                if      (value <        0x10)
                    if      (value <     0x4)
                        if  (value <     0x2) return  0; else return  1;
                    else if (value <     0x8) return  2; else return  3;
                else if (value <        0x40)
                    if      (value <    0x20) return  4; else return  5;
                else if (value <        0x80) return  6; else return  7;
            else if (value <          0x1000)
                if      (value <       0x400)
                    if      (value <   0x200) return  8; else return  9;
                else if (value <       0x800) return 10; else return 11;
            else if (value <          0x4000)
                if       (value <     0x2000) return 12; else return 13;
            else if (value <          0x8000) return 14; else return 15;
        else if (value <           0x1000000)
            if      (value <        0x100000)
                if      (value <     0x40000)
                    if      (value < 0x20000) return 16; else return 17;
                else if (value <     0x80000) return 18; else return 19;
            else if (value <        0x400000)
                if      (value <    0x200000) return 20; else return 21;
            else if (value <        0x800000) return 22; else return 23;
        else if (value <          0x10000000)
            if      (value <       0x4000000)
                if      (value <   0x2000000) return 24; else return 25;
            else if (value <       0x8000000) return 26; else return 27;
        else if (value <          0x40000000)
            if      (value <      0x20000000) return 28; else return 29;
        else return 30;
    }


    /**
     * Returns the number of significant bits of num.
     * @param num the number to determine the significant number of bits from.
     */
    public static int determineBitCount( int num )
    {
        if ( num < 0 )
        {
            return 32;
        }

        if      (num <             0x10000)
            if      (num <           0x100)
                if      (num <        0x10)
                    if      (num <     0x4)
                        if  (num <     0x2)
                            if ( num == 0x0 ) return  0; else return  1;
                        else                return 2;
                    else if (num <     0x8) return  3; else return  4;
                else if (num <        0x40)
                    if      (num <    0x20) return  5; else return  6;
                else if (num <        0x80) return  7; else return  8;
            else if (num <          0x1000)
                if      (num <       0x400)
                    if      (num <   0x200) return  9; else return  10;
                else if (num <       0x800) return 11; else return 12;
            else if (num <          0x4000)
                if       (num <     0x2000) return 13; else return 14;
            else if (num <          0x8000) return 15; else return 16;
        else if (num <           0x1000000)
            if      (num <        0x100000)
                if      (num <     0x40000)
                    if      (num < 0x20000) return 17; else return 18;
                else if (num <     0x80000) return 19; else return 20;
            else if (num <        0x400000)
                if      (num <    0x200000) return 21; else return 22;
            else if (num <        0x800000) return 23; else return 24;
        else if (num <          0x10000000)
            if      (num <       0x4000000)
                if      (num <   0x2000000) return 25; else return 26;
            else if (num <       0x8000000) return 27; else return 28;
        else if (num <          0x40000000)
            if      (num <      0x20000000) return 29; else return 30;
        else                                return 31;
    }

    /**
     * Uses a zip compression to compress a given data array.
     */
    public static byte[] deflate( byte[] data )
    {
        try
        {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DeflaterOutputStream compressor = new DeflaterOutputStream( outStream );

            compressor.write( data, 0, data.length );
            compressor.close();

            return outStream.toByteArray();
        }
        catch ( IOException exp )
        {
            //NLogger.error( IOUtil.class , exp, exp );
            // this should never occur...
            throw new RuntimeException( exp );
        }
    }

    /**
     * Uses a zlib compression to decompress a given data array.
     */
    public static byte[] inflate( Inflater inflater, byte[] data )
        throws DataFormatException
    {
        // The use of InflaterInputStream causes an error when inflating data.
        //InflaterInputStream inflaterStream = new InflaterInputStream(
        //    new ByteArrayInputStream( data ), inflater );
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        inflater.setInput( data );
    
        byte[] buffer = new byte[1024];
        int lengthRead = 0;
        do
        {
            lengthRead = inflater.inflate( buffer );
            if ( lengthRead > 0 )
            {
                outStream.write( buffer, 0, lengthRead );
            }
        }
        while ( lengthRead > 0 );
    
        return outStream.toByteArray();
    }

    /**
     * Uses a zlib compression to decompress a given data array.
     */
    public static byte[] inflate( byte[] data )
        throws DataFormatException
    {
        return inflate( new Inflater(), data );
    }
    
    /**
     * Use COBS to encode a byte array.
     * The eliminated byte value is 0x00
     * http://www.acm.org/sigcomm/sigcomm97/papers/p062.pdf
     */
    public static byte[] cobsEncode( byte[] data )
    {
        try
        {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
            
            int code = 0x01;
            for ( int i = 0; i < data.length; i++ )
            {
                if ( data[ i ] == 0x00 )
                {
                    outStream.write( code );
                    outStream.write( tempBuffer.toByteArray() );
                    tempBuffer.reset();
                    code = (byte) 0x01;
                }
                else
                {
                    tempBuffer.write( data[ i ] );
                    code++;
                    if (code == 0xFF)
                    {
                        outStream.write( code );
                        outStream.write( tempBuffer.toByteArray() );
                        tempBuffer.reset();
                        code = (byte) 0x01;
                    }
                }
            }
    
            outStream.write( code );
            outStream.write( tempBuffer.toByteArray() );
            return outStream.toByteArray();
        }
        catch ( IOException exp )
        {
            //NLogger.error( IOUtil.class , exp, exp );
            // this should never occur...
            throw new RuntimeException( exp );
        }
    }

    /**
     * Use COBS to decode a COBS-encoded byte array.
     * The eliminated byte value is 0x00
     * http://www.acm.org/sigcomm/sigcomm97/papers/p062.pdf
     * @return the decoded byte array..
     * @throws IOException 
     */
    public static byte[] cobsDecode( byte[] data ) throws IOException
    {       
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        int index = 0;
        int code = 0x00;
        while ( index < data.length )
        {
            code = unsignedByte2int( data[ index++ ] );
            if ( ( index + ( code-2 ) ) >= data.length )
            {
//                if ( //NLogger.isWarnEnabled( IOUtil.class ) )
//                {
//                    //NLogger.warn( IOUtil.class, "Invalid COBS InputData: " +
//                    //    HexConverter.toHexString(data) );
//                }
                throw new IOException("Invalid COBS InputData" );
            }
            for (int i = 1; i < code; i++)
            {
                outStream.write( (int)data[index++] );
            }
            if (code < 0xFF && index < data.length)
            {
                outStream.write( 0 );
            } 
        }
        return outStream.toByteArray();
    }
    
    /**
     * Unconditionally close a <code>Socket</code>.
     * Equivalent to {@link Socket#close()}, except any exceptions will be ignored.
     * @param socket A (possibly null) Socket
     */
    public static void closeQuietly( Socket socket )
    {
        if( socket == null )
        {
            return;
        }

        try
        {
            socket.close();
        }
        catch( IOException ioe )
        {
        }
    }
    
    /**
     * Unconditionally close an <code>Closeable</code>.
     * Equivalent to {@link Closeable#close()}, except any exceptions will be ignored.
     * @param closeable A (possibly null) Closeable
     */
    public static void closeQuietly( Closeable closeable )
    {
        if( closeable == null )
        {
            return;
        }

        try
        {
            closeable.close();
        }
        catch( IOException ioe )
        {
        }
    }
    
    /**
     * Unconditionally close a <code>ManagedFile</code>.
     * Equivalent to {@link ManagedFile#closeFile()}, except any exceptions will be ignored.
     * @param input A (possibly null) InputStream
     */
    public static void closeQuietly( ManagedFile file )
    {
        if( file == null )
        {
            return;
        }

        try
        {
            file.closeFile();
        }
        catch( ManagedFileException exp )
        {
        }
    }
    
    /**
     * Unconditionally close a <code>ManagedFile</code>.
     * Equivalent to {@link ManagedFile#closeFile()}, except any exceptions will be ignored.
     * @param input A (possibly null) InputStream
     */
    public static void closeQuietly( ReadOnlyManagedFile file )
    {
        if( file == null )
        {
            return;
        }

        try
        {
            file.closeFile();
        }
        catch( ManagedFileException exp )
        {
        }
    }
}