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
 */
package phex.utils;

import java.util.HashMap;

/**
 * The ReadWriteLock is a helper class for synchronized execution of
 * multiple threads on a single resource.
 * This class is responsible for maintaining a locking strategy that
 * allows multiple read but single write locking. The programmers is responsible
 * for using this class wisely and should be aware of possible deadlock situations.
 * Locking a resource should be done like this:
 * <pre>
 * rwLock.readLock();  // or writeLock()
 * try
 * {
 *    // use the resource here
 * }
 * finally
 * {
 *     try{ rwLock.readUnlock(); }  // or writeUnlock()
 *     catch (IllegalAccessException exp )
 *     { Trace.error( exp ); }
 * }
 * </pre>
 */
public class ReadWriteLock
{
    /**
     * For security reasons this max wait time is introduced. It should make
     * sure threads dont get stuck in a dead lock situation over a long long time
     * and bring down the system.
     * After this time is elapesed a RuntimeException is thrown to cause
     * a strong abortion of the process and indicates a major error.
     */
    private static final long MAX_WAIT_TIME = 2 * 60 * 1000;

    private Thread writeLockOwner;
    private HashMap readLockOwners;

    public ReadWriteLock()
    {
        readLockOwners = new HashMap( 10 );
    }

    /**
     * Sets up a read lock. The methods blocks if a write lock is currently
     * set and waits till it's released except when the current thread also
     * owns the write lock.
     */
    public synchronized void readLock( )
    {
        Thread currentThread = Thread.currentThread();
        if ( writeLockOwner == currentThread )
        {
            // I already have the write lock
            return;
        }
        Integer readLockCount = (Integer)readLockOwners.get( currentThread );
        if ( readLockCount != null )
        {
            // I already have the read lock... raise the count
            readLockOwners.put( currentThread, Integer.valueOf( readLockCount.intValue() + 1 ) );
            return;
        }
        boolean wasInterrupted = false;
        while ( writeLockOwner != null )
        {
            try
            {
                long startTime = System.currentTimeMillis();
                wait( MAX_WAIT_TIME );
                long stopTime = System.currentTimeMillis();
                if ( stopTime >= startTime + MAX_WAIT_TIME )
                {
                    throwWaitedTooLongError();
                }
            }
            catch (InterruptedException exp)
            {
                wasInterrupted = true;
                
            }
        }
        readLockOwners.put( currentThread, Integer.valueOf( 1 ) );
        if ( wasInterrupted )
        {//reset interrupted
            currentThread.interrupt();
        }
    }

    /**
     * Unlocks a read lock.
     */
    public synchronized void readUnlock()
        throws IllegalAccessException
    {
        Thread currentThread = Thread.currentThread();
        if ( writeLockOwner == currentThread )
        {
            // I already have the write lock
            return;
        }

        Integer readLockCount = (Integer)readLockOwners.remove( currentThread );
        if ( readLockCount == null )
        {
            throw new IllegalAccessException(
                "Thread without holding read lock trys to unlock." );
        }
        int newCount = readLockCount.intValue() - 1;
        if ( newCount > 0 )
        {
            readLockOwners.put( currentThread, Integer.valueOf( newCount ) );
        }
        else if ( readLockOwners.size() == 0 )
        {
            // notify ALL threads waiting to get a lock.
            notifyAll();
        }
    }

    /**
     * Sets up a write lock. The methods blocks if a write lock is currently
     * set and waits till it's released except when the current thread also
     * owns the write lock.
     * After the write lock is set up no more read locks can be set and when
     * all read locks are unlocked the methods returns control.
     * TODO count the set up write locks and decrease the count here. But
     * actualy for a clean implementation no multiple write locks should be set
     * up by a single thread.
     * @throws InterruptedException
     */
    public synchronized void writeLock( )
    {
        Thread currentThread = Thread.currentThread();
        if ( writeLockOwner == currentThread )
        {
            // I already have the write lock
            return;
        }
        
        // Check if I don't hold a read lock...
        if ( readLockOwners.containsKey( currentThread ) )
        {
            throw new RuntimeException( "Can't upgrade read lock, this could cause deadlocks." );
        }
        
        boolean wasInterrupted = false;
        while ( writeLockOwner != null )
        {
            try
            {
                long startTime = System.currentTimeMillis();
                wait( MAX_WAIT_TIME );
                long stopTime = System.currentTimeMillis();
                if ( stopTime >= startTime + MAX_WAIT_TIME )
                {
                    throwWaitedTooLongError();
                }
            }
            catch (InterruptedException e)
            {
                wasInterrupted = true;
            }
        }
        // set the lock to block other readers...
        writeLockOwner = currentThread;

        // I have the write lock now...

        // wait till all readers are gone...
        while ( readLockOwners.size() > 0 )
        {
            try
            {
                long startTime = System.currentTimeMillis();
                wait( MAX_WAIT_TIME/20 );
                long stopTime = System.currentTimeMillis();
                if ( stopTime >= startTime + MAX_WAIT_TIME )
                {
                    // release already owned write lock...
                    writeLockOwner = null;
                    //NLogger.error( ReadWriteLock.class,
                    //    "Waited too long to ensure write lock." );
                    throw new RuntimeException(
                        "Waited too long to ensure write lock." );
                }
            }
            catch (InterruptedException e)
            {
                wasInterrupted = true;
            }
        }
        if ( wasInterrupted )
        {//reset interrupted
            currentThread.interrupt();
        }
    }

    /**
     * Unlocks a write lock. Be aware that a single call to this method unlocks
     * a write lock even though there have been multiple write locks from the
     * same thread!!
     * TODO count the set up write locks and decrease the count here. But
     * actualy for a clean implementation no multiple write locks should be set
     * up by a single thread.
     */
    public synchronized void writeUnlock()
        throws IllegalAccessException
    {
        Thread currentThread = Thread.currentThread();
        if ( writeLockOwner != currentThread )
        {
            throw new IllegalAccessException("Current thread not owner of write lock.");
        }
        // release write lock
        writeLockOwner = null;

        // notify ALL threads waiting to get a lock.
        notifyAll();
    }
    
    /**
     * Can be called to assert that the write lock is owned.
     * @throws IllegalAccessException 
     *
     */
    public void assertWriteLock() throws IllegalAccessException
    {
        Thread currentThread = Thread.currentThread();
        if ( writeLockOwner != currentThread )
        {
            throw new IllegalAccessException("Current thread not owner of write lock.");
        }
    }
    
    private void throwWaitedTooLongError()
    {
        //NLogger.error( ReadWriteLock.class,
        //    "Waited too long to aquire lock. WriteOwner: " + writeLockOwner );
        if ( writeLockOwner != null )
        {
            StackTraceElement[] stack = writeLockOwner.getStackTrace();
            for ( int i = 0; i < 5 && i < stack.length; i++ )
            {
                //NLogger.error( ReadWriteLock.class, "Stack: " + stack[i] );
            }
        }
        throw new RuntimeException( "Waited too long to aquire lock." );
    }

    /*private void debug()
    {
        System.out.println( "readLock: " + readLockOwners.size() +
        " - writeLock: " + writeLock + " - " + Thread.currentThread() );
    }*/
}