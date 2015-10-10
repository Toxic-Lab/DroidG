/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Phex Development Group
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Cuts the wrapped InputStream off after a specified number of bytes.
 * 
 * @author jakarta-commons httpclient org.apache.commons.httpclient.ContentLengthInputStream
 */
public class LengthLimitedInputStream extends FilterInputStream
{
    /**
     * The maximum number of bytes that can be read from the stream. Subsequent
     * read operations will return -1.
     */
    private long lengthLimit;

    /** 
     * The current position 
     */
    private long lengthRead;

    /** 
     * True if the stream is closed. 
     */
    private boolean closed = false;

    /**
     * Creates a new length limited stream
     * @param in the stream to wrap
     * @param maxLength the max number of bytes to read from the stream.
     * Subsequent read operations will return -1.
     */
    public LengthLimitedInputStream(InputStream in, long maxLength)
    {
        super(in);
        lengthLimit = maxLength;
    }
    
    public void setLengthLimit( long maxLength )
    {
        lengthLimit = maxLength;
        lengthRead = 0;
    }

    /**
     * <p>Reads until the end of the known length of content.</p>
     *
     * <p>Does not close the underlying socket input, but instead leaves it
     * primed to parse the next response.</p>
     * @throws IOException If an IO problem occurs.
     */
    public void close() throws IOException
    {
        if (!closed)
        {
            try
            {
                // read and discard the remainder of the message
                byte buffer[] = new byte[1024];
                while ( read( buffer ) >= 0 )
                {
                    ;
                }
            }
            finally
            {
                // close after above so that we don't throw an exception trying
                // to read after closed!
                closed = true;
            }
        }
    }

    /**
     * Read the next byte from the stream
     * @return The next byte or -1 if the end of stream has been reached.
     * @throws IOException If an IO problem occurs
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException
    {
        if (closed)
        {
            throw new IOException("Attempted read from closed stream.");
        }

        if (lengthRead >= lengthLimit)
        {
            return -1;
        }
        lengthRead++;
        return super.read();
    }

    /**
     * Does standard {@link InputStream#read(byte[], int, int)} behavior, but
     * also notifies the watcher when the contents have been consumed.
     *
     * @param b     The byte array to fill.
     * @param off   Start filling at this position.
     * @param len   The number of bytes to attempt to read.
     * @return The number of bytes read, or -1 if the end of content has been
     *  reached.
     *
     * @throws java.io.IOException Should an error occur on the wrapped stream.
     */
    public int read(byte[] b, int off, int len) throws java.io.IOException
    {
        if (closed)
        {
            throw new IOException("Attempted read from closed stream.");
        }

        if (lengthRead >= lengthLimit)
        {
            return -1;
        }

        if (lengthRead + len > lengthLimit)
        {
            len = (int)(lengthLimit - lengthRead);
        }
        int count = super.read(b, off, len);
        lengthRead += count;
        return count;
    }

    /**
     * Read more bytes from the stream.
     * @param b The byte array to put the new data in.
     * @return The number of bytes read into the buffer.
     * @throws IOException If an IO problem occurs
     * @see java.io.InputStream#read(byte[])
     */
    public int read(byte[] b) throws IOException
    {
        return read(b, 0, b.length);
    }

}