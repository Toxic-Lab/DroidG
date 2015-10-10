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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import phex.common.FileHandlingException;
import phex.common.RunnerQueueWorker;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.download.ThexVerificationData.ThexData;
import phex.download.strategy.ScopeSelectionStrategy;
import phex.download.strategy.ScopeSelectionStrategyProvider;
import phex.download.swarming.SWDownloadCandidate;
import phex.download.swarming.SWDownloadConstants;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SWDownloadSegment;
import phex.prefs.core.DownloadPrefs;
import phex.thex.TTHashCalcUtils;
import phex.xml.sax.downloads.DDownloadFile;
import phex.xml.sax.downloads.DDownloadScope;

/**
 * A memory representation of the file data that is downloaded. It keeps track
 * of the download scopes and maintains a small memory cache of downloaded data
 * before it is written to disk.
 * Download scopes are hold in the following lists and live cycle:
 * 
 * 1) missingScope List
 *      gets allocated -> blockedScopeList (2)
 * 2) blockedScopeList
 *      gets released 
 *           missing parts -> missingScopeList (1)
 *           finished parts -> bufferedDataScopeList (3)
 * 3) bufferedDataScopeList
 *      gets written to disk -> unverifiedScopeList (4)
 * 4) unverifiedScopeList
 *      gets verified against THEX -> toBeVerifiedScopeList (5)
 *      download finished and no THEX -> finishedScopeList (6)
 * 5) toBeVerifiedScopeList
 *      scopes during verification 
 *      when verification successful -> finishedScopeList (6)
 *      when verification failed -> finishedScopeList (1)
 * 6) finishedScopeList
 *      used to verify if download is complete...
 *      
 * *) finalizationPhaseScopeList
 *      The finalizationPhaseScopeList aggregates all scope lists
 *      from bufferedDataScopeList downwards. It is used to ensure
 *      that double downloaded scopes don't creep into the 
 *      finalization phase and onto the disk.
 *      
 * Locking:
 *   The scope lists have two main areas that get locked during
 *   access. They are represented by the allocationLock and
 *   the finalizationLock. 
 *   The allocationLock must be locked during all access to the 
 *   missingScopeList and blockedScopeList.
 *   The finalizationLock must be locked during all access to the
 *   bufferedDataScopeList, unverifiedScopeList, toBeVerifiedScopeList,
 *   finishedScopeList and finalizationPhaseScopeList.
 *   In case both locks must be used the allocationLock should always 
 *   be locked before locking the finalizationLock, and released vice
 *   versa.
 *     
 */
public class MemoryFile
{
//    private static final Logger logger = LoggerFactory.getLogger( MemoryFile.class );
    
    private ScopeSelectionStrategy scopeSelectionStrategy;
    
    /**
     * A list of missing download scopes.
     */
    private DownloadScopeList missingScopeList;
    
    /**
     * A list of download scopes currently blocked in downloads.
     */
    private DownloadScopeList blockedScopeList;
    
    /**
     * Contains DataDownloadScope with the downloaded data. No DownloadScopeList
     * is used since we don't like to mess with the merging of DirectByteBuffers
     * that are part of the DataDownloadScope. The bufferedDataScopeList is 
     * written to disk in regular intervals.
     */
    private List<DataDownloadScope> bufferedDataScopeList;
    
    /**
     * Counts the number of buffered bytes and ensures that buffer size
     * is limited to configured value.
     * Access needs no locking.
     */
    private BufferVolumeTracker bufferedVolume;
    
    /**
     * A list of unverified and written to disc download scopes.
     */
    private DownloadScopeList unverifiedScopeList;
    
    /**
     * A list of scopes ready to be verified.
     */
    private DownloadScopeList toBeVerifiedScopeList;
    
    /**
     * A list of finished download scopes.
     */
    private DownloadScopeList finishedScopeList;
    
    /**
     * The finalizationPhaseScopeList aggregates all scope lists
     * from bufferedDataScopeList downwards. It is used to ensure
     * that double downloaded scopes don't creep into the 
     * finalization phase and onto the disk.
     */
    private DownloadScopeList finalizationPhaseScopeList;
    
    private final ReentrantLock allocationLock;
    private final ReentrantLock finalizationLock;
    
    /**
     * This list contains rated download scopes representing the availability
     * of scopes from candidates.
     */
    private RatedDownloadScopeList ratedScopeList;
    
    /**
     * The last time the rated download scope list was build.
     */
    private long ratedScopeListBuildTime;
    
    /**
     * If true indicates that the buffers should be writing to disk 
     * from the DownloadDataWriter.
     */
    private boolean isBufferWritingRequested;
    
    /**
     * Indicates it the {@link MemoryFile} has ever allocated a blocked
     * {@link DownloadScope}. This is mainly used to tune performance
     * during the release of scopes.
     */
    private boolean isBlockedScopeAllocated;
    
    private final SWDownloadFile downloadFile;
    private final RunnerQueueWorker downloadVerifyRunner;
    
