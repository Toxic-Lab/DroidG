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
package phex.msg;

import phex.io.buffer.ByteBuffer;
import phex.utils.*;


public class QRResetTableMsg extends RouteTableUpdateMsg
{
    // length = variant: 1 + tablelength: 4 + infinity: 1
    private static final int MESSAGE_LENGTH = 6;
    private int tableSize;
    private byte infinityByte;

    public QRResetTableMsg( int aTableSize, byte aInfinityByte )
    {
        super( RESET_TABLE_VARIANT, MESSAGE_LENGTH );
        tableSize = aTableSize;
        infinityByte = aInfinityByte;
    }

    public QRResetTableMsg( MsgHeader header, byte[] aBody )
    {
        super( RESET_TABLE_VARIANT, header );
        header.setDataLength( aBody.length );
        // since we dont forward this message we are not memorizing the body!
        tableSize = IOUtil.deserializeIntLE( aBody, 1 );
        infinityByte = aBody[5];
    }

    /**
     * Returns the used table size.
     * @return the used table size.
     */
    public int getTableSize()
    {
        return tableSize;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer createMessageBuffer()
    {
        ByteBuffer buffer = ByteBuffer.allocate( MESSAGE_LENGTH );
        buffer.put( variant )
              .putIntLE( tableSize )
              .put( infinityByte );
        buffer.rewind();
        return buffer;
    }
}