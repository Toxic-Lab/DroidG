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

import phex.common.bandwidth.BandwidthController;
import phex.common.format.NumberFormatUtils;
import phex.utils.Localizer;

public class TransferAverageStatisticProvider implements StatisticProvider
{
    private BandwidthController bwController;
    
    public TransferAverageStatisticProvider( BandwidthController bwController )
    {
        this.bwController = bwController;
    }
    
    /**
     * Returns the current value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the current value or null.
     */
    public Object getValue()
    {
        return Long.valueOf( bwController.getShortTransferAvg().getAverage() );
    }

    /**
     * Returns the avarage value this provider presents.
     * The return value can be null in case no value is provided.
     * @return the avarage value or null.
     */
    public Object getAverageValue()
    {
        return Long.valueOf( bwController.getLongTransferAvg().getAverage() );
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
        return NumberFormatUtils.formatSignificantByteSize( 
            (Number)value ) + Localizer.getString( "PerSec" );
    }
}