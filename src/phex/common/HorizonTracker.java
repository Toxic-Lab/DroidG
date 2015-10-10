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
 *  --- CVS Information ---
 *  $Id$
 */
package phex.common;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import phex.common.address.DestAddress;
import phex.host.Host;
import phex.msg.InvalidMessageException;
import phex.msg.PongMsg;
import phex.msghandling.MessageSubscriber;

/**
 * The class tracks the estimated size of the horizon in host count,
 * file count and file size. 
 * Tracking is done by keeping all seen Pongs from hosts up to a 
 * maximal count. After a certain time the collection is dropped and
 * recounting starts. The old values are lazy updated on each recount cycle.
 * The numbers are only a very rough estimation of the actual numbers.
 */
public class HorizonTracker implements MessageSubscriber<PongMsg>
{   
    /**
     * The maximal number of PONGS to track
     */
    private static final int MAX_PONG_COUNT = 10000;
    
    /**
     * Indicates if the values of the last or current count should 
     * be used.
     */
    private boolean useLastCountValues;
    
    /**
     * A set to keep track of the already counted addresses. 
     */
    private Set<DestAddress> trackedAddresses;
    
    private int currentHostCount;
    private long currentFileCount;
    private long currentFileSize;
    
    private int lastHostCount;
    private long lastFileCount;
    private long lastFileSize;
    
    /**
     * Singleton to make sure there is only one tracker!
     */
    public HorizonTracker()
    {
        Environment.getInstance().scheduleTimerTask( 
                new TrackerRefreshTimer(), TrackerRefreshTimer.TIMER_PERIOD,
                TrackerRefreshTimer.TIMER_PERIOD );
        
        useLastCountValues = false;
        trackedAddresses = new HashSet<DestAddress>();
    }
    
    /**
     * @return Returns the lastFileCount.
     */
    public long getTotalFileCount()
    {
        return useLastCountValues ? lastFileCount : currentFileCount;
    }

    /**
     * @return Returns the lastFileSize.
     */
    public long getTotalFileSize()
    {
        return useLastCountValues ? lastFileSize : currentFileSize;
    }
    
    /**
     * @return Returns the lastHostCount.
     */
    public int getTotalHostCount()
    {
        return useLastCountValues ? lastHostCount : currentHostCount;
    }
    
    public synchronized void onMessage(PongMsg message, Host sourceHost)
        throws InvalidMessageException
    {
        if ( trackedAddresses.size() >= MAX_PONG_COUNT )
        {
            return;
        }

        boolean isAdded = trackedAddresses.add( message.getPongAddress() );
        if ( isAdded )
        { 
            currentFileCount += message.getFileCount();
            currentFileSize += message.getFileSizeInKB();
            currentHostCount ++;
        }
    }
    
    private synchronized void refreshTrackerStats()
    {
        lastHostCount = currentHostCount;
        lastFileCount = currentFileCount;
        lastFileSize = currentFileSize;
        
        useLastCountValues = true;
        
        currentFileCount = 0;
        currentFileSize = 0;
        currentHostCount = 0;
        
        trackedAddresses.clear();
    }
    
    private class TrackerRefreshTimer extends TimerTask
    {
        /**
         * The time after which the horizon calculation is updated.
         */
        private static final long TIMER_PERIOD = 15*60*1000;
        
        /**
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run()
        {
            refreshTrackerStats();
        }
    }
 }
