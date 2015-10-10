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
 *  Created on 14.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.rules.condition;

import java.util.*;

import org.apache.commons.collections.set.ListOrderedSet;

import phex.download.RemoteFile;
import phex.query.Search;
import phex.xml.sax.rules.DCondition;
import phex.xml.sax.rules.DFileSizeCondition;

public class FileSizeCondition implements Condition
{
    private ListOrderedSet ranges;
    
    public FileSizeCondition( )
    {
        ranges = new ListOrderedSet();
    }
    
    public FileSizeCondition( long min, long max )
    {
        this();
        addRange(min, max);
    }
    
    /**
     * Deep copy constructor.
     * @param condition
     */
    public FileSizeCondition( FileSizeCondition condition )
    {
        this();
        update( condition );
    }
    
    public synchronized void update( FileSizeCondition condition )
    {
        ranges.clear();
        Iterator<Range> iterator = condition.ranges.iterator();
        while( iterator.hasNext() )
        {
            Range range = iterator.next();
            ranges.add( new Range( range.min, range.max ) );
        }
    }
    
    public synchronized int getRangesCount()
    {
        return ranges.size();
    }

    
    public synchronized Set<Range> getRanges()
    {
        return Collections.unmodifiableSet( ranges );
    }
    
    /**
     * Returns a unmodifiable list of the ranges.
     * @return
     */
    public synchronized List<Range> getRangesList()
    {
        return ranges.asList();
    }
    
    public synchronized FileSizeCondition addRange( long min, long max )
    {
        ranges.add( new Range( min, max ) );
        return this;
    }
    
    public synchronized FileSizeCondition addRange( Range range )
    {
        ranges.add( range );
        return this;
    }
    
    public synchronized void removeRange( Range range )
    {
        ranges.remove( range );
    }

    public synchronized boolean isMatched( Search search, RemoteFile remoteFile )
    {
        long fileSize = remoteFile.getFileSize();
        Iterator<Range> iterator = ranges.iterator();
        while( iterator.hasNext() )
        {
            Range range = iterator.next();
            // check file size.. min/max values should be > 0 to be valid
            if ( ( range.min < 0 || fileSize >= range.min ) &&
                 ( range.max < 0 || fileSize <= range.max ) )
            {
                return true;
            }
        }
        return false;
    }
    
    public synchronized boolean isComplete()
    {
        return getRangesCount() > 0;
    }
    
    @Override
    public synchronized Object clone()
    {
        try
        {
            FileSizeCondition clone = (FileSizeCondition) super.clone();
            clone.ranges = new ListOrderedSet();
            clone.ranges.addAll( ranges );
            return clone;
        }
        catch (CloneNotSupportedException exp)
        {
            throw new InternalError();
        }
    }
    
    public synchronized DCondition createDCondition()
    {
        DFileSizeCondition dCond = new DFileSizeCondition();
        List<Range> newList = new ArrayList<Range>( ranges );
        dCond.setRanges( newList );
        return dCond;
    }
    
    @Override
    public String toString()
    {
        return super.toString() + "[Ranges: " + ranges.toString() + "]";
    }
    
    public static FileSizeCondition create( DFileSizeCondition dCond )
    {
        FileSizeCondition cond = new FileSizeCondition( );
        cond.ranges.addAll( dCond.getRanges() );
        return cond;
    }
    
    public static class Range
    {
        public final long min;
        public final long max;
        
        public Range ( long min, long max )
        {
            this.min = min;
            this.max = max;
        }
        
        @Override
        public boolean equals( Object obj )
        {
            if ( !(obj instanceof Range ) )
            {
                return false;
            }
            Range range = (Range)obj;
            return range.min == min &&
                   range.max == max;
        }
        
        @Override
        public int hashCode()
        {
            int val = 17 * 37 + ((int) (min ^ (min >> 32)));
            val = val * 37 + ((int) (max ^ (max >> 32)));
            return val;
        }
        
        @Override
        public String toString()
        {
            return super.toString() + "[min:" + min + ", max:" + max + "]";
        }
    }
}
