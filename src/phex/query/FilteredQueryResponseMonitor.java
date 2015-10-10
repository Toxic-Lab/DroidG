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
package phex.query;

import java.util.ArrayList;

import phex.common.Phex;
import phex.common.URN;
import phex.common.address.DestAddress;
import phex.download.RemoteFile;
import phex.event.PhexEventTopics;
import phex.host.Host;
import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.QueryResponseMsg;
import phex.msg.QueryResponseRecord;
import phex.msghandling.MessageSubscriber;
import phex.security.AccessType;
import phex.servent.Servent;

/**
 * This class monitors query results going through the node.
 * The data is used to look into query results.
 */
public class FilteredQueryResponseMonitor implements MessageSubscriber<QueryResponseMsg>
{
    private final Servent servent;
    
    /**
     * The list of query hits returned by the query. Contains the RemoteFile
     * objects.
     */
    private ArrayList<RemoteFile> queryHitList;
    
    /**
     * The passive search filter to look for.
     */
    private SearchFilter searchFilter;
    
    public FilteredQueryResponseMonitor( Servent servent )
    {
        queryHitList = new ArrayList<RemoteFile>();
        this.servent = servent;
    }
    
    /**
     * Updates the used search filter and forces a filtering of the query hits. 
     * @param aSearchFilter the search filter to use.
     */
    public void updatePassiveSearchFilter( SearchFilter aSearchFilter )
    {
        synchronized( queryHitList )
        {
            searchFilter = aSearchFilter;
            if ( searchFilter != null )
            {
                searchFilter.setLastTimeUsed( System.currentTimeMillis() );
            }
        }
    }
    
    /**
     * 
     */
    public SearchFilter getPassiveSearchFilter()
    {
        return searchFilter;
    }
    
    /**
     * Checks incoming {@link QueryResponseMsg} against the {@link SearchFilter}
     * to find results to monitor.<br>
     * If searchFilter is null no responses are captured. If searchFilter
     * is empty all responses are captured. 
     */
    public void onMessage(QueryResponseMsg message, Host sourceHost)
    {
        if ( searchFilter == null )
        {
            return;
        }

        QueryHitHost qhHost;
        QueryResponseRecord[] records;
        try
        {
            qhHost = QueryHitHost.createFrom( message );
            records = message.getMsgRecords();
        }
        catch (InvalidMessageException e)
        {
            // message is invalid
            return;
        }
        
        ArrayList<RemoteFile> newHitList = new ArrayList<RemoteFile>( records.length );
        for (int i = 0; i < records.length; i++)
        {
            if ( !isResponseRecordValid( records[i] ) )
            {// skip record.
                continue;
            }
            
            if ( searchFilter != null )
            {
                monitorPassiveSearch( qhHost, records[i], newHitList );
            }
        } // for
        
        // if something was added...
        if ( newHitList.size() > 0 )
        {
            RemoteFile[] newHits = new RemoteFile[ newHitList.size() ]; 
            newHitList.toArray( newHits );
            fireSearchHitsAdded( newHits );
        }
    }
    
    /**
     * Used to check if the record is valid. In this case a 
     * security check is done on the record URN.
     * @param record
     * @return true if valid, false otherwise.
     */
    private boolean isResponseRecordValid( QueryResponseRecord record )
    {
        // REWORK maybe we should if we should move the altloc security check
        // from GGEPExtension.parseAltExtensionData to here to?
        // MERGE with Search.isResponseRecordValid()
        
        URN urn = record.getURN();
        if ( urn != null && servent.getSecurityService().controlUrnAccess( urn ) != AccessType.ACCESS_GRANTED )
        {
            //NLogger.debug( FilteredQueryResponseMonitor.class, "Record contains blocked URN: " + urn.getAsString() );
            return false;
        }
        return true;
    }

