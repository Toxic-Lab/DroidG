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
package phex.msg.vendor;

import phex.common.address.*;
import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.MsgHeader;
import phex.utils.IOUtil;

/**
 *
 */
public class PushProxyAcknowledgementVMsg extends VendorMsg
{
    private static final int VERSION = 2;
    
    private DestAddress hostAddress;
    
    public PushProxyAcknowledgementVMsg( DestAddress address, GUID guid )
    {
        super( VENDORID_LIME, SUBSELECTOR_PUSH_PROXY_ACKNOWLEDGEMENT, VERSION, 
            buildDataBody( address ) );
        getHeader().setMsgID( guid );
        hostAddress = address;
    }
    
    public PushProxyAcknowledgementVMsg( MsgHeader header, byte[] vendorId, 
        int subSelector, int version, byte[] data )
        throws InvalidMessageException
    {
        super( header, vendorId, subSelector, version, data );
        if ( version < VERSION )
        {
            throw new InvalidMessageException(
                "Vendor Message 'PushProxyAcknowledgement' with deprecated version: " + version );
        }
        if ( version > VERSION )
        {
            throw new InvalidMessageException(
                "Vendor Message 'PushProxyAcknowledgement' with invalid version: " + version );
        }
        if ( data.length != 6 )
        {
            throw new InvalidMessageException(
                "Vendor Message 'PushProxyAcknowledgement' invalid data length: " + data.length );
        }
        
        byte[] ip = new byte[4];
        // the ip starts at byte 0
        ip[0] = data[0];
        ip[1] = data[1];
        ip[2] = data[2];
        ip[3] = data[3];
        
        int port = IOUtil.unsignedShort2Int( IOUtil.deserializeShortLE( data, 4 ) );
        if ( !AddressUtils.isPortInRange( port ) )
        {
            throw new InvalidMessageException( "Port out of range: " + port );
        }
        hostAddress = new DefaultDestAddress( ip, port );
    }
    
    /**
     * @return Returns the hostAddress.
     */
    public DestAddress getHostAddress()
    {
        return hostAddress;
    }
    
    /**
     * @param address
     */
    private static byte[] buildDataBody( DestAddress hostAddress )
        throws IllegalArgumentException
    {
        byte[] data = new byte[6];
        IpAddress ip = hostAddress.getIpAddress();
        if ( ip == null )
        {
            throw new IllegalArgumentException(
                "Vendor Message 'PushProxyAcknowledgement' contains no IP." );
        }
        byte[] ipAddress = hostAddress.getIpAddress().getHostIP();
        data[ 0 ] = ipAddress[ 0 ];
        data[ 1 ] = ipAddress[ 1 ];
        data[ 2 ] = ipAddress[ 2 ];
        data[ 3 ] = ipAddress[ 3 ];        
        IOUtil.serializeShortLE( (short)hostAddress.getPort(), data, 4 );
        return data; 
    }
}