    public MemoryFile( SWDownloadFile downloadFile, 
        BufferVolumeTracker globalBufferVolumeTracker, 
        DownloadDataWriter downloadDataWriter,
        RunnerQueueWorker downloadVerifyRunner )
    {
        this.downloadFile = downloadFile;
        this.downloadVerifyRunner = downloadVerifyRunner;
        
        isBufferWritingRequested = false;
        isBlockedScopeAllocated = false;
        missingScopeList = new DownloadScopeList();
        blockedScopeList = new DownloadScopeList();
        bufferedDataScopeList = new ArrayList<DataDownloadScope>();
        unverifiedScopeList = new DownloadScopeList();
        toBeVerifiedScopeList = new DownloadScopeList();
        finishedScopeList = new DownloadScopeList();
        finalizationPhaseScopeList = new DownloadScopeList( );
        allocationLock = new ReentrantLock( );
        finalizationLock = new ReentrantLock( );
        
        bufferedVolume = new BufferVolumeTracker(
            globalBufferVolumeTracker,
            DownloadPrefs.MaxWriteBufferPerDownload.get().intValue(), 
            downloadDataWriter );
        
        long fileSize = downloadFile.getTotalDataSize();
        if ( fileSize == SWDownloadConstants.UNKNOWN_FILE_SIZE )
        {
            missingScopeList.add( new DownloadScope( 0, Long.MAX_VALUE ) );
        }
        else
        {
            missingScopeList.add( new DownloadScope( 0, fileSize - 1 ) );
        }
        
        scopeSelectionStrategy = ScopeSelectionStrategyProvider.getAvailBeginRandSelectionStrategy();
    }
    
    public void setScopeSelectionStrategy( ScopeSelectionStrategy strategy )
    {
        scopeSelectionStrategy = strategy;
    }
    
    public ScopeSelectionStrategy getScopeSelectionStrategy()
    {
        return scopeSelectionStrategy;
    }
    
    public void updateFileSize(  )
    {
        long fileSize = downloadFile.getTotalDataSize();
        
        allocationLock.lock();
        try 
        {
            missingScopeList.remove( new DownloadScope( fileSize, Long.MAX_VALUE) );
            blockedScopeList.remove( new DownloadScope( fileSize, Long.MAX_VALUE) );
        } 
        finally 
        {
            allocationLock.unlock();
        }
        
        scopeConsistencyCheck();
    }
    
    /**
     * For user interface... no modifications to the scope list should be done.
     */
    public DownloadScopeList getBlockedScopeList()
    {
        return blockedScopeList;
    }
    
    /**
     * For user interface... no modifications to the scope list should be done.
     */
    public DownloadScopeList getUnverifiedScopeList()
    {
        return unverifiedScopeList;
    }
    
    /**
     * For user interface... no modifications to the scope list should be done.
     */
    public DownloadScopeList getToBeVerifiedScopeList()
    {
        return toBeVerifiedScopeList;
    }
    
    /**
     * For user interface... no modifications to the scope list should be done.
     */
    public DownloadScopeList getFinishedScopeList()
    {
        return finishedScopeList;
    }
    
    public List<DownloadScope> getFinishedScopeListCopy()
    {
        finalizationLock.lock();
        try 
        {
            return finishedScopeList.getScopeListCopy();
        } 
        finally 
        {
            finalizationLock.unlock();
        }
    }
    
    public List<DownloadScope> getUnverifiedScopeListCopy()
    {
        finalizationLock.lock();
        try 
        {
            return unverifiedScopeList.getScopeListCopy();
        } 
        finally 
        {
            finalizationLock.unlock();
        }
    }
    
    /**
     * Returns the number of downloaded fragments.
     * @return the number of downloaded fragments.
     */
    public int getDownloadedFragmentCount()
    {
        finalizationLock.lock();
        try 
        {
            return finishedScopeList.size() + unverifiedScopeList.size() + toBeVerifiedScopeList.size();
        } 
        finally 
        {
            finalizationLock.unlock();
        }
    }
    
    /**
     * The aggregated length of finished download fragments.
     * @return aggregated length of finished download fragments.
     */
    public long getFinishedLength()
    {
        finalizationLock.lock();
        try 
        {
            return finishedScopeList.getAggregatedLength();
        } 
        finally 
        {
            finalizationLock.unlock();
        }
    }
    
    /**
     * The aggregated length of missing download fragments.
     * @return aggregated length of missing download fragments.
     */
    public long getMissingLength()
    {
        allocationLock.lock();
        try 
        {
            return missingScopeList.getAggregatedLength();
        } 
        finally 
        {
            allocationLock.unlock();
        }
    }
    
    private boolean isComplete()
    {
        finalizationLock.lock();
        try 
        {
            return finishedScopeList.getAggregatedLength() == downloadFile.getTotalDataSize();
        } 
        finally 
        {
            finalizationLock.unlock();
        }
    }
    
    /**
     * Checks if the beginning of the file (scope starting at byte 0) is 
     * available as unverified or finished scope. This info is used to check
     * if a file preview would be possible.
     * @return true if file beginning is available, false otherwise.
     */
    public boolean isFileBeginningAvailable( )
    {
        finalizationLock.lock();
        try 
        {
            if ( unverifiedScopeList.size() > 0 )
            {
                DownloadScope scope = unverifiedScopeList.getScopeAt( 0 );
                if ( scope.getStart() == 0 )
                {// file beginning available.
                    return true;
                }
            }
            if ( finishedScopeList.size() > 0 )
            {
                DownloadScope scope = finishedScopeList.getScopeAt( 0 );
                if ( scope.getStart() == 0 )
                {// file beginning available.
                    return true;
                }
            }
            if ( toBeVerifiedScopeList.size() > 0 )
            {
                DownloadScope scope = toBeVerifiedScopeList.getScopeAt( 0 );
                if ( scope.getStart() == 0 )
                {// file beginning available.
                    return true;
                }
            }
            return false;
        } 
        finally 
        {
            finalizationLock.unlock();
        }
    }
    
    public long getFileBeginningScopeLength()
    {
        finalizationLock.lock();
        try 
        {
            // build a scope list containing all available scopes.
            DownloadScopeList scopeList = (DownloadScopeList) finishedScopeList.clone();
            scopeList.addAll( unverifiedScopeList );
            scopeList.addAll( toBeVerifiedScopeList );
            
            DownloadScope startScope = scopeList.getScopeAt( 0 );
            // no matching start scope found...
            if ( startScope == null || startScope.getStart() != 0 )
            {
                return 0;
            }
            return startScope.getLength();
        } 
        finally 
        {
            finalizationLock.unlock();
        }
    }
    
    public RatedDownloadScopeList getRatedScopeList()
    {
        long now = System.currentTimeMillis();
        if ( ratedScopeListBuildTime + SWDownloadConstants.RATED_SCOPE_LIST_TIMEOUT > now )
        {
            return ratedScopeList;
        }
        if ( ratedScopeList == null )
        {
            ratedScopeList = new RatedDownloadScopeList();
        }
        else
        {
            ratedScopeList.clear();
        }
        ratedScopeList.addAll( missingScopeList );
        downloadFile.rateDownloadScopeList( ratedScopeList );
        ratedScopeListBuildTime = System.currentTimeMillis();
        
        return ratedScopeList;
    }
    
    /**
     * Used to check if a scope is allocatable. This check is done early
     * before a connection to a candidate is opened. The actual allocation happens
     * after the connection to the candidate is established. Though it can happen
     * that all scopes are already allocated until then.
     * @param candidateScopeList the ranges that are wanted for this download if
     *        set to null all ranges are allowed.
     * @return true if there is a scope available. false otherwise.
     */
    public boolean isMissingScopeAllocateable( DownloadScopeList candidateScopeList, 
        boolean isCandidateScopeComplete, boolean includeBlocked )
    {
        allocationLock.lock();
        try 
        {
            if ( missingScopeList.isEmpty() && blockedScopeList.isEmpty() )
            {
                return false;
            }
            if ( missingScopeList.isEmpty() && 
                 downloadFile.getTotalDataSize() == SWDownloadConstants.UNKNOWN_FILE_SIZE )
            {
                return false;
            }
            if (  downloadFile.getTotalDataSize() != SWDownloadConstants.UNKNOWN_FILE_SIZE 
                 && candidateScopeList != null && isCandidateScopeComplete )
            {
                DownloadScopeList wantedScopeList = (DownloadScopeList) missingScopeList.clone();
                if ( includeBlocked )
                {
                    wantedScopeList.addAll( blockedScopeList );
                }
                wantedScopeList.retainAll( candidateScopeList );
                return wantedScopeList.size() > 0;
            }
            else
            {
                return true;
            }
        } 
        finally 
        {
            allocationLock.unlock();
        }       
    }
    
    /**
     * Allocates a missing {@link DownloadScope} trying to match the 
     * preferredSize.
     * @param preferredSize the preferredSize of the scope to allocate.
     * @return a missing scope ready to download.
     */
    public DownloadScope allocateMissingScope( long preferredSize )
    {
        allocationLock.lock();
        try 
        {
            if ( missingScopeList.isEmpty() )
            {
                return null;
            }
            DownloadScope scope = missingScopeList.getScopeAt( 0 );
            missingScopeList.remove( scope );
            synchronized( scope )
            {
                // ignore preferred size if fileSize is unknown
                if ( downloadFile.getTotalDataSize() != SWDownloadConstants.UNKNOWN_FILE_SIZE && 
                     scope.getLength() > preferredSize )
                {
                    DownloadScope beforeScope = new DownloadScope( 
                        scope.getStart(), scope.getStart() + preferredSize - 1 );
                    DownloadScope afterScope = new DownloadScope( 
                        scope.getStart() + preferredSize, scope.getEnd() );
                    missingScopeList.add( afterScope );
                    scope = beforeScope;
                }
            }
            blockedScopeList.add( scope );
            return scope;
        }
        finally
        {
            allocationLock.unlock();
            scopeConsistencyCheck();
        }
    }
    
    public DownloadScope allocateMissingScopeForCandidate( DownloadScopeList candidateScopeList,
        boolean isCandidateScopeComplete, long preferredSize )
    {
        allocationLock.lock();
        
        try 
        {
            int segmentMultiple = DownloadPrefs.SegmentMultiple.get().intValue();
            
            // adjust preferredSize to allocation situation.
            long missingLength = missingScopeList.getAggregatedLength();
            if ( missingLength < preferredSize*2 )
            {
                // only take half of the size that is left.
                preferredSize = Math.max( missingLength / 2, segmentMultiple );
            }
            
            DownloadScopeList wantedScopeList = (DownloadScopeList)missingScopeList.clone();
            wantedScopeList.addAll( missingScopeList );
            wantedScopeList.retainAll( candidateScopeList );
            if ( wantedScopeList.size() > 0 )
            {
                DownloadScope scope = scopeSelectionStrategy.selectDownloadScope( 
                    downloadFile, wantedScopeList, preferredSize );
                if ( scope != null )
                {
                    missingScopeList.remove( scope );
                    blockedScopeList.add( scope );
                    return scope;
                }
            }
            
            // no matching scope found... do we have the complete scope of the candidate?
            if ( !isCandidateScopeComplete )
            {
                // remove all scopes we know of the candidate and try some unknown...
                wantedScopeList = (DownloadScopeList)missingScopeList.clone();
                wantedScopeList.removeAll( candidateScopeList );
                if ( wantedScopeList.size() > 0 )
                {
                    DownloadScope scope = scopeSelectionStrategy.selectDownloadScope( 
                        downloadFile, wantedScopeList, preferredSize );
                    if ( scope != null )
                    {
                        missingScopeList.remove( scope );
                        blockedScopeList.add( scope );
                        return scope;
                    }
                }
            }
            
            
            // don't touch blocked scope list in case we would still have missing scope
            // available but just nothing is matching this candidate.
            if ( missingLength > 0 )
            {
                return null;
            }
            
// for now... disable double scope allocation... to risky...
            return null;
            
//            wantedScopeList = (DownloadScopeList)blockedScopeList.clone();
//            wantedScopeList.addAll( blockedScopeList );
//            wantedScopeList.retainAll( candidateScopeList );
//            if ( wantedScopeList.size() == 0 )
//            {
//                return null;
//            }
//
//            // find the slowest candidate
//            SWDownloadCandidate slowestCandidate = null;
//            long slowestSpeed = Integer.MAX_VALUE;
//            DownloadScope slowestScope = null;
//            for ( SWDownloadCandidate candidate : downloadFile.getAllocatedCandidates() )
//            {
//                SWDownloadSegment downloadSegment = candidate.getDownloadSegment();
//                if ( downloadSegment == null )
//                {
//                    continue;
//                }
//                long start = downloadSegment.getTransferStartPosition();
//                long end = downloadSegment.getEnd();
//                DownloadScope scope = new DownloadScope( start, end );
//                if ( !wantedScopeList.contains( scope ) || scope.getLength() < segmentMultiple*2 )
//                {
//                    // not a matching candidate
//                    continue;
//                }
//                // average speed of this transfer, and the last transfer, if available ( >0 ) 
//                long speed = downloadSegment.getTransferSpeed();
//                long candidateSpeed = candidate.getSpeed();
//                if ( candidateSpeed > 0 )
//                {
//                    speed = (speed + candidateSpeed) / 2;
//                }
//                if ( speed < 1 )
//                {
//                    // no data yet... max out...
//                    speed = Integer.MAX_VALUE;
//                }
//                if ( slowestCandidate == null || slowestSpeed > candidateSpeed )
//                {
//                    slowestCandidate = candidate;
//                    slowestSpeed = speed;
//                    slowestScope = scope;
//                }
//            }
//            
//            if ( slowestScope == null )
//            {
//                return null;
//            }
//            
//            // split slowest scope...
//            long splitPoint = slowestScope.getStart() + slowestScope.getLength() / 2;
//            DownloadScope scope = new DownloadScope( splitPoint, slowestScope.getEnd() );
//            isBlockedScopeAllocated = true;
//            return scope;
        }
        finally
        {
            allocationLock.unlock();
            scopeConsistencyCheck();
        }
    }
    
    /**
     * Releases the missing part of the download scope. The downloaded part
     * is handled through the buffered data writer.
     */
    public void releaseAllocatedScope( DownloadScope downloadScope, 
        long transferredSize, SWDownloadCandidate downloadCandidate )
    {      
        if ( downloadScope.getEnd() == Long.MAX_VALUE
            && downloadFile.getTotalDataSize() != SWDownloadConstants.UNKNOWN_FILE_SIZE )
        {
            downloadScope = new DownloadScope( downloadScope.getStart(),
                downloadFile.getTotalDataSize() - 1 );
        }
        
        
        DownloadScope unblockScope;
        if ( transferredSize == 0 )
        {// just give back the scope for unblock.
            unblockScope = downloadScope;
        }
        else if ( transferredSize < downloadScope.getLength() )
        {
            unblockScope = new DownloadScope( downloadScope.getStart()
                + transferredSize, downloadScope.getEnd() );
        }
        else
        {
            // fully downloaded scope nothing to unblock
            return;
        }
        
        if ( isBlockedScopeAllocated )
        {
            releaseMultiBlockedAllocScope( unblockScope, downloadCandidate );
            releaseMultiBlockedAllocScope( unblockScope, downloadCandidate );
        }
        else
        {
            releaseSingleAllocScope( unblockScope );
        }
    }
    
    private void releaseMultiBlockedAllocScope( DownloadScope unblockScope,
       SWDownloadCandidate downloadCandidate)
    {
        allocationLock.lock();
        try 
        {
            finalizationLock.lock();
            try 
            {
                // allocationLock must include this block otherwise candidate segment
                // alloc/dealoc could happen concurrently
                DownloadScopeList unblockScopeList = new DownloadScopeList( );
                unblockScopeList.add( unblockScope );

                // we need to ensure that the scope we release dosn't contain data 
                // already downloaded through a different candidate.
                unblockScopeList.removeAll( finalizationPhaseScopeList );
                
                // we might have allocated a blocked scoped which could be in use by 
                // other allocated candidates... the unblocking gets more complex, 
                // since we don't like to unblocked still allocated scopes.
                long scopeStart = unblockScope.getStart();
                long scopeEnd = unblockScope.getEnd();
                List<SWDownloadCandidate> allocCandidatesList = downloadFile.getAllocatedCandidates();
                for( SWDownloadCandidate allocCandidate : allocCandidatesList )
                {
                    if ( allocCandidate == downloadCandidate )
                    {
                        continue;
                    }
                    SWDownloadSegment downloadSegment = allocCandidate.getDownloadSegment();
                    if ( downloadSegment == null )
                    {
                        continue;
                    }
                    long segStart = downloadSegment.getStart();
                    long segEnd = downloadSegment.getEnd();
                    if ( segEnd == -1 )
                    {
                        segEnd = Long.MAX_VALUE;
                    }
                    // not overlapping.. skip..
                    if ( !(scopeEnd >= segStart && scopeStart <= segEnd ) )
                    {
                        continue;
                    }
                    if ( segStart <= scopeStart && segEnd >= scopeEnd )
                    {// segment contains scope... nothing to be done...
                        return;
                    }
                    unblockScopeList.remove( new DownloadScope( segStart, segEnd ) );
                }
            
                if ( unblockScopeList.size() > 0 )
                {
                    blockedScopeList.removeAll( unblockScopeList );
                    missingScopeList.addAll( unblockScopeList );
                }
                scopeConsistencyCheck();
            }
            finally
            {
                finalizationLock.unlock();
            }
        }
        finally
        {
            allocationLock.unlock();
            scopeConsistencyCheck();
        }
    }

    private void releaseSingleAllocScope(DownloadScope unblockScope)
    {
        allocationLock.lock();
        try 
        {
            blockedScopeList.remove( unblockScope );
            missingScopeList.add( unblockScope );
        }
        finally
        {
            allocationLock.unlock();
            scopeConsistencyCheck();
        }
    }

    public long getDownloadedLength()
    {
        finalizationLock.lock();
        try 
        {
            long length = unverifiedScopeList.getAggregatedLength();
            length += toBeVerifiedScopeList.getAggregatedLength();
            length += finishedScopeList.getAggregatedLength();
            length += bufferedVolume.getUsedBufferSize();
            return length;
        }
        finally
        {
            finalizationLock.unlock();
        }
    }
    
    public int getBufferedDataLength()
    {
        return bufferedVolume.getUsedBufferSize();
    }
    
    public void bufferDataScope( DataDownloadScope dataScope ) throws IOException
    {
        allocationLock.lock();
        try 
        {
            finalizationLock.lock();
            try 
            {
                for ( DownloadScope finScope : finalizationPhaseScopeList )
                {
                    if ( dataScope.isOverlapping( finScope ) )
                    {
                        // scope is overlapping a scope already in the fin phase..
                        // this is problematic... how is the split case handled??
                        throw new IOException( "Double download." ); 
                    }
                    if ( finScope.getStart() > dataScope.getEnd() )
                    {// performance optimization.. scopes are sorted...
                        break;
                    }
                }
                // dataScope not in fin list.. add it..
                finalizationPhaseScopeList.add( dataScope );
                
                // make sure any buffered data is removed from 
                // missingScopeList and blockedScopeList.
                // This prevents it from getting allocated again.
                // Removing from missing might be unnecessary.. 
                // but it wont hurt.
                missingScopeList.remove( dataScope );
                blockedScopeList.remove( dataScope );
                bufferedDataScopeList.add( dataScope );
            }
            finally
            {
                finalizationLock.unlock();
            }
        }
        finally
        {
            allocationLock.unlock();
            scopeConsistencyCheck();
        }
        assert dataScope.getLength() < Integer.MAX_VALUE;
        // this must happen after releasing locks.. otherwise
        // deadlock occurs in bufferedVolumne when full.
        bufferedVolume.addBufferedSize( (int)dataScope.getLength() );
    }
    
    public boolean isBufferWritingRequested()
    {
        return isBufferWritingRequested;
    }
    
    /**
     * Requests that the buffers of this MemoryFile are written to disk from the 
     * DownloadDataWriter thread.
     */
    public void requestBufferWriting()
    {
        //logger.debug( "MemoryFile requesting buffer write." );
        isBufferWritingRequested = true;
    }
    
    /**
     * This method should only be called by the single DownloadDataWriter thread,
     * to ensure integrity. This method is not able to handle multiple thread access
     * to its data scopes.
     */
    public void writeBuffersToDisk()
    {
        if ( bufferedDataScopeList.isEmpty() )
        {
            return;
        }
        //logger.debug( "MemoryFile write buffers to disk." );
        isBufferWritingRequested = false;
        try
        {
            ManagedFile destFile = downloadFile.getIncompleteDownloadFile();
            List<DataDownloadScope> list;
            
            finalizationLock.lock();
            try 
            {
                list = new ArrayList<DataDownloadScope>( bufferedDataScopeList );
            }
            finally
            {
                finalizationLock.unlock();
            }

            for( DataDownloadScope dataScope : list )
            {
                // if any part of the dataScope is already finished, 
                // unverified, tobeverified 
                
                destFile.write( dataScope.getDataBuffer(), dataScope.getStart() );
                
                finalizationLock.lock();
                try 
                {
                    bufferedDataScopeList.remove( dataScope );
                    //bufferedScopeList.remove( dataScope );
                    unverifiedScopeList.add( dataScope );
                }
                finally
                {
                    finalizationLock.unlock();
                }
                
                assert dataScope.getLength() < Integer.MAX_VALUE;
                bufferedVolume.reduceBufferedSize( (int)dataScope.getLength() );
                
                // release scope buffer. After releasing we can use the
                // DataDownloadScope like a simple DownloadScope.
                dataScope.releaseDataBuffer();
            }
            
            findScopesToVerify();
            
            scopeConsistencyCheck();
            
            if ( !downloadFile.isFileCompletedOrMoved() && isComplete() )
            {
                downloadFile.setStatus( SWDownloadConstants.STATUS_FILE_COMPLETED );
                downloadFile.moveToDestinationFile();
            }
        }
        catch ( FileHandlingException exp )
        {
            // this exp stops the download in download file...
            //logger.error( exp.toString(), exp );
        }
        catch ( ManagedFileException exp )
        {
            //logger.error( exp.toString(), exp );
        }
    }
    
    private void findScopesToVerify()
    {
        List<DownloadScope> verifyableScopes;
        finalizationLock.lock();
        try 
        {
            verifyableScopes = new ArrayList<DownloadScope>();
            ThexData thexData = downloadFile.getThexVerificationData().getThexData();
            if ( thexData == null )
            {
                if ( unverifiedScopeList.getAggregatedLength() == downloadFile.getTotalDataSize() )
                {// the download has completed without having verification data available...
                 // move all scopes to finished...
                    List<DownloadScope> scopes = unverifiedScopeList.getScopeListCopy();
                    for ( DownloadScope scope : scopes )
                    {
                        unverifiedScopeList.remove( scope );
                        finishedScopeList.add( scope );
                    }
                    if ( !downloadFile.isFileCompletedOrMoved() && isComplete() )
                    {
                        downloadFile.setStatus( SWDownloadConstants.STATUS_FILE_COMPLETED );
                        downloadFile.moveToDestinationFile();
                    }
                }
                return;
            }
            long totalFileSize = downloadFile.getTotalDataSize();
            int nodeSize = thexData.getNodeSize();
            
            long lastNodeStart = totalFileSize - ( totalFileSize % nodeSize );
            if ( lastNodeStart == totalFileSize )
            {
                lastNodeStart -= nodeSize;
            }
    
            for ( DownloadScope scope : unverifiedScopeList )
            {
                boolean isLastScope = scope.getEnd()+1 == totalFileSize;
                
                if ( !isLastScope && scope.getLength() < nodeSize  )
                {
                    continue;
                }

                // calc start offset of thex node
                long nodeStart;
                if ( scope.getStart() % nodeSize == 0 )
                {
                    nodeStart = scope.getStart();
                }
                else if ( isLastScope )
                {// special handling for start of last node since it might not be a full nodeSize large.
                    nodeStart = totalFileSize - ( totalFileSize % nodeSize );
                    if ( nodeStart == totalFileSize )
                    {
                        nodeStart -= nodeSize;
                    }
                }
                else
                {
                    nodeStart = scope.getStart() + nodeSize - scope.getStart() % nodeSize; 
                }
                long nodeEnd;
                if ( (scope.getEnd()+1) % nodeSize == 0 || isLastScope )
                {
                    nodeEnd = scope.getEnd();
                }
                else
                {
                    nodeEnd = scope.getEnd() - 1 - (scope.getEnd() % nodeSize);
                }
                if ( nodeEnd - nodeStart + 1 >= nodeSize && (nodeEnd - nodeStart+1)%nodeSize == 0 )
                { 
                    long rangeStart = nodeStart;
                    long rangeEnd;
                    do
                    {
                        rangeEnd = rangeStart + nodeSize - 1;
                        verifyableScopes.add( new DownloadScope( rangeStart, rangeEnd ) );
                        rangeStart = rangeEnd + 1;
                    }
                    while( rangeStart < nodeEnd );
                }
                else if ( isLastScope && scope.getStart() <= nodeStart )
                {
                    verifyableScopes.add( new DownloadScope( nodeStart, nodeEnd ) );
                }
            }
            if ( verifyableScopes.size() > 0 )
            {
                for ( DownloadScope scope : verifyableScopes )
                {
                    unverifiedScopeList.remove( scope );
                    toBeVerifiedScopeList.add( scope );
                    downloadVerifyRunner.add( 
                        new DownloadVerificationWorker( scope ) );
                }
            }
            
        }
        finally
        {
            finalizationLock.unlock();
            scopeConsistencyCheck();
        }
    }
    
    private void verifyScope( DownloadScope scope )
    {
        try
        {
            ManagedFile destFile = downloadFile.getIncompleteDownloadFile();
            boolean succ = TTHashCalcUtils.verifyTigerTreeHash( downloadFile.getThexVerificationData().getThexData(), 
                destFile, scope.getStart(), scope.getLength() );
            
            if ( succ )
            {
                finalizationLock.lock();
                try 
                {
                    toBeVerifiedScopeList.remove( scope );
                    finishedScopeList.add( scope );
                }
                finally
                {
                    finalizationLock.unlock();
                }
            }
            else
            {
                allocationLock.lock();
                try 
                {
                    finalizationLock.lock();
                    try 
                    {
                        toBeVerifiedScopeList.remove( scope );
                        missingScopeList.add( scope );
                        finalizationPhaseScopeList.remove( scope );
                    }
                    finally
                    {
                        finalizationLock.unlock();
                    }
                }
                finally
                {
                    allocationLock.unlock();
                }
            }
        }
        catch ( FileHandlingException exp )
        {
            // this exp should stop the download in download file...
            //logger.error( exp.toString(), exp );
        }
        catch ( ManagedFileException exp )
        {
            //logger.error( exp.toString(), exp );
        }
        finally
        {
            scopeConsistencyCheck();
        }
        
        if ( !downloadFile.isFileCompletedOrMoved() && isComplete() )
        {
            downloadFile.setStatus( SWDownloadConstants.STATUS_FILE_COMPLETED );
            downloadFile.moveToDestinationFile();
        }
    }
    
    ////////////////////////// XJB stuff ///////////////////////////////////////
    
    public void createDownloadScopes( DDownloadFile dFile )
    {
        //clean scopes
        missingScopeList.clear();
        blockedScopeList.clear();
        unverifiedScopeList.clear();
        toBeVerifiedScopeList.clear();
        finishedScopeList.clear();
        
        setScopeSelectionStrategy( ScopeSelectionStrategyProvider.getByClassName( 
            dFile.getScopeSelectionStrategy() ) );
        
        Iterator<DDownloadScope> iterator = dFile.getUnverifiedScopesList().getSubElementList().iterator();
        while( iterator.hasNext() )
        {
            DDownloadScope dScope = iterator.next();
            DownloadScope downloadScope = new DownloadScope( dScope.getStart(),
                dScope.getEnd() );
            unverifiedScopeList.add( downloadScope );
        }
        
        iterator = dFile.getFinishedScopesList().getSubElementList().iterator();
        while( iterator.hasNext() )
        {
            DDownloadScope dScope = iterator.next();
            DownloadScope downloadScope = new DownloadScope( dScope.getStart(),
                dScope.getEnd() );
            finishedScopeList.add( downloadScope );
        }
        
        // build missing scope list.
        long fileSize = downloadFile.getTotalDataSize();
        if ( fileSize == SWDownloadConstants.UNKNOWN_FILE_SIZE )
        {
            missingScopeList.add( new DownloadScope( 0, Long.MAX_VALUE ) );
        }
        else
        {
            missingScopeList.add( new DownloadScope( 0, fileSize - 1 ) );
        }
        // remove finished scopes...
        missingScopeList.removeAll( unverifiedScopeList );
        missingScopeList.removeAll( finishedScopeList );
        
        finalizationPhaseScopeList.addAll( unverifiedScopeList );
        finalizationPhaseScopeList.addAll( finishedScopeList );
        
        scopeConsistencyCheck();
    }
    
    public void createXJBFinishedScopes( DDownloadFile dFile )
    {
        dFile.setScopeSelectionStrategy( scopeSelectionStrategy.getClass().getName() );
        
        List<DDownloadScope> list = dFile.getUnverifiedScopesList().getSubElementList();
        
        // copy scope list to prevent ConcurrentModificationException during iteration.
        List<DownloadScope> unvScopeListCopy = getUnverifiedScopeListCopy();
        for( DownloadScope scope : unvScopeListCopy )
        {
            DDownloadScope dScope = new DDownloadScope( 
                DDownloadScope.UNVERIFIED_SCOPE_ELEMENT_NAME );
            dScope.setStart( scope.getStart() );
            dScope.setEnd( scope.getEnd() );
            list.add( dScope );
        }
        
        list = dFile.getFinishedScopesList().getSubElementList();
        
        // copy finished scope list to prevent ConcurrentModificationException during iteration.
        List<DownloadScope> finScopeListCopy = getFinishedScopeListCopy();
        for( DownloadScope scope : finScopeListCopy )
        {
            DDownloadScope dScope = new DDownloadScope( 
                DDownloadScope.FINISHED_SCOPE_ELEMENT_NAME );
            dScope.setStart( scope.getStart() );
            dScope.setEnd( scope.getEnd() );
            list.add( dScope );
        }
    }
    
    /**
        * 1) missingScope List
        *      gets allocated -> blockedScopeList (2)
        * 2) blockedScopeList
        *      gets released 
        *           missing parts -> missingScopeList (1)
        *           finished parts -> bufferedDataScopeList (3)
        * 3) bufferedDataScopeList
        *      gets written to disk -> unverifiedScopeList (4)
        * 4) unverifiedScopeList
        *      gets verified against THEX -> toBeVerifiedScopeList (5)
        *      download finished and no THEX -> finishedScopeList (6)
        * 5) toBeVerifiedScopeList
        *      scopes during verification 
        *      when verification successful -> finishedScopeList (6)
        *      when verification failed -> finishedScopeList (1)
        * 6) finishedScopeList
        *      used to verify if download is complete...
        *      
        * *) finalizationPhaseScopeList
        *      The finalizationPhaseScopeList aggregates all scope lists
        *      from bufferedDataScopeList downwards. It is used to ensure
        *      that double downloaded scopes don't creep into the 
        *      finalization phase and onto the disk.
        */
    private void scopeConsistencyCheck()
    {
        allocationLock.lock();
        try 
        {
            finalizationLock.lock();
            try 
            {
                // adding all scopes to a full file should result in a 
                // single scope of the full file size...
                DownloadScopeList fullFile = new DownloadScopeList();
                fullFile.addAll( missingScopeList );
                fullFile.addAll( blockedScopeList );
                for( DownloadScope scope : bufferedDataScopeList )
                {
                    fullFile.add( scope );
                }
                fullFile.addAll( unverifiedScopeList );
                fullFile.addAll( toBeVerifiedScopeList );
                fullFile.addAll( finishedScopeList );
                DownloadScope fullScope = fullFile.getScopeAt( 0 );
                if ( fullScope.getStart() != 0 || fullScope.getEnd() != downloadFile.getTotalDataSize() -1 )
                {
                    System.out.println("ouch1");
                    //throw new RuntimeException();
                }
                
                // missing and blocked scope length should not overlap each other.
                fullFile = new DownloadScopeList();
                fullFile.addAll( missingScopeList );
                fullFile.addAll( blockedScopeList );
                long expLength = missingScopeList.getAggregatedLength() 
                    + blockedScopeList.getAggregatedLength();
                if ( expLength != fullFile.getAggregatedLength() )
                {
                    System.out.println("ouch2");
                }
                expLength += unverifiedScopeList.getAggregatedLength();
                fullFile.addAll( unverifiedScopeList );
                if ( expLength != fullFile.getAggregatedLength() )
                {
                    System.out.println("ouch3");
                }
                
                expLength += toBeVerifiedScopeList.getAggregatedLength();
                fullFile.addAll( toBeVerifiedScopeList );
                if ( expLength != fullFile.getAggregatedLength() )
                {
                    System.out.println("ouch4");
                }
                
                expLength += finishedScopeList.getAggregatedLength();
                fullFile.addAll( finishedScopeList );
                if ( expLength != fullFile.getAggregatedLength() )
                {
                    System.out.println("ouch5");
                }
                
                
                
                // check finalization phase aggregation
                expLength = 0;
                fullFile = new DownloadScopeList();
                
                expLength += unverifiedScopeList.getAggregatedLength();
                fullFile.addAll( unverifiedScopeList );
                
                expLength += toBeVerifiedScopeList.getAggregatedLength();
                fullFile.addAll( toBeVerifiedScopeList );
                
                expLength += finishedScopeList.getAggregatedLength();
                fullFile.addAll( finishedScopeList );
                
                for( DownloadScope scope : bufferedDataScopeList )
                {
                    fullFile.add( scope );
                    expLength += scope.getLength();
                }
                
                if ( expLength != fullFile.getAggregatedLength() )
                {
                    System.out.println("ouch6");
                }
                
                if ( expLength != finalizationPhaseScopeList.getAggregatedLength() )
                {
                    System.out.println("ouch7");
                }
            }
            finally
            {
                finalizationLock.unlock();
            }
        }
        finally
        {
            allocationLock.unlock();
        }
    }
    
    public class DownloadVerificationWorker implements Runnable
    {
        private DownloadScope scope;
        
        public DownloadVerificationWorker( DownloadScope scope )
        {
            this.scope = scope;
        }
        
        public void run()
        {
            try
            {
                verifyScope( scope );
            }
            catch ( Throwable th )
            {
                // this is a very bad error situation...
                // it could break the download scope consistency!
                //logger.error( "Download scope consistency in danger!" );
                //logger.error( th.toString(), th );
            }
        }
    }
}