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
 * Represents a shared resource on the file system.
 * This can be a file or directory.
 */
public abstract class SharedResource
{
    protected File systemFile;
    
    public SharedResource( File file )
    {
        systemFile = file;
    }
    
    /**
     * Only called from subclasses like PartialShareFile.
     */
    protected SharedResource( )
    {
    }

    /**
     * Returns the backed file object.
     * @return the backed file object.
     */
    public File getSystemFile()
    {
        return systemFile;
    }

    /**
     * Returns the file name without path information.
     * @return the file name without path information.
     */
    public String getFileName()
    {
        return systemFile.getName();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 307;
        int result = 1;
        result = prime * result
            + ((systemFile == null) ? 0 : systemFile.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        final SharedResource other = (SharedResource) obj;
        if ( systemFile == null )
        {
            if ( other.systemFile != null )
                return false;
        }
        else if ( !systemFile.equals( other.systemFile ) )
            return false;
        return true;
    }
    
    
}
