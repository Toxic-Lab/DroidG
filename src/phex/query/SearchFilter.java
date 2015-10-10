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
 */
package phex.query;

import java.util.*;

import phex.common.MediaType;
import phex.download.RemoteFile;

public class SearchFilter
{
    /**
     * The name of the search filter.
     */
    private String name;

    /**
     * The filtered tokens of the refine search text.
     */
    private List<String> filterTokens;

    /**
     * The filter string that is tokenized into the filterTokens
     */
    private String filterString;

    /**
     * The media type the results must be of.
     */
    private MediaType mediaType;

    /**
     * The minimum file size.
     */
    private long minFileSize;

    /**
     * The maximum file size.
     */
    private long maxFileSize;

    /**
     * The timestamp of the last use of this filter.
     */
    private long lastTimeUsed;

    public SearchFilter( String aName )
    {
        name = aName;
        filterTokens = new ArrayList<String>();
        mediaType = MediaType.getMediaTypeAny();
        minFileSize = -1;
        maxFileSize = -1;
    }
    
    public SearchFilter( )
    {
        this( null );
    }
    
    public void clearFilter()
    {
        filterTokens.clear();
        mediaType = MediaType.getMediaTypeAny();
        minFileSize = -1;
        maxFileSize = -1;
    }

    public String getFilterString()
    {
        return filterString;
    }

    public List getFilterTokens()
    {
        return filterTokens;
    }

    public long getMaxFileSize()
    {
        return maxFileSize;
    }

    public MediaType getMediaType()
    {
        return mediaType;
    }

    public long getMinFileSize()
    {
        return minFileSize;
    }

    public String getName()
    {
        return name;
    }

    public long getLastTimeUsed()
    {
        return lastTimeUsed;
    }

    public void setLastTimeUsed(long lastTimeUsed)
    {
        this.lastTimeUsed = lastTimeUsed;
        //QueryManager.getInstance().getSearchFilterContainer().saveSearchFilters();
    }

    @Override
    public String toString()
    {
        return name;
    }
    
    public void updateSearchFilter( String filterString )
    {
        setFilterString( filterString );
    }

    public void updateSearchFilter( String filterString, MediaType mediaType,
        long minFileSize, long maxFileSize )
    {
        setFilterString( filterString );
        this.mediaType = mediaType;
        this.minFileSize = minFileSize;
        this.maxFileSize = maxFileSize;
        //QueryManager.getInstance().getSearchFilterContainer().saveSearchFilters();
    }

    public void updateSearchFilter( long minFileSize, long maxFileSize )
    {
        this.minFileSize = minFileSize;
        this.maxFileSize = maxFileSize;
        // no saving.. this is only updating a temporary filter...
    }
    
    public boolean isFiltered( RemoteFile remoteFile )
    {
        return isFiltered( remoteFile.getFileSize(),
            remoteFile.getFilename(), remoteFile.getSpeed(),
            remoteFile.getQueryHitHost().getHostRating() );
    }

    public boolean isFiltered( long fileSize,
        String fullFileName, long hostSpeed, int hostRating )
    {
        // check file size.. min/max values should be > 0 to be valid
        if( ( minFileSize > 0 && fileSize < minFileSize ) ||
            ( maxFileSize > 0 && fileSize > maxFileSize ) )
        {
            return true;
        }

        // check media type
        if ( !mediaType.isFilenameOf( fullFileName ) )
        {
            return true;
        }

        if ( isFilenameFiltered( fullFileName ) )
        {
            return true;
        }

        return false;
    }    

    private void setFilterString( String aFilterString )
    {
        filterString = aFilterString;
        StringTokenizer tokenizer = new StringTokenizer( filterString, " " );
        filterTokens.clear();
        while ( tokenizer.hasMoreTokens() )
        {
            filterTokens.add( tokenizer.nextToken().toLowerCase() );
        }
    }

    private boolean isFilenameFiltered( String filename )
    {
        if ( filterTokens == null || filterTokens.size() == 0 )
        {
            return false;
        }
        filename = filename.toLowerCase();
        Iterator iterator = filterTokens.iterator();
        while ( iterator.hasNext() )
        {
            String token = (String)iterator.next();
            if ( filename.indexOf( token ) == -1 )
            {
                return true;
            }
        }
        return false;
    }
}