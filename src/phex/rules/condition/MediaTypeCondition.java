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
 *  Created on 14.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.rules.condition;

import java.util.*;

import org.apache.commons.collections.set.ListOrderedSet;

import phex.common.MediaType;
import phex.download.RemoteFile;
import phex.query.Search;
import phex.xml.sax.rules.DCondition;
import phex.xml.sax.rules.DMediaTypeCondition;

public class MediaTypeCondition implements Condition
{
    private ListOrderedSet types;
    
    public MediaTypeCondition( )
    {
        types = new ListOrderedSet();
    }
    
    public MediaTypeCondition( MediaType type )
    {
        this();
        addType(type);
    }
    
    public MediaTypeCondition( MediaTypeCondition condition )
    {
        this();
        update( condition );
    }
    
    public synchronized void update( MediaTypeCondition condition )
    {
        types.clear();
        // we simply add the MediaTypes.. since they are not mutable. 
        types.addAll(condition.types);
    }
    
    public synchronized Set<MediaType> getTypes()
    {
        return Collections.unmodifiableSet(types);
    }
    
    public synchronized MediaTypeCondition addType( MediaType type )
    {
        types.add( type );
        return this;
    }
    
    public synchronized void removeType( MediaType type )
    {
        types.remove( type );
    }

    public synchronized boolean isMatched( Search search, RemoteFile remoteFile )
    {
        Iterator iterator = types.iterator();
        while( iterator.hasNext() )
        {
            MediaType type = (MediaType) iterator.next();
            if ( type.isFilenameOf( remoteFile.getFilename() ) )
            {
                return true;
            }
        }
        return false;
    }
    
    public synchronized boolean isComplete()
    {
        return types.size() > 0;
    }

    public synchronized Object clone()
    {
        try
        {
            MediaTypeCondition clone = (MediaTypeCondition) super.clone();
            clone.types = new ListOrderedSet();
            clone.types.addAll( types );
            return clone;
        }
        catch (CloneNotSupportedException exp)
        {
            throw new InternalError();
        }
    }
    
    public synchronized DCondition createDCondition()
    {
        DMediaTypeCondition dCond = new DMediaTypeCondition();
        List newList = new ArrayList( types );
        dCond.setTypes( newList );
        return dCond;
    }
}
