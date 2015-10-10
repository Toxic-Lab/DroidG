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
package phex.download;

import phex.io.buffer.ByteBuffer;

public class DataDownloadScope extends DownloadScope
{
    private ByteBuffer dataBuffer;

    /**
     * @param startOffset The start offset of the download scope, inclusive.
     * @param endOffset The end offset of the download scope, inclusive.
     */
    public DataDownloadScope( long startOffset, long endOffset, ByteBuffer buffer )
    {
        super( startOffset, endOffset );
        if ( buffer == null )
        {
            throw new NullPointerException( "Null DirectByteBuffer given.");
        }
        dataBuffer = buffer;
    }

    public ByteBuffer getDataBuffer()
    {
        assert dataBuffer != null : "Data buffer already released.";
        return dataBuffer;
    }

    public void releaseDataBuffer()
    {
        assert dataBuffer != null : "Data buffer already released.";
        dataBuffer = null;
    }
}
