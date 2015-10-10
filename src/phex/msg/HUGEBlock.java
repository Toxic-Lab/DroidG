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
 *  Created on 04.01.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.msg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import phex.common.URN;

// TODO extend this class to also support writing of huge blocks.
public class HUGEBlock
{
    private GGEPBlock[] ggepBlocks = null;

    private Set<URN> urns = null;

    private Set<String> others = null;
    
    public HUGEBlock( byte[] data )
    {
        ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream(64);
        
        ByteArrayInputStream baInStream = new ByteArrayInputStream( data );
        PushbackInputStream inStream = new PushbackInputStream( 
            baInStream );

        try
        {
            byte b;
            while( true )
            {
                b = (byte)inStream.read();
                if( b == -1 || b == 0x00 )
                {
                    break;
                }
                // GGEP
                if ( b == GGEPBlock.MAGIC_NUMBER )
                {
                    inStream.unread(b);
                    parseGGEPBlock( inStream );
                }
                else
                { // HUGE
                    tempBuffer.reset();
                    while ( true )
                    {
                        tempBuffer.write(b);
                        b = (byte)inStream.read();
                        if( b == -1 || b == 0x1c )
                        {
                            break;
                        }
                    }
                    if ( b == -1 || b == 0x1c )
                    {
                        try
                        {
                            String extString = new String( tempBuffer.toByteArray(), "UTF-8" );
                            
                            // first check if this is a query by URN
                            // ( urn:<NID>:<NSS> )
                            if ( URN.isValidURN( extString ) )
                            {
                                URN urn = new URN( extString );
                                if ( urns == null )
                                {
                                    urns = new HashSet<URN>( 3 );
                                }
                                urns.add( urn );
                            }
                            // TODO3 we dont track URN type request yet. we always 
                            // return sha1 type urns on query replies since there is
                            // currently only one known urn type
                            else
                            {
                                // other extensions, might be rich query XML
                                if ( others == null )
                                {
                                    others = new HashSet<String>( 3 );
                                }
                                others.add( extString );
                            }
                        }
                        catch (IOException exp)
                        {
                        }
                    }
                }
            }
        }
        catch (IOException exp)
        {// should not happen
            //NLogger.error(HUGEBlock.class, exp, exp);
        }
    }

    /**
     * @param offset
     * @return
     */
    private void parseGGEPBlock( PushbackInputStream inStream )
    {
        try
        {
            GGEPBlock[] ggeps = GGEPBlock.parseGGEPBlocks( inStream );
            ggepBlocks = ggeps;
        }
        catch ( InvalidGGEPBlockException exp )
        {
        }
        catch ( IOException exp )
        {
        }
    }

    public GGEPBlock[] getGGEPBlocks()
    {
        return ggepBlocks;
    }

    public Set<URN> getURNS()
    {
        if ( urns == null )
        {
            return Collections.emptySet();
        }
        else
        {
            return urns;
        }
    }

    public Set<String> getOthers()
    {
        if ( others == null )
        {
            return Collections.emptySet();
        }
        else
        {
            return others;
        }
    }
}
