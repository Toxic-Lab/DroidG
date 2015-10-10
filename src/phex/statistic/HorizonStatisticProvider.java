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

import phex.common.HorizonTracker;

/**
 *
 */
public class HorizonStatisticProvider implements StatisticProvider
{
    public enum Type 
    { 
        HOST_COUNT, FILE_COUNT, FILE_SIZE 
    }
    
    private final HorizonTracker horizonTracker;
    private final Type type;
    
    public HorizonStatisticProvider( Type type, HorizonTracker horizonTracker )
    {
        this.horizonTracker = horizonTracker;
        this.type = type;
    }
    
    /**
     * @see phex.statistic.StatisticProvider#getValue()
     */
    @SuppressWarnings("boxing")
    public Object getValue()
    {
        switch ( type )
        {
        case HOST_COUNT:
            return horizonTracker.getTotalHostCount();
        case FILE_COUNT:
            return horizonTracker.getTotalFileCount();
        case FILE_SIZE:
            return horizonTracker.getTotalFileSize();
        }
        return null;
    }

    /**
     * @see phex.statistic.StatisticProvider#getAverageValue()
     */
    public Object getAverageValue()
    {
        switch ( type )
        {
        case FILE_SIZE:
            long count = horizonTracker.getTotalFileCount();
            if ( count != 0 )
            {
                int val = (int)((double)horizonTracker.getTotalFileSize() / (double)count);
                return Integer.valueOf( val );
            }
            break;
        case FILE_COUNT:
            int hostCount = horizonTracker.getTotalHostCount();
            if ( hostCount != 0 )
            {
                int val = (int)((double)horizonTracker.getTotalFileCount() / (double)hostCount);
                return Integer.valueOf( val );
            }
            break;            
        }
        return null;
    }

    /**
     * @see phex.statistic.StatisticProvider#getMaxValue()
     */
    public Object getMaxValue()
    {
        return null;
    }

    /**
     * @see phex.statistic.StatisticProvider#toStatisticString(java.lang.Object)
     */
    public String toStatisticString(Object value)
    {
        return value.toString();
    }
}