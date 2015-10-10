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
package phex.common.bandwidth;

import java.io.IOException;


/**
 * A class that units a clamping bandwidth throttle with memory and a simple
 * current/avg. bandwidth tracker.
 * <p>
 * Bandwidth generally does not exceed set value within a one second period.
 * Excess bandwidth is partially available for the
 * next period, exceeded bandwidth is fully unavailable for the next period.
 */
public class BandwidthController
{
//    private static final Logger logger = LoggerFactory.getLogger( BandwidthController.class );
    
    private static final int WINDOWS_PER_SECONDS = 5;

    private static final int MILLIS_PER_WINDOW = 1000 / WINDOWS_PER_SECONDS;
    
    /**
     * The number of bytes each window has.
     */
    private int bytesPerWindow;

    /**
     * The number of bytes left in the current window
     */
    private int bytesRemaining;

    /**
     * The timestamp of the start of the current window.
     */
    private long lastWindowTime;

    /**
     * The maximal rate in bytes per second.
     */
    private volatile long throttlingRate;

    /**
     * The name of this BandwidthController.
     */
    private final String controllerName;

    /**
     * To measure bandwidth in different levels BandwidthControllers can be
     * chained together so that a throttling would have to pass all controllers
     * and tracking can be done in higher levels too.
     */
    private final BandwidthController nextContollerInChain;
    
    private TransferAverage shortTransferAvg;
    private TransferAverage longTransferAvg;

    /**
     * Create a new bandwidth controller through acquireController()
     * @param controllerName the name of this BandwidthController.
     * @param throttlingRate the used throttling rate in bytes per second.
     */
    public BandwidthController(String controllerName, long throttlingRate )
    {
        this( controllerName, throttlingRate, null );
    }
        
    /**
     * Create a new bandwidth controller through acquireController()
     * @param controllerName the name of this BandwidthController.
     * @param throttlingRate the used throttling rate in bytes per second.
     * @param parent a parent BandwidthController to chain controller.
     */
    public BandwidthController(String controllerName, long throttlingRate, BandwidthController parent )
    {
        this.controllerName = controllerName + " " 
            + Integer.toHexString( hashCode() );
        
        setThrottlingRate( throttlingRate );
        nextContollerInChain = parent;
        
        // init the bytes remaining on start to ensure correct stats on start.
        bytesRemaining = bytesPerWindow;
    }
    
    public synchronized void activateShortTransferAvg( int refreshRate, int period )
    {
        shortTransferAvg = new TransferAverage( refreshRate, period );
    }
    
    public synchronized void activateLongTransferAvg( int refreshRate, int period )
    {
        longTransferAvg = new TransferAverage( refreshRate, period );
    }
    
    public TransferAverage getShortTransferAvg()
    {
        return shortTransferAvg;
    }
    
    public TransferAverage getLongTransferAvg()
    {
        return longTransferAvg;
    }

    /**
     * Call to set the desired throttling rate.
     */
    public synchronized void setThrottlingRate(long bytesPerSecond)
    {
        if ( bytesPerSecond == throttlingRate )
        {
            return;
        }
        throttlingRate = bytesPerSecond;
        // ensure that bytes per window is at least 1
        bytesPerWindow = Math.max( (int) ((double) throttlingRate / (double) WINDOWS_PER_SECONDS), 1 ) ;
        
//        if ( logger.isDebugEnabled() )
//        {
//            logger.debug( "["+controllerName + "] Set throttling rate to " 
//                + bytesPerSecond + "bps (" + bytesPerWindow + " per window)");
//        }
        
        // keep the bytes remaining on the current window when bandwidth is dropping down..
        bytesRemaining = bytesRemaining < bytesPerWindow ? Math.min( bytesRemaining, bytesPerWindow ) : bytesRemaining;
    }
    
    /**
     * Returns the throttling rate in bytes per seconds
     * @return the throttling rate in bytes per seconds
     */
    public long getThrottlingRate()
    {
        return throttlingRate;
    }
    
