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
 *  Created on 14.08.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.common.format;

import phex.utils.Localizer;


public class HostSpeedFormatUtils
{
    public static final int SPEED_MODEM = 56;
    public static final int SPEED_ISDN = 64;
    public static final int SPEED_DualISDN = 128;
    public static final int SPEED_CableDSL1 = 256;
    public static final int SPEED_CableDSL2 = 512;
    public static final int SPEED_CableDSL3 = 768;
    public static final int SPEED_T1 = 1544;
    public static final int SPEED_10LAN = 10000;
    public static final int SPEED_T3 = 44736;
    public static final int SPEED_100LAN = 100000;
    private static final String SPEED_STRING_MODEM = Localizer.getString("HostSpeed_Modem");
    private static final String SPEED_STRING_DSL = Localizer.getString("HostSpeed_DSLCable");
    private static final String SPEED_STRING_T1 = Localizer.getString("HostSpeed_T1");
    private static final String SPEED_STRING_T3 = Localizer.getString("HostSpeed_T3");
    private static final String SPEED_KBITSSEC_POSTFIX = Localizer.getString("HostSpeed_KPerSec_Postfix");
    
    private static final String SPEED_STRING_MODEM2 = "Modem";
    private static final String SPEED_STRING_DSL2 = "DSL/Cable";
    private static final String SPEED_STRING_T12 = "T1";
    private static final String SPEED_STRING_T32 = "T3";
    private static final String SPEED_KBITSSEC_POSTFIX2 = "KB/s";
    
    
    /**
     * Format the host speed in kilo bits / sec
     * @param speedVal in kilo bits / sec
     * @return the string representation
     */
    public static String formatHostSpeed( long speedVal )
    {
        StringBuffer buf = new StringBuffer( 22 );
        buf.append( NumberFormatUtils.formatDecimal( speedVal, 0 ) );
        buf.append( SPEED_KBITSSEC_POSTFIX );
        buf.append( " (" );
        if ( speedVal <= SPEED_MODEM )
        {
            buf.append( SPEED_STRING_MODEM );
        }
        else if (speedVal <= SPEED_CableDSL3 )
        {
            buf.append( SPEED_STRING_DSL );
        }
        else if (speedVal <= SPEED_T1 )
        {
            buf.append( SPEED_STRING_T1 );
        }
        else //if (speedVal <= Integer.MAX_VALUE)
        {
            buf.append( SPEED_STRING_T3 );
        }
        buf.append( ")" );
        return buf.toString();
    }
    
    public static String formatHostSpeed2( long speedVal )
    {
        StringBuffer buf = new StringBuffer( 22 );
        buf.append( NumberFormatUtils.formatDecimal( speedVal, 0 ) );
        buf.append( SPEED_KBITSSEC_POSTFIX2 );
        buf.append( " (" );
        if ( speedVal <= SPEED_MODEM )
        {
            buf.append( SPEED_STRING_MODEM2 );
        }
        else if (speedVal <= SPEED_CableDSL3 )
        {
            buf.append( SPEED_STRING_DSL2 );
        }
        else if (speedVal <= SPEED_T1 )
        {
            buf.append( SPEED_STRING_T12 );
        }
        else //if (speedVal <= Integer.MAX_VALUE)
        {
            buf.append( SPEED_STRING_T32 );
        }
        buf.append( ")" );
        return buf.toString();
    }
    
}
