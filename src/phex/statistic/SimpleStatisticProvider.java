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

import phex.common.*;

public class SimpleStatisticProvider implements StatisticProvider
{
    protected LongObj value;

    public SimpleStatisticProvider()
    {
        value = new LongObj( 0 );
    }

    /**
     * Increments the statistic by inc.
     * @param inc the value to increment the statistic by.
     */
    public void increment( int inc )
    {
        value.inc( inc );
    }

    /**
     * Returns the current value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the current value or null.
     */
    public Object getValue()
    {
        return value;
    }
    
    /**
     * Sets the current value this provider presents.
     */
    public void setValue(long longValue)
    {
        value.setValue( longValue );
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
        return this.value.toString();
    }
}