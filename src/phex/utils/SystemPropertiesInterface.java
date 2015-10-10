package phex.utils;

import java.io.File;

public interface SystemPropertiesInterface {
	public  void updateProxyProperties();
	public  File getOldPhexConfigRoot( );
	public  void migratePhexConfigRoot();
	public  File getPhexConfigRoot();
	public  File initPhexConfigRoot();
	public  File getPhexDownloadsRoot();
	public  File initPhexDownloadsRoot();
}
