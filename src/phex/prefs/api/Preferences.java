/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
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
 *  Created on 15.08.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.prefs.api;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import phex.common.collections.SortedProperties;
import phex.utils.FileUtils;
import phex.utils.IOUtil;

public class Preferences
{
    private Map<String, Setting<?>> settingMap;
    private File prefFile;
    private Properties valueProperties;
    private boolean isSaveRequired; 
    
    public Preferences( File file )
    {
        prefFile = file;
        settingMap = new HashMap<String, Setting<?>>();
    }
    
    protected String getLoadedProperty( String name )
    {
        return valueProperties.getProperty( name );
    }
    
    protected List<String> getPrefixedPropertyNames( String prefix )
    {
        List<String> found = new ArrayList<String>();
        Set<Object> keys = valueProperties.keySet();
        for ( Object keyObj : keys )
        {
            String key = (String)keyObj;
            if ( key.startsWith( prefix ) )
            {
                found.add( key );
            }
        }
        return found;
    }
    
    protected void registerSetting( String name, Setting<?> setting )
    {
        settingMap.put( name, setting );
    }
    
    public synchronized void saveRequiredNotify()
    {
        isSaveRequired = true;
    }
    
    public synchronized void load()
    {
        Properties loadProperties = new Properties();
        InputStream inStream = null;
        try
        {
            inStream = new BufferedInputStream( new FileInputStream( prefFile ) );
            loadProperties.load( inStream );
        }
        catch ( IOException exp )
        {
            IOUtil.closeQuietly( inStream );
            if ( !(exp instanceof FileNotFoundException) )
            {
                //NLogger.error( Preferences.class, exp );
            }
            // There was a problem loading the properties file.. try to load a
            // possible backup...
            File bakFile = new File( prefFile.getParentFile(),
                prefFile.getName() + ".bak" );
            try
            {
                inStream = new BufferedInputStream( new FileInputStream( bakFile ) );
                loadProperties.load( inStream );
            }
            catch ( FileNotFoundException exp2 )
            {/* ignore */ }
            catch ( IOException exp2 )
            {
                //NLogger.error( Preferences.class, exp );
            }
        }
        finally
        {
            IOUtil.closeQuietly( inStream );
        }
        valueProperties = loadProperties;
        
        
        /*

        deserializeSimpleFields();
        deserializeComplexFields();

        handlePhexVersionAdjustments();

        // no listening port is set yet. Choose a random port from 4000-63999
        if (mListeningPort == -1 || mListeningPort > 65500 )
        {
            Random random = new Random(System.currentTimeMillis());
            mListeningPort = random.nextInt( 60000 );
            mListeningPort += 4000;
        }
        updateSystemSettings();
        
        // count startup...
        totalStartupCounter ++;
        
        // make sure directories exists...
        File dir = new File( mDownloadDir );
        dir.mkdirs();
        dir = new File( incompleteDir );
        dir.mkdirs();
        
        */
    }
    
    public synchronized void save()
    {
        if ( !isSaveRequired )
        {
            //NLogger.debug( Preferences.class, 
            //    "No saving of preferences required." );
            return;
        }
        //NLogger.debug( Preferences.class, 
//            "Saving preferences to: " + prefFile.getAbsolutePath() );
        Properties saveProperties = new SortedProperties();
        
        for ( Setting<?> setting : settingMap.values() )
        {
            if ( setting.isDefault() && !setting.isAlwaysSaved() )
            {
                continue;
            }
            PreferencesFactory.serializeSetting( setting, saveProperties );
        }
        
        File bakFile = new File( prefFile.getParentFile(), prefFile.getName() + ".bak" );
        try
        {
            // make a backup of old pref File.
            if ( prefFile.exists() )
            {
                FileUtils.copyFile( prefFile, bakFile );
            }
            
            // create a new pref file.
            OutputStream os = null;
            try
            {
                os = new BufferedOutputStream( new FileOutputStream( prefFile ) );
                saveProperties.store( os, "Phex Preferences" );
            }
            finally
            {
                IOUtil.closeQuietly( os );
            }
        }
        catch (IOException exp )
        {
            //NLogger.error( Preferences.class, exp, exp );
        }
    }
}