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

import java.util.Iterator;
import java.util.NoSuchElementException;

import phex.utils.CircularQueue;

/**
 * This class is able to manage a priority based queue. Once the capacity of a
 * priority is reached the oldest items of this priority are overwritten.
 * The priorities are indexed from 0 ( lowest priority ) to n ( highest priortiy ).
 */
public class PriorityQueue
{
    /**
     * The priority queues.
     */
    private CircularQueue[] queues;

    /**
     * Size of the queue, maintained to save performence without iterating over
     * all queues to calculate the size.
     */
    private int size;

    /**
     * Creates a new priority queue. The length of the capacities array represent
     * the number of used prioritys. The value of a array element defines the
     * capacity of a priority.
     * @param capacities The length of the capacities array represent
     *        the number of used prioritys. The value of a array element defines
     *        the capacity of a priority.
     */
    public PriorityQueue( int[] capacities )
    {
        queues = new CircularQueue[ capacities.length ];
        for ( int i = 0; i < capacities.length; i++ )
        {
            queues[i] = new CircularQueue( (int)Math.ceil( capacities[i] / 4.0 ),
                capacities[i] );
        }
    }

    /**
     * Adds a element to the head of the given priority. If the capacity of this
     * priority is reached the tail element of the priority is removed and returned.
     * @param obj The element to add.
     * @param priority The priority that this elements belongs to.
     */
    public Object addToHead( Object obj, int priority )
    {
        if ( priority < 0 || priority >= queues.length )
        {
            throw new IllegalArgumentException( "Priority out of range: "
                + priority );
        }
        Object dropObj = queues[priority].addToHead( obj );
        if ( dropObj == null )
        {// no element is dropped raise size...
            size++;
        }
        return dropObj;
    }

    /**
     *
     * Removes and returns the element with the hightest priority.
     * @return
     * @throws NoSuchElementException
     */
    public Object removeMaxPriority( )
    	throws NoSuchElementException
    {
        for ( int i = queues.length - 1; i >= 0; i-- )
        {
            if ( queues[i].isEmpty() )
            {
                continue;
            }
            // maintain size.
            size--;
            return queues[i].removeFromHead();
        }
        throw new NoSuchElementException( "PriorityQueue is empty" );
    }
    
    /**
     * Removes and returns a structure containing :
     * the element with the highest priority 
     * the priority associated with the element
     * @return
     * @throws NoSuchElementException
     */
    public Object[] remove()
        throws NoSuchElementException
    {
        for ( int i = queues.length - 1; i >= 0; i-- )
        {
            if ( queues[i].isEmpty() )
            {
                continue;
            }
            // maintain size.
            size--;
            
            Object[] elementAndPriotity = new Object[2];
            elementAndPriotity[0] = queues[i].removeFromHead();
            elementAndPriotity[1] = Integer.valueOf(i);
            return elementAndPriotity;
        }
        throw new NoSuchElementException( "PriorityQueue is empty" );
    }
    
    /**
     * Removes the object from all priorities
     * @param obj
     * @return
     */
    public boolean removeFromAll( Object obj )
    {
        boolean removed = false;
        for ( int i=0; i < queues.length; i++) 
        {
            removed = removed | queues[i].removeAll( obj );
        }
        
        // elements have been removed.. recalculate the size
        if ( removed ) 
        {
            size = 0;
            for (int i = 0; i < queues.length; i++)
            {
                size += queues[i].getSize();
            }
        }
        return removed;
    }
    
    public int getSize()
    {
        return size;
    }

    public boolean isEmpty()
    {
        return size == 0;
    }

    /**
     * Returns true if the queue for the priority is full.
     * @param priority
     * @return
     */
    public boolean isFull( int priority )
    {
        return queues[priority].isFull();
    }

    /**
     * Clears the queues. Afterwards no elements from the queues can be accessed
     * anymore.
     */
    public void clear()
    {
        for ( int i = 0; i < queues.length; i++ )
        {
            queues[i].clear();
        }
        size = 0;
    }

    public Iterator iterator()
    {
        // TODO this could be improved with creating its own iterator...
        // but that would be more work ;-)
        CompoundIterator iterator = new CompoundIterator( queues.length );
        for ( int i = queues.length - 1; i >= 0; i-- )
        {
            if ( queues[i].getSize() > 0 )
            {
                iterator.addIterator( queues[i].iterator() );
            }
        }
        return iterator;
    }
}