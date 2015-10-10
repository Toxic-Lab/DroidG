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


/**
 * The StatisticProvider interface. When providing statistics implement this
 * interface and register the provider at the StatisticsManager.
 * <p>
 * Usually all the get...() methods return Objects of type Number or String. But
 * to be flexible they are able to return any kind of Objects the provider feels
 * appropriate.
 */
// TODO2 Add ratio column to provide % data, like message percent or qrt hit ratio
public interface StatisticProvider
{
    /**
     * Returns the current value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the current value or null.
     */
    public Object getValue();

    /**
     * Returns the avarage value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the avarage value or null.
     */
    public Object getAverageValue();

    /**
     * Returns the max value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the max value or null.
     */
    public Object getMaxValue();

    /**
     * Returns the presentation string that should be displayed for the corresponding
     * value.
     * @param value the value returned from getValue(), getAverageValue() or
     * getMaxValue()
     * @return the statistic presentation string.
     */
    public String toStatisticString( Object value );

}