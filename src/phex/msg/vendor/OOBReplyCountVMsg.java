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

import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.MsgHeader;
import phex.utils.IOUtil;

public class OOBReplyCountVMsg extends VendorMsg
{   
    public static final int VERSION = 3;
    
    public int resultCount;
    
    public OOBReplyCountVMsg( MsgHeader header, byte[] vendorId,
        int subSelector, int version, byte[] data) 
        throws InvalidMessageException
    {
        super(header, vendorId, subSelector, version, data);
        if ( version != VERSION )
        {
            throw new InvalidMessageException("Vendor Message 'OOBReplyCountVMsg' with invalid version: "
                + version);
        }
        
        if ( data.length < 2 )
        {
            throw new InvalidMessageException("Vendor Message 'OOBReplyCountVMsg' with invalid data length: "
                + data.length );
        }
        
        resultCount = IOUtil.unsignedByte2int( data[0] );
        // ignore data[1] since we don't support unsocialized results.
    }
    
    public int getResultCount()
    {
        return resultCount;
    }
    
    public OOBReplyCountVMsg( GUID guid, int resultCount ) 
    {
        super( VENDORID_LIME, SUBSELECTOR_OOB_REPLY_COUNT, VERSION,
            buildDataBody( resultCount ) );
        this.resultCount = resultCount;
        getHeader().setMsgID( guid );
    }
    
    private static byte[] buildDataBody( int resultCount )
    {
        if ( resultCount < 1 || resultCount > 255 )
        {
            throw new IllegalArgumentException( "Invalid number of results: " + resultCount );
        }
        byte[] resultBytes = new byte[2];
        IOUtil.serializeShortLE( (short) resultCount, resultBytes, 0 );
        // don't support unsocialized results
        resultBytes[1] = 0x0;
        return resultBytes;
    }
}