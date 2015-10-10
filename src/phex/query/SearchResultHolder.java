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
 *  Created on 21.01.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.query;

import java.util.ArrayList;
import java.util.List;

import phex.common.URN;
import phex.common.address.DestAddress;
import phex.download.RemoteFile;
import phex.msg.GUID;

/**
 * Holds search results of a Search.
 */
public class SearchResultHolder
{
    /**
     * The list of query hits returned by the query. Contains the RemoteFile
     * objects.
     */
    private List<RemoteFile> queryHitList;
    
    protected SearchResultHolder()
    {
        queryHitList = new ArrayList<RemoteFile>();
    }
    
    public void addQueryHit( RemoteFile remoteFile )
    {
        queryHitList.add(remoteFile);
    }
    
    /**
     * Returns the query hit count.
     */
    public int getQueryHitCount()
    {
        return queryHitList.size();
    }
    
    /**
     * Returns the query hit at the given index.
     */
    public RemoteFile getQueryHit( int index )
    {
        if ( index < 0 || index >= queryHitList.size() )
        {
            return null;
        }
        return queryHitList.get( index );

    }

    /**
     * Returns the query hits at the given indices.
     */
    public RemoteFile[] getQueryHits( int[] indices )
    {
        RemoteFile[] results = new RemoteFile[indices.length];
        for ( int i = 0; i < indices.length; i++ )
        {
            results[i] = queryHitList.get( indices[i] );
        }
        return results;
    }
    
    /**
     * Trys to find a query hit in the search results. It will first check for
     * hostGUID and URN if no URN is provided it will use fileName, fileSize and
     * fileIndex to indentify a file.
     * If not query hit is found null is returned.
     * @param hostGUID the host GUID to look for.
     * @param urn the host URN to look for.
     * @param fileName The file name to look for if no URN is provided.
     * @param fileSize The file size to look for if no URN is provided.
     * @param fileIndex The file index to look for if no URN is provided.
     * @return The RemoteFile if found or null otherwise.
     */
    public RemoteFile findQueryHit( QueryHitHost qhh, URN urn,
        String fileName, long fileSize, int fileIndex )
    {
        GUID fileHostGUID;
        GUID hostGUID = qhh.getHostGUID();
        DestAddress hostAddress = qhh.getHostAddress();

        int size = queryHitList.size();
        for ( int i = 0; i < size; i++ )
        {
            RemoteFile file = queryHitList.get( i );

            fileHostGUID = file.getRemoteClientID();
            
            boolean foundMatch = false;
            // first try by comparing GUIDs if possible
            if ( fileHostGUID != null && hostGUID != null )
            {
                if ( fileHostGUID.equals( hostGUID ) )
                {
                    foundMatch = true;
                }
            }
            
            if ( !foundMatch )
            {// now try by comparing IP:port
                DestAddress fileHostAddress = file.getHostAddress();
                if ( fileHostAddress.equals( hostAddress ) )
                {
                    foundMatch = true;
                }
            }
            
            if ( !foundMatch )
            {
                continue;
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
                if ( fileIndex == file.getFileIndex()
                    && fileSize == file.getFileSize()
                    && fileName.equals( file.getFilename() ) )
                {
                    return file;
                }
            }
        }

        return null;
    }
}