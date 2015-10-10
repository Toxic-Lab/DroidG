/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2007 Phex Development Group
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
package phex.http;



public class Range
{
    public enum RangeAvailability
    {
        /**
         * Indicates that the range is available
         */
        RANGE_AVAILABLE,

        /**
         * Indicates that the range is not available
         */
        RANGE_NOT_AVAILABLE,

        /**
         * Indicates that the range is available
         */
        RANGE_NOT_SATISFIABLE
    }
    
    
    public static final int NOT_SET = -1;

    private long suffixLength;

    /**
     * The start offset of the download.
     */
    private long startOffset;

    /**
     * The end offset of the download (inclusive)
     */
    private long endOffset;

    /**
     * Creates a HTTPRange object for suffix lengths like '-500' which requests
     * the last 500 bytes of a file.
     * @param suffixLength the suffix length.
     */
    public Range( long suffixLength)
    {
        this.startOffset = NOT_SET;
        this.endOffset = NOT_SET;
        this.suffixLength = suffixLength;
    }

    /**
     * Creates a HTTPRange object with a start and end offset. The end offset
     * can be NOT_SET to form request like '100-'
     * @param startOffset the start offset.
     * @param endOffset the end offset.
     * @param rating the rating
     */
    public Range( long startOffset, long endOffset )
    {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.suffixLength = NOT_SET;
    }

    /**
     * Updates a HTTPRange object for suffix lengths like '-500' which requests
     * the last 500 bytes of a file.
     * @param suffixLength the suffix length.
     */
    public void update( long suffix )
    {
        this.startOffset = NOT_SET;
        this.endOffset = NOT_SET;
        this.suffixLength = suffix;
    }

    /**
     * Updats a HTTPRange object with a start and end offset. The end offset
     * can be NOT_SET to form request like '100-'
     * @param startOffset the start offset.
     * @param endOffset the end offset.
     */
    public void update( long startOffset, long endOffset )
    {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.suffixLength = NOT_SET;
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer("[Range: " + startOffset + "-" + endOffset + "]");
        return buf.toString();
    }

    /**
     * Returns the start offset of the download.
     * @param fileSize the file size is needed in case of suffix byte range
     * requests.
     * @return
     */
    public long getStartOffset( long fileSize )
    {
        if ( suffixLength == NOT_SET )
        {// we have absolute ranges.
            return Math.min( startOffset, fileSize-1 );
        }
        else
        {
            return fileSize - suffixLength;
        }
    }

    /**
     * Returns the end offset (inclusive) of the download.
     * @param fileSize the file size is needed in case of suffix byte range
     * requests.
     * @return
     */
    public long getEndOffset( long fileSize )
    {
        if ( suffixLength == NOT_SET && endOffset != NOT_SET )
        {// we have absolut ranges.
            return Math.min( endOffset, fileSize-1 );
        }
        else
        {
            return fileSize-1;
        }
    }

    public boolean isRangeSatisfiable( Range range, long fileSize )
    {
        // a------a
        // r--r
        //   r--r
        //     r--r
        //          r--r
        long rangeStart = range.getStartOffset( fileSize );
        if (   getStartOffset( fileSize ) <= rangeStart
            && rangeStart <= getEndOffset( fileSize ) )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public String buildHTTPRangeString()
    {
        if ( suffixLength == NOT_SET )
        {
            return String.valueOf( startOffset ) + '-' + String.valueOf(
                endOffset );
        }
        else
        {
            return '-' + String.valueOf( suffixLength );
        }
    }
}

