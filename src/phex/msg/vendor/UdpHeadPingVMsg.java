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

import phex.common.URN;
import phex.msg.GGEPBlock;
import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.MsgHeader;
import phex.utils.IOUtil;

public class UdpHeadPingVMsg extends VendorMsg
{
    public static final int VERSION = 2;
    
    private static final int FEATURE_MASK=0x1F;
    
    private static final int PLAIN = 0x0;
    private static final int INTERVALS = 0x1;
    private static final int ALT_LOCS = 0x2;
    private static final int PUSH_ALTLOCS = 0x4;
    private static final int FWT_PUSH_ALTLOCS = 0x8;
    private static final int GGEP_PING = 0x10;
        
    private byte features;
    
    private URN urn;
    
    private GUID guid;
    
    public UdpHeadPingVMsg( MsgHeader header, byte[] vendorId,
        int subSelector, int version, byte[] data) 
        throws InvalidMessageException
    {
        super(header, vendorId, subSelector, version, data);
        if ( version != VERSION )
        {
            throw new InvalidMessageException("Vendor Message 'UdpHeadPingVMsg' with invalid version: "
                + version);
        }
        
        if ( data.length < 42 )
        {
            throw new InvalidMessageException("Vendor Message 'UdpHeadPingVMsg' with invalid data length: "
                + data.length );
        }
      
        // parse feature info
        features = (byte) (data[0] & FEATURE_MASK);
        
        // parse urn
        String urnStr = new String( data, 1, 41);
        if ( !URN.isValidURN( urnStr ) )
        {
            throw new InvalidMessageException("Vendor Message 'UdpHeadPingVMsg' with invalid URN." );
        }
        urn = new URN( urnStr );
        
        // check for possible ggep
        if ( (features & GGEP_PING) == GGEP_PING )
        {
            GGEPBlock[] ggepBlocks = GGEPBlock.parseGGEPBlocks( data, 42 );
            if ( GGEPBlock.isExtensionHeaderInBlocks( ggepBlocks, "PUSH" ))
            {
                byte[] guidArr = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, "PUSH" );
                guid = new GUID( guidArr );
            }
        }
    }

    /**
     * @return the features
     */
    public byte getFeatures()
    {
        return features;
    }

    /**
     * @return the urn
     */
    public URN getUrn()
    {
        return urn;
    }

    /**
     * @return the guid
     */
    public GUID getGuid()
    {
        return guid;
    }
}