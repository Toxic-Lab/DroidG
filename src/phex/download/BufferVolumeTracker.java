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
package phex.download;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class BufferVolumeTracker
{
    private final BufferVolumeTracker parent;
    private final Sync sync;
    private final DownloadDataWriter dataWriter;
    
    // TODO the maxBufferSize is coming from a setting, changes to the setting
    //      during runtime are not reflected in the used Sync.
    public BufferVolumeTracker( int maxBufferSize, DownloadDataWriter dataWriter )
    {
        this( null, maxBufferSize, dataWriter  );
    }
    
    public BufferVolumeTracker( BufferVolumeTracker parent, int maxBufferSize, DownloadDataWriter dataWriter )
    {
        this.parent = parent;
        this.dataWriter = dataWriter;
        sync = new Sync( maxBufferSize );
    }
    
    public int getUsedBufferSize()
    {
        return sync.getUsedStates();
    }
    public int getFreeBufferSize()
    {
        return sync.getFreeStates();
    }
    
    public void addBufferedSize( int amount )
    {
        if ( parent != null )
        {
            parent.addBufferedSize(amount);
        }
        sync.acquireShared(amount);
    }
    
    public void reduceBufferedSize( int amount )
    {
        if ( parent != null )
        {
            parent.reduceBufferedSize(amount);
        }
        sync.releaseShared(amount);
    }
    
    class Sync extends AbstractQueuedSynchronizer 
    {
        private int currentMax;
        
        Sync( int max )
        {
            currentMax = max;
            setState( max );
        }
        
        public int getUsedStates()
        {
            return currentMax - getState();
        }
        
        public int getFreeStates()
        {
            return getState();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        protected final int tryAcquireShared( int acquires ) 
        {
            for (;;) 
            {
                int available = getState();
                int remaining = available - acquires;
                if ( remaining < 0 )
                {
                    dataWriter.triggerWriteCycle();
                    return remaining;
                }
                if ( compareAndSetState(available, remaining) )
                {
                    return remaining;
                }
            }
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        protected final boolean tryReleaseShared(int releases)
        {
            for (;;) {
                int p = getState();
                if (compareAndSetState(p, p + releases)) 
                    return true;
            }
        }
    }
}