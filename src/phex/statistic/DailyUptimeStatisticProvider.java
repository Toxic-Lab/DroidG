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
package phex.statistic;


import phex.common.format.TimeFormatUtils;
import phex.prefs.core.StatisticPrefs;

/**
 * The class provides the avg. daily uptime calculation required for Limewires
 * GGEP Pong extension.
 * To follow the same calculation Limewire uses the calculation is mostly based 
 * of Limewire code. 
 */
public class DailyUptimeStatisticProvider implements StatisticProvider
{
    /** The number of seconds in a day. */
    private static final int SECONDS_PER_DAY=24*60*60;
    /** Controls how much past is remembered in calculateFractionalUptime.
     *  Default: 7 days, which doesn't quite mean what you might think
     *  see calculateFractionalUptime. */
    private static final int WINDOW_MILLISECONDS=7*SECONDS_PER_DAY*1000;
    
    /** The time this was initialized. */
    private long startTime;
    
    public DailyUptimeStatisticProvider()
    {
        startUptimeMeasurement();
    }
    
    private void startUptimeMeasurement()
    {
        startTime = System.currentTimeMillis();
    }
    
    
    /**
     * Returns the current value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the current value or null.
     */
    @SuppressWarnings("boxing")
    public Object getValue()
    {
        return calculateDailyUptime();
    }

    /**
     * Returns the avarage value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the avarage value or null.
     */
    public Object getAverageValue()
    {
        return null;
    }

    /**
     * Returns the max value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the max value or null.
     */
    public Object getMaxValue()
    {
        return null;
    }
    
    /**
     * Returns the presentation string that should be displayed for the corresponding
     * value.
     * @param value the value returned from getValue(), getAverageValue() or
     * getMaxValue()
     * @return the statistic presentation string.
     */
    public String toStatisticString( Object value )
    {
        return TimeFormatUtils.formatSignificantElapsedTime(
            ((Integer)value).intValue() );
    }


    /**
     * Calculates the average number of seconds this host runs per day, i.e.,
     * calculateFractionRunning*24*60*60.
     * @return uptime in seconds/day.
     * @see #calculateFractionalUptime()
     */
    private int calculateDailyUptime()
    {
        //System.out.println(calculateFractionalUptime()*(float)SECONDS_PER_DAY);
        return (int)(calculateFractionalUptime() * SECONDS_PER_DAY);
    }

    /** 
     * Calculates the fraction of time this is running, a unitless quantity
     * between zero and 1.  Implemented using an exponential moving average
     * (EMA) that discounts the past.  Does not update the FRACTION_RUNNING
     * property; that should only be done once, on shutdown
     * @see #calculateDailyUptime()  
     */
    private float calculateFractionalUptime()
    { 
        //Let
        //     P = the last value returned by calculateFractionRunning stored
        //         in the SettingsMangager
        //     W = the window size in seconds.  (See note below.)
        //     t = the uptime for this session.  It is assumed that
        //         t<W; otherwise set t=W. 
        //     T = the elapsed time since the end of the previous session, i.e.,
        //         since P' was updated.  Note that t<T.  It is assumed that
        //         T<W; otherwise set T=W.
        //
        //The new fraction P' of the time this is running can be calculated as
        //a weighted average of the current session (t/T) and the past (P):
        //     P' = (T/W)*t/T + (1-T/W)*P
        //        =  t/W      + (W-T)/W*P
        //
        //W's name is misleading, because more than W seconds worth of history
        //are factored into the calculation.  More specifically, a session i
        //days ago contributes 1/W * ((W-1)/W)^i part of the average.  The
        //default value of W (7 days) means, for example, that the past 9 days
        //account for 75% of the calculation.
        
        final float W=WINDOW_MILLISECONDS;
        float T = Math.min(W, System.currentTimeMillis() -
            StatisticPrefs.LastShutdownTime.get().longValue() );
        float t = Math.min(W, System.currentTimeMillis() - startTime );
        float P = StatisticPrefs.FractionalUptime.get().floatValue();
        
        //Occasionally clocks can go backwards, e.g., if user adjusts them or
        //from daylight savings time.  In this case, ignore the current session
        //and just return P.
        if (t<0 || T<0 || t>T)
            return P;
        return t/W + (W-T)/W*P;
    }

    public void shutdown()
    {
        //Order matters, as calculateFractionalUptime() depends on the
        //LAST_SHUTDOWN_TIME property.
        StatisticPrefs.FractionalUptime.set( Float.valueOf( calculateFractionalUptime() ) );
        StatisticPrefs.LastShutdownTime.set( Long.valueOf( System.currentTimeMillis() ) );
    }
}
