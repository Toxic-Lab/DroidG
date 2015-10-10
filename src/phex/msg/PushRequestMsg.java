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

import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.io.buffer.ByteBuffer;
import phex.utils.IOUtil;


/**
 * <p>A gnutella push request</p>
 *
 * <p>According to the 0.6 specs, <quote>Servents may send push requests if it
 * receives a QueryHist message from a servent that doesn't support incoming
 * connections</quote> to allow firewall tunneling.</p>
 *
 * <p>This does not support GGEP extentions.</p>
 */
public class PushRequestMsg extends Message
{
    /**
     * <p>The un-parsed body of the message.</p>
     */
    private byte[] body;

    private GUID clientGUID;
    private long fileIndex;
    private DestAddress requestAddress;

    public PushRequestMsg( MsgHeader aHeader, byte[] payload )
    {
        super( aHeader );
        getHeader().setPayloadType( MsgHeader.PUSH_PAYLOAD );
        body = payload;
        getHeader().setDataLength( body.length );

        parseBody();
    }

    /**
     * <p>Create a new MsgPushRequest with an empty header but with all other
     * information provided.</p>
     *
     * @param aClientGUID  the GUID of the servent being requested to make a
     *                     push
     * @param aFileIndex   the index of the file that is being requested
     * @param aAddress     the HostAddress of the servent that the data should
     *                     be pushed to
     */
    public PushRequestMsg( GUID aClientGUID, long aFileIndex, DestAddress aAddress )
    {
        super( new MsgHeader( MsgHeader.PUSH_PAYLOAD, 0 ) );
        if ( aAddress.getIpAddress() == null )
        {
            throw new IllegalArgumentException( "Push request address must have IP." );
        }
        
        clientGUID = aClientGUID;
        fileIndex = aFileIndex;
        requestAddress = aAddress;
        buildBody();
        getHeader().setDataLength( body.length );
    }

    /**
     * <p>Get the GUID of the servent that is being asked to push the file.</p>
     *
     * <p>This must match the GUID of the query response hit entry with the push
     * flag set.</p>
     *
     * @return the GUID if the servent that stores the file that should be
     *         pushed back
     */
    public GUID getClientGUID()
    {
        return clientGUID;
    }

    /**
     * <p>Get the index of the file that should be retrieved.</p>
     *
     * <p>This should match the index returned in a query response message with
     * the push flag set to true.</p>
     *
     * @return the index of the file to retrieve via push
     */
    public long getFileIndex()
    {
        return fileIndex;
    }

    /**
     * <p>Get the HostAddress of the servent that initiated the push message.
     * </p>
     *
     * <p>The HostAddress represents an end-point to which the file should be
     * tunneled.</p>
     *
     * @return the HostAddress to send the file
     */
    public DestAddress getRequestAddress()
    {
        return requestAddress;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer createMessageBuffer()
    {
        return ByteBuffer.wrap( body );
    }

    @Override
    public String toString()
    {
        return	"[" +
            getHeader() + " " +
                "ClientGUID=" + clientGUID + ", " +
                "FileIndex=" + fileIndex + ", " +
                "RequestAddress=" + requestAddress + ", " +
                "]";
    }

    private void buildBody()
    {
        body = new byte[ 26 ];

        clientGUID.serialize( body, 0 );
        IOUtil.serializeIntLE( (int)fileIndex, body, 16 );
        IpAddress ip = requestAddress.getIpAddress();        
        System.arraycopy( ip.getHostIP(), 0, body, 20, 4 );
        IOUtil.serializeShortLE( (short)requestAddress.getPort(),
            body, 24 );
    }

    private void parseBody()
    {
        if ( clientGUID == null )
        {
            clientGUID = new GUID();
        }
        clientGUID.deserialize( body, 0 );
        fileIndex = IOUtil.unsignedInt2Long( IOUtil.deserializeIntLE( body, 16 ) );
        byte[] ip = new byte[4];
        ip[0] = body[ 20 ];
        ip[1] = body[ 21 ];
        ip[2] = body[ 22 ];
        ip[3] = body[ 23 ];
        int port = IOUtil.deserializeShortLE( body, 24 );
        requestAddress = new DefaultDestAddress( ip, port );
    }
}

