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
package phex.common;

import java.util.Vector;

/**
 * This class represents a queue of Runnable that will
 * get served one after another.
 * A own thread is used for execution.
 */
public class RunnerQueueWorker
{
    private boolean isInterrupted;
    private boolean isPaused;
    private Vector<Runnable> queue;
    private Thread runnerThread;
    private int threadPriority;
    
    /**
     * Creates a RunnerQueueWorker with NORM_PRIORITY.
     */
    public RunnerQueueWorker()
    {
        this( Thread.NORM_PRIORITY );
    }
    
    /**
     * Creates a RunnerQueueWorker with custom priority.
     */
    public RunnerQueueWorker( int threadPriority )
    {
        this.threadPriority = threadPriority;
        queue = new Vector<Runnable>();
        isInterrupted = false;
        isPaused = false;
    }
    
    public synchronized int getQueueSize()
    {
        return queue.size();
    }
    
    /**
     * Pauses the queue.
     */
    public synchronized void setPause( boolean state )
    {
        isPaused = state;
        notify();
    }
    
    /**
     * Clears the queue.
     */
    public synchronized void stopAndClear()
    {
       queue.clear();
       if ( runnerThread != null )
       {
           runnerThread.interrupt();
           isInterrupted = true;
       }
    }
    
    /**
     * Adds a runnable to be processed.
     */
    public synchronized void add( Runnable runable )
    {
        queue.add( runable );
        notify();
        if( runnerThread == null )
        {
            createRunner();
        }
    }

    private synchronized void createRunner()
    {
        isInterrupted = false;
        runnerThread = new Thread( ThreadTracking.rootThreadGroup, 
            new QueueWorker() );
        runnerThread.setPriority(Thread.NORM_PRIORITY);
        runnerThread.setDaemon( true );
        runnerThread.start();
    }
    
    private class QueueWorker implements Runnable 
    {
        public void run() 
        {
            try
            {
                while( true ) 
                {
                    Runnable next = queue.remove(0);
                    try
                    {
                        next.run();
                    }
                    catch ( Throwable th )
                    {
                        //NLogger.error( QueueWorker.class, th, th);
                    }
                    
                    synchronized(RunnerQueueWorker.this) 
                    {
                        if( !queue.isEmpty() && !isInterrupted && !isPaused)
                        {
                            continue;
                        }
                        try 
                        {
                            // wait a short while for possible notify
                            while ( isPaused )
                            {
                                RunnerQueueWorker.this.wait(5 * 1000);
                            }
                        } 
                        catch(InterruptedException exp) 
                        {// ignore and take next from queue
                         // if its still full stopAndClear()
                        }
                        if( !queue.isEmpty() && !isInterrupted )
                        {
                            continue;
                        }
                        runnerThread = null;
                        break;
                    }
                }
            } 
            catch ( Throwable th )
            {
                runnerThread = null;
                //NLogger.error( QueueWorker.class, th, th );
            }
            // Safety check
            if ( !queue.isEmpty() )
            {// oups... somebody is left we need to restart..
                createRunner();
            }
        }
    }
}