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
package phex.download;

import java.util.*;

import phex.http.HTTPRangeSet;
import phex.http.Range;

/**
 * Represents a collection of DownloadScopes.
 * This class is not thread safe. You should always lock this object when 
 * accessing it or iterating through its iterator!
 */
public class DownloadScopeList implements Iterable<DownloadScope>, Cloneable
{
    private static DownloadScopeComparator DOWNLOAD_SCOPE_COMPARATOR = 
        new DownloadScopeComparator();
    
    /**
     * Counts the number of modifications on this scope list. A modification
     * is a add or remove operation to the underlying list.
     * The field is used to determine if cached values like aggregated length 
     * needs to be recalculated.
     */
    private volatile int modificationCount;
    
    /**
     * The list of download scopes, orderd by scope start position.
     */
    private List<DownloadScope> scopeList;
    
    /**
     * Cached value of aggregated lengths. It is recalculated if modification
     * count changed.
     */
    private long aggregatedLengthCache;
    
    /**
     * Modification count of last aggregated length calculation.
     */
    private int aggregatedLengthModCount;
    
    public DownloadScopeList()
    {
        scopeList = new ArrayList<DownloadScope>();
        aggregatedLengthCache = 0;
        modificationCount = 0;
        aggregatedLengthModCount = 0;
    }
    
    public void addAll( DownloadScopeList listToAdd )
    {
        for ( DownloadScope scope : listToAdd )
        {
            add( scope );
        }
    }
    
    public void add( DownloadScope newScope )
    {
        DownloadScope neighborBefore = null;
        DownloadScope neighborAfter = null;
        
        Iterator<DownloadScope> iterator = scopeList.iterator();
        while ( iterator.hasNext() )
        {
            DownloadScope scope = iterator.next();
            // check if
            // new   |------|
            // scope   |--|
            if ( newScope.contains( scope ) ) 
            {   // the new range completely contains the existing scope
                // we remove the scope from the list... and check further
                iterator.remove();
                continue;
            }
            
            // check if
            // scope |------|
            // new     |--|
            if ( scope.contains(newScope) )
            {
                // we don't need to add anything since this scope is already 
                // part of a existing scope.
                return;
            }
            
            // check if neighbor/overlap
            if ( newScope.isNeighborBefore( scope ) )
            {
                neighborBefore = scope;
            }
            if ( newScope.isNeighborAfter( scope ) )
            {
                neighborAfter = scope;
            }
            
            // we went far enough in the sorted scope list...
            if ( neighborAfter != null || scope.getStart() > newScope.getEnd() )
            {
                break;
            }
        }
        
        // add or merge the newScope into the list
        if ( neighborBefore == null && neighborAfter == null)
        {
            addInternal( newScope );
        }
        else if( neighborBefore != null && neighborAfter != null )
        {
            // remove both and add a merged scope
            removeInternal( neighborBefore );
            removeInternal( neighborAfter );
            addInternal( new DownloadScope( 
                neighborBefore.getStart(), neighborAfter.getEnd() ) );
        }
        else if ( neighborAfter != null)
        {
            removeInternal( neighborAfter );
            addInternal( new DownloadScope( 
                newScope.getStart(), neighborAfter.getEnd() ) );
        }
        else if ( neighborBefore != null)
        {
            removeInternal( neighborBefore );
            addInternal( new DownloadScope( 
                neighborBefore.getStart(), newScope.getEnd() ) );
        }
    }
    
    public void removeAll( DownloadScopeList listToRemove )
    {
    	for ( DownloadScope scope : listToRemove )
    	{
    		remove( scope );
    	}    	
    }
    
    /**
     * Remove cases:
     * |--remove--|-----| case1
     * |--|--remove--|--| case2
     * |-----|--remove--| case3
     * 
     * @param removeScope
     */
    public void remove( DownloadScope removeScope )
    {
        DownloadScope beforeScope = null;
        DownloadScope afterScope = null;
        Iterator<DownloadScope> iterator = scopeList.iterator();
        while ( iterator.hasNext() )
        {
            DownloadScope scope = iterator.next();
            // check for overlap
            if ( scope.isOverlapping( removeScope ) ) 
            {
                // we remove the scope from the list... and check how to split
                iterator.remove();
                modificationCount ++;
                if ( removeScope.getEnd() >= scope.getEnd() )
                { // case2 and case3
                    if ( removeScope.getStart() > scope.getStart() )
                    {
                        beforeScope = new DownloadScope( 
                            scope.getStart(), removeScope.getStart() - 1 );
                    }
                    // continue searching for possible case2
                }
                else if ( scope.getStart() >= removeScope.getStart() )
                { // case1 and case2
                    afterScope = new DownloadScope( 
                        removeScope.getEnd() + 1, scope.getEnd() );
                    // end of removeScope position reached.
                    break;
                }
                else
                {
                    beforeScope = new DownloadScope( 
                        scope.getStart(), removeScope.getStart() - 1 );
                    afterScope = new DownloadScope( 
                        removeScope.getEnd() + 1, scope.getEnd() );
                    break;
                }
            }
            else if ( scope.getStart() >= removeScope.getEnd() )
            {
                break;
            }
        }
        if ( beforeScope != null )
        {
            add( beforeScope );
        }
        if ( afterScope != null )
        {
            add( afterScope );
        }
    }
    
    public boolean contains( DownloadScope scope )
    {
        for ( DownloadScope thisScope : scopeList )
        {
            if ( thisScope.contains( scope ) )
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Retains all elements in the retain list.
     * @param retainList
     */
    public void retainAll( DownloadScopeList retainList )
    {
        ListIterator<DownloadScope> thisIterator = scopeList.listIterator();
        boolean removeThis;
        while( thisIterator.hasNext() )
        {
            removeThis = true;
            DownloadScope thisScope = thisIterator.next();
            
            for( DownloadScope retainScope : retainList )
            {
                if (  thisScope.isOverlapping(retainScope) )
                {
                    // we remove the scope from the list... and check how to split
                    thisIterator.remove();
                    long retainStart = Math.max( retainScope.getStart(), thisScope.getStart() );
                    long retainEnd = Math.min( retainScope.getEnd(), thisScope.getEnd() );
                    
                    // since we can assume that the segment size is always only
                    // reduced there will be no futher overlapping 
                    thisIterator.add( new DownloadScope( retainStart, retainEnd ) );
                    removeThis = false;
                    break;
                }
                else if ( retainScope.getStart() >= thisScope.getEnd() )
                {
                    break;
                }
            }
            if ( removeThis )
            {
                thisIterator.remove();
            }
        }
    }
    
    /**
     * The aggregated length of all download scopes.
     * @return The aggregated length of all download scopes.
     */
    public long getAggregatedLength()
    {
        if ( modificationCount == aggregatedLengthModCount )
        {
            return aggregatedLengthCache;
        }
        
        long length = 0;
        for( DownloadScope scope : scopeList )
        {
            length += scope.getLength();
        }
        aggregatedLengthCache = length;
        aggregatedLengthModCount = modificationCount;
        return length;
    }
    
    public Iterator<DownloadScope> iterator()
	{
		return scopeList.iterator();
	}
    
    public boolean isEmpty()
    {
        return scopeList.isEmpty();
    }
    
    public int size()
    {
        return scopeList.size();
    }
    
    public DownloadScope getScopeAt( int pos )
    {
        if ( pos < 0 || pos >= scopeList.size() )
        {
            return null;
        }
        return scopeList.get( pos );
    }
    
    public void clear()
    {
        scopeList.clear();
        aggregatedLengthCache = 0;
        modificationCount = 0;
        aggregatedLengthModCount = 0;
    }
    
    @Override
    public Object clone()
    {
        DownloadScopeList copy = new DownloadScopeList();
        copy.scopeList.addAll( scopeList );
        return copy;
    }
    
    protected void addInternal( DownloadScope scope )
    {
        int index = Collections.binarySearch(scopeList, scope, DOWNLOAD_SCOPE_COMPARATOR );
        assert index < 0;
        scopeList.add( -(index+1), scope);
        modificationCount ++;
    }
    
    protected void removeInternal( DownloadScope scope )
    {
        int index = Collections.binarySearch(scopeList, scope, DOWNLOAD_SCOPE_COMPARATOR );
        assert index >= 0;
        scopeList.remove( index );
        modificationCount ++;
    }
    
    protected List<DownloadScope> getScopeListCopy()
    {
        return new ArrayList<DownloadScope>( scopeList );
    }
    
    private static class DownloadScopeComparator implements Comparator<DownloadScope>
    {
        public int compare( DownloadScope scope1, DownloadScope scope2 )
        {
            if ( scope1 == scope2 || scope1.equals(scope2) )
            {
                return 0;
            }
            
            if( scope1.getStart() > scope2.getStart() )
            {
                return 1;
            }            
            return -1;
        }
    }
}
