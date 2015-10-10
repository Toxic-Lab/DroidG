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
 *  Created on 04.04.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.msg.vendor;

import phex.msg.InvalidMessageException;
import phex.msg.MsgHeader;

/**
 *
 */
public class HopsFlowVMsg extends VendorMsg
{
    public static final int VERSION = 1;
    
    /**
     * Creates a new hops flow vendor message
     * @param hopsValue the upper bound for hops to receive.
     */
    public HopsFlowVMsg( int hopsValue )
    {
        super( VENDORID_BEAR, SUBSELECTOR_HOPS_FLOW, VERSION, 
            buildDataBody( hopsValue ) );
    }
    
    public HopsFlowVMsg( MsgHeader header, byte[] vendorId, 
        int subSelector, int version, byte[] data )
        throws InvalidMessageException
    {
        super( header, vendorId, subSelector, version, data );
        if ( version < VERSION )
        {
            throw new InvalidMessageException(
                "Vendor Message 'HopsFlowVMsg' with deprecated version: " + version );
        }
        if ( version > VERSION )
        {
            throw new InvalidMessageException(
                "Vendor Message 'HopsFlowVMsg' with invalid version: " + version );
        }
        if ( data.length != 1 )
        {
            throw new InvalidMessageException(
                "Vendor Message 'HopsFlowVMsg' invalid data length: " + data.length );
        }
    }
    
    public byte getHopsValue()
    {
        return getVenderMsgData()[0];
    }

    /**
     * @param hopsValue
     * @return
     */
    private static byte[] buildDataBody( int hopsValue )
    {
        byte[] body = { (byte)hopsValue };
        return body;
    }
    
    
}
