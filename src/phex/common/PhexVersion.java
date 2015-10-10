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
package phex.common;
 
/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * This file is auto generated through the maven build process, it is only 
 * recommended to be modified if you know what you are doing.
 *!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

public class PhexVersion
{
    // The Phex version number MUST always consists of 3 digits separated by a dot.
    // Even if the last or last two digest are a zero. This is important to attach
    // the build number always as the fourth position of the version. The build
    // number is not defined here for update reasons.
    // Version number definition: major.minor.micro(.build)
    private static final String VERSION = "3.4.2";
    private static final String BUILD = "116";
    private static final String FULL_VERSION = "3.4.2.116";
    
    private static final int MAJOR_VERSION = Integer.parseInt( 
        VERSION.substring( 0, VERSION.indexOf('.') ) );
    private static final int MINOR_VERSION = Integer.parseInt( 
        VERSION.substring( VERSION.indexOf('.')+1, 
        VERSION.indexOf('.', VERSION.indexOf('.')+1) ) );
    
    
    public static String getFullVersion()
    {
        return FULL_VERSION;
    }
    
    public static String getVersion()
    {
        return VERSION;
    }
    
    /**
     * Returns the major version number of the version.
     * @return the major version.
     */
    public static int getMajorVersion()
    {
        return MAJOR_VERSION;
    }
    
    /**
     * Returns the major version number of the version.
     * @return the major version.
     */
    public static int getMinorVersion()
    {
        return MINOR_VERSION;
    }
    
    public static String getBuild()
    {
        return BUILD;
    }
}