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
package phex.utils;

import java.io.File;

public class SystemProperties
{
    public static final String PHEX_CONFIG_PATH_SYSPROP = "phex.config.path";
    public static final String PHEX_DOWNLOAD_PATH_SYSPROP = "phex.download.path";
    private volatile static File phexConfigRoot;
    private volatile static File phexDownloadsRoot;
    private static SystemPropertiesInterface outside;
        
    /**
     * For a HTTPURLConnection java uses configured proxy settings.
     */
    
    public static void initSettingOutside(SystemPropertiesInterface c){
    	outside = c;
    }
    
    public static void updateProxyProperties()
    {
    	outside.updateProxyProperties();
    }
    
    private static File getOldPhexConfigRoot( )
    {        
    	return outside.getOldPhexConfigRoot();
    }
    
    public static void migratePhexConfigRoot()
    {
    	outside.migratePhexConfigRoot();
    }

    public static File getPhexConfigRoot()
    {
    	return outside.getPhexConfigRoot();
    }

    private static File initPhexConfigRoot()
    {
    	return outside.initPhexConfigRoot();
    }


    public static File getPhexDownloadsRoot()
    {
    	return outside.getPhexDownloadsRoot();
    }

    private static File initPhexDownloadsRoot()
    {
    	return outside.initPhexDownloadsRoot();
    }        


}
