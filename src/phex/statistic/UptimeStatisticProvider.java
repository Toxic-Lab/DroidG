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

import phex.common.LongObj;
import phex.common.format.TimeFormatUtils;
import phex.prefs.core.StatisticPrefs;

public class UptimeStatisticProvider implements StatisticProvider
{
    private long startTime;
    private LongObj valueObj;
    private LongObj avgObj;
    private LongObj maxObj;


    public UptimeStatisticProvider()
    {
        valueObj = new LongObj();
        avgObj = new LongObj( );
        maxObj = new LongObj( StatisticPrefs.MaximalUptime.get().longValue() );
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
    public Object getValue()
    {
        long value = System.currentTimeMillis() - startTime;
        valueObj.setValue( value );
        return valueObj;
    }

    /**
     * Returns the average value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the average value or null.
     */
    public Object getAverageValue()
    {
        LongObj currentUptimeObj = ( LongObj )getValue();
        long currentUptime = currentUptimeObj.longValue();
        // current uptime might be negative...
        currentUptime = Math.max( currentUptime, 0 );
        long avgUptime = ( currentUptime + 
            StatisticPrefs.MovingTotalUptime.get().longValue() )
            / ( StatisticPrefs.MovingTotalUptimeCount.get().intValue() + 1 );
        avgObj.setValue( avgUptime );
        return avgObj;
    }

    /**
     * Returns the max value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the max value or null.
     */
    public Object getMaxValue()
    {
        long uptime = System.currentTimeMillis() - startTime;
        if ( uptime > maxObj.getValue() )
        {
            maxObj.setValue( uptime );
        }
        return maxObj;
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
            ((LongObj)value).longValue() / 1000 );
    }

    public void saveUptimeStats()
    {
        LongObj obj = ( LongObj )getMaxValue();
        StatisticPrefs.MaximalUptime.set( Long.valueOf( obj.getValue() ) );

        long mtu = StatisticPrefs.MovingTotalUptime.get().intValue();
        int mtuCount = StatisticPrefs.MovingTotalUptimeCount.get().intValue();
        if ( mtuCount >= 25 )
        {
            // substract one average uptime...
            mtu -= ( mtu / mtuCount );
            StatisticPrefs.MovingTotalUptime.set( Long.valueOf( mtu ) );
            mtuCount--;
            StatisticPrefs.MovingTotalUptimeCount.set( Integer.valueOf( mtuCount ) );
        }

        obj = ( LongObj )getValue();
        // sometimes time might be negative since clocks can go backwards
        // due to DST adjustments. In this case ignore the uptime value.
        if ( obj.longValue() > 0 )
        {
            mtu += obj.longValue();
            StatisticPrefs.MovingTotalUptime.set( Long.valueOf( mtu ) );
            mtuCount++;
            StatisticPrefs.MovingTotalUptimeCount.set( Integer.valueOf( mtuCount ) );
        }
    }
}