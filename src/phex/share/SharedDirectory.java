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
 *  Created on 13.12.2004
 *  --- CVS Information ---
 *  $Id$
 */
package phex.share;

import java.io.File;

/**
 *
 */
public class SharedDirectory extends SharedResource
{
    public static final short SHARED_DIRECTORY = 1;
    public static final short UNSHARED_PARENT_DIRECTORY = 2;
    
    /**
     * The type of the shared directory. This can be SHARED_DIRECTORY or PARTIALY_SHARED_DIRECTORY.
     */
    private short type;
    
    public SharedDirectory( File file )
    {
        super( file );
    }
    
    /**
     * @return Returns the type.
     */
    public short getType()
    {
        return type;
    }
    
    /**
     * @param type The type to set.
     */
    public void setType(short type)
    {
        this.type = type;
    }
}