    /**
     * Returns the max number of bytes available through this bandwidth controller
     * and its parents.
     * @return the max number of bytes available.
     * @throws IOException 
     */
    public synchronized int getAvailableByteCount( int maxToRequest, 
        boolean blockTillAvailable, boolean markBytesUsed ) throws IOException
    {
        updateWindow( blockTillAvailable );
        int bytesAllowed = Math.max( 0, Math.min( maxToRequest, bytesRemaining ) );
        // If there is another controller we are chained to, call it.
        if( nextContollerInChain != null )
        {
            bytesAllowed = nextContollerInChain.getAvailableByteCount(
                bytesAllowed, blockTillAvailable, false );
        }
        
        if ( markBytesUsed )
        {
            markBytesUsed( bytesAllowed );
        }
        
//        if ( bytesRemaining < 0 /*&& logger.isErrorEnabled() */)
//        {
////            logger.error(
////                "["+controllerName + "] Available byte count " + bytesAllowed 
////                + "bps - Remaining: " + bytesRemaining + ".");
//        }
////        else if ( logger.isDebugEnabled() )
////        {
////            logger.debug(
////                "["+controllerName + "] Available byte count " + bytesAllowed 
////                + "bps - Remaining: " + bytesRemaining + ".");
////        }
        
        return bytesAllowed;
    }
    
    /**
     * Marks bytes as used.
     * @param byteCount
     * @throws IOException 
     */
    public synchronized void markBytesUsed( int byteCount ) throws IOException
    {
        assert byteCount >= 0 : "Cant mark negative byteCount used: " + byteCount;
        updateWindow( false );
        bytesRemaining -= byteCount;
        if  ( bytesRemaining < 0 )
        {
            // let the remaining byte buffer lack max 15 seconds
            // behind in case it is over used.
            bytesRemaining = Math.max( bytesRemaining, 
                -15 * WINDOWS_PER_SECONDS * bytesPerWindow );
            updateWindow( true );
        }

//        if ( bytesRemaining < 0 && logger.isErrorEnabled() )
//        {
//            logger.error(
//                "["+controllerName + "] !Mark bytes used " + byteCount 
//                + " - remaining: " + bytesRemaining + ".");
//        }
//        else if ( logger.isDebugEnabled() )
//        {
//            logger.debug(
//                "["+controllerName + "] !Mark bytes used " + byteCount 
//                + " - remaining: " + bytesRemaining + ".");
//        }
        
        if ( shortTransferAvg != null )
        {
            shortTransferAvg.addValue( byteCount );
        }
        if ( longTransferAvg != null )
        {
            longTransferAvg.addValue( byteCount );
        }
        // If there is another controller we are chained to, call it.
        if( nextContollerInChain != null )
        {
            nextContollerInChain.markBytesUsed( byteCount );
        }
    }
    
    private void updateWindow( boolean blockTillAvailable )
    {
        boolean wasInterrupted = false;
        long elapsedWindowMillis;
        long now;
        int updateTries = 0;
        while ( true )
        {
            now = System.currentTimeMillis();
            elapsedWindowMillis = now - lastWindowTime;
            if (elapsedWindowMillis >= MILLIS_PER_WINDOW )
            {
                // last window used up too many bytes... 
                if ( bytesRemaining < 0 )
                {
                    bytesRemaining += bytesPerWindow;
                }
                else
                {
                    bytesRemaining = bytesPerWindow; 
                }                
                lastWindowTime = now;
//                if ( logger.isDebugEnabled( ) )
//                {
//                    logger.debug(  
//                        "["+controllerName + "] Update new Window " + bytesPerWindow 
//                        + " - Remaining: " + bytesRemaining + ".");
//                }
            }
            if ( !blockTillAvailable || bytesRemaining > 0 )
            {
                break;
            }
            updateTries ++;
            if ( updateTries > WINDOWS_PER_SECONDS * 2 )
            {
                break;
            }
            try
            {
                Thread.sleep( Math.max( 
                    MILLIS_PER_WINDOW - elapsedWindowMillis, 0 ) );
            }
            catch (InterruptedException e)
            {
                wasInterrupted = true;
                break;
            }
        }
        if ( wasInterrupted )
        {//reset interrupted
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns the name of this BandwidthController.
     * @return the name.
     */
    public String getName()
    {
        return controllerName;
    }

    /**
     * Returns a debug string of this BandwidthController.
     * @return a debug string.
     */
    public String toDebugString()
    {
        return "ThrottleController[Name:" + controllerName + 
            ",bytesPerWindow:" + bytesPerWindow + ",bytesRemaining:" + bytesRemaining 
            //+ ",Rate:" + getValue() + ",Avg:" + getAverageValue()
            ;
    }
}