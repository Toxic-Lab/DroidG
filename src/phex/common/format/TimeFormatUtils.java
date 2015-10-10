/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Phex Development Group
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
 *  Created on 29.09.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.common.format;

import phex.utils.Localizer;

public final class TimeFormatUtils
{
    // dont allow instances
    private TimeFormatUtils() {}

    /**
     * Print only the most significant portion of the time. This is
     * the two most significant units of time. Form will be something
     * like "3h 26m" indicating 3 hours 26 minutes and some insignificant
     * number of seconds.
     */
    public static String formatSignificantElapsedTime( long seconds )
    {
        final long days = seconds / 86400;
        if ( days > 0 ) // Display days and hours
        {
            Object[] args = new Object[]
            {
                Long.valueOf( days ),
                Integer.valueOf( (int)((seconds / 3600) % 24) ) // hours
            };
            return Localizer.getFormatedString( "TimeFormatDH", args );
        }
    
        final int hours = (int)((seconds / 3600) % 24);
        if( hours > 0 ) // Display hours and minutes
        {
            Object[] args = new Object[]
            {
                Integer.valueOf( hours ),
                Integer.valueOf( (int)((seconds / 60) % 60) ) // minutes
            };
            return Localizer.getFormatedString( "TimeFormatHM", args );
        }
    
        final int minutes =  (int)((seconds / 60) % 60);
        if( minutes > 0 ) // Display minutes and seconds
        {
            Object[] args = new Object[]
            {
                Integer.valueOf( minutes ),
                Integer.valueOf( (int)(seconds % 60) ) // seconds
            };
            return Localizer.getFormatedString( "TimeFormatMS", args );
        }
    
        final int secs = (int)(seconds % 60);
        Object[] args = new Object[]
        {
            Integer.valueOf( secs ),
        };
        return Localizer.getFormatedString( "TimeFormatS", args );
    }

    /**
     * Converts the given seconds to a time format with following format:
     * days:hours:minutes:seconds. When days &lt; 0 the format will be 
     * hours:minutes:seconds. When hours &lt; 0 the format will be minutes:seconds.
     * Values &lt; 10 will be padded with a 0. 
     */
    public static String convertSecondsToTime( int seconds )
    {
        StringBuffer buffer = new StringBuffer();
        int days = seconds / 86400;
        int hours = (seconds / 3600) % 24;
        int minutes = (seconds / 60) % 60;
        int secs = seconds % 60;
        
        if ( days > 0 ) // Display days and hours
        {
            buffer.append( Integer.toString( days ) );
            buffer.append( ":" );
            if ( hours < 10 )
            {
                buffer.append( "0" );
            }
        }
        if ( days > 0 || hours > 0 )
        {
            buffer.append( Integer.toString( hours ) );
            buffer.append( ":" );
            if ( minutes < 10 )
            {
                buffer.append( "0" );
            }
        }
        
        buffer.append( Integer.toString( minutes ) );
        buffer.append( ":" );
        if ( secs < 10 )
        {
            buffer.append( "0" );
        }
        buffer.append( Integer.toString( secs ) );
        return buffer.toString();
    }
    
    public static String formatSignificantElapsedTime2( long seconds )
    {
        final long days = seconds / 86400;
        if ( days > 0 ) // Display days and hours
        {
            Object[] args = new Object[]
            {
                Long.valueOf( days ),
                Integer.valueOf( (int)((seconds / 3600) % 24) ) // hours
            };
            return String.format( "%dd %dh", args );
        }
    
        final int hours = (int)((seconds / 3600) % 24);
        if( hours > 0 ) // Display hours and minutes
        {
            Object[] args = new Object[]
            {
                Integer.valueOf( hours ),
                Integer.valueOf( (int)((seconds / 60) % 60) ) // minutes
            };
            return String.format( "%dh %dm", args );
        }
    
        final int minutes =  (int)((seconds / 60) % 60);
        if( minutes > 0 ) // Display minutes and seconds
        {
            Object[] args = new Object[]
            {
                Integer.valueOf( minutes ),
                Integer.valueOf( (int)(seconds % 60) ) // seconds
            };
            return String.format( "%dm %ds", args );
        }
    
        final int secs = (int)(seconds % 60);
        Object[] args = new Object[]
        {
            Integer.valueOf( secs ),
        };
        return String.format( "%ds", args );
    }
}
