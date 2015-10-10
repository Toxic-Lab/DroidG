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
package phex.io.buffer;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.ShortBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

import phex.utils.HexConverter;

/**
 * We use our own ByteBuffer implementations to be able to 
 * add convenience functionalities.
 * 
 * ByteBuffer allocation is done using heap byte buffers
 * since long lasting direct byte buffer with pooling as we 
 * used before (see DirectByteBuffer and DirectByteBufferProvider)
 * turn out to not be a very efficient alternative (in Java 6).
 */
public class ByteBuffer implements Comparable<ByteBuffer>
{
    private java.nio.ByteBuffer buf;
    
    
    protected ByteBuffer( java.nio.ByteBuffer buf )
    {
        this.buf = buf;
    }
    
    /**
     * Relative <i>put</i> method for writing a short
     * value in little endian byte order.
     *
     * <p> Writes two bytes containing the given int value, in little
     * endian byte order, into this buffer at the current position, and
     * increments the position by four.  </p>
     *
     * @param  value
     *         The int value to be written
     *
     * @return  This buffer
     *
     * @throws  BufferOverflowException
     *          If there are fewer than four bytes
     *          remaining in this buffer
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public ByteBuffer putIntLE( int value )
    {
        put( (byte)(value >>  0) );
        put( (byte)(value >>  8) );
        put( (byte)(value >> 16) );
        put( (byte)(value >> 24) );
        return this;
    }
    
    /**
     * Relative <i>put</i> method for writing a short
     * value in little endian byte order.
     *
     * <p> Writes two bytes containing the given short value, in little
     * endian byte order, into this buffer at the current position, and
     * increments the position by two.  </p>
     *
     * @param  value
     *         The short value to be written
     *
     * @return  This buffer
     *
     * @throws  BufferOverflowException
     *          If there are fewer than two bytes
     *          remaining in this buffer
     *
     * @throws  ReadOnlyBufferException
     *          If this buffer is read-only
     */
    public ByteBuffer putShortLE( short value )
    {
        put( (byte)(value >> 0) );
        put( (byte)(value >> 8) );
        return this;
    }
    
    /**
     * Reads characters from this ByteBuffer, appending it to the given 
     * StringBuffer, until a '\n' terminated line is read ('\r' is dropped).
     * Returns true if a line was read, false if more data needs to be in the 
     * the buffer until a full line can be read.
     * @return true if a line was read, false if more data needs to be in the 
     *   the buffer until a full line can be read.
     */
    public boolean readLine( StringBuilder strBuf ) 
    {
        int c = -1; //the character just read        
        while( hasRemaining() )
        {
            c = get();
            switch(c) 
            {
                // line full
                case  '\n': 
                    return true;
                // drop \r character.
                case  '\r': 
                    continue;
                default: 
                    strBuf.append((char)c);
            }
        }
        return false;
    }
    
    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.  This method reads
     * until the limit of this buffer if no <tt>NUL</tt> is found.
     * @return the string read.
     */
    public String getString( CharsetDecoder decoder ) throws CharacterCodingException
    {
        if( !hasRemaining() )
        {
            return "";
        }

        boolean utf16 = decoder.charset().name().startsWith( "UTF-16" );

        int oldPos = position();
        int oldLimit = limit();
        int end = -1;
        int newPos;

        if( !utf16 )
        {
            end = indexOf( ( byte ) 0x00 );
            if( end < 0 )
            {
                newPos = end = oldLimit;
            }
            else
            {
                newPos = end + 1;
            }
        }
        else
        {
            int i = oldPos;
            for( ;; )
            {
                boolean wasZero = get( i ) == 0;
                i ++;
                
                if( i >= oldLimit )
                {
                    break;
                }
                
                if( get( i ) != 0 )
                {
                    i ++;
                    if( i >= oldLimit )
                    {
                        break;
                    }
                    else
                    {
                        continue;
                    }
                }
                
                if( wasZero )
                {
                    end = i - 1;
                    break;
                }
            }

            if( end < 0 )
            {
                newPos = end = oldPos + ( ( oldLimit - oldPos ) & 0xFFFFFFFE );
            }
            else
            {
                if( end + 2 <= oldLimit )
                {
                    newPos = end + 2;
                }
                else
                {
                    newPos = end;
                }
            }
        }

        if( oldPos == end )
        {
            position( newPos );
            return "";
        }

        limit( end );
        decoder.reset();

        int expectedLength = (int)( remaining() * decoder.averageCharsPerByte() ) + 1;
        CharBuffer out = CharBuffer.allocate( expectedLength );
        for( ; ; )
        {
            CoderResult cr;
            if( hasRemaining() )
            {
                cr = decoder.decode( internalBuffer(), out, true );
            }
            else
            {
                cr = decoder.flush( out );
            }

            if( cr.isUnderflow() )
            {
                break;
            }

            if( cr.isOverflow() )
            {
                CharBuffer o = CharBuffer.allocate( out.capacity() + expectedLength );
                out.flip();
                o.put( out );
                out = o;
                continue;
            }

            if( cr.isError() )
            {
                // Revert the buffer back to the previous state.
                limit( oldLimit );
                position( oldPos );
                cr.throwException();
            }
        }

        limit( oldLimit );
        position( newPos );
        return out.flip().toString();
    }

    /**
     * Reads a <code>NUL</code>-terminated string from this buffer using the
     * specified <code>decoder</code> and returns it.
     *
     * @param fieldSize the maximum number of bytes to read
     * @return the read string
     */
    public String getString( int fieldSize, CharsetDecoder decoder ) throws CharacterCodingException
    {
        if( fieldSize < 0 )
        {
            throw new IllegalArgumentException(
                "fieldSize cannot be negative: " + fieldSize );
        }

        if( fieldSize == 0 )
        {
            return "";
        }

        if( !hasRemaining() )
        {
            return "";
        }

        boolean utf16 = decoder.charset().name().startsWith( "UTF-16" );

        if( utf16 && ( ( fieldSize & 1 ) != 0 ) )
        {
            throw new IllegalArgumentException( "fieldSize is not even." );
        }

        int oldPos = position();
        int oldLimit = limit();
        int end = oldPos + fieldSize;

        if( oldLimit < end )
        {
            throw new BufferUnderflowException();
        }

        int i;
        
        if( !utf16 )
        {
            for( i = oldPos; i < end; i ++ )
            {
                if( get( i ) == 0 )
                {
                    break;
                }
            }

            if( i == end )
            {
                limit( end );
            }
            else
            {
                limit( i );
            }
        }
        else
        {
            for( i = oldPos; i < end; i += 2 )
            {
                if( ( get( i ) == 0 ) && ( get( i + 1 ) == 0 ) )
                {
                    break;
                }
            }

            if( i == end )
            {
                limit( end );
            }
            else
            {
                limit( i );
            }
        }

        if( !hasRemaining() )
        {
            limit( oldLimit );
            position( end );
            return "";
        }
        decoder.reset();

        int expectedLength = (int)( remaining() * decoder.averageCharsPerByte() ) + 1;
        CharBuffer out = CharBuffer.allocate( expectedLength );
        for( ; ; )
        {
            CoderResult cr;
            if( hasRemaining() )
            {
                cr = decoder.decode( internalBuffer(), out, true );
            }
            else
            {
                cr = decoder.flush( out );
            }

            if( cr.isUnderflow() )
            {
                break;
            }

            if( cr.isOverflow() )
            {
                CharBuffer o = CharBuffer.allocate( out.capacity() + expectedLength );
                out.flip();
                o.put( out );
                out = o;
                continue;
            }

            if( cr.isError() )
            {
                // Revert the buffer back to the previous state.
                limit( oldLimit );
                position( oldPos );
                cr.throwException();
            }
        }

        limit( oldLimit );
        position( end );
        return out.flip().toString();
    }
    
    /**
     * Returns the first occurence position of the specified byte from the current position to
     * the current limit.
     * 
     * @return <tt>-1</tt> if the specified byte is not found
     */
    public int indexOf( byte b )
    {
        if( hasArray() )
        {
            int arrayOffset = arrayOffset();
            int beginPos = arrayOffset + position();
            int limit = arrayOffset + limit();
            byte[] array = array();
            
            for( int i = beginPos; i < limit; i ++ )
            {
                if( array[ i ] == b )
                {
                    return i - arrayOffset;
                }
            }
        }
        else
        {
            int beginPos = position();
            int limit = limit();
            
            for( int i = beginPos; i < limit; i ++ )
            {
                if( get( i ) == b )
                {
                    return i;
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Forwards the position of this buffer the specified <code>size</code>
     * in bytes.
     * @return this buffer
     */
    public ByteBuffer skip( int size )
    {
        return position( position() + size );
    }
    
    /**
     * 
     * @return the internal buffer
     */
    public java.nio.ByteBuffer internalBuffer()
    {
        return buf;
    }
    
    /**
     * Returns a hexdump for this buffer.
     * @return hex dump as string
     */
    public String getHexDump()
    {
        return this.getHexDump( Integer.MAX_VALUE );
    }
    
    /**
     * Return a hexdump for this buffer with limited length.
     * 
     * @param lengthLimit The maximum number of bytes to dump from 
     *                    the current buffer position.
     * @return hex dump as string
     */
    public String getHexDump( int lengthLimit ) 
    {
        return HexConverter.toHexString(this, lengthLimit);
    }
    
    @Override
    public String toString()
    {
        StringBuffer sBuf = new StringBuffer();
        if( isDirect() )
        {
            sBuf.append( "DirectBuffer" );
        }
        else
        {
            sBuf.append( "HeapBuffer" );
        }
        sBuf.append( "[pos=" );
        sBuf.append( position() );
        sBuf.append( " lim=" );
        sBuf.append( limit() );
        sBuf.append( " cap=" );
        sBuf.append( capacity() );
        sBuf.append( ": " );
        sBuf.append( getHexDump( 16 ) );
        sBuf.append( ']' );
        return sBuf.toString();
    }

    @Override
    public int hashCode()
    {
        int h = 1;
        int p = position();
        for( int i = limit() - 1; i >= p; i -- )
        {
            h = 31 * h + get( i );
        }
        return h;
    }

    @Override
    public boolean equals( Object o )
    {
        if( !( o instanceof ByteBuffer ) )
        {
            return false;
        }

        ByteBuffer that = (ByteBuffer)o;
        if( this.remaining() != that.remaining() )
        {
            return false;
        }

        int p = this.position();
        for( int i = this.limit() - 1, j = that.limit() - 1; i >= p; i --, j -- )
        {
            byte v1 = this.get( i );
            byte v2 = that.get( j );
            if( v1 != v2 )
            {
                return false;
            }
        }
        return true;
    }
    
    public int compareTo(ByteBuffer that)
    {
        int n = this.position() + Math.min( this.remaining(), that.remaining() );
        for( int i = this.position(), j = that.position(); i < n; i ++, j ++ )
        {
            byte v1 = this.get( i );
            byte v2 = that.get( j );
            if( v1 == v2 )
            {
                continue;
            }
            if( v1 < v2 )
            {
                return -1;
            }

            return +1;
        }
        return this.remaining() - that.remaining();
    }
    
    ///////////////////////////////////////////////
    // java.nio.ByteBuffer delegation functions //

    public byte[] array()
    {
        return buf.array();
    }
    
    public int arrayOffset()
    {
        return buf.arrayOffset();
    }
    
    /**
     * @see java.nio.ByteBuffer#asReadOnlyBuffer()
     */
    public ByteBuffer asReadOnlyBuffer()
    {
        return new ByteBuffer( buf.asReadOnlyBuffer() );
    }

    /**
     * @see java.nio.ByteBuffer#asShortBuffer()
     */
    public ShortBuffer asShortBuffer()
    {
        return buf.asShortBuffer();
    }
    
    /**
     * @see java.nio.ByteBuffer#capacity()
     */
    public int capacity()
    {
        return buf.capacity();
    }
    
    /**
     * @see java.nio.ByteBuffer#clear()
     * @return this buffer
     */
    public ByteBuffer clear()
    {
        buf.clear();
        return this;
    }

    /**
     * @see java.nio.ByteBuffer#compact()
     * @return this buffer
     */
    public ByteBuffer compact()
    {
        buf.compact();
        return this;
    }

    /**
     * @see java.nio.ByteBuffer#duplicate()
     */
    public ByteBuffer duplicate()
    {
        return new ByteBuffer( this.buf.duplicate() );
    }
    
    /**
     * @see java.nio.ByteBuffer#flip()
     * @return this buffer
     */
    public ByteBuffer flip()
    {
        buf.flip();
        return this;
    }

    /**
     * @see java.nio.ByteBuffer#get()
     */
    public byte get()
    {
        return buf.get();
    }

    /**
     * @see java.nio.ByteBuffer#get(byte[], int, int)
     * @return this buffer
     */
    public ByteBuffer get(byte[] dst, int offset, int length)
    {
        buf.get( dst, offset, length );
        return this;
    }

    /**
     * @see java.nio.ByteBuffer#get(byte[])
     * @return this buffer
     */
    public ByteBuffer get(byte[] dst)
    {
        buf.get( dst );
        return this;
    }

    /**
     * @see java.nio.ByteBuffer#get(int)
     */
    public byte get(int index)
    {
        return buf.get( index );
    }
    
    public boolean hasArray()
    {
        return buf.hasArray();
    }
    
    public boolean hasRemaining()
    {
        return buf.hasRemaining();
    }

    public boolean isDirect()
    {
        return buf.isDirect();
    }

    public boolean isReadOnly()
    {
        return buf.isReadOnly();
    }
    
    /**
     * @see java.nio.Buffer#limit()
     */
    public int limit()
    {
        return buf.limit();
    }
    
    /**
     * @see java.nio.Buffer#limit(int)
     * @return this buffer
     */
    public ByteBuffer limit( int newLimit)
    {
        buf.limit( newLimit );
        return this;
    }
    
    /**
     * @see java.nio.Buffer#mark()
     * @return this buffer
     */
    public ByteBuffer mark()
    {
        buf.mark();
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#position()
     */
    public int position()
    {
        return buf.position();
    }
    
    /**
     * @see java.nio.ByteBuffer#position(int)
     * @return this buffer
     */
    public ByteBuffer position( int newPosition )
    {
        buf.position(newPosition);
        return this;
    }

    /**
     * @see java.nio.ByteBuffer#put(byte)
     * @return this buffer
     */
    public ByteBuffer put(byte b)
    {
        buf.put( b );
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#put(byte[])
     * @return this buffer
     */
    public ByteBuffer put( byte[] src )
    {
        buf.put( src );
        return this;
    }

    /**
     * @see java.nio.ByteBuffer#put(byte[], int, int)
     * @return this buffer
     */
    public ByteBuffer put(byte[] src, int offset, int length)
    {
        buf.put( src, offset, length );
        return this;
    }

    /**
     * 
     * @param src
     * @return this buffer
     */
    public ByteBuffer put(ByteBuffer src)
    {
        buf.put( src.buf );
        return this;
    }

    /**
     * 
     * @param index
     * @param b
     * @return this buffer
     */
    public ByteBuffer put(int index, byte b)
    {
        buf.put( index, b );
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#remaining()
     */
    public int remaining()
    {
        return buf.remaining();
    }
    
    /**
     * @see java.nio.ByteBuffer#reset()
     * @return this buffer
     */
    public ByteBuffer reset()
    {
        buf.reset();
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#rewind()
     * @return this buffer
     */
    public ByteBuffer rewind()
    {
        buf.rewind();
        return this;
    }
    
    /**
     * @see java.nio.ByteBuffer#slice()
     */
    public ByteBuffer slice()
    {
        return new ByteBuffer( this.buf.slice() );
    }
    
    ///////////////////////////////////////////////
    // ByteBuffer allocation functions          //
    
    /**
     * Returns a heap buffer which is capable to hold the specified size.  
     * @param capacity the capacity of the buffer
     * @return a new allocated buffer
     */
    public static ByteBuffer allocate( int capacity )
    {
        java.nio.ByteBuffer nioBuffer = java.nio.ByteBuffer.allocate( capacity );
        return new ByteBuffer( nioBuffer );
    }
    
    /**
     * Wraps the specified byte array into a ByteBuffer.
     * @return a new allocated buffer
     */
    public static ByteBuffer wrap( byte[] byteArray )
    {
        return wrap( java.nio.ByteBuffer.wrap( byteArray ) );
    }

    /**
     * Wraps the specified byte array into a ByteBuffer.
     * @return a new allocated buffer
     */
    public static ByteBuffer wrap( byte[] byteArray, int offset, int length )
    {
        return wrap( java.nio.ByteBuffer.wrap( byteArray, offset, length ) );
    }
    
    /**
     * Wraps the specified NIO {@link java.nio.ByteBuffer} into a ByteBuffer.
     * @return a new allocated buffer
     */
    public static ByteBuffer wrap( java.nio.ByteBuffer nioBuffer )
    {
        return new ByteBuffer( nioBuffer );
    }
}