/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2008 Phex Development Group
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
package phex.query;


public class DefaultSearchProgress implements SearchProgress
{
    //private static final Logger logger = LoggerFactory.getLogger( DefaultSearchProgress.class );
    /**
     * The default value defining the number of millis a query is running
     * before it times out. ( 5 minutes ) 
     */
    public static final int DEFAULT_QUERY_TIMEOUT = 5 * 60 * 1000;
    
    /**
     * The number of results to get if we are starting a dynamic query
     * as a ultrapeer.
     */
    public static final int DESIRED_RESULTS = 200;
    
    /**
     * The number of results to get if we are starting a dynamic query
     * with hash.
     */
    public static final int DESIRED_HASH_RESULTS = 20;
    
    
    private long startTime;
    
    private long lifetime;
    
    private int desiredResultsCount;
    
    protected volatile int receivedResultsCount;
        
    protected DefaultSearchProgress( long lifetime, int desiredResultsCount )
    {
        this.lifetime = lifetime;
        this.desiredResultsCount = desiredResultsCount;
        receivedResultsCount = 0;
        startTime = 0;
    }
    
    public int getProgress()
    {
        if ( isSearchFinished() )
        {
            return 100;
        }
        if ( startTime <= 0 )
        {
            return 0;
        }
        
        int resultsProgress = 0;
        if ( getDesiredResultsCount() > 0 )
        {
            resultsProgress = (int)((double)getReceivedResultsCount() / (double)getDesiredResultsCount() * 100d );
        }
                
        int timeProgress = 0;
        if ( lifetime > 0 )
        {
            long currentTime = System.currentTimeMillis();
            // time progress...
            long timeLeft = startTime + lifetime - currentTime;
            timeProgress = (int)(100D - Math.max( 0D, timeLeft ) / lifetime * 100D );
        }
        // return the max from all
        int totalProgress = Math.min( Math.max( resultsProgress, timeProgress ), 100);
//        logger.debug( "Search progress: r{}, t{}, ={}", 
//            new Object[] { Integer.valueOf( resultsProgress ), Integer.valueOf( timeProgress ),
//            Integer.valueOf( totalProgress ) } );
        return totalProgress;
    }
    
    public void searchStarted()
    {
        startTime = System.currentTimeMillis();
    }
    
    public int getDesiredResultsCount()
    {
        return desiredResultsCount;
    }
    
    public int getReceivedResultsCount()
    {
        return receivedResultsCount;
    }
    
    public void incReceivedResultsCount( int inc )
    {
        receivedResultsCount += inc;
    }
    
    public boolean isSearchFinished()
    {
        if ( getDesiredResultsCount() > 0 && 
            getReceivedResultsCount() >= getDesiredResultsCount() )
        {
            return true;
        }
        
        if ( lifetime > 0 )
        {
            // check if the query has timed out
            long currentTime = System.currentTimeMillis();
            if ( currentTime > startTime + lifetime )
            {
                return true;
            }
        }
        
        return false;
    }
    
    public static DefaultSearchProgress createForForMeProgress( boolean isUrnQuery )
    {
        return new DefaultSearchProgress( DEFAULT_QUERY_TIMEOUT, 
            isUrnQuery ? DESIRED_HASH_RESULTS : DESIRED_RESULTS );
    }
}