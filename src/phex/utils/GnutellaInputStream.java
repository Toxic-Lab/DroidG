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
 *  --- CVS Information ---
 *  $Id$
 */
package phex.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;


public class GnutellaInputStream extends InputStream
{
    private static final char CR = '\r';
    private static final char LF = '\n';
    
    //private static final Logger logger = LoggerFactory.getLogger( GnutellaInputStream.class );

    /**
     * The init length of the buffer.
     */
    private int READ_BUFFER_LENGTH = 2048;
    private int LINE_BUFFER_LENGTH = 64;

    private volatile byte[] buffer;
    private int position;
    private int count;

    private volatile InputStream inStream;
    private Inflater inflater; 

    /**
     * Creates a GnutellaInputStream. The given InputStream will be buffered
     * using a internal buffer.
     */
    public GnutellaInputStream( InputStream aInputStream )
    {
        inStream = aInputStream;

        buffer = new byte[ READ_BUFFER_LENGTH ];
    }
    
    public void activateInputInflation()
        throws IOException
    {
        // first we need to inflate what is left in out buffer
        inflater = new Inflater();
        if ( (count - position) > 0 )
        {
            byte[] dummy = new byte[(count - position)];

            System.arraycopy( buffer, position, dummy, 0, (count - position) );
            inflater.setInput( dummy );

            try
            {
                inflater.inflate( buffer );
            }
            catch ( DataFormatException exp )
            {
                //logger.error( exp.toString(), exp );
                throw new IOException( exp.getMessage() );
            }
        }
        InflaterInputStream inflaterInStream = new InflaterInputStream( inStream,
            inflater );
        inStream = inflaterInStream;
        position = count = 0;
    }

    /**
     * Reads till the next \n. In the returned string \r and \n is not included.
     * If the stream ended null is returned.
     * Throttle is controlled when using this method.
     */
    public String readLine( )
        throws IOException
    {
        if ( inStream == null )
        {
            return null;
        }
        int totalIn = 0;
        if ( inflater != null )
        {
            totalIn = inflater.getTotalIn();
        }

        StringBuffer lineBuffer = new StringBuffer( LINE_BUFFER_LENGTH );
        int c;
        int lengthRead = 0;
        while ( true )
        {
            c = read();
            lengthRead ++;
            if ( c < 0 )
            {// stream ended... a valid line could not be read... return null
                if ( lineBuffer.length() == 0 )
                {
                    return null;
                }
                else
                {
                    break;
                }
            }
            else if ( c == CR )
            {// skip \r and continue... we only care for \n
                continue;
            }
            else if ( c == LF )
            {// found the end of the line... break here
                break;
            }
            else
            {
                // appending char is much faster then int!
                lineBuffer.append( (char)c );
            }
        }
        

//        if ( bandwidthController != null )
//        {
//            // adjust the read length to the compressed data read.
//            if ( inflater != null )
//            {
//                lengthRead = inflater.getTotalIn() - totalIn;
//            }
//            bandwidthController.controlBandwidth( lengthRead );
//        }

        return lineBuffer.toString();
    }

    /**
     * Reads into a byte array.
     * @param b
     * @param offset
     * @param length
     * @throws IOException
     */
    public int read( byte[] b, int offset, int length )
        throws IOException
    {
        if ( inStream == null )
        {
            return -1;
        }

        if ((offset | length | (offset + length) | (b.length - (offset + length))) < 0)
        {
            throw new IndexOutOfBoundsException();
        }
        else if (length == 0)
        {
            return 0;
        }

        int totalIn = 0;
        if ( inflater != null )
        {
            totalIn = inflater.getTotalIn();
        }

        int lengthRead = readInternal( b, offset, length );
        
        
        /*if ( lengthRead <= 0 )
        {
            return lengthRead;
        }*/
        // Dont read in length loop.. this will break display tracking on low bandwidth settings.
        /*while ( ( lengthRead < length ) && ( inStream.available() > 0) )
        {
            int n1 = readInternal( b, offset + lengthRead, length - lengthRead);
            if ( n1 <= 0)
            {
                break;
            }
            lengthRead += n1;
        }*/

        return lengthRead;
    }

    /**
     * Read characters into a portion of an array, reading from the underlying
     * stream at most once if necessary. Throttle is not controlled using
     * this method.
     */
    private int readInternal(byte[] b, int off, int len)
        throws IOException
    {
        int avail = count - position;
        if (avail <= 0)
        {
            /* If the requested length is at least as large as the buffer, and
               if there is no mark/reset activity, do not bother to copy the
               bytes into the local buffer.  In this way buffered streams will
               cascade harmlessly. */
            if ( len >= buffer.length )
            {
                if ( inStream == null )
                {
                    return -1;
                }
                return inStream.read(b, off, len);
            }
            fill();
            avail = count - position;
            if (avail <= 0)
            {
                return -1;
            }
        }
        int cnt = (avail < len) ? avail : len;
        if ( inStream == null )
        {
            return -1;
        }
    	System.arraycopy(buffer, position, b, off, cnt);
        position += cnt;
        return cnt;
    }

    /**
     * Peeks a byte from the stream without removing it. The next call to peek()
     * or read() will return the same byte again.
     */
    public int peek()
        throws IOException
    {
        if ( position >= count)
        {
            fill();
            if ( position >= count )
            {
                return -1;
            }
        }
        return buffer[ position ] & 0xff;
    }

    /**
     * Read byte. Without tracking throttle.
     */
    public int read()
        throws IOException
    {
        if ( position >= count)
        {
            fill();
            if ( position >= count )
            {
                return -1;
            }
        }
        return buffer[ position++ ] & 0xff;
    }

    /**
     * Returns the number of bytes that can be read from this input
     * stream without blocking.
     * <p>
     * The <code>available</code> method of
     * <code>BufferedInputStream</code> returns the sum of the the number
     * of bytes remaining to be read in the buffer
     * (<code>count&nbsp;- pos</code>)
     * and the result of calling the <code>available</code> method of the
     * underlying input stream.
     *
     * @return     the number of bytes that can be read from this input
     *             stream without blocking.
     * @exception  IOException  if an I/O error occurs.
     */
    public synchronized int available() throws IOException
    {
        return (count - position) + inStream.available();
    }

    /**
     * Close the input stream.
     */
    @Override
    public void close()
    {
        IOUtil.closeQuietly( inStream );
        inStream = null;
        count = position = 0;
        buffer = IOUtil.EMPTY_BYTE_ARRAY;
    }

    /**
     * Fill the internal buffer using data from the underlying input stream.
     */
    protected void fill() throws IOException
    {
        position = 0;
        count = 0;
        try
        {
            int nRead = inStream.read( buffer, 0, buffer.length);
            if ( nRead > 0 && inStream != null )
            {
                count = nRead;
            }
        }
        catch ( EOFException exp )
        {// InflaterInputStream throws EOFException if underlying InputStream
         // reads data amount of -1. We don't like to forward this exception
         // instead we just don't fill buffer.
            return;
        }
    }
}