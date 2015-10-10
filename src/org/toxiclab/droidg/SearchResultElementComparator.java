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
package org.toxiclab.droidg;
import java.util.Comparator;


/**
 * 
 */
public class SearchResultElementComparator implements Comparator<SearchResultElement>
{
    
    public static final int UNSORTED = 0;
    public static final int SORT_BY_SIZE = 1;
    public static final int SORT_BY_FILE = 2;
    public static final int SORT_BY_EXTENSION = 3;
    public static final int SORT_BY_BITZI_RATING = 4;
    public static final int SORT_BY_SHA1 = 5;
    public static final int SORT_BY_HOST = 6;
    public static final int SORT_BY_META_DATA = 7;
    public static final int SORT_BY_VENDOR = 8;
    public static final int SORT_BY_SPEED = 9;
    public static final int SORT_BY_RATING = 10;
    public static final int SORT_BY_SCORE = 11;
    
    private int sortField;
    private boolean isSortedAscending;
    private DestAddressComparator destAddressComparator;
    
    public void setSortField( int sortField, boolean isSortedAscending )
    {
        this.sortField = sortField;
        this.isSortedAscending = isSortedAscending;
        destAddressComparator = new DestAddressComparator();
    }
    
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( SearchResultElement sr1, SearchResultElement sr2 )
    {
        if( sr1 == sr2 || sr1.equals( sr2 ) )
        {
            return 0;
        }
                
        long diff;
        switch ( sortField )
        {
            case SORT_BY_SIZE:
                diff = sr1.getSingleRemoteFile().getFileSize() - sr2.getSingleRemoteFile().getFileSize();
                break;
            case SORT_BY_FILE:
                diff = sr1.getSingleRemoteFile().getFilename().compareTo( sr2.getSingleRemoteFile().getFilename() );
                break;
            case SORT_BY_EXTENSION:
                diff = sr1.getSingleRemoteFile().getFileExt().compareTo( sr2.getSingleRemoteFile().getFileExt() );
                break;
            case SORT_BY_BITZI_RATING:
                diff = Float.valueOf( sr1.getSingleRemoteFile().getBitzRating() ).compareTo( 
                    sr2.getSingleRemoteFile().getBitzRating() );
                break;
            case SORT_BY_SHA1:
                diff = sr1.getSingleRemoteFile().getSHA1().compareTo( sr2.getSingleRemoteFile().getSHA1() );
                break;
            case SORT_BY_HOST:
                if ( sr1.getRemoteFileListCount() == 0 && sr2.getRemoteFileListCount() == 0 )
                {
                    diff = destAddressComparator.compare(
                        sr1.getSingleRemoteFile().getHostAddress(),
                        sr2.getSingleRemoteFile().getHostAddress() );
                }
                else
                {
                    diff = sr1.getRemoteFileListCount() - sr2.getRemoteFileListCount();
                }
                break;
            case SORT_BY_META_DATA:
                if ( sr1.getRemoteFileListCount() == 0 && sr2.getRemoteFileListCount() == 0 )
                {
                    String meta1 = sr1.getSingleRemoteFile().getMetaData();
                    String meta2 = sr2.getSingleRemoteFile().getMetaData();
                    diff = (meta1 == null || meta2 == null) ? -1 : meta1.compareTo( meta2 );
                }
                else
                {
                    diff = sr1.getRemoteFileListCount() - sr2.getRemoteFileListCount();
                }
                break;
            case SORT_BY_VENDOR:
                if ( sr1.getRemoteFileListCount() == 0 && sr2.getRemoteFileListCount() == 0 )
                {
                    String v1 = sr1.getSingleRemoteFile().getQueryHitHost().getVendor();
                    String v2 = sr2.getSingleRemoteFile().getQueryHitHost().getVendor();
                    diff = (v1 == null || v2 == null) ? -1 : v1.compareTo( v2 );
                }
                else
                {
                    diff = sr1.getRemoteFileListCount() - sr2.getRemoteFileListCount();
                }
                break;
            case SORT_BY_SPEED:
                if ( sr1.getRemoteFileListCount() == 0 && sr2.getRemoteFileListCount() == 0 )
                {
                    diff = sr1.getSingleRemoteFile().getSpeed() -
                        sr2.getSingleRemoteFile().getSpeed();
                }
                else
                {
                    diff = sr1.getRemoteFileListCount() - sr2.getRemoteFileListCount();
                }
                break;
            case SORT_BY_RATING:
                if ( sr1.getRemoteFileListCount() == 0 && sr2.getRemoteFileListCount() == 0 )
                {
                    diff = sr1.getSingleRemoteFile().getQueryHitHost().getHostRating() -
                        sr2.getSingleRemoteFile().getQueryHitHost().getHostRating();
                }
                else
                {
                    diff = sr1.getRemoteFileListCount() - sr2.getRemoteFileListCount();
                }
                break;
            case SORT_BY_SCORE:
                if ( sr1.getRemoteFileListCount() == 0 && sr2.getRemoteFileListCount() == 0 )
                {
                    diff = sr1.getSingleRemoteFile().getScore() -
                        sr2.getSingleRemoteFile().getScore();
                }
                else
                {
                    diff = sr1.getRemoteFileListCount() - sr2.getRemoteFileListCount();
                }
                break;
            default:
                diff = 1;
        }
        if ( diff == 0 )
        {   // diff is 0 we need to determine a difference value that stays unique on
            // consecutive calls. Using a hashCode sounds reasonable though it might
            // still result to 0...
            diff = sr1.hashCode() - sr2.hashCode();
        }
        diff = isSortedAscending ? diff : -diff;
        if ( diff < 0 )
        {
            return -1;
        }
        else if ( diff > 0 )
        {
            return 1;
        }
        else
        {
            return 1;
        }
    }

}
