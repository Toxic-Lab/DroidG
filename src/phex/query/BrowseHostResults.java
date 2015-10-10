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

import java.io.IOException;
import java.util.ArrayList;

import phex.common.Environment;
import phex.common.URN;
import phex.common.address.DestAddress;
import phex.connection.BrowseHostConnection;
import phex.connection.BrowseHostException;
import phex.download.RemoteFile;
import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.QueryResponseMsg;
import phex.msg.QueryResponseRecord;
import phex.servent.Servent;


public class BrowseHostResults extends Search
{
    //private static final Logger logger = LoggerFactory.getLogger( BrowseHostResults.class );
    /**
     *
     */
    public enum BrowseHostStatus
    {
        INITIALIZING, CONNECTING, FETCHING, FINISHED, CONNECTION_ERROR, BROWSE_HOST_ERROR
    }

    private final DestAddress destAddress;
    private final GUID hostGUID;
    
    private BrowseHostStatus browseHostStatus;

    public BrowseHostResults( Servent servent, DestAddress hostAddress, GUID aHostGUID )
    {
        super( servent );
        this.destAddress = hostAddress;
        hostGUID = aHostGUID;
        browseHostStatus = BrowseHostStatus.INITIALIZING;
    }

    public DestAddress getDestAddress()
    {
        return destAddress;
    }

    public GUID getHostGUID()
    {
        return hostGUID;
    }

    /**
     * queryService is not required for browse host
     */
    @Override
    public void startSearching( SearchProgress searchProgress )
    {
        isSearchFinished = false;
        Runnable runner = new Runnable()
        {
            public void run()
            {
                BrowseHostConnection connection = new BrowseHostConnection(
                    servent, destAddress, hostGUID, BrowseHostResults.this );
                try
                {
                    connection.sendBrowseHostRequest();
                    browseHostStatus = BrowseHostStatus.FINISHED;
                }
                catch ( BrowseHostException exp )
                {
                    //logger.warn( exp.toString(), exp);
                    browseHostStatus = BrowseHostStatus.BROWSE_HOST_ERROR;
                }
                catch ( IOException exp )
                {// TODO integrate error handling if no results have been returned
                    //logger.warn( exp.toString(), exp);
                    browseHostStatus = BrowseHostStatus.CONNECTION_ERROR;
                }
                finally
                {
                    stopSearching();   
                }
            }
        };
        Environment.getInstance().executeOnThreadPool( runner,
            "BrowseHostConnection-" + Integer.toHexString(runner.hashCode()) );
        fireSearchStarted();
    }
    
    public void setBrowseHostStatus( BrowseHostStatus status )
    {
        if ( status == null )
        {
            throw new NullPointerException();
        }
        browseHostStatus = status;
    }

    public BrowseHostStatus getBrowseHostStatus()
    {
        return browseHostStatus;
    }

    @Override
    public void stopSearching()
    {
        isSearchFinished = true;
        fireSearchStoped();
    }
    
    @Override
    public int getProgress()
    {
        switch ( browseHostStatus )
        {
        case INITIALIZING:
            return 0;
        case CONNECTING:
            return 10;
        case FETCHING:
            return 50;
        case FINISHED:
        case CONNECTION_ERROR:
        case BROWSE_HOST_ERROR:
        default:
            return 100;
        }
    }

    @Override
    public void processResponse( QueryResponseMsg msg )
        throws InvalidMessageException
    {
        QueryHitHost qhHost = QueryHitHost.createFrom( msg );

        RemoteFile rfile;
        QueryResponseRecord[] records = msg.getMsgRecords();
        ArrayList<RemoteFile> newHitList = new ArrayList<RemoteFile>( records.length );
        for (int i = 0; i < records.length; i++)
        {
            if ( !isResponseRecordValid( records[i] ) )
            {// skip record.
                continue;
            }
            synchronized( this )
            {
                long fileSize = records[i].getFileSize();
                String filename = records[i].getFilename();
                URN urn = records[i].getURN();
                int fileIndex = records[i].getFileIndex();
                String metaData = records[i].getMetaData();
                String pathInfo = records[i].getPathInfo();

                rfile = new RemoteFile( qhHost, fileIndex, filename, pathInfo,
                    fileSize, urn, metaData, (short)100 );
                searchResultHolder.addQueryHit( rfile );
                newHitList.add( rfile );
            }
        }
        // if something was added...
        if ( newHitList.size() > 0 )
        {
            RemoteFile[] newHits = new RemoteFile[ newHitList.size() ];
            newHitList.toArray( newHits );
            fireSearchHitsAdded( newHits );
        }
    }
    
    @Override
    public String toString()
    {
        return "[BrowseHostResults:" + destAddress + "," + "@" + Integer.toHexString(hashCode()) + "]";
    }
}