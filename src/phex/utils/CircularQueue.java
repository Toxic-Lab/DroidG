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
 */
package phex.utils;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This is a fast double-ended circular queue.
 * For performance reasons the class is not synchronized for thread safty the
 * user needs to synchronized the access to its CircularQueue object.
 */
// TODO to represent a framework class this should maybe implement the List interface.
public class CircularQueue
{
    /**
     * The size of the queue. There is always one unused element in the queue.
     */
    private int size;

    /**
     * The queue contents.
     */
    private Object[] elements;

    /**
     * The head index of the queue.
     */
    private int headIdx;

    /**
     * The tailhead index of the queue.
     */
    private int tailIdx;

    /**
     * The minimum maxSize is 1. Creates a circularQueue with a initial size of
     * 10.
     */
    public CircularQueue( int maxSize )
    {
        this( Math.min( 10, maxSize ), maxSize );
    }

    /**
     * Creates a CircularQueue with a initialSize and a maxSize. The
     * queue is dynamicaly expanded until maxSize is reached. This behaivor
     * is usefull for large queues that might not always get totaly filled.
     * For queues that are always filled it is adviced to set initialSize and
     * maxSize to the same value.
     * The minimum maxSize is 1.
     */
    public CircularQueue( int initialSize, int maxSize )
    {
        // this is asserted
        if ( maxSize < 1 )
        {
            throw new RuntimeException( "Min size of the CircularQueue is 1" );
        }

        size = maxSize + 1;
        elements = new Object[ initialSize + 1];
        headIdx = tailIdx = 0;
    }

    /**
     * Adds a object to the tail of the queue. If the queue is full a element
     * from the head is dropped to free space.
     */
    public Object addToTail( Object obj )
    {
        //logQueue();
        Object dropObj = null;
        if ( isFull() )
        {// drop the head element
            dropObj = removeFromHead();
        }
        ensureCapacity();
        elements[ tailIdx ] = obj;
        tailIdx = nextIndex( tailIdx );
        //logQueue();
        return dropObj;
    }

    /**
     * Adds a object to the head of the queue. If the queue is full a element
     * from the tail is dropped to free space. The dropped element is returned.
     */
    public Object addToHead( Object obj )
    {
        //logQueue();
        Object dropObj = null;
        if ( isFull() )
        {// drop the head element
            dropObj = removeFromTail();
        }
        ensureCapacity();
        headIdx = prevIndex( headIdx );
        elements[ headIdx ] = obj;
        //logQueue();
        return dropObj;
    }

    /**
     * Clears the queue. Afterwards no elements from the queue can be accessed
     * anymore.
     * The references to the elements in the queue are not released from the
     * internal array they are not freed for garbage collection until they
     * are overwritten with new references.
     */
    public void clear()
    {
        headIdx = 0;
        tailIdx = 0;
    }

    /**
     * Returns the head element of the queue.
     * @throws NoSuchElementException if queue is empty.
     */
    public Object getFirst() throws NoSuchElementException
    {
        if ( isEmpty() )
        {
            throw new NoSuchElementException();
        }
        return elements[ headIdx ];
    }

    /**
     * Returns the tail element of the queue.
     * @throws NoSuchElementException if queue is empty.
     */
    public Object getLast() throws NoSuchElementException
    {
        if ( isEmpty() )
        {
            throw new NoSuchElementException();
        }
        // adjust last index...
        int index = prevIndex( tailIdx );
        return elements[ index ];
    }

    /**
     * Returns the element at index in the queue.
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Object get( int index ) throws IndexOutOfBoundsException
    {
        int idx = mapIndex( index );
        return elements[ idx ];
    }

    /**
     * Returns the number of elements in the queue.
     */
    public int getSize()
    {
        if ( headIdx <= tailIdx )
        {
            //    H     T
            // [ |x|x|x| | ]
            //  0 1 2 3 4 5
            return tailIdx - headIdx;
        }
        else
        {
            //    T     H
            // [x| | | |x|x]
            //  0 1 2 3 4 5
            return elements.length - headIdx + tailIdx;
        }
    }

    /**
     * Returns the maximum number of elements this queue can hold.
     */
    public int getCapacity()
    {
        return size - 1;
    }

    /**
     * Returns true is the queue is empty.
     */
    public boolean isEmpty()
    {
        return headIdx == tailIdx;
    }

    /**
     * Returns true if the queue is full.
     */
    public boolean isFull()
    {
        if ( elements.length == size )
        {// the queue is fully expanded
            return nextIndex( tailIdx ) == headIdx;
        }
        return false;
    }

    /**
     * Removes and returns the element on the head of the queue
     * @throws NoSuchElementException if queue is empty.
     */
    public Object removeFromHead() throws NoSuchElementException
    {
        if ( isEmpty() )
        {
            throw new NoSuchElementException();
        }
        Object obj = elements[ headIdx ];
        elements[ headIdx ] = null;
        headIdx = nextIndex( headIdx );
        return obj;
    }

    /**
     * Removes and returns the element on the tail of the queue
     * @throws NoSuchElementException if queue is empty.
     */
    public Object removeFromTail() throws NoSuchElementException
    {
        if ( isEmpty() )
        {
            throw new NoSuchElementException();
        }
        tailIdx = prevIndex( tailIdx );
        Object obj = elements[ tailIdx ];
        elements[ tailIdx ] = null;
        return obj;
    }
    
    public Object remove(int idx) 
        throws IndexOutOfBoundsException
    {
        Object obj = get( idx );
        for ( int i = mapIndex( idx ); i != tailIdx; i = nextIndex( i ) ) 
        {
            elements[ i ] = elements[ nextIndex( i ) ];
        }
        tailIdx = prevIndex( tailIdx );
        elements[ tailIdx ] = null;
        return obj;
    }
    
    /**
     * Removes all occurences of the object from this queue.
     * @param obj
     * @return
     */
    public boolean removeAll( Object obj ) 
    {
        boolean removed = false;
        for (int i=0; i < getSize(); i++)
        {
            if ( obj.equals( get(i) ) )
            {
                remove( i );
                i--;
                removed = true;
            }
        }
        return removed;
    }

    public Iterator iterator()
    {
        return new CircularQueueIterator();
    }

    private void ensureCapacity()
    {
        if ( elements.length == size )
        {
            return;
        }
        if ( nextIndex( tailIdx ) != headIdx )
        {
            return;
        }
        // expand array and copy over
        int newSize = Math.min( elements.length * 2, size );
        Object[] newElements = new Object[ newSize ];
        if ( headIdx <= tailIdx )
        {
            //    H     T
            // [ |x|x|x| | ]
            //  0 1 2 3 4 5
            System.arraycopy( elements, headIdx, newElements, headIdx,
                tailIdx - headIdx );
        }
        else
        {
            //    T     H
            // [x| | | |x|x]
            //  0 1 2 3 4 5
            int newHeadIdx = newSize - ( elements.length - headIdx );
            if ( tailIdx > 0 )
            {
                System.arraycopy( elements, 0, newElements, 0, tailIdx - 1 );
            }
            System.arraycopy( elements, headIdx, newElements, newHeadIdx,
                elements.length - headIdx);
            headIdx = newHeadIdx;
        }
        elements = newElements;
    }

    /**
     * Maps the given index into the index in the internal array.
     */
    private int mapIndex( int index ) throws IndexOutOfBoundsException
    {
        if (index >= elements.length || index < 0)
        {
            throw new IndexOutOfBoundsException( "Index: " + index + ", Size: "
                + elements.length);
        }
        return ( index + headIdx ) % elements.length;
    }

    /**
     * Gets the next index for the given index.
     */
    private int nextIndex( int idx )
    {
        if ( idx == elements.length - 1 )
        {
            return 0;
        }
        else
        {
            return idx + 1;
        }
    }

    /**
     * Gets the previous index for the given index.
     */
    private int prevIndex( int idx )
    {
        if ( idx == 0 )
        {
            return elements.length - 1;
        }
        else
        {
            return idx - 1;
        }
    }

 /*   private void logQueue()
    {
        System.out.println( "-------------------------------" );
        System.out.println( headIdx + " " + tailIdx + " " + getSize() );
        System.out.print( "[ " );
        for( int i = 0; i < elements.length; i++ )
        {
            System.out.print( elements[i] + " | " );
        }
        System.out.println( " ]" );
        System.out.println( "-------------------------------" );

    }*/

    private class CircularQueueIterator implements Iterator
    {
        /**
         * Store originalHead to check for concurent modifications.
         */
        int originalHead;

        /**
         * Store originalTail to check for concurent modifications.
         */
        int originalTail;

        /**
         * Next element index.
         */
        int nextElement;

        public CircularQueueIterator()
        {
            nextElement = headIdx;
            originalHead = headIdx;
            originalTail = tailIdx;
        }

        public boolean hasNext()
        {
            checkForComodification();
            return nextElement != tailIdx;
        }

        public Object next() throws NoSuchElementException
        {
            checkForComodification();
            if ( nextElement == tailIdx )
            {
                throw new NoSuchElementException();
            }

            Object obj = elements[ nextElement ];
            nextElement = nextIndex( nextElement );
            return obj;
        }

        /**
         * This operation is not supported.
         */
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        private void checkForComodification()
        {
            if ( originalHead != headIdx || originalTail != tailIdx )
            {
                throw new ConcurrentModificationException();
            }
        }
    }
}