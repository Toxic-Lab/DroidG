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


import java.util.HashSet;
import java.util.Set;

import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.security.AccessType;
import phex.security.PhexSecurityManager;
import phex.utils.IOUtil;

/**
 * Used for parsing GGEP extension data.
 */
public class GGEPExtension
{
    /**
     * Parses the HostAddresses of the ALT GGEP extension.
     * Returns null incase the parsing fails.
     * @param ggepBlocks the GGEP Blocks to look in
     * @return the parsed HostAddresses
     */
    public static DestAddress[] parseAltExtensionData( GGEPBlock[] ggepBlocks, PhexSecurityManager securityService )
    {
        // The payload is an array of 6-byte entries.  The first 4 bytes encode
        // the IP of the server (in big-endian, as usual), and the remaining
        // 2 bytes encode the port (in little-endian).
        byte[] altLocData = GGEPBlock.getExtensionDataInBlocks( ggepBlocks,
            GGEPBlock.ALTERNATE_LOCATIONS_HEADER_ID );
            
        if ( altLocData == null )
        {
            return null;
        }
            
        // check for valid length
        if ( altLocData.length % 6 != 0 )
        {
//            NLogger.warn( GGEPExtension.class,
//                "Invalid ALT GGEPBlock length: " + HexConverter.toHexString( altLocData ) );
            return null;
        }
        
        int count = altLocData.length / 6;
        
        Set<DefaultDestAddress> altLocSet = new HashSet<DefaultDestAddress>( );
        int offset;
        byte[] ip;
        int port;
        DefaultDestAddress address;
        for ( int i = 0; i < count; i ++ )
        {
            offset = i * 6;
            ip = new byte[4];
            ip[0] = altLocData[ offset ];
            ip[1] = altLocData[ offset + 1 ];
            ip[2] = altLocData[ offset + 2 ];
            ip[3] = altLocData[ offset + 3 ];
            port = IOUtil.unsignedShort2Int(IOUtil.deserializeShortLE( altLocData, offset + 4 ));
            address = new DefaultDestAddress( ip, port );
            if ( address.isValidAddress() && 
                securityService.controlHostAddressAccess( address ) == AccessType.ACCESS_GRANTED )
            {
                altLocSet.add( address );
            }
        }
        DestAddress[] altLocAddresses = new DestAddress[ altLocSet.size() ];
        altLocSet.toArray( altLocAddresses );
        return altLocAddresses;
    }
    
    public static DestAddress[] parsePushProxyExtensionData( GGEPBlock[] ggepBlocks, PhexSecurityManager securityService )
    {
        // The payload is an array of 6-byte entries.  The first 4 bytes encode
        // the IP of the server (in big-endian, as usual), and the remaining
        // 2 bytes encode the port (in little-endian).
        byte[] data = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, GGEPBlock.PUSH_PROXY_HEADER_ID );
        if ( data == null )
        {
            return null;
        }
        
        // check for valid length
        if ( data.length % 6 != 0 )
        {
//            NLogger.warn( GGEPExtension.class,
//                "Invalid PushProxy GGEPBlock length: " + HexConverter.toHexString( data ) );
            return null;
        }
        
        int count = data.length / 6;
        
        Set<DefaultDestAddress> proxySet = new HashSet<DefaultDestAddress>( );
        int offset;
        byte[] ip;
        int port;
        DefaultDestAddress address;
        for ( int i = 0; i < count; i ++ )
        {
            offset = i * 6;
            ip = new byte[4];
            ip[0] = data[ offset ];
            ip[1] = data[ offset + 1 ];
            ip[2] = data[ offset + 2 ];
            ip[3] = data[ offset + 3 ];
            port = IOUtil.unsignedShort2Int(IOUtil.deserializeShortLE( data, offset + 4 ));
            
            address = new DefaultDestAddress( ip, port );
            if ( address.isValidAddress() && 
                securityService.controlHostAddressAccess( address ) == AccessType.ACCESS_GRANTED )
            {
                proxySet.add( address );
            }
        }
        DestAddress[] pushProxyAddresses = new DestAddress[ proxySet.size() ];
        proxySet.toArray( pushProxyAddresses );
        return pushProxyAddresses;
    }
    
    /**
     * Returns defaultValue in case no data is found or data is not valid.
     * @param ggepBlocks
     * @param headerID
     * @param defaultValue
     * @return
     */
    public static int parseIntExtensionData( GGEPBlock[] ggepBlocks, String headerID,
        int defaultValue )
    {
        byte[] data = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, headerID );
        if ( data == null )
        {
            return defaultValue;
        }
        if ( data.length < 1 )
        {
            return defaultValue;
        }
        if ( data.length > 4 )
        {
            return defaultValue;
        }
        return IOUtil.deserializeIntLE(data, 0, data.length );
    }
}
