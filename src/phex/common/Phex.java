/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2009 Phex Development Group
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

import java.io.File;

import phex.common.file.FileManager;
import phex.event.PhexEventService;
import phex.event.PhexEventServiceImpl;
import phex.prefs.core.PrivateNetworkConstants;
import phex.utils.SystemProperties;

/**
 *
 */
public class Phex
{
    private static FileManager fileManager;
    private static PhexEventService eventService;
    
    public static void initialize()
    {
        //LogUtils.initializeLogging();
        
        eventService = new PhexEventServiceImpl();
        
        fileManager = new FileManager();        
    }
    
    /**
     * Returns the {@link File} representing the complete path to the configuration file
     * with the given configFileName.
     * @param configFileName the name of the config file to determine the complete
     *        path for.
     * @return the File representing the complete path to the configuration file
     *         with the given configFileName.
     */
    public static File getPhexConfigFile( String configFileName )
    {
        return new File( SystemProperties.getPhexConfigRoot(), configFileName );
    }

    /**
     * Returns the PhexEventService that provides access to
     * the Phex event bus.
     * @return the PhexEventService
     */
    public static PhexEventService getEventService()
    {
        return eventService;
    }

    public static FileManager getFileManager()
    {
        return fileManager;
    }
    
    /**
     * Returns the Phex full vendor string including the version.
     * @return full vendor string including version.
     */
    public static String getFullPhexVendor()
    {
        return "Phex " + PrivateNetworkConstants.PRIVATE_BUILD_ID + PhexVersion.getFullVersion();
    }
    
    /**
     * Returns the Phex vendor name.
     * @return vendor name.
     */
    public static String getPhexVendorName()
    {
        return "Phex";
    }
    
    public static boolean isPhexVendor( String vendor )
    {
        if ( vendor.length() > 4 )
        {
            return vendor.startsWith( "Phex " );
        }
        else
        {
            return vendor.startsWith( "Phex" );
        }
    }
}