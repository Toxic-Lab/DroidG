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
 *  Created on 16.03.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.gui;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public class DGuiSettings implements DElement
{
    public static final String ELEMENT_NAME = "gui-settings";
    
    private String lookAndFeelClass;
    private String iconPackName;
    
    private boolean isSetToolbarVisible;
    private boolean isToolbarVisible;
    
    private boolean isSetStatusbarVisible;
    private boolean isStatusbarVisible;
    
    private boolean isSetSearchBarVisible;
    private boolean isSearchBarVisible;
    
    private boolean isSetSearchListVisible;
    private boolean isSearchListVisible;
    
    private boolean isSetSearchFilterPanelVisible;
    private boolean isSearchFilterPanelVisible;
    
    private boolean isSetLogBandwidthSliderUsed;
    private boolean isLogBandwidthSliderUsed;
    
    private boolean isSetShowRespectCopyrightNotice;
    private boolean showRespectCopyrightNotice;
    
    private boolean isSetWindowPosX;
    private int windowPosX;
    
    private boolean isSetWindowPosY;
    private int windowPosY;
    
    private boolean isSetWindowWidth;
    private int windowWidth;
    
    private boolean isSetWindowHeight;
    private int windowHeight;
    
    private List<DTab> tabList;
    private DTableList tableList;
    

    public DGuiSettings()
    {
        tabList = new ArrayList<DTab>();
    }
    
    public boolean isLogBandwidthSliderUsed()
    {
        return isLogBandwidthSliderUsed;
    }

    public void setLogBandwidthSliderUsed( boolean isLogBandwidthSliderUsed )
    {
        isSetLogBandwidthSliderUsed = true;
        this.isLogBandwidthSliderUsed = isLogBandwidthSliderUsed;
    }
    
    public boolean isSetToolbarVisible()
    {
        return isSetToolbarVisible;
    }

    public boolean isToolbarVisible()
    {
        return isToolbarVisible;
    }

    public void setToolbarVisible( boolean isToolbarVisible )
    {
        isSetToolbarVisible = true;
        this.isToolbarVisible = isToolbarVisible;
    }
    
    public boolean isSetStatusbarVisible()
    {
        return isSetStatusbarVisible;
    }

    public boolean isStatusbarVisible()
    {
        return isStatusbarVisible;
    }

    public void setStatusbarVisible( boolean isStatusbarVisible )
    {
        isSetStatusbarVisible = true;
        this.isStatusbarVisible = isStatusbarVisible;
    }

    public boolean isSearchBarVisible()
    {
        return isSearchBarVisible;
    }

    public void setSearchBarVisible( boolean isSearchBarVisible )
    {
        isSetSearchBarVisible = true;
        this.isSearchBarVisible = isSearchBarVisible;
    }

    public boolean isSetSearchBarVisible()
    {
        return isSetSearchBarVisible;
    }

    public String getLookAndFeelClass()
    {
        return lookAndFeelClass;
    }

    public void setLookAndFeelClass( String lookAndFeelClass )
    {
        this.lookAndFeelClass = lookAndFeelClass;
    }

    public String getIconPackName()
    {
        return iconPackName;
    }

    public void setIconPackName( String iconPackName )
    {
        this.iconPackName = iconPackName;
    }

    public boolean isShowRespectCopyrightNotice()
    {
        return showRespectCopyrightNotice;
    }

    public void setShowRespectCopyrightNotice( boolean showRespectCopyrightNotice )
    {
        isSetShowRespectCopyrightNotice = true;
        this.showRespectCopyrightNotice = showRespectCopyrightNotice;
    }

    public DTableList getTableList()
    {
        return tableList;
    }

    public void setTableList( DTableList tableList )
    {
        this.tableList = tableList;
    }

    public int getWindowHeight()
    {
        return windowHeight;
    }

    public void setWindowHeight( int windowHeight )
    {
        isSetWindowHeight = true;
        this.windowHeight = windowHeight;
    }

    public int getWindowPosX()
    {
        return windowPosX;
    }

    public void setWindowPosX( int windowPosX )
    {
        isSetWindowPosX = true;
        this.windowPosX = windowPosX;
    }

    public int getWindowPosY()
    {
        return windowPosY;
    }

    public void setWindowPosY( int windowPosY )
    {
        isSetWindowPosY = true;
        this.windowPosY = windowPosY;
    }

    public int getWindowWidth()
    {
        return windowWidth;
    }

    public void setWindowWidth( int windowWidth )
    {
        isSetWindowWidth = true;
        this.windowWidth = windowWidth;
    }

    public List<DTab> getTabList()
    {
        return tabList;
    }

    public boolean isSetLogBandwidthSliderUsed()
    {
        return isSetLogBandwidthSliderUsed;
    }

    public boolean isSetShowRespectCopyrightNotice()
    {
        return isSetShowRespectCopyrightNotice;
    }

    public boolean isSetWindowHeight()
    {
        return isSetWindowHeight;
    }

    public boolean isSetWindowPosX()
    {
        return isSetWindowPosX;
    }

    public boolean isSetWindowPosY()
    {
        return isSetWindowPosY;
    }

    public boolean isSetWindowWidth()
    {
        return isSetWindowWidth;
    }
    
    public boolean isSearchFilterPanelVisible()
    {
        return isSearchFilterPanelVisible;
    }

    public void setSearchFilterPanelVisible( boolean isSearchFilterPanelVisible )
    {
        isSetSearchFilterPanelVisible = true;
        this.isSearchFilterPanelVisible = isSearchFilterPanelVisible;
    }
    
    public boolean isSetSearchFilterPanelVisible()
    {
        return isSetSearchFilterPanelVisible;
    }

    public boolean isSearchListVisible()
    {
        return isSearchListVisible;
    }

    public void setSearchListVisible( boolean isSearchListVisible )
    {
        isSetSearchListVisible = true;
        this.isSearchListVisible = isSearchListVisible;
    }

    public boolean isSetSearchListVisible()
    {
        return isSetSearchListVisible;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );
        
        if( lookAndFeelClass != null )
        {
            writer.startElm( "look-and-feel-class", null );
            writer.elmText( lookAndFeelClass );
            writer.endElm( "look-and-feel-class" );
        }
        
        if( iconPackName != null )
        {
            writer.startElm( "icon-pack", null );
            writer.elmText( iconPackName );
            writer.endElm( "icon-pack" );
        }
        
        if( isSetToolbarVisible )
        {
            writer.startElm( "is-toolbar-visible", null );
            writer.elmBol( isToolbarVisible );
            writer.endElm( "is-toolbar-visible" );
        }
        
        if( isSetStatusbarVisible )
        {
            writer.startElm( "is-statusbar-visible", null );
            writer.elmBol( isStatusbarVisible );
            writer.endElm( "is-statusbar-visible" );
        }
        
        if( isSetSearchBarVisible )
        {
            writer.startElm( "is-searchbar-visible", null );
            writer.elmBol( isSearchBarVisible );
            writer.endElm( "is-searchbar-visible" );
        }
        
        if( isSetSearchListVisible )
        {
            writer.startElm( "is-searchlist-visible", null );
            writer.elmBol( isSearchListVisible );
            writer.endElm( "is-searchlist-visible" );
        }
        
        if( isSetSearchFilterPanelVisible )
        {
            writer.startElm( "is-searchfilterpanel-visible", null );
            writer.elmBol( isSearchFilterPanelVisible );
            writer.endElm( "is-searchfilterpanel-visible" );
        }
        
        if( isSetLogBandwidthSliderUsed )
        {
            writer.startElm( "is-log-bandwidth-slider-used", null );
            writer.elmBol( isLogBandwidthSliderUsed );
            writer.endElm( "is-log-bandwidth-slider-used" );
        }
        
        if( isSetShowRespectCopyrightNotice )
        {
            writer.startElm( "show-respect-copyright-notice", null );
            writer.elmBol( showRespectCopyrightNotice );
            writer.endElm( "show-respect-copyright-notice" );
        }
        
        if( isSetWindowPosX )
        {
            writer.startElm( "window-posX", null );
            writer.elmInt( windowPosX );
            writer.endElm( "window-posX" );
        }
        
        if( isSetWindowPosY )
        {
            writer.startElm( "window-posY", null );
            writer.elmInt( windowPosY );
            writer.endElm( "window-posY" );
        }
        
        if( isSetWindowWidth )
        {
            writer.startElm( "window-width", null );
            writer.elmInt( windowWidth );
            writer.endElm( "window-width" );
        }
        
        if( isSetWindowHeight )
        {
            writer.startElm( "window-height", null );
            writer.elmInt( windowHeight );
            writer.endElm( "window-height" );
        }
        
        for ( DTab tab : tabList )
        {
            tab.serialize(writer);
        }
        
        tableList.serialize( writer );
        
        writer.endElm( ELEMENT_NAME );
    }
}