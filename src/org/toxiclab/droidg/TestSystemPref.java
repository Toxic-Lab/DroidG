package org.toxiclab.droidg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;

import phex.common.Phex;
import phex.prefs.core.ProxyPrefs;
import phex.utils.FileUtils;
import phex.utils.SystemPropertiesInterface;
import android.content.res.AssetManager;
import android.os.Environment;

public class TestSystemPref implements SystemPropertiesInterface{
    public static final String PHEX_CONFIG_PATH_SYSPROP = "phex.config.path";
    public static final String PHEX_DOWNLOAD_PATH_SYSPROP = "phex.download.path";
    private volatile static File phexConfigRoot;
    private volatile static File phexDownloadsRoot;
        
    /**
     * For a HTTPURLConnection java uses configured proxy settings.
     */
    public  void updateProxyProperties()
    {
        System.setProperty( "http.agent", Phex.getFullPhexVendor() );
        if ( ProxyPrefs.UseHttp.get().booleanValue() )
        {
            System.setProperty( "http.proxyHost", ProxyPrefs.HttpHost.get() );
            System.setProperty( "http.proxyPort", ProxyPrefs.HttpPort.get().toString() );
        }
        else
        {
            System.setProperty( "http.proxyHost", "" );
            System.setProperty( "http.proxyPort", "" );
        }
        
        // cache DNS name lookups for only 30 minutes
        System.setProperty( "networkaddress.cache.ttl", "1800" );
        Security.setProperty( "networkaddress.cache.ttl", "1800" );
    }
    
    /**
     * Sets the directory into which Phex adds its configuration files. When
     * configRoot is null the directory is set to:<br>
     * {user.home}/phex on windows systems and<br>
     * {user.home}/.phex on unix and mac systems.
     * 
     * @deprecated since Phex 3.0, drop support once 2.x is not in use anymore
     */
    @Deprecated
    public  File getOldPhexConfigRoot( )
    {        
    	return null;
    }
    
    /**
     * @deprecated since Phex 3.0, drop support once 2.x is not in use anymore
     */
    @Deprecated
    public  void migratePhexConfigRoot()
    {
    	return;
    }
    
    /**
     * Returns the full path to the Phex directory of the user's home. This
     * directory is plattform dependent.
     * - Unix: ~/.phex/
     * - OSX: ~/Library/Application Support/Phex/
     * - Windows: ..../Documents and Settings/username/Application Data/Phex/
     * @throws RuntimeException in case creating a possible missing Phex config root fails
     *         or the phexConfigRoot is not a directory.
     */
    public  File getPhexConfigRoot()
    {
        if ( phexConfigRoot == null )
        {
            phexConfigRoot = initPhexConfigRoot();
        }
        if ( phexConfigRoot.exists() )
        {
            if ( !phexConfigRoot.isDirectory() )
            {
                throw new RuntimeException( "Config location is not a directory: " + phexConfigRoot.getAbsolutePath() );
            }
        }
        else
        {
            try
            {
                FileUtils.forceMkdir( phexConfigRoot );
                copyFileFromAsset(phexConfigRoot, "phexCorePrefs.properties");
                copyFileFromAsset(phexConfigRoot, "security.xml");
                copyFileFromAsset(phexConfigRoot, "udphostcache.cfg");
            }
            catch ( IOException exp )
            {
                throw new RuntimeException( "Failed creating config directory: " + phexConfigRoot.getAbsolutePath() );
            }
        }
        return phexConfigRoot;
    }

    public  File initPhexConfigRoot()
    {
        // to prevent problems wait with assigning userPath...
        File sdroot = Environment.getExternalStorageDirectory();
        File dir = new File(sdroot, "/Android/data/org.toxiclab.droidg/files/config");
        return dir;
    }



    /**
     * Returns the full path to the Phex directory of the user's home. This
     * directory is plattform dependent.
     * - Unix: ~/.phex/
     * - OSX: ~/Library/Application Support/Phex/
     * - Windows: ..../Documents and Settings/username/Application Data/Phex/
     * @throws RuntimeException in case creating a possible missing Phex config root fails
     *         or the phexConfigRoot is not a directory.
     */
    public  File getPhexDownloadsRoot()
    {
        if ( phexDownloadsRoot == null )
        {
            phexDownloadsRoot = initPhexDownloadsRoot();
        }
        if ( phexDownloadsRoot.exists() )
        {
            if ( !phexDownloadsRoot.isDirectory() )
            {
                throw new RuntimeException( "Config location is not a directory: " + phexDownloadsRoot.getAbsolutePath() );
            }
        }
        else
        {
            try
            {
                FileUtils.forceMkdir( phexDownloadsRoot );
                
            }
            catch ( IOException exp )
            {
                throw new RuntimeException( "Failed creating config directory: " + phexDownloadsRoot.getAbsolutePath() );
            }
        }
        File f = new File(phexDownloadsRoot, "download");
        if(!f.exists()){
        	try{
        		FileUtils.forceMkdir( f);
        	}catch(IOException exp){
        		throw new RuntimeException( "Failed creating config directory: " + phexDownloadsRoot.getAbsolutePath() );
        	}
        }
        
        return phexDownloadsRoot;
    }

    public  File initPhexDownloadsRoot()
    {
        // to prevent problems wait with assigning userPath...
        File sdroot = Environment.getExternalStorageDirectory();
        File dir = new File(sdroot, "/Android/data/org.toxiclab.droidg/files");
        return dir;
    }        

    private void copyFileFromAsset(File dest, String filename) throws IOException{
        File nf = new File(dest, filename);
        AssetManager am = DPhex.getAssetManager();
        InputStream is = am.open(filename);
        if(!nf.createNewFile())
        	throw new IOException();
        OutputStream os = new FileOutputStream(nf);
        byte[] temp = new byte[256];
        int len;
        while((len = is.read(temp))> 0){
        	os.write(temp, 0, len);
        }
        is.close();
        os.close();
    }

}