    private void monitorPassiveSearch( QueryHitHost qhHost, QueryResponseRecord rec,
        ArrayList<RemoteFile> newHitList )
    {
        synchronized( queryHitList )
        {
            int speed = qhHost.getHostSpeed();
            int hostRating = qhHost.getHostRating();
            long fileSize = rec.getFileSize();
            String filename = rec.getFilename();
            boolean isFiltered = searchFilter.isFiltered( fileSize, filename, 
                speed, hostRating );
            
            if ( isFiltered )
            {
                return;
            }
            
            URN urn = rec.getURN();
            int fileIndex = rec.getFileIndex();
            String metaData = rec.getMetaData();
            
            // find duplicate from same host...
            RemoteFile availableHit = findQueryHit( qhHost, urn, filename,
                fileSize, fileIndex );
            short score = KeywordSearch.calculateSearchScore(
                searchFilter.getFilterString(), filename );
            if ( availableHit != null )
            {
                // update availableHit
                availableHit.updateQueryHitHost( qhHost );
                availableHit.setMetaData( metaData );
            }
            else
            {
                String pathInfo = rec.getPathInfo();
                RemoteFile rfile = new RemoteFile( qhHost, fileIndex, filename, pathInfo,
                    fileSize, urn, metaData, score );
                queryHitList.add( rfile );
                newHitList.add( rfile );
            }
            
            // handle possible AlternateLocations
            DestAddress[] alternateLocations = rec.getAlternateLocations();
            if ( urn != null && alternateLocations != null)
            {
                for ( int j = 0; j < alternateLocations.length; j++ )
                {
                    // find duplicate from same host...
                    QueryHitHost qhh = new QueryHitHost( null, alternateLocations[j], -1 );
                    
                    availableHit = findQueryHit( qhHost, urn, filename, fileSize,
                        fileIndex );
                    if ( availableHit != null )
                    {
                        // update availableHit
                        availableHit.updateQueryHitHost( qhHost );
                        availableHit.setMetaData( metaData );
                    }
                    else
                    {
                        RemoteFile rfile = new RemoteFile( qhh, -1, filename, "",
                            fileSize, urn, metaData, score );
                        queryHitList.add( rfile );
                        newHitList.add( rfile );
                    }
                }
            }
        }
    }
    
    /**
     * Tries to find a query hit in the search results. It will first check for
     * hostGUID and URN if no URN is provided it will use fileName, fileSize and
     * fileIndex to identify a file.
     * If not query hit is found null is returned.
     * @param hostGUID the host GUID to look for.
     * @param urn the host URN to look for.
     * @param fileName The file name to look for if no URN is provided.
     * @param fileSize The file size to look for if no URN is provided.
     * @param fileIndex The file index to look for if no URN is provided.
     * @return The RemoteFile if found or null otherwise.
     */
    private RemoteFile findQueryHit( QueryHitHost qhh, URN urn, String fileName,
        long fileSize, int fileIndex  )
    {
        GUID fileHostGUID;
        GUID hostGUID = qhh.getHostGUID();
        DestAddress hostAddress = qhh.getHostAddress();
        
        synchronized( queryHitList )
        {
            int size = queryHitList.size();
            for ( int i = 0; i < size; i++ )
            {
                RemoteFile file = queryHitList.get( i );
                
                fileHostGUID = file.getRemoteClientID();
                // first try by comparing GUIDs if possible
                if ( fileHostGUID != null && hostGUID != null )
                {
                    if ( !fileHostGUID.equals( hostGUID ) )
                    {
                        continue;
                    }
                }
                else
                {// now try by comparing IP:port
                    DestAddress fileHostAddress = file.getHostAddress();
                    if ( !fileHostAddress.equals( hostAddress ) )
                    {
                        continue;
                    }
                }
                
                if ( urn != null && file.getURN() != null )
                {
                    if ( urn.equals( file.getURN() ) )
                    {
                        return file;
                    }
                }
                else
                {
                    if ( fileIndex == file.getFileIndex() &&
                         fileSize == file.getFileSize() &&
                         fileName.equals( file.getFilename() ) )
                    {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    //// Event Handling
    ////////////////////////////////////////////////////////////////////////////
    protected void fireSearchHitsAdded( RemoteFile[] newHits )
    {        
        SearchDataEvent dataEvent = new SearchDataEvent( this,
            SearchDataEvent.SEARCH_HITS_ADDED, newHits );
        fireSearchDataEvent( dataEvent );
    }
    
    private void fireSearchDataEvent( final SearchDataEvent searchDataEvent )
    {
        Phex.getEventService().publish( PhexEventTopics.Search_Monitor_Results,
            searchDataEvent );
    }
}