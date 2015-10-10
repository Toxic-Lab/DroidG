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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import phex.msg.GGEPBlock;
import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.MsgHeader;
import phex.utils.IOUtil;

public class MessageAcknowledgementVMsg extends VendorMsg
{
//    private static final Logger logger = LoggerFactory.getLogger( 
//        MessageAcknowledgementVMsg.class );
    
    public static final int VERSION = 3;
    
    public MessageAcknowledgementVMsg(MsgHeader header, byte[] vendorId,
        int subSelector, int version, byte[] data)
        throws InvalidMessageException
    {
        super(header, vendorId, subSelector, version, data);
        if ( version != VERSION )
        {
            throw new InvalidMessageException("Vendor Message 'MessageACKVMsg' with invalid version: "
                    + version);
        }
        // TODO validate payload length
        //if ( data.length < MIN_PAYLOAD_LENGTH )
        //    throw new InvalidMessageException("Vendor Message 'MessageACKVMsg' invalid data length: "
        //            + data.length);
    }
    
    
    public MessageAcknowledgementVMsg( GUID guid, int resultCount, byte[] securityToken )
    {
        super( VENDORID_LIME, SUBSELECTOR_MESSAGE_ACK, VERSION, 
            buildDataBody( resultCount, securityToken ) );
        getHeader().setMsgID( guid );
    }
    
    private static byte[] buildDataBody( int resultCount, byte[] securityToken )
    {
        if ( resultCount <= 0 || resultCount > 255)
            throw new IllegalArgumentException(
                "Invalid number of results: " + resultCount );
        byte[] countBytes = new byte[2];
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtil.serializeShortLE( (short)resultCount, countBytes, 0 );
            out.write(countBytes[0]);
            GGEPBlock ggepBlock = new GGEPBlock( true );
            ggepBlock.addExtension( GGEPBlock.SECURE_OOB_ID, securityToken );
            out.write( ggepBlock.getBytes() );
            return out.toByteArray();
        }
        catch (IOException exp)
        {
            // should never happen
            //logger.error( exp.toString(), exp );
            throw new RuntimeException( exp );
        }
    }
}