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

import java.util.*;

/**
 * Represents a collection of RatedDownloadScopes.
 * This class is not thread safe. You should always lock this object when 
 * accessing it or iterating through its iterator!
 */
public class RatedDownloadScopeList
{
    private static DownloadScopeComparator DOWNLOAD_SCOPE_COMPARATOR = 
        new DownloadScopeComparator();
    private static RatedDownloadScopeComparator RATED_DOWNLOAD_SCOPE_COMPARATOR = 
        new RatedDownloadScopeComparator();
    
    /**
     * Counts the number of modifications on this scope list. A modification
     * is a add or remove operation to the underlying list.
     * The field is used to determine if cached values like aggregated length 
     * needs to be recalculated.
     */
    private int modificationCount;
    
    /**
     * A list ordered by scope start position.
     */
    private List<RatedDownloadScope> scopeList;
    
    /**
     * A list ordered by download scope rating.
     */
    private List<RatedDownloadScope> ratedScopeList;
    
    /**
     * Cached value of aggregated lengths. It is recalculated if modification
     * count changed.
     */
    private long aggregatedLengthCache;
    
    /**
     * Modification count of last aggregated length calculation.
     */
    private int aggregatedLengthModCount;
    
    public RatedDownloadScopeList( )
    {
        scopeList = new ArrayList<RatedDownloadScope>();
        ratedScopeList = new ArrayList<RatedDownloadScope>();
        
        aggregatedLengthCache = 0;
        modificationCount = 0;
        aggregatedLengthModCount = 0;
    }
    
    public RatedDownloadScopeList( DownloadScopeList downloadScopes )
    {
        this();
        for( DownloadScope scope : downloadScopes )
        {
            add( new RatedDownloadScope( scope.getStart(), scope.getEnd() ) );
        }
    }
    
    public void addAll( RatedDownloadScopeList ratedScopes )
    {
    	Iterator<RatedDownloadScope> iterator = ratedScopes.getScopeIterator();
        while( iterator.hasNext() )
        {
            RatedDownloadScope scope = iterator.next();
            add( scope );
        }
    }
    
    public void addAll( DownloadScopeList downloadScopes )
    {
        for( DownloadScope scope : downloadScopes )
        {
            add( new RatedDownloadScope( scope.getStart(), scope.getEnd() ) );
        }
    }
    
    public void add( RatedDownloadScope newScope )
    {
        RatedDownloadScope neighborBefore = null;
        RatedDownloadScope neighborAfter = null;
        
        Iterator<RatedDownloadScope> iterator = getScopeIterator();
        while ( iterator.hasNext() )
        {
            RatedDownloadScope scope = iterator.next();
            // check if
            // new   |------|
            // scope   |--|
            if ( newScope.contains( scope ) )
            {   
                iterator.remove();
                RatedDownloadScope scope1 = new RatedDownloadScope(
                    scope.getStart(), scope.getEnd(),
                    scope.getCountRating() + newScope.getCountRating(), 
                    scope.getSpeedRating() + newScope.getSpeedRating() );
                addInternal( scope1 );
                if ( newScope.getStart() < scope.getStart() )
                {
                    RatedDownloadScope scope2 = new RatedDownloadScope(
                        newScope.getStart(), scope.getStart() - 1,
                        newScope.getCountRating(), newScope.getSpeedRating() );
                    add( scope2 );
                }
                if ( scope.getEnd() < newScope.getEnd() )
                {
                    RatedDownloadScope scope3 = new RatedDownloadScope(
                        scope.getEnd() + 1, newScope.getEnd(),
                        newScope.getCountRating(), newScope.getSpeedRating() );
                    add( scope3 );
                }
                
                return;
            }
            
            // check if
            // scope |------|
            // new     |--|
            if ( scope.contains( newScope ) )
            {
                if ( newScope.getCountRating() == 0 && newScope.getSpeedRating() == 0 )
                {
                    // we dont need to add anything since this scope is already 
                    // part of a existing scope.
                    return;
                }
                
                iterator.remove();
                if ( scope.getStart() != newScope.getStart() )
                {
                    RatedDownloadScope scope1 = new RatedDownloadScope(
                        scope.getStart(), newScope.getStart() - 1,
                        scope.getCountRating(), scope.getSpeedRating() );
                    addInternal( scope1 );
                }
                // add the new segment on top of the existing segment
                RatedDownloadScope scope2 = new RatedDownloadScope(
                    newScope.getStart(), newScope.getEnd(),
                    scope.getCountRating() + newScope.getCountRating(),
                    scope.getSpeedRating() + newScope.getSpeedRating() );
                addInternal( scope2 );
                
                if ( scope.getEnd() != newScope.getEnd() )
                {
                    RatedDownloadScope scope3 = new RatedDownloadScope(
                        newScope.getEnd() + 1, scope.getEnd(),
                        scope.getCountRating(), scope.getSpeedRating() );
                    addInternal( scope3 );
                }
                
                return;
            }
            
            // check if neighbor/overlapp
            if ( newScope.isNeighborBefore( scope ) && newScope.isOverlapping( scope ) )
            {
                neighborBefore = scope;
            }
            if ( newScope.isNeighborAfter( scope ) && scope.isOverlapping( newScope ) )
            {
                neighborAfter = scope;
            }
            
            // we went far enought in the sorted scope list...
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
        else if ( neighborAfter != null && neighborBefore != null &&
                  newScope.getCountRating() == 0 && newScope.getSpeedRating() == 0 &&
                  neighborAfter.getCountRating() == neighborBefore.getCountRating() &&
                  neighborAfter.getSpeedRating() == neighborBefore.getSpeedRating() )
        {
            // remove both and add a merged scope
            removeInternal( neighborBefore );
            removeInternal( neighborAfter );
            RatedDownloadScope scope1 = new RatedDownloadScope( 
                neighborBefore.getStart(), neighborAfter.getEnd(),
                neighborAfter.getCountRating(), neighborAfter.getSpeedRating() );
            addInternal( scope1 );
        }
        else
        {
            if ( neighborAfter != null)
            {
                if ( newScope.getCountRating() == 0 && newScope.getSpeedRating() == 0 )
                {
                    removeInternal( neighborAfter );
                    RatedDownloadScope scope1 = new RatedDownloadScope( 
                        newScope.getStart(), neighborAfter.getEnd(),
                        neighborAfter.getCountRating(), neighborAfter.getSpeedRating() );
                    add( scope1 );
                }
                else
                {
                    removeInternal( neighborAfter );
                    RatedDownloadScope scope1 = new RatedDownloadScope(
                        neighborAfter.getStart(), newScope.getEnd(),
                        neighborAfter.getCountRating() + newScope.getCountRating(),
                        neighborAfter.getSpeedRating() + newScope.getSpeedRating() );
                    add( scope1 );
                    
                    RatedDownloadScope scope2 = new RatedDownloadScope(
                        newScope.getEnd() + 1, neighborAfter.getEnd(),
                        neighborAfter.getCountRating(), neighborAfter.getSpeedRating() );
                    add( scope2 );
                }
            }
            if ( neighborBefore != null)
            {
                if ( newScope.getCountRating() == 0 && newScope.getSpeedRating() == 0 )
                {
                    removeInternal( neighborBefore );
                    RatedDownloadScope scope1 = new RatedDownloadScope( 
                        neighborBefore.getStart(), newScope.getEnd(),
                        neighborBefore.getCountRating(), neighborBefore.getSpeedRating() );
                    add( scope1 );
                }
                else
                {
                    removeInternal( neighborBefore );                
                    RatedDownloadScope scope1 = new RatedDownloadScope(
                        neighborBefore.getStart(), newScope.getStart() - 1,
                        neighborBefore.getCountRating(), neighborBefore.getSpeedRating() );
                    add( scope1 );
    
                    RatedDownloadScope scope2 = new RatedDownloadScope(
                        newScope.getStart(), neighborBefore.getEnd(),
                        neighborBefore.getCountRating() + newScope.getCountRating(),
                        neighborBefore.getSpeedRating() + newScope.getSpeedRating() );
                    add( scope2 );
                }
            }
        }
    }
    
    public void removeAll( RatedDownloadScopeList ratedScops )
    {
        Iterator<RatedDownloadScope> iterator = ratedScops.getScopeIterator();
        while( iterator.hasNext() )
        {
            RatedDownloadScope scope = iterator.next();
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
    public void remove( RatedDownloadScope removeScope )
    {
        RatedDownloadScope beforeScope = null;
        RatedDownloadScope afterScope = null;
        Iterator<RatedDownloadScope> iterator = scopeList.iterator();
        while ( iterator.hasNext() )
        {
            RatedDownloadScope scope = iterator.next();
            // check for overlap
            if ( scope.isOverlapping( removeScope ) ) 
            {
                // we remove the scope from the list... and check how to split
                iterator.remove();
                if ( removeScope.getEnd() >= scope.getEnd() )
                { // case2 and case3
                    if ( removeScope.getStart() > scope.getStart() )
                    {
                        beforeScope = new RatedDownloadScope( 
                            scope.getStart(), removeScope.getStart() - 1,
                            scope.getCountRating(), scope.getSpeedRating() );
                    }
                    // continue searching for possible case2
                }
                else if ( scope.getStart() >= removeScope.getStart() )
                { // case1 and case2
                    afterScope = new RatedDownloadScope( 
                        removeScope.getEnd() + 1, scope.getEnd(),
                        scope.getCountRating(), scope.getSpeedRating() );
                    // end of removeScope position reached.
                    break;
                }
                else
                {
                    beforeScope = new RatedDownloadScope( 
                        scope.getStart(), removeScope.getStart() - 1, 
                        scope.getCountRating(), scope.getSpeedRating() );
                    afterScope = new RatedDownloadScope( 
                        removeScope.getEnd() + 1, scope.getEnd(),
                        scope.getCountRating(), scope.getSpeedRating() );
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
    
    /**
     * Retains all elements in the retain list.
     * @param retainList
     */
    public void retainAll( DownloadScopeList retainList )
    {
        ListIterator<RatedDownloadScope> thisIterator = scopeList.listIterator();
        boolean removeThis;
        while( thisIterator.hasNext() )
        {
            removeThis = true;
            RatedDownloadScope thisScope = thisIterator.next();
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
                    thisIterator.add( new RatedDownloadScope( retainStart, retainEnd,
                        thisScope.getCountRating(), thisScope.getSpeedRating() ) );
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
    
    public void rateDownloadScopeList( DownloadScopeList downloadScopeList, long speedRateValue )
    {
        for( DownloadScope scope : downloadScopeList )
        {
            rateDownloadScope( scope, speedRateValue );
        }
    }
    
    public void compressByRatings( )
    {
        ListIterator<RatedDownloadScope> iterator = scopeList.listIterator();
        if ( !iterator.hasNext() )
        {
            return;
        }
        RatedDownloadScope prev = iterator.next();
        RatedDownloadScope current;
        while ( iterator.hasNext() )
        {
            current = iterator.next();
            if ( current.getStart() == prev.getEnd() + 1 &&
                 hasCloseToleranze(prev, current, 5.0) )
            {
                iterator.remove();
                iterator.previous();
                iterator.remove();
                int minCount = (int)Math.round( ( prev.getLength() * prev.getCountRating() 
                             + current.getLength() * current.getCountRating() )
                             / (double)(prev.getLength() + current.getLength()) );
                long minSpeed;
                if ( prev.getSpeedRating() == 0 || current.getSpeedRating() == 0 )
                {
                    minSpeed = Math.max( prev.getSpeedRating(), current.getSpeedRating() );
                }
                else
                {
                    minSpeed = Math.min( prev.getSpeedRating(), current.getSpeedRating() );
                }
                RatedDownloadScope scope1 = new RatedDownloadScope( 
                    prev.getStart(), current.getEnd(), minCount, minSpeed );
                iterator.add(scope1);
                current = scope1;
            }
            prev = current;
        }
    }
    
    /**
     * 
     * @param scope1
     * @param scope2
     * @param tolerance the toleranze percentage, value of 4 indicates 4% toleranze.
     * @return
     */
    private boolean hasCloseToleranze( RatedDownloadScope scope1, RatedDownloadScope scope2, 
        double tolerance )
    {
        double maxCount = Math.max( scope1.getCountRating(), scope2.getCountRating() );
        double minCount = Math.min( scope1.getCountRating(), scope2.getCountRating() );
        double threshold = maxCount - (maxCount/100.0*tolerance);
        return threshold < minCount;
    }
    
    private void rateDownloadScope( DownloadScope rateScope, long speedRateValue )
    {
        ArrayList<RatedDownloadScope> scopesToAdd = new ArrayList<RatedDownloadScope>();
        Iterator<RatedDownloadScope> iterator = scopeList.iterator();
        while( iterator.hasNext() )
        {
            RatedDownloadScope scope = iterator.next();
            if( scope.isOverlapping( rateScope ) )
            {                
                int overlapStart = (int) Math.max( scope.getStart(), rateScope.getStart() );
                int overlapEnd = (int) Math.min( scope.getEnd(), rateScope.getEnd() );
                RatedDownloadScope overlapScope = new RatedDownloadScope( 
                    overlapStart, overlapEnd, 1, speedRateValue );
                scopesToAdd.add( overlapScope );
            }
            // we went far enough in the sorted scope list...
            if ( scope.getStart() > rateScope.getEnd() )
            {
                break;
            }
        }
        iterator = scopesToAdd.iterator();
        while( iterator.hasNext() )
        {
            RatedDownloadScope scope = iterator.next();
            add( scope );
        }
    }
    
    /**
     * Should be called once before accessing the sorted rating results.
     * getBestRated(), getWorstRated()
     *
     */
    public void prepareRating()
    {
        compressByRatings();
        ratedScopeList.clear();
        ratedScopeList.addAll( scopeList );
        Collections.sort(ratedScopeList, RATED_DOWNLOAD_SCOPE_COMPARATOR );
        dumpRatings();
    }
    
    public RatedDownloadScope getBestRated()
    {
        if ( ratedScopeList.size() > 0 )
        {
            return ratedScopeList.get( 0 );
        }
        return null;
    }
    
    public RatedDownloadScope getWorstRated()
    {
        if ( ratedScopeList.size() > 0 )
        {
            return ratedScopeList.get( ratedScopeList.size()-1 );
        }
        return null;
    }
    
    /**
     * Returns true if the rated scopes are show a significant difference in 
     * availability, false otherwise.
     * @return true if the rated scopes are show a significant difference in 
     * availability, false otherwise.
     */
    public boolean isRatingFruitful()
    {
        RatedDownloadScope best = getBestRated();
        RatedDownloadScope worst = getWorstRated();
        if ( best == null || worst == null )
        {
            return false;
        }
        return !hasCloseToleranze( best, worst, 15.0 );
    }
    
    public long getAggregatedLength()
    {
        if ( modificationCount == aggregatedLengthModCount )
        {
            return aggregatedLengthCache;
        }
        
        long length = 0;
        Iterator<RatedDownloadScope> iterator = scopeList.iterator();
        while( iterator.hasNext() )
        {
            DownloadScope scope = iterator.next();
            length += scope.getLength();
        }
        aggregatedLengthCache = length;
        aggregatedLengthModCount = modificationCount;
        return length;
    }
    
    /**
     * Returns a new list containing all download scops of this list.
     * @return
     */
    public Iterator<RatedDownloadScope> getScopeIterator()
    {
        return scopeList.listIterator();
    }
    
    public int size()
    {
        return scopeList.size();
    }
    
    public void clear()
    {
        scopeList.clear();
        ratedScopeList.clear();
        aggregatedLengthCache = 0;
        modificationCount = 0;
        aggregatedLengthModCount = 0;
    }
    
    protected void addInternal( RatedDownloadScope scope )
    {
        int index = Collections.binarySearch(scopeList, scope, DOWNLOAD_SCOPE_COMPARATOR );
        assert index < 0;
        scopeList.add( -(index+1), scope);
        modificationCount ++;
    }
    
    protected void removeInternal( RatedDownloadScope scope )
    {
        int index = Collections.binarySearch(scopeList, scope, DOWNLOAD_SCOPE_COMPARATOR );
        assert index >= 0;
        scopeList.remove( index );
        modificationCount ++;
    }
    
    private void dumpRatings()
    {
//        System.out.println( "---------------------------" );
//        Iterator iterator = scopeList.iterator();
//        while( iterator.hasNext() )
//        {
//            System.out.println( "SL: " + iterator.next() );
//        }
//        System.out.println( "---------------------------" );
//        iterator = ratedScopeList.iterator();
//        while( iterator.hasNext() )
//        {
//            System.out.println( "RL: " + iterator.next() );
//        }
//        System.out.println( "---------------------------" );
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
    
    private static class RatedDownloadScopeComparator implements Comparator<RatedDownloadScope>
    {
        public int compare( RatedDownloadScope scope1, RatedDownloadScope scope2 )
        {
            if ( scope1 == scope2 || scope1.equals(scope2) )
            {
                return 0;
            }
            if ( scope1.getCountRating() > scope2.getCountRating() )
            {
                return 1;
            }
            else if ( scope1.getCountRating() < scope2.getCountRating() )
            {
                return -1;
            }
            else
            {
                if( scope1.getSpeedRating() > scope2.getSpeedRating() )
                {
                    return 1;
                }            
                return -1;
            }
        }
    }
}