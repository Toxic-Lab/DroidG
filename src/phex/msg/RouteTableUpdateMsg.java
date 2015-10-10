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


public abstract class RouteTableUpdateMsg extends Message
{
    public static final byte RESET_TABLE_VARIANT = 0x00;
    public static final byte PATCH_TABLE_VARIANT = 0x01;

    protected byte variant;

    public RouteTableUpdateMsg( byte aVariant, int length )
    {
        super( new MsgHeader( MsgHeader.ROUTE_TABLE_UPDATE_PAYLOAD, (byte)1, length ) );
        variant = aVariant;
    }

    public RouteTableUpdateMsg( byte aVariant, MsgHeader aHeader )
    {
        super( aHeader );
        variant = aVariant;
    }

    /**
     * Returns the variant of the RouteTableUpdateMsg. Possible variants are
     * RESET_TABLE_VARIANT, PATH_TABLE_VARIANT.
     * @return the message variant.
     */
    public byte getVariant()
    {
        return variant;
    }
    
    public static RouteTableUpdateMsg parseMessage( MsgHeader header, byte[] aBody )
        throws InvalidMessageException
    {
        // read message variant
        byte variant = aBody[0];

        switch (variant)
        {
            case RESET_TABLE_VARIANT:
                return new QRResetTableMsg( header, aBody );
            case PATCH_TABLE_VARIANT:
                return new QRPatchTableMsg( header, aBody );
            default:
                throw new InvalidMessageException(
                    "Unknown RouteTableUpdateMsg variant");
        }
    }
}