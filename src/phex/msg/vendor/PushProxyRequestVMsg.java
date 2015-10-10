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

import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.MsgHeader;
import phex.utils.IOUtil;

/**
 *
 *
 */
public class PushProxyRequestVMsg extends VendorMsg
{
    public static final int VERSION = 2;
    
    public PushProxyRequestVMsg( GUID serventGuid )
    {
        super( VENDORID_LIME, SUBSELECTOR_PUSH_PROXY_REQUEST, VERSION, 
              IOUtil.EMPTY_BYTE_ARRAY );
        getHeader().setMsgID( serventGuid );
    }
    
    public PushProxyRequestVMsg( MsgHeader header, byte[] vendorId, 
        int subSelector, int version, byte[] data )
        throws InvalidMessageException
    {
        super( header, vendorId, subSelector, version, data );
        // we accept version 1 PushProxyRequests since Limewire sends them this way
        if ( version <= 0 )
        {
            throw new InvalidMessageException(
                "Vendor Message 'PushProxyRequest' with deprecated version: " + version );
        }
        if ( version > VERSION )
        {
            throw new InvalidMessageException(
                "Vendor Message 'PushProxyRequest' with invalid version: " + version );
        }
        if ( data.length != 0 )
        {
            throw new InvalidMessageException(
                "Vendor Message 'PushProxyRequest' invalid data length: " + data.length );
        }
    }
}
