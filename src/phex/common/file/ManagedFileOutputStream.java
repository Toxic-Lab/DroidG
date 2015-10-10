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
import java.io.OutputStream;

import phex.io.buffer.BufferSize;
import phex.io.buffer.ByteBuffer;

public class ManagedFileOutputStream extends OutputStream
{   
    private ByteBuffer buffer;
    private ManagedFile managedFile;
    private long outputPosition;
    
    public ManagedFileOutputStream( ManagedFile managedFile, long outputPosition )
    {
        this.managedFile = managedFile;
        this.outputPosition = outputPosition;
        buffer = ByteBuffer.allocate( BufferSize._64K );
    }
    
    @Override
	public void write( int b ) throws IOException
    {
        if ( !buffer.hasRemaining() )
        {
            flush();
        }
        buffer.put( (byte)b );
    }
    
    @Override
	public void write(byte[] b, int offset, int length) throws IOException
    {
        if ((offset < 0) || (offset > b.length) || (length < 0) ||
            ((offset + length) > b.length) || ((offset + length) < 0)) 
        {
            throw new IndexOutOfBoundsException();
        }
        else if (length == 0) 
        {
            return;
        }
        
        int written = 0;
        while ( written < length )
        {
            int toWrite = Math.min( length-written, buffer.remaining() );
            buffer.put( b, offset+written, toWrite );
            written += toWrite;
            if ( !buffer.hasRemaining() )
            {
                flush();
            }
        }
    }
    
    @Override
	public void flush() throws IOException
    {
        buffer.flip();
        try
        {
            managedFile.write(buffer, outputPosition );
        }
        catch ( ManagedFileException exp )
        {
            IOException ioExp = new IOException( "ManagedFileException: " 
                + exp.getMessage());
            ioExp.initCause(exp);
            throw ioExp;
        }
        outputPosition += buffer.limit();
        buffer.clear();
    }

    @Override
	public void close() throws IOException
    {
        flush();
        buffer = null;
    }
}