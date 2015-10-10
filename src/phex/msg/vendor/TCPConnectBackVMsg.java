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

import phex.common.address.AddressUtils;
import phex.msg.*;
import phex.utils.IOUtil;

/**
 * 
 */
public class TCPConnectBackVMsg extends VendorMsg
{
    public static final int VERSION = 1;
    
    private int port;
    
    public TCPConnectBackVMsg( MsgHeader header, byte[] vendorId, int subSelector, 
        int version, byte[] data ) throws InvalidMessageException
    {
        super( header, vendorId, subSelector, version, data );
        if ( version > VERSION )
        {
            throw new InvalidMessageException(
                "Vendor Message 'TCPConnectBack' with invalid version: " + version );
        }
        if ( data.length != 2 )
        {
            throw new InvalidMessageException(
                "Vendor Message 'TCPConnectBack' invalid data length: " + data.length );
        }
        
        // parse connect back port
        port = IOUtil.unsignedShort2Int( IOUtil.deserializeShortLE( data, 0  ) );
        if( !AddressUtils.isPortInRange( port ) )
        {
            throw new InvalidMessageException( 
                "Invalid connect back port: " + port );
        }
    }
    
    public TCPConnectBackVMsg( int port )
    {
        super( VENDORID_BEAR, SUBSELECTOR_TCP_CONNECT_BACK, VERSION, 
              buildDataBody(port) );
        this.port = port;
    }
    
    public int getPort()
    {
        return port;
    }

    /**
     * @param port
     */
    private static byte[] buildDataBody( int port )
    {
        byte[] data = new byte[2];
        IOUtil.serializeShortLE( (short)port, data, 0 );
        return data; 
    }
}
