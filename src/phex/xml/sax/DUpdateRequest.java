package phex.xml.sax;

public class DUpdateRequest
{
    private boolean hasSharedFiles;

    private int sharedFiles;

    private boolean hasLastInfoId;

    private int lastInfoId;

    private String lastCheckVersion;

    private boolean hasAvgUptime;

    private long avgUptime;

    private String currentVersion;

    private boolean hasShowBetaInfo;

    private boolean showBetaInfo;

    private String hostid;

    private boolean hasStartupCount;

    private int startupCount;

    private boolean hasSharedSize;

    private int sharedSize;

    private String lafUsed;
    
    private String language;

    private boolean hasDailyAvgUptime;

    private int dailyAvgUptime;

    private String errorLog;

    private boolean hasDownloadCount;

    private int downloadCount;

    private String operatingSystem;

    private String javaVersion;

    private boolean hasUploadCount;

    private int uploadCount;
    
    private boolean hasLeafUltrapeerStatus;
    
    private int leafUltrapeerStatus;

    public int getSharedFiles()
    {
        return sharedFiles;
    }

    public void setSharedFiles( int value )
    {
        sharedFiles = value;
        hasSharedFiles = true;
    }

    public int getLastInfoId()
    {
        return lastInfoId;
    }

    public void setLastInfoId( int value )
    {
        lastInfoId = value;
        hasLastInfoId = true;
    }

    public String getLastCheckVersion()
    {
        return lastCheckVersion;
    }

    public void setLastCheckVersion( String value )
    {
        lastCheckVersion = value;
    }

    public long getAvgUptime()
    {
        return avgUptime;
    }

    public void setAvgUptime( long value )
    {
        avgUptime = value;
        hasAvgUptime = true;
    }

    public String getCurrentVersion()
    {
        return currentVersion;
    }

    public void setCurrentVersion( String value )
    {
        currentVersion = value;
    }

    public boolean isShowBetaInfo()
    {
        return showBetaInfo;
    }

    public void setShowBetaInfo( boolean value )
    {
        showBetaInfo = value;
        hasShowBetaInfo = true;
    }

    public String getHostid()
    {
        return hostid;
    }

    public void setHostid( String value )
    {
        hostid = value;
    }

    public int getStartupCount()
    {
        return startupCount;
    }

    public void setStartupCount( int value )
    {
        startupCount = value;
        hasStartupCount = true;
    }

    public int getSharedSize()
    {
        return sharedSize;
    }

    public void setSharedSize( int value )
    {
        sharedSize = value;
        hasSharedSize = true;
    }

    public String getLafUsed()
    {
        return lafUsed;
    }

    public void setLafUsed( String value )
    {
        lafUsed = value;
    }
    
    /**
     * @return the language
     */
    public String getLanguage()
    {
        return language;
    }

    /**
     * @param language the language to set
     */
    public void setLanguage(String language)
    {
        this.language = language;
    }

    public int getDailyAvgUptime()
    {
        return dailyAvgUptime;
    }

    public void setDailyAvgUptime( int value )
    {
        dailyAvgUptime = value;
        hasDailyAvgUptime = true;
    }

    public String getErrorLog()
    {
        return errorLog;
    }

    public void setErrorLog( String value )
    {
        errorLog = value;
    }

    public int getDownloadCount()
    {
        return downloadCount;
    }

    public void setDownloadCount( int value )
    {
        downloadCount = value;
        hasDownloadCount = true;
    }

    public String getOperatingSystem()
    {
        return operatingSystem;
    }

    public void setOperatingSystem( String value )
    {
        operatingSystem = value;
    }

    public String getJavaVersion()
    {
        return javaVersion;
    }

    public void setJavaVersion( String value )
    {
        javaVersion = value;
    }

    public int getUploadCount()
    {
        return uploadCount;
    }

    public void setUploadCount( int value )
    {
        uploadCount = value;
        hasUploadCount = true;
    }

    /**
     * @return the leafUltrapeerStatus
     */
    public int getLeafUltrapeerStatus()
    {
        return leafUltrapeerStatus;
    }

    /**
     * @param leafUltrapeerStatus the leafUltrapeerStatus to set
     */
    public void setLeafUltrapeerStatus(int leafUltrapeerStatus)
    {
        this.leafUltrapeerStatus = leafUltrapeerStatus;
        hasLeafUltrapeerStatus = true;
    }
    
    
    public void serialize( PhexXmlSaxWriter writer ) throws org.xml.sax.SAXException
    {
        writer.startElm( "update-request", null );
        
        if( hostid != null )
        {
            writer.startElm( "hostid", null );
            writer.elmText( hostid );
            writer.endElm( "hostid" );
        }
        
        if( currentVersion != null )
        {
            writer.startElm( "current-version", null );
            writer.elmText( currentVersion );
            writer.endElm( "current-version" );
        }
        
        if( hasStartupCount )
        {
            writer.startElm( "startup-count", null );
            writer.elmInt( startupCount );
            writer.endElm( "startup-count" );
        }
        
        if( lafUsed != null )
        {
            writer.startElm( "laf-used", null );
            writer.elmText( lafUsed );
            writer.endElm( "laf-used" );
        }
        
        if( lafUsed != null )
        {
            writer.startElm( "lang", null );
            writer.elmText( language );
            writer.endElm( "lang" );
        }
        
        if( operatingSystem != null )
        {
            writer.startElm( "operating-system", null );
            writer.elmText( operatingSystem );
            writer.endElm( "operating-system" );
        }
        
        if( javaVersion != null )
        {
            writer.startElm( "java-version", null );
            writer.elmText( javaVersion );
            writer.endElm( "java-version" );
        }
        
        if ( hasLeafUltrapeerStatus )
        {
            writer.startElm( "lup-status", null );
            writer.elmInt( leafUltrapeerStatus );
            writer.endElm( "lup-status" );
        }
        
        if( hasAvgUptime )
        {
            writer.startElm( "avg-uptime", null );
            writer.elmLong( avgUptime );
            writer.endElm( "avg-uptime" );
        }
        
        if( hasDailyAvgUptime )
        {
            writer.startElm( "daily-avg-uptime", null );
            writer.elmLong( dailyAvgUptime );
            writer.endElm( "daily-avg-uptime" );
        }
        
        if( hasDownloadCount )
        {
            writer.startElm( "download-count", null );
            writer.elmInt( downloadCount );
            writer.endElm( "download-count" );
        }
        
        if( hasUploadCount )
        {
            writer.startElm( "upload-count", null );
            writer.elmInt( uploadCount );
            writer.endElm( "upload-count" );
        }
        
        if( hasSharedFiles )
        {
            writer.startElm( "shared-files", null );
            writer.elmInt( sharedFiles );
            writer.endElm( "shared-files" );
        }
        
        if( hasSharedSize )
        {
            writer.startElm( "shared-size", null );
            writer.elmInt( sharedSize );
            writer.endElm( "shared-size" );
        }
        
        if( lastCheckVersion != null )
        {
            writer.startElm( "last-check-version", null );
            writer.elmText( lastCheckVersion );
            writer.endElm( "last-check-version" );
        }
        
        if( hasLastInfoId )
        {
            writer.startElm( "last-info-id", null );
            writer.elmInt( lastInfoId );
            writer.endElm( "last-info-id" );
        }
        
        if( hasShowBetaInfo )
        {
            writer.startElm( "show-beta-info", null );
            writer.elmBol( showBetaInfo );
            writer.endElm( "show-beta-info" );
        }
        
        if( errorLog != null )
        {
            writer.startElm( "error-log", null );
            writer.elmText( errorLog );
            writer.endElm( "error-log" );
        }
        
        writer.endElm( "update-request" );
    }
}
