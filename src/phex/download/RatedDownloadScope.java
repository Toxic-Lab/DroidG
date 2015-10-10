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
 *  Created on 16.09.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.download;

/**
 * A rated download scope is a download scope with a attached rating of its 
 * availability. It is used to determine the scope with the lowest availability
 * of a download file.
 */
public class RatedDownloadScope extends DownloadScope
{
    private long speedRating;
    private int countRating;

    public RatedDownloadScope( long startOffset, long endOffset )
    {
        super( startOffset, endOffset );
    }
    
    public RatedDownloadScope( long startOffset, long endOffset,
        int countRating, long speedRating )
    {
        super( startOffset, endOffset );
        this.countRating = countRating;
        this.speedRating = speedRating;
    }
    
    public long getSpeedRating()
    {
        return speedRating;
    }
    
    public int getCountRating()
    {
        return countRating;
    }
    
    @Override
    public String toString()
    {
        return "[RatedDownloadScope: start:" + getStart() + ",end:" + getEnd()
            + ",countRating:" + countRating + ",speedRating:" + speedRating 
            + "@" + Integer.toHexString(hashCode()) + "]";
    }
}
