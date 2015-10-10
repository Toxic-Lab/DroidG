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
package phex.common.collections;

import java.util.*;

/**
 * With the compound iterator it is possible to easily concat different interators
 * with each other.
 */
// TODO3 could be replaced by org.apache.commons.collections.iterators.IteratorChain.
public class CompoundIterator implements Iterator
{
    /**
     * The list of iterators.
     */
    private List<Iterator> iteratorList;

    /**
     * The position of the current iterator in the list.
     */
    private int counter;

    public CompoundIterator( int numberOfIterators )
    {
        iteratorList = new ArrayList<Iterator>( numberOfIterators );
        counter = 0;
    }

    public void addIterator( Iterator iterator )
    {
        iteratorList.add( iterator );
    }

    public boolean hasNext()
    {
        while ( counter < iteratorList.size() )
        {
            Iterator current = iteratorList.get( counter );
            if ( current.hasNext() )
            {
                return true;
            }
            counter ++;
        }
        return false;
    }

    public Object next()
    {
        while ( counter < iteratorList.size())
        {
            Iterator current = iteratorList.get( counter );
            if ( current.hasNext())
            {
                return current.next();
            }
            counter++;
        }
        throw new NoSuchElementException( "No more elements in iterator." );
    }

    /**
     * This operation is not supported.
     */
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}