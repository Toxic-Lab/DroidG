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
package phex.common.file;

import java.io.IOException;
import java.io.InputStream;

import phex.io.buffer.BufferSize;
import phex.io.buffer.ByteBuffer;

public class ManagedFileInputStream extends InputStream
{
    private ByteBuffer buffer;
    private ManagedFile managedFile;
    private long inputOffset;
    
    public ManagedFileInputStream( ManagedFile managedFile, long inputOffset )
    {
        this.managedFile = managedFile;
        this.inputOffset = inputOffset;
        buffer = ByteBuffer.allocate( BufferSize._64K );
        buffer.flip();// flip buffer to let it appear empty
//        NLogger.debug(NLoggerNames.GLOBAL, "Created ManagedInputStream: Buffer[" 
//            + buffer + "] ManagedFile[" + managedFile + "].");
    }
        
    @Override
	public int read() throws IOException
    {
        if ( !buffer.hasRemaining() )
        {
            fill();
        }
        if ( !buffer.hasRemaining() )
        {
            return -1;
        }
        
        byte b = buffer.get();
        return b;
    }

    @Override
	public int read(byte b[]) throws IOException
    {
        return this.read(b, 0, b.length);
    }
    
    @Override
	public int read( byte b[], int offset, int length ) throws IOException
    {
        if ((offset | length | (offset + length) | (b.length - (offset + length))) < 0)
        {
            throw new IndexOutOfBoundsException();
        } 
        else if ( length == 0 )
        {
            return 0;
        }
        
        if ( !buffer.hasRemaining() )
        {
            fill();
        }
        if ( !buffer.hasRemaining() )
        {
            return -1;
        }
        int read = 0;
        while ( read < length && buffer.hasRemaining() )
        {
            int toRead = Math.min( length-read, buffer.remaining() );
            buffer.get( b, offset+read, toRead );

            read += toRead;
            if ( !buffer.hasRemaining() )
            {
                fill();
            }
        }
        return read;
    }

    @Override
	public int available() throws IOException
    {
        if ( !buffer.hasRemaining() )
        {
            fill();
        }
        return buffer.remaining();
    }
    
    private void fill() throws IOException
    {
        assert !buffer.hasRemaining();
        buffer.clear();
        try
        {
            managedFile.read( buffer, inputOffset );
        }
        catch ( ManagedFileException exp )
        {
            IOException ioExp = new IOException( "Cause: " + exp.getMessage() );
            ioExp.initCause(exp);
            throw ioExp;
        }
        buffer.flip();
        inputOffset += buffer.limit();
    }

    @Override
	public void close() throws IOException
    {
//        NLogger.debug(NLoggerNames.GLOBAL, "Releasing ManagedInputStream: Buffer[" 
//            + buffer + "] ManagedFile[" + managedFile + "].");
        buffer = null;
    }
}