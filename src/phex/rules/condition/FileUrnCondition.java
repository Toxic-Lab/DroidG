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

import phex.common.URN;
import phex.download.RemoteFile;
import phex.query.Search;
import phex.xml.sax.rules.DCondition;
import phex.xml.sax.rules.DFileUrnCondition;

/**
 * Filters all files matching the expression.
 */
public class FileUrnCondition implements Condition
{
    private ListOrderedSet/*<URN>*/ urnSet;
    
    /**
     * @param expression
     * @param case1
     */
    public FileUrnCondition( )
    {
        urnSet = new ListOrderedSet();
    }
    
    /**
     * Deep copy constructor.
     * @param condition
     */
    public FileUrnCondition( FileUrnCondition condition )
    {
        this();
        update( condition );
    }
    
    public synchronized void update( FileUrnCondition condition )
    {
        urnSet.clear();
        // we simply add the string.. since they are not mutable. 
        urnSet.addAll(condition.urnSet);
    }
    
    
    public synchronized int getUrnCount()
    {
        return urnSet.size();
    }
    
    public synchronized Set<URN> getUrnSet()
    {
        return Collections.unmodifiableSet(urnSet);
    }
    
    /**
     * Returns a list of the ranges.
     * @return
     */
    public synchronized List<URN> getUrnList()
    {
        return urnSet.asList();
    }
    
    public synchronized FileUrnCondition addUrn( URN urn )
    {
        urnSet.add( urn );
        return this;
    }
    
    public synchronized void removeHash( URN urn )
    {
        urnSet.remove( urn );
    }

    public synchronized boolean isMatched( Search search, RemoteFile remoteFile )
    {
        URN fileUrn = remoteFile.getURN();

        Iterator<URN> iterator = urnSet.iterator();
        while( iterator.hasNext() )
        {
            URN urn = iterator.next();
            if ( fileUrn.equals( urn ) )
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Validates if this condition is completly edited and ready for storage or 
     * requires further modifications.
     * @return true if complet false otherwise.
     */
    public synchronized boolean isComplete()
    {
        return getUrnCount() > 0;
    }
    
    @Override
    public synchronized Object clone()
    {
        try
        {
            FileUrnCondition clone = (FileUrnCondition) super.clone();
            clone.urnSet = new ListOrderedSet();
            clone.urnSet.addAll( urnSet );
            return clone;
        }
        catch (CloneNotSupportedException exp)
        {
            throw new InternalError();
        }
    }

    public synchronized DCondition createDCondition()
    {
        DFileUrnCondition dCond = new DFileUrnCondition();
        List<String> newList = new ArrayList<String>();
        Iterator<URN> iterator = urnSet.iterator();
        while ( iterator.hasNext() )
        {
            URN urn = iterator.next();
            newList.add( urn.getAsString() );
        }
        dCond.setUrns(newList);
        return dCond;
    }
}