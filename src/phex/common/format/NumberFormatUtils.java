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

public final class NumberFormatUtils
{
    public static final DoubleToString doubleToStringFormat = new DoubleToString();
    
    /**
     * Represents 1 Kilo Byte ( 1024 ).
     */
    public static final long ONE_KB = 1024L;
    
    /**
     * Represents 1 Mega Byte ( 1024^2 ).
     */
    public static final long ONE_MB = ONE_KB * 1024L;
    
    /**
     * Represents 1 Giga Byte ( 1024^3 ).
     */
    public static final long ONE_GB = ONE_MB * 1024L;
    
    /**
     * Represents 1 Tera Byte ( 1024^4 ).
     */
    public static final long ONE_TB = ONE_GB * 1024L;

    // dont allow instances
    private NumberFormatUtils() {}

    public static String formatDecimal( double value, int precision )
    {
        if (Double.isNaN(value) || Double.isInfinite(value)) 
        {
            return "\u221E"; // "oo"
        }
        
        if ( precision == 0 )
        {// when no precision is needed integerFormat is doing a slightly faster job
            return formatNumber( (long)value );
        }
        
        StringBuffer buf = new StringBuffer();
        doubleToStringFormat.appendFormatted( 
            buf, value, precision,
            Localizer.getDecimalFormatSymbols().getDecimalSeparator(),
            Localizer.getDecimalFormatSymbols().getGroupingSeparator(), 3, // = no grouping
            Localizer.getDecimalFormatSymbols().getMinusSign(), '\uFFFF' ); // uFFFF = no negative suffix
        return buf.toString();
    }
    
    /**
     * Formates a int or long number.
     * @param size
     * @return
     */
    public static String formatNumber( long size )
    {
        return Localizer.getIntegerNumberFormat().format( size );
    }
    
    /**
     * Formats the size with the unit bytes.
     * @param size
     * @return
     */
    public static String formatFullByteSize( long size )
    {
        return formatNumber( size ) + " " + Localizer.getString( "BytesToken" );
    }

    /**
     * Formats the the size as a most significant number of bytes.
     */
    public static String formatSignificantByteSize( double size )
    {
        String text;
        double divider;
        int precision;
        if (size < 10 * NumberFormatUtils.ONE_KB) // < 10K
        {
            text = Localizer.getString( "BytesToken" );
            divider = 1.0;
            precision = 0;
        }
        else if (size < 10 * NumberFormatUtils.ONE_MB) // < 10M
        {
            text = Localizer.getString( "KBToken" );
            divider = NumberFormatUtils.ONE_KB;
            precision = 1;
        }
        else if (size < 10 * NumberFormatUtils.ONE_GB) // < 10G
        {
            text = Localizer.getString( "MBToken" );
            divider = NumberFormatUtils.ONE_MB;
            precision = 1;
        }
        else if (size < 10 * NumberFormatUtils.ONE_TB) // < 10T
        {
            text = Localizer.getString( "GBToken" );
            divider = NumberFormatUtils.ONE_GB;
            precision = 2;
        }
        else
        {
            text = Localizer.getString( "TBToken" );
            divider = NumberFormatUtils.ONE_TB;
            precision = 3;
        }
        double d = size / divider;
        String valStr = formatDecimal(d, precision);
        return valStr + " " + text;
    }
    
    public static String formatSignificantByteSize2( double size )
    {
        String text;
        double divider;
        int precision;
        if (size < 10 * NumberFormatUtils.ONE_KB) // < 10K
        {
            text = "bytes";
            divider = 1.0;
            precision = 0;
        }
        else if (size < 10 * NumberFormatUtils.ONE_MB) // < 10M
        {
            text = "KB";
            divider = NumberFormatUtils.ONE_KB;
            precision = 1;
        }
        else if (size < 10 * NumberFormatUtils.ONE_GB) // < 10G
        {
            text = "MB";
            divider = NumberFormatUtils.ONE_MB;
            precision = 1;
        }
        else if (size < 10 * NumberFormatUtils.ONE_TB) // < 10T
        {
            text = "GB";
            divider = NumberFormatUtils.ONE_GB;
            precision = 2;
        }
        else
        {
            text = "TB";
            divider = NumberFormatUtils.ONE_TB;
            precision = 3;
        }
        double d = size / divider;
        String valStr = formatDecimal(d, precision);
        return valStr + " " + text;
    }
    

    public static String formatSignificantByteSize( Number number )
    {
        return formatSignificantByteSize( number.doubleValue() );
    }
}