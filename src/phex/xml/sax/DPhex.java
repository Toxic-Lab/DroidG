package phex.xml.sax;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import phex.xml.sax.downloads.DDownloadFile;
import phex.xml.sax.favorites.DFavoriteHost;
import phex.xml.sax.gui.DGuiSettings;
import phex.xml.sax.rules.DSearchRule;
import phex.xml.sax.security.DSecurity;
import phex.xml.sax.share.DSharedLibrary;

public class DPhex
{
    private String phexVersion;

    private DUpdateRequest updateRequest;
    private DUpdateResponse updateResponse;
    private DSubElementList<DFavoriteHost> favoritesList;
    private DSubElementList<DSearchRule> searchRuleList;
    private DSubElementList<DDownloadFile> downloadList;
    private DSecurity securityList;
    private DSharedLibrary sharedLibrary;
    private DGuiSettings guiSettings;

    public String getPhexVersion()
    {
        return phexVersion;
    }

    public void setPhexVersion( String value )
    {
        phexVersion = value;
    }

    public DSubElementList<DFavoriteHost> getFavoritesList()
    {
        return favoritesList;
    }

    public void setFavoritesList( DSubElementList<DFavoriteHost> value )
    {
        favoritesList = value;
    }
    
    /**
     * @return the securityList
     */
    public DSecurity getSecurityList()
    {
        return securityList;
    }

    /**
     * @param securityList the securityList to set
     */
    public void setSecurityList( DSecurity securityList )
    {
        this.securityList = securityList;
    }

    public DSharedLibrary getSharedLibrary()
    {
        return sharedLibrary;
    }

    public void setSharedLibrary( DSharedLibrary value )
    {
        sharedLibrary = value;
    }

    public DUpdateResponse getUpdateResponse()
    {
        return updateResponse;
    }

    public void setUpdateResponse( DUpdateResponse value )
    {
        updateResponse = value;
    }

    public DUpdateRequest getUpdateRequest()
    {
        return updateRequest;
    }

    public void setUpdateRequest( DUpdateRequest value )
    {
        updateRequest = value;
    }

    public DSubElementList<DSearchRule> getSearchRuleList()
    {
        return searchRuleList;
    }

    public void setSearchRuleList( DSubElementList<DSearchRule> rulesList )
    {
        this.searchRuleList = rulesList;
    }
    
    public DSubElementList<DDownloadFile> getDownloadList()
    {
        return downloadList;
    }

    public void setDownloadList( DSubElementList<DDownloadFile> downloadList )
    {
        this.downloadList = downloadList;
    }

    public DGuiSettings getGuiSettings()
    {
        return guiSettings;
    }

    public void setGuiSettings( DGuiSettings guiSettings )
    {
        this.guiSettings = guiSettings;
    }
    
    

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        AttributesImpl attributes = null;
        if ( phexVersion != null )
        {
            attributes = new AttributesImpl();
            attributes.addAttribute( "", "", "phex-version", "CDATA",
                phexVersion );
        }
        writer.startElm( "phex", attributes );

        if ( updateRequest != null )
        {
            updateRequest.serialize( writer );
        }

        if ( favoritesList != null )
        {
            favoritesList.serialize( writer );
        }

        if ( searchRuleList != null )
        {
            searchRuleList.serialize( writer );
        }
        
        if ( downloadList != null )
        {
            downloadList.serialize( writer );
        }

        if ( sharedLibrary != null )
        {
            sharedLibrary.serialize( writer );
        }

        if ( guiSettings != null )
        {
            guiSettings.serialize( writer );
        }
        
        if ( securityList != null )
        {
            securityList.serialize( writer );
        }

        // we dont need to serialize update response

        writer.endElm( "phex" );
    }
}
