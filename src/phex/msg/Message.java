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

import phex.io.buffer.ByteBuffer;


/**
 * A Gnutella network message.
 */
public abstract class Message
{
    private boolean isUdpMsg;
    private long creationTime;
    private MsgHeader header;

    protected Message( MsgHeader header )
    {
        this.header = header;
        creationTime = System.currentTimeMillis();
    }
    
    /**
     * @return the isUdpMsg
     */
    public boolean isUdpMsg()
    {
        return isUdpMsg;
    }

    /**
     * @param isUdpMsg the isUdpMsg to set
     */
    public void setUdpMsg(boolean isUdpMsg)
    {
        this.isUdpMsg = isUdpMsg;
    }

    /**
     * Returns this message's header.
     *
     * @return the MsgHeader associated with this message
     */    
    public MsgHeader getHeader()
    {
        return header;
    }

    public long getCreationTime( )
    {
        return creationTime;
    }

    /**
     * This is a dirty workaround for the static myMsgInit of MsgManager
     */
    public void setCreationTime( long time )
    {
        creationTime = time;
    }

    public ByteBuffer createHeaderBuffer()
    {
        return header.createHeaderBuffer();
    }
    
    /**
     * Creates a ByteBuffer containing this message 
     * body content, without its header.<br>
     * To get the header use createHeaderBuffer()
     * @return the ByteBuffer of this message.
     * @see #createHeaderBuffer()
     */
    public abstract ByteBuffer createMessageBuffer();
}


