package phex.bootstrap;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class BootstrapHost
{
    private static final long RECONNECT_PENALTY = 1000 * 60 * 60; // 60 minutes
    
    protected final boolean isPhexCache;
    
    protected long lastRequestTime;
    
    /**
     * Maintains the no of times this host has consecutively failed.   
     */
    private AtomicInteger failedInRowCount;
    
    public BootstrapHost( boolean isPhexCache )
    {
        failedInRowCount = new AtomicInteger( 0 );
        this.isPhexCache = isPhexCache;
    }
    
    /**
     * @return the isPhexCache
     */
    public boolean isPhexCache()
    {
        return isPhexCache;
    }

    /**
     * @return the lastRequestTime
     */
    public long getLastRequestTime()
    {
        return lastRequestTime;
    }

    /**
     * @param lastRequestTime the lastRequestTime to set
     */
    public void setLastRequestTime(long lastRequestTime)
    {
        this.lastRequestTime = lastRequestTime;
    }

    /**
     * @return the failedInRowCount
     */
    public int getFailedInRowCount()
    {
        return failedInRowCount.get();
    }
    
    public void incFailedInRowCount()
    {
        failedInRowCount.incrementAndGet();
    }
    
    public void resetFailedInRowCount()
    {
        failedInRowCount.set( 0 );
    }
    
    /**
     * WARNING: This method is not thread save. Only
     * use to initialize value. Not to set from a 
     * dependent/expected state of failedInRowCount.
     * @param val the new value
     */
    public void setFailedInRowCount( int val )
    {
        failedInRowCount.set( val );
    }
    
    public long getEarliestReConnectTime()
    {
        return getReconnectPenalty() * (getFailedInRowCount()+1) + lastRequestTime; 
    }
    
    /**
     * The minimum time that needs to be passed before this
     * BootstrapHost should be used again.  
     * @return the min. time between reconnects in millis. 
     */
    public long getReconnectPenalty()
    {
        return RECONNECT_PENALTY;
    }
}