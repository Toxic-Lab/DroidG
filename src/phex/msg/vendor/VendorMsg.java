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
package phex.msg.vendor;

import java.util.Arrays;

import phex.io.buffer.ByteBuffer;
import phex.msg.InvalidMessageException;
import phex.msg.Message;
import phex.msg.MsgHeader;
import phex.utils.HexConverter;
import phex.utils.IOUtil;

/**
 * 
 */
public abstract class VendorMsg extends Message implements VendorMessageConstants
{
    private static final int VM_PREFIX_LENGTH = 8;
    
    private byte[] vendorId;
    private int subSelector;
    private int version;
    private byte[] data;
    
    public VendorMsg( MsgHeader header, byte[] vendorId, int subSelector, 
        int version, byte[] data )
    {
        super( header );
        this.vendorId = vendorId;
        this.subSelector = subSelector;
        this.version = version;
        this.data = data;
    }
    
    public VendorMsg( byte[] vendorId, int subSelector, 
        int version, byte[] data )
    {
        super( new MsgHeader( MsgHeader.VENDOR_MESSAGE_PAYLOAD,
            (byte)1, (VM_PREFIX_LENGTH + data.length)  ) );
        this.vendorId = vendorId;
        this.subSelector = subSelector;
        this.version = version;
        this.data = data;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer createMessageBuffer()
    {
        ByteBuffer buffer = ByteBuffer.allocate( VM_PREFIX_LENGTH + data.length );
        buffer.put( vendorId )
              .putShortLE( (short)subSelector )
              .putShortLE( (short)version )
              .put( data );
        buffer.rewind();
        return buffer;
    }
    
    public static VendorMsg parseMessage( MsgHeader header, byte[] aBody )
        throws InvalidMessageException
    {
        if ( aBody.length < VM_PREFIX_LENGTH )
        {
            throw new InvalidMessageException( "Vendor Message Wrong Format" );
        }
        byte[] vendorId = new byte[4];
        System.arraycopy(aBody, 0, vendorId, 0, 4);

        int subSelector =
            IOUtil.unsignedShort2Int(IOUtil.deserializeShortLE(aBody, 4));
        int version =
            IOUtil.unsignedShort2Int(IOUtil.deserializeShortLE(aBody, 6));

        byte[] data = new byte[aBody.length - VM_PREFIX_LENGTH];
        System.arraycopy(aBody, VM_PREFIX_LENGTH, data, 0, data.length);
        
        if ( subSelector == SUBSELECTOR_MESSAGES_SUPPORTED
            && Arrays.equals( vendorId, VENDORID_NULL ) )
        {
            return new MessagesSupportedVMsg( header, vendorId, subSelector, 
                version, data );
        }
        
        if ( subSelector == SUBSELECTOR_TCP_CONNECT_BACK )
        {
            if ( Arrays.equals( vendorId, VENDORID_BEAR ) )
            {
                return new TCPConnectBackVMsg( header, vendorId, subSelector,
                    version, data );
            }
            else if ( Arrays.equals( vendorId, VENDORID_LIME ) )
            {
                return new TCPConnectBackRedirectVMsg( header, vendorId, subSelector,
                    version, data );
            }
        }
        
        if ( subSelector == SUBSELECTOR_MESSAGE_ACK 
            && Arrays.equals( vendorId, VENDORID_LIME ))
        {
            return new MessageAcknowledgementVMsg( header, vendorId,
                subSelector, version, data );
        }
        
        if ( subSelector == SUBSELECTOR_OOB_REPLY_COUNT 
            && Arrays.equals( vendorId, VENDORID_LIME ))
        {
            return new OOBReplyCountVMsg( header, vendorId,
                subSelector, version, data );
        }
        
        if ( subSelector == SUBSELECTOR_HOPS_FLOW 
            && Arrays.equals( vendorId, VENDORID_BEAR ))
        {
            return new HopsFlowVMsg( header, vendorId,
                subSelector, version, data );
        }
        
        if ( subSelector == SUBSELECTOR_PUSH_PROXY_REQUEST 
            && Arrays.equals( vendorId, VENDORID_LIME ))
        {
            return new PushProxyRequestVMsg( header, vendorId, subSelector,
                version, data );
        }
        
        if ( subSelector == SUBSELECTOR_PUSH_PROXY_ACKNOWLEDGEMENT 
            && Arrays.equals( vendorId, VENDORID_LIME ))
        {
            return new PushProxyAcknowledgementVMsg( header, vendorId,
                subSelector, version, data );
        }
        if ( subSelector == SUBSELECTOR_CAPABILITIES
            && Arrays.equals( vendorId, VENDORID_NULL ))
        {
            return new CapabilitiesVMsg( header, vendorId, subSelector,
                version, data );
        }
        
        if ( subSelector == SUBSELECTOR_UDP_HEAD_PING
             && Arrays.equals( vendorId, VENDORID_LIME ))
        {
            return new UdpHeadPingVMsg( header, vendorId, subSelector,
                version, data );
        }
        
        if ( subSelector == SUBSELECTOR_HORIZON_PING
            && Arrays.equals( vendorId, VENDORID_BEAR ))
       {
           // ignoring BEAR5v1 (BearHorizonPing)
           throw new InvalidMessageException(
               "Unsupported Vendor Message: " + new String( vendorId )
               + subSelector + "v" + version);
       }
        
        
        throw new InvalidMessageException(
            "Unknown Vendor Message variant: " + new String( vendorId )
            + subSelector + "v" + version);
    }
    
    public void setVersion( int ver )
    {
        this.version = ver;
    }

    /**
     * @param data
     */
    protected void setVenderMsgData(byte[] data)
    {
        this.data = data;
        getHeader().setDataLength(VM_PREFIX_LENGTH+data.length);
    }
    
    protected byte[] getVenderMsgData()
    {
        return data;
    }
    
    public String toString()
    {
        return "VendorMsg[ VendorId=" + new String(vendorId) +
            ", SubSelector=" + subSelector +
            ", Version=" + version +
            ", DataHEX=[" + HexConverter.toHexString( data ) +
            "] ]";
    }
}