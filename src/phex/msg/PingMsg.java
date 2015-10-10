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
import phex.utils.HexConverter;
import phex.utils.IOUtil;


/**
 * <p>A Gnutella Ping message.</p>
 *
 * <p>This represents a ping message. It informs other Gnutella nodes that this
 * node wants to know about them. The responses to this will be pongs,
 * encapsulated by the MsgInitResponse class.</p>
 *
 * <p>This implementation handles GGEP extension blocks.</p>
 */
public class PingMsg extends Message
{
    /**
     * <p>The un-parsed body of the message.</p>
     * It might include the optional payload.
     */
    private byte[] body;
    private GGEPBlock ggepBlock;
    
    public static final byte UDP_SCP_MASK = 0x1;
    public static final byte UDP_SCP_LEAF = 0x0;
    public static final byte UDP_SCP_ULTRAPEER = 0x1;

    /**
     * Create a new init message with a default header.
     */
    public PingMsg( byte ttl )
    {
        super( new MsgHeader( MsgHeader.PING_PAYLOAD, ttl, 0 ) );
        body = IOUtil.EMPTY_BYTE_ARRAY;
    }

    /**
     * <p>Create a new init message using a header.</p>
     *
     * <p>This will set the function property of the header to MsgHeader.sInit.
     * </p>
     *
     * @param header  the MsgHeader to use
     */
    public PingMsg( MsgHeader aHeader, byte[] aBody )
    {
        super( aHeader );
        getHeader().setPayloadType( MsgHeader.PING_PAYLOAD );
        body = aBody;
        getHeader().setDataLength( body.length );        
    }
    
    /**
     * Create a Udp ping messsage 
     * it contains the scp flag and is sent over udp to all the 
     * connected hosts
     * @return a brand new udp ping message with ttl = 1
     */
    public static PingMsg createUdpPingMsg( boolean isUltrapeer )
    {
        // first set the data field for  the scp extension
        byte[] udpScpByteArr = new byte[1];
        if( isUltrapeer )
        {
            udpScpByteArr[0] = UDP_SCP_ULTRAPEER;
        }
        else
        {
            udpScpByteArr[0] = UDP_SCP_LEAF;
        }       
        
        GGEPBlock scpExtension = new GGEPBlock( false );        
        scpExtension.addExtension( GGEPBlock.UDP_HOST_CACHE_SCP, udpScpByteArr );
        
        byte[] body = scpExtension.getBytes();
        
        PingMsg udpPingMsg = new PingMsg( (byte)1 );
        udpPingMsg.getHeader().setDataLength( body.length );
        udpPingMsg.body = body;
        udpPingMsg.ggepBlock = scpExtension; 
        
//        NLogger.debug( PingMsg.class, 
//            "Created UDP Ping " + udpPingMsg.toString() );
        
       return udpPingMsg;        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer createMessageBuffer()
    {
        return ByteBuffer.wrap( body );
    }
    
    public byte[] getBytes()
    {
        byte[] data = new byte[ MsgHeader.DATA_LENGTH + body.length ];
        byte[] hdr = getHeader().getBytes();
        System.arraycopy( hdr, 0, data, 0, MsgHeader.DATA_LENGTH );
        System.arraycopy( body, 0, data, MsgHeader.DATA_LENGTH , body.length );
        return data;
    }

    private void parseGGEPBlocks()
    {
        if ( ggepBlock == null )
        {
            ggepBlock = GGEPBlock.mergeGGEPBlocks( 
                GGEPBlock.parseGGEPBlocks( body, 0 ) );
        }
    }
    
    /**
     * Returns the value of the SCP (supports cached pongs) GGEP field.
     * @return the value of the SCP (supports cached pongs) GGEP field.
     */
    public byte[] getScpByte()
    {
        parseGGEPBlocks();
        if ( ggepBlock != null && ggepBlock.isExtensionAvailable( 
            GGEPBlock.UDP_HOST_CACHE_SCP ) )
        {
            return ggepBlock.getExtensionData( GGEPBlock.UDP_HOST_CACHE_SCP );
        }
        else
        {
            return null;
        }
    }
    
    @Override
    public String toString()
    {
        return "Ping[ HEX=[" + HexConverter.toHexString( body ) +
            "] ]";
    }
}