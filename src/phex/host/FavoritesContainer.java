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
package phex.host;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import phex.common.Environment;
import phex.common.Phex;
import phex.common.PhexVersion;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.event.ChangeEvent;
import phex.event.ContainerEvent;
import phex.event.EventHandler;
import phex.event.PhexEventTopics;
import phex.event.UserMessageListener;
import phex.event.ContainerEvent.Type;
import phex.servent.Servent;
import phex.xml.sax.DPhex;
import phex.xml.sax.DSubElementList;
import phex.xml.sax.XMLBuilder;
import phex.xml.sax.favorites.DFavoriteHost;
import phex.xml.sax.parser.favorites.FavoritesListHandler;

/**
 * Holds user favorite hosts.
 */
public class FavoritesContainer implements EventHandler
{
    private final Servent servent;
    private final ArrayList<FavoriteHost> favoritesList;
    private boolean hasChangedSinceLastSave;
    
    public FavoritesContainer( Servent servent )
    {
        this.servent = servent;
        favoritesList = new ArrayList<FavoriteHost>();
        hasChangedSinceLastSave = false;
        Environment.getInstance().scheduleTimerTask(
            new SaveFavoritesTimer(), SaveFavoritesTimer.TIMER_PERIOD,
            SaveFavoritesTimer.TIMER_PERIOD );
        
        initializeFavorites();
        
        Phex.getEventService().register( this, new String[]{PhexEventTopics.Servent_GnutellaNetwork});
    }
    
    /**
     * Clears and initializes favorite hosts.
     */
    private void initializeFavorites()
    {
        favoritesList.clear();
        loadFromFile();
    }
    
    /**
     * Adds multiple favorite addresses
     * @param addresses the addresses to add
     */
    public synchronized void addFavorites(DestAddress[] addresses)
    {
        for ( DestAddress address : addresses )
        {
            FavoriteHost host = new FavoriteHost( address );
            insertBookmarkedHost( host, favoritesList.size() );            
        }
    }
    
    /**
     * Adds a single favorite address
     * @param address the address to add
     */
    public synchronized void addFavorite(DestAddress address)
    {
        FavoriteHost host = new FavoriteHost( address );
        insertBookmarkedHost( host, favoritesList.size() );            
    }
    
    /**
     * Reacts on gnutella network changes to initialize or save favorite hosts.
     */
    public void onGnutellaNetworkEvent( String topic, ChangeEvent event )
    {
        saveFavoriteHosts();
        initializeFavorites();
    }
        
    /**
     * Loads the hosts file phex.hosts.
     */
    private void loadFromFile()
    {
        //NLogger.debug( FavoritesContainer.class, "Loading favorites file." );

        DPhex dPhex;
        try
        {
            File favoritesFile = servent.getGnutellaNetwork().getFavoritesFile();
            if ( !favoritesFile.exists() )
            {
                return;
            }
            FileManager fileMgr = Phex.getFileManager();
            ManagedFile managedFile = fileMgr.getReadWriteManagedFile( favoritesFile );
            dPhex = XMLBuilder.loadDPhexFromFile( managedFile );
            if ( dPhex == null )
            {
                //NLogger.debug( FavoritesContainer.class,
                //    "No bookmarked hosts file found." );
                return;
            }
            DSubElementList<DFavoriteHost> dHostList = dPhex.getFavoritesList();
            if ( dHostList == null )
            {
                //NLogger.warn( FavoritesContainer.class, "No DFavoritesList found." );
                return;
            }
            
            for ( DFavoriteHost dHost : dHostList.getSubElementList() )
            {
                int port = dHost.getPort();
                
                DestAddress address = null;
                String hostName = dHost.getHostName();
                byte[] ip = dHost.getIp();
                if ( hostName != null )
                {
                    address = new DefaultDestAddress( hostName, port );
                }
                else if ( ip != null )
                {
                    address = new DefaultDestAddress( ip, port );
                }
                if ( address == null )
                {// seems to be a bad entry in the favorites file.. skip..
                    continue;
                }
                
                FavoriteHost bookmarkedHost = new FavoriteHost( address );
                
                // TODO2 no security checking is done here, assuming the user
                // always wants to have the bookmarked hosts even if faulty
                // but this concept is week... security is needed.
                insertBookmarkedHost( bookmarkedHost, favoritesList.size() );
            }
        }
        catch ( IOException exp )
        {
            //NLogger.error( FavoritesContainer.class, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.FavoritesSettingsLoadFailed, 
                new String[]{ exp.toString() } );
            return;
        }
        catch ( ManagedFileException exp )
        {
            //NLogger.error( FavoritesContainer.class, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.FavoritesSettingsLoadFailed, 
                new String[]{ exp.toString() } );
            return;
        }
    }    
    
    /**
     * Blocking operation which saves the bookmarked hosts if they changed since
     * the last save operation.
     */
    synchronized void saveFavoriteHosts( )
    {
        if ( !hasChangedSinceLastSave )
        {
            return;
        }
        
        try
        {
            DPhex dPhex = new DPhex();
            dPhex.setPhexVersion( PhexVersion.getFullVersion() );
            
            DSubElementList<DFavoriteHost> dList = new DSubElementList<DFavoriteHost>( 
                FavoritesListHandler.THIS_TAG_NAME );
            dPhex.setFavoritesList( dList );
            
            List<DFavoriteHost> list = dList.getSubElementList();
            for( FavoriteHost host : favoritesList )
            {
                DFavoriteHost dHost = new DFavoriteHost();
                DestAddress address = host.getHostAddress();
                if ( address.isIpHostName() )
                {
                    dHost.setIp( address.getIpAddress().getHostIP() );
                }
                else
                {
                    dHost.setHostName( address.getHostName() );
                }
                dHost.setPort( address.getPort() );
                
                list.add( dHost );
            }
            
            File favoritesFile = servent.getGnutellaNetwork().getFavoritesFile();
            ManagedFile managedFile = Phex.getFileManager().getReadWriteManagedFile( favoritesFile );
            
            XMLBuilder.saveToFile( managedFile, dPhex );
            hasChangedSinceLastSave = false;
        }
        catch ( IOException exp )
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            //NLogger.error( FavoritesContainer.class, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.FavoritesSettingsSaveFailed, 
                new String[]{ exp.toString() } );
        }
        catch ( ManagedFileException exp )
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            //NLogger.error( FavoritesContainer.class, exp, exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.FavoritesSettingsSaveFailed, 
                new String[]{ exp.toString() } );
        }
    }
    
    public synchronized int getBookmarkedHostsCount()
    {
        return favoritesList.size();
    }

    public synchronized FavoriteHost getBookmarkedHostAt( int index )
    {
        if ( index >= favoritesList.size() )
        {
            return null;
        }
        return favoritesList.get( index );
    }

    /**
     * inserts a auto connect host and fires the add event..
     */
    private synchronized void insertBookmarkedHost( FavoriteHost host, int position )
    {
        // if host is not already in the list
        if ( !favoritesList.contains( host ) )
        {
            favoritesList.add( position, host );
            hasChangedSinceLastSave = true;
            fireBookmarkedHostAdded( host, position );
        }
    }

    public synchronized void removeBookmarkedHost( FavoriteHost host )
    {
        int position = favoritesList.indexOf( host );
        if ( position >= 0 )
        {
            favoritesList.remove( position );
            fireBookmarkedHostRemoved( host, position );
            hasChangedSinceLastSave = true;
        }
    }

    ////////////////////////END Auto connect host methods //////////////////////
    
    
    ///////////////////// START event handling methods ////////////////////////
    private void fireBookmarkedHostAdded( FavoriteHost host, int position )
    {
        Phex.getEventService().publish( 
            PhexEventTopics.Net_Favorites,
            new ContainerEvent( Type.ADDED, host, this, position ) );
    }

    private void fireBookmarkedHostRemoved( FavoriteHost host, int position )
    {
        Phex.getEventService().publish(
            PhexEventTopics.Net_Favorites,
            new ContainerEvent( Type.REMOVED, host, this, position ) );
    }

    ////////////////////// END event handling methods //////////////////////////
    
    ////////////////////// START inner classes //////////////////////////
    
    private class SaveFavoritesRunner implements Runnable
    {
        public void run()
        {
            saveFavoriteHosts();
        }
    }
    
    private class SaveFavoritesTimer extends TimerTask
    {
        // once per minute
        public static final long TIMER_PERIOD = 1000 * 60;
        
        @Override
        public void run()
        {
            // trigger the save inside a background job
            Environment.getInstance().executeOnThreadPool( new SaveFavoritesRunner(),
                "SaveBookmarkedHosts" );
        }
    }

	@Override
	public void onEvent(String topic, Object event) {
		// TODO Auto-generated method stub
    	if(topic.compareTo(PhexEventTopics.Servent_GnutellaNetwork)== 0)
    		onGnutellaNetworkEvent(  topic, (ChangeEvent) event );
	}
}