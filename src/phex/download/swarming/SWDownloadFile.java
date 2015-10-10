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

package phex.download.swarming;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.lang.time.DateUtils;

import phex.common.AddressCounter;
import phex.common.AltLocContainer;
import phex.common.AlternateLocation;
import phex.common.Environment;
import phex.common.FileHandlingException;
import phex.common.MediaType;
import phex.common.Phex;
import phex.common.TransferDataProvider;
import phex.common.URN;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.MalformedDestAddressException;
import phex.common.bandwidth.BandwidthController;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.MagnetData;
import phex.download.MemoryFile;
import phex.download.RatedDownloadScopeList;
import phex.download.RemoteFile;
import phex.download.ThexVerificationData;
import phex.download.swarming.SWDownloadCandidate.CandidateStatus;
import phex.event.ChangeEvent;
import phex.event.ContainerEvent;
import phex.event.EventHandler;
import phex.event.PhexEventService;
import phex.event.PhexEventTopics;
import phex.event.UserMessageListener;
import phex.event.ContainerEvent.Type;
import phex.http.HTTPRangeSet;
import phex.prefs.core.DownloadPrefs;
import phex.query.ResearchSetting;
import phex.servent.Servent;
import phex.statistic.SimpleStatisticProvider;
import phex.statistic.StatisticsManager;
import phex.utils.FileUtils;
import phex.utils.StringUtils;
import phex.utils.URLUtil;
import phex.xml.sax.downloads.DDownloadCandidate;
import phex.xml.sax.downloads.DDownloadFile;

/**
 * 
 */
public class SWDownloadFile implements TransferDataProvider, SWDownloadConstants, EventHandler
{
    //private static final Logger logger = LoggerFactory.getLogger( SWDownloadFile.class );
    
    private static Random random = new Random();
    
    private final SwarmingManager downloadService;
    private final PhexEventService eventService;
    
    private MemoryFile memoryFile;
    
    /**
     * Alternate location container which holds all correctly validate alt locs.
     * A validated alt loc is one which was proved to connect correctly during
     * the running session.
     */
    private AltLocContainer goodAltLocContainer;
    
    /**
     * Alternate location container which holds all bad alt locs. A bad alt loc
     * is one which was proved to not be reachable during the running session.
     */
    private AltLocContainer badAltLocContainer;
    
    /**
     * A lock object used to lock access to good and bad download candidate lists.
     */
    private Object candidatesLock = new Object();
    
    /**
     * A list of all download candidates. On access candidatesLock should be locked. 
     */
    private final List<SWDownloadCandidate> allCandidatesList;
    
    /**
     * A list of good download candidates for this download file. Good 
     * candidates are candidates that have been verified to be available,
     * or have been available in the last 24h after a restart of Phex.
     * 
     * 60% of connect tries will go to good candidates. When no good
     * candidates are available we try medium candidates.
     * 
     * On access candidatesLock should be locked.
     */
    private final List<SWDownloadCandidate> goodCandidatesList;
    
    /**
     * A list of medium download candidates for this download file. Medium 
     * candidates are candidates that have not been verified to be available and
     * have not reached the bad candidates requirements yet.
     * 
     * 30% of connect tries will go to medium candidates. When no medium
     * candidates are available we try bad candidates.
     * 
     * On access candidatesLock should be locked.
     */
    private final List<SWDownloadCandidate> mediumCandidatesList;
     
    /**
     * A list of known bad download candidates for this download file. Most 
     * likely they are offline. A candidate reaches bad candidate status if it
     * has failed to connect more then 3 times or is a ignored candidate.
     * 
     * Bad candidates will not be retried for a long time, they have a high
     * error status timeout. Only 10% of connect tries will go to bad
     * candidates.
     * 
     * On access candidatesLock should be locked.
     */
    private final List<SWDownloadCandidate> badCandidatesList;
    
    /**
     * CandidateAllocator for good quality candidates.
     */
    private final CandidateAllocator goodCandidateAllocator;
    
    /**
     * CandidateAllocator for medium quality candidates.
     */
    private final CandidateAllocator mediumCandidateAllocator;
    
    /**
     * CandidateAllocator for bad quality candidates.
     */
    private final CandidateAllocator badCandidateAllocator;
    
    
    /**
     * A list of download candidates that are currently in status:
     * CONNECTING
     * ALLOCATING_SEGMENT
     * PUSH_REQUEST
     * REMOTLY_QUEUED
     * REQUESTING
     * DOWNLOADING
     * On access candidatesLock should be locked.
     */
    private List<SWDownloadCandidate> transferCandidatesList;
    
    /**
     * A list of download candidates that are queued.
     * On access candidatesLock should be locked.
     */
    private Set<SWDownloadCandidate> queuedCandidatesSet;
    
    /**
     * A hash map of candidate worker associations. They represent from worker 
     * allocated download candidates.
     * Modifying access needs to be locked with candidatesLock
     */
    private Map<SWDownloadCandidate,SWDownloadWorker> allocatedCandidateWorkerMap;
    
    /**
     * The number of candidates currently downloading. This is a cached value
     * to reduce the number of count processes.
     * @see #updateCandidateWorkerCounts()
     */
    private int downloadingCandidateCount;

    /**
     * The number of candidates currently connecting. This is a cached value
     * to reduce the number of count processes.
     * @see #updateCandidateWorkerCounts()
     */
    private int connectingCandidateCount;
    
    /**
     * The number of candidates currently allocating segments. This is a 
     * cached value to reduce the number of count processes.
     */
    private int allocatingSegmentCandidateCount;

    /**
     * Indicates the last update of downloadingCandidateCount. It should be
     * updated every second (CANDIDATE_WORKER_COUNT_TIMEOUT).
     */
    private long lastCandidateWorkerCountUpdate;
    private static final int CANDIDATE_WORKER_COUNT_TIMEOUT = 1000;

    /**
     * The file size of the download.
     */
    private long fileSize;
    
    /**
     * The name of the download file. It is used to build the full destination
     * path and the incomplete file. 
     */
    private String fileName;

    /**
     * The destination directory of the finished download. In case it is null
     * the default download directory is used to store the finished download
     * file. In case the destination directory is changed by the user this
     * field contains the new destination directory.
     */
    private File destinationDirectory;
    
    /**
     * Used to indicate if the destination is streamable
     */
    private boolean isDestStreamable;
    
    /**
     * The incomplete file of the download. 
     */
    private ManagedFile incompleteManagedFile;

    /**
     * The time indicating when the download file was created for download.
     */
    private Date createdDate;

    /**
     * The time indicating the last modification (download) of the download file.
     */
    private Date modifiedDate;

    /**
     * The status of the download.
     */
    private int status;

    /**
     * Transfer start time
     */
    private long transferStartTime;

    /**
     * Transfer stop time
     */
    private long transferStopTime;

    /**
     * Used to store the current progress.
     */
    private Integer currentProgress;

    /**
     * The number of workers currently working on downloading this file
     */
    private short workerCount;

    /**
     * The URN of the download. This is the most unique identifier of the file
     * network wide. If it is none null we should only accept candidates with
     * the same URN and also add this urn to researchs for better results.
     */
    private URN fileURN;

    /**
     * Settings for the research.
     */
    private ResearchSetting researchSetting;

    /*
     * If preview mode is selected, this is the size at the start of the file to prefer.
     */
    private long previewSize;
    
    /**
     * The download file own bandwidth controller.
     */
    private BandwidthController bandwidthController;
    
    /**
     * Thex verification status tracking.
     * TODO init with stored values!
     */
    private ThexVerificationData thexVerificationData;
    
    private SWDownloadFile( SwarmingManager downloadService, PhexEventService eventService )
    {
        if ( downloadService == null )
        {
            throw new NullPointerException( "DownloadService is null." );
        }
        if ( eventService == null )
        {
            throw new NullPointerException( "eventService is null." );
        }
        this.downloadService = downloadService;
        this.eventService = eventService;
        eventService.register( this, new String[]{PhexEventTopics.Download_Candidate_Status});
        
        allCandidatesList = new ArrayList<SWDownloadCandidate>();
        goodCandidatesList = new ArrayList<SWDownloadCandidate>();
        mediumCandidatesList = new ArrayList<SWDownloadCandidate>();
        badCandidatesList = new ArrayList<SWDownloadCandidate>();
        transferCandidatesList = new ArrayList<SWDownloadCandidate>();
        queuedCandidatesSet = new HashSet<SWDownloadCandidate>();
        allocatedCandidateWorkerMap = new LinkedMap();
        
        goodCandidateAllocator = new CandidateAllocator( goodCandidatesList );
        mediumCandidateAllocator = new CandidateAllocator( mediumCandidatesList );
        badCandidateAllocator = new CandidateAllocator( badCandidatesList );
        
        downloadingCandidateCount = 0;
        allocatingSegmentCandidateCount = 0;
        connectingCandidateCount = 0;
        lastCandidateWorkerCountUpdate = 0;
        currentProgress = Integer.valueOf( 0 );
        createdDate = modifiedDate = new Date( System.currentTimeMillis() );
        thexVerificationData = new ThexVerificationData();
        
        status = STATUS_FILE_WAITING;
        bandwidthController = downloadService.createBandwidthControllerFor( this );
    }
    
    public SWDownloadFile( String filename, String searchString,
        long aFileSize, URN aFileURN, SwarmingManager swMgr,
        PhexEventService eventService )
    {
        this( swMgr, eventService );
        initialize(filename, aFileURN, aFileSize, searchString, true);
        try
        {
            initIncompleteFile();
        }
        catch ( FileHandlingException exp )
        {
            //logger.error( exp.toString(), exp );
        }
        catch ( ManagedFileException exp )
        {
            //logger.error( exp.toString(), exp );
        }
    }
    
    /**
     *
     */
    public SWDownloadFile( URI downloadUri, SwarmingManager swMgr,
        PhexEventService eventService )
        throws URIException
    {
        this( swMgr, eventService );
        String protocol = downloadUri.getScheme();
        if ( "magnet".equals( protocol ) )
        {
            MagnetData magnetData = MagnetData.parseFromURI( downloadUri );
            
            URN urn = MagnetData.lookupSHA1URN(magnetData);
            String magnetFileName = MagnetData.lookupFileName( magnetData );
            magnetFileName = FileUtils.convertToLocalSystemFilename( magnetFileName );
            String searchTerm;
            if ( magnetData.getKeywordTopic() != null )
            {
                searchTerm = magnetData.getKeywordTopic();
            }
            else
            {
                searchTerm = StringUtils.createNaturalSearchTerm( 
                    MagnetData.lookupSearchName(magnetData) );
            }
            
            initialize( magnetFileName, urn, UNKNOWN_FILE_SIZE, searchTerm, true);
            try
            {
                initIncompleteFile();
            }
            catch ( FileHandlingException exp )
            {
                //logger.error( exp.toString(), exp );
            }
            catch ( ManagedFileException exp )
            {
                //logger.error( exp.toString(), exp );
            }
            
            List<URI> urlList = MagnetData.lookupHttpURIs(magnetData);
            for ( URI uri : urlList )
            {
                String host = uri.getHost();
                int port = uri.getPort();
                if ( port == -1 )
                { 
                    port = 80;
                }
                DestAddress address = new DefaultDestAddress( host, port );
                SWDownloadCandidate candidate = new SWDownloadCandidate( address,
                    uri, this, eventService, downloadService.getCandidateLogBuffer() );
                addDownloadCandidate( candidate );
            }
            
            // fire off a search in case this is a magnet download to get sources.
            if ( urn != null || getCandidatesCount() == 0 )
            {
                startSearchForCandidates();
            }
        }
        else
        {
            String uriFileName = URLUtil.getFileNameFromUri( downloadUri );
            uriFileName = FileUtils.convertToLocalSystemFilename( uriFileName );
            String searchTerm = StringUtils.createNaturalSearchTerm( uriFileName );
            initialize( uriFileName, null, UNKNOWN_FILE_SIZE, searchTerm, true);
            try
            {
                initIncompleteFile();
            }
            catch ( FileHandlingException exp )
            {
                //logger.error( exp.toString(), exp );
            }
            catch ( ManagedFileException exp )
            {
                //logger.error( exp.toString(), exp );
            }
            
            String host = downloadUri.getHost();
            if ( host != null )
            {
                int port = downloadUri.getPort();
                if ( port == -1 )
                { 
                    port = 80;
                }
                DestAddress address = new DefaultDestAddress( host, port );
                SWDownloadCandidate candidate = new SWDownloadCandidate( address,
                    downloadUri, this, eventService, downloadService.getCandidateLogBuffer() );
                addDownloadCandidate( candidate );
            }
        }
    }
    
    public SWDownloadFile( DDownloadFile dFile, SwarmingManager swMgr,
        PhexEventService eventService )
    {
        this( swMgr, eventService );
        URN fileUrn = null;
        if ( dFile.getFileURN() != null )
        {
            fileUrn = new URN( dFile.getFileURN() );
        }
        
        String destDir = dFile.getDestinationDirectory();
        if ( !StringUtils.isEmpty( destDir ) )
        {
            destinationDirectory = new File( destDir );
        }
        
        initialize( dFile.getLocalFileName(), fileUrn, dFile.getFileSize(),
            dFile.getSearchTerm(), false );
        
        String incompleteFileName = dFile.getIncompleteFileName();
        if ( !StringUtils.isEmpty( incompleteFileName ) )
        {
            try
            {
                incompleteManagedFile = Phex.getFileManager().getReadWriteManagedFile( 
                    new File( incompleteFileName ) );
            }
            catch ( ManagedFileException exp )
            {
                //logger.error( exp.toString(), exp );
                incompleteManagedFile = null;
            }
        }
        
        setCreatedDate( new Date( dFile.getCreationTime() ) );
        setDownloadedDate( new Date( dFile.getModificationTime() ) );

        status = dFile.getStatus();
        
        memoryFile.createDownloadScopes( dFile );
        createDownloadCandidates( dFile );

        verifyStatus();

        if ( isFileCompletedOrMoved() )
        {
            // in case some interrupted move occured... or some old downloads 
            // are still sitting there
            if ( isFileCompleted() )
            {
                if ( memoryFile.getFinishedLength() == 0 )
                {
                    setStatus(STATUS_FILE_COMPLETED_MOVED);
                }
            }
        }
    }

    /** 
     * @param fileName The file name of the file.
     * @param aFileURN The URN of the file. This is the most unique identifier of the file
     * network wide. If it is none null we should only accept candidates with
     * the same URN and also add this urn to researchs for better results.
     * @param aFileSize
     * @param searchTerm
     * @param createSegments
     */
    private void initialize( String fileName, URN aFileURN, long aFileSize,
        String searchTerm, boolean createSegments )
    {
        this.fileName = fileName;
        updateDestinationStreamable();
        
        if ( aFileURN != null )
        {
            this.fileURN = aFileURN;
            initAltLocContainers();
        }
        
        fileSize = aFileSize;
        previewSize = fileSize / 10;
        
        Servent servent = Servent.getInstance();
        researchSetting = new ResearchSetting( this, servent.getQueryService(),
            eventService, servent );
        researchSetting.setSearchTerm( searchTerm );
        
        memoryFile = downloadService.createMemoryFile( this );
    }
    
    public void setFileSize( long fileSize )
    {
        this.fileSize = fileSize;
        memoryFile.updateFileSize( );
        previewSize = fileSize / 10;
    }

    /**
     * Used to allocate and reserve a download candidate for a download worker.
     * @param worker the worker to allocate for
     * @param ipCounter an IPCounter to check the address availability against.
     * @return an allocated candidate.
     */
    public SWDownloadCandidate allocateDownloadCandidate( SWDownloadWorker worker,
        AddressCounter ipCounter )
    {
        // random from 0-9
        int val = random.nextInt(10);
        if ( val < 6 )
        {
            return allocateDownloadCandidate( worker, ipCounter, 
                goodCandidateAllocator, mediumCandidateAllocator, 
                badCandidateAllocator );
        }
        else if ( val < 9 )
        {
            return allocateDownloadCandidate( worker, ipCounter, 
                mediumCandidateAllocator, badCandidateAllocator, 
                goodCandidateAllocator );
        }        
        else
        {
            return allocateDownloadCandidate( worker, ipCounter, 
                badCandidateAllocator, mediumCandidateAllocator, 
                goodCandidateAllocator );
        }
    }
    
    /**
     * Allocates and reserve a download candidate for a download worker, going
     * through the given CandidateAllocators to allocate the candidate from the
     * first allocator able to provide one.
     * @param worker the worker to allocate for
     * @param ipCounter an IPCounter to check the address availability against.
     * @param candidateAllocators multiple allocators that are checked in order
     *        to find a allocatable candidate.
     * @return an allocated candidate.
     */
    private SWDownloadCandidate allocateDownloadCandidate( SWDownloadWorker worker,
        AddressCounter ipCounter, CandidateAllocator ... candidateAllocators )
    {
        for ( int i = 0; i < candidateAllocators.length; i++ )
        {
            SWDownloadCandidate candidate = candidateAllocators[i].allocate( 
                worker, ipCounter );
            if ( candidate != null )
            {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Releases a allocated download segment.
     */
    public void releaseDownloadCandidate( SWDownloadCandidate candidate )
    {
        synchronized( candidatesLock )
        {
            downloadService.releaseCandidateAddress( candidate );
            //logger.debug( "Release allocation {}.", candidate );
            // Sets the segment to be not allocated by a worker.
            allocatedCandidateWorkerMap.remove( candidate );
        }
    }
    
    public List<SWDownloadCandidate> getAllocatedCandidates()
    {
        synchronized( candidatesLock )
        {
            List<SWDownloadCandidate> list = new ArrayList<SWDownloadCandidate>(
                allocatedCandidateWorkerMap.size() );
            list.addAll( allocatedCandidateWorkerMap.keySet() );
            return list;
        }
    }

    public boolean addDownloadCandidate( RemoteFile remoteFile )
    {
        SWDownloadCandidate candidate = new SWDownloadCandidate( remoteFile, this,
            eventService, downloadService.getCandidateLogBuffer() );
        return addDownloadCandidate( candidate );
    }

    public boolean addDownloadCandidate( AlternateLocation altLoc )
    {
        URN altLocURN = altLoc.getURN();
        if ( fileURN != null && !altLocURN.equals( fileURN ) )
        {
            //logger.debug( "AlternateLocation URN does not match!" );
            return false;
        }

        // use get request class to parse url
        DestAddress hostAddress = altLoc.getHostAddress();
        if ( hostAddress.isLocalHost( Servent.getInstance().getLocalAddress() ) )
        {// don't add myself as candidate.
            return false;
        }
        SWDownloadCandidate candidate = new SWDownloadCandidate( hostAddress,
            0, null, altLocURN, this, eventService, downloadService.getCandidateLogBuffer() );
        return addDownloadCandidate( candidate );
    }

    

    protected boolean addDownloadCandidate( SWDownloadCandidate candidate )
    {
        URN candidateURN = candidate.getResourceURN();
        // update the urn of the file if null
        if ( fileURN == null && candidateURN != null )
        {
            fileURN = candidateURN;
            initAltLocContainers();
        }
        if ( ( fileURN != null && candidateURN != null )
            && !fileURN.equals( candidateURN ) )
        {// make sure URNs match!
            //logger.debug( "Candidate URN to add does not match!" );
            return false;
        }

        int pos;
        synchronized( candidatesLock )
        {
            int idx = allCandidatesList.indexOf( candidate );
            if ( idx != -1 )
            {
                // update push proxies of known candidates
                DestAddress[] pushProxies = candidate.getPushProxyAddresses();
                if ( pushProxies!= null && pushProxies.length > 0 )
                {
                    allCandidatesList.get( idx ).setPushProxyAddresses( 
                        pushProxies );
                }
                return false;
            }
            //logger.debug( "Adding download candidate {}", candidate );
            pos = allCandidatesList.size();
            allCandidatesList.add( candidate );
            mediumCandidatesList.add( candidate );
        }
        // fire after lock release otherwise we run into a deadlock.
        fireDownloadCandidateAdded( candidate, pos );

        return true;
    }
    
    /**
     * Makes the given download candidate as a good candidate, in case it was a
     * bad one...
     * 
     * @param candidate the candidate to make good.
     */
    public void markCandidateGood( SWDownloadCandidate candidate )
    {
        if ( candidate == null )
        {
            throw new NullPointerException( "Candidate is null.");
        }
        synchronized( candidatesLock )
        {
            int pos = badCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from bad...
                badCandidatesList.remove( pos );
            }
            pos = mediumCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from medium...
                mediumCandidatesList.remove( pos );
            }
            
            // ...and add to good.
            if ( !goodCandidatesList.contains( candidate ) )
            {
                goodCandidatesList.add( candidate );
                //logger.debug( "Moving candidate to good list: {}", 
                  //  candidate.getHostAddress() );
                candidate.addToCandidateLog("Moving candidate to good list.");
            }
        }
    }
    
    /**
     * Makes the given download candidate as a medium candidate, in case it was a
     * good or bad one...
     * 
     * @param candidate the candidate to make medium.
     */
    public void markCandidateMedium( SWDownloadCandidate candidate )
    {
        if ( candidate == null )
        {
            throw new NullPointerException( "Candidate is null.");
        }
        synchronized( candidatesLock )
        {
            int pos = badCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from bad...
                badCandidatesList.remove( pos );
            }
            pos = goodCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from good...
                goodCandidatesList.remove( pos );
            }
            
            // ...and add to good.
            if ( !mediumCandidatesList.contains( candidate ) )
            {
                mediumCandidatesList.add( candidate );
                //logger.debug( "Moving candidate to medium list: {}", 
                   // candidate.getHostAddress() );
                candidate.addToCandidateLog("Moving candidate to medium list.");
            }
        }
    }

    /**
     * Makes the given download candidate a bad download candidate. This operation
     * will NOT stop a running download of this candidate.
     * The candidate might be used again.
     * 
     * @param candidate the candidate to make bad.
     */
    public void markCandidateBad( SWDownloadCandidate candidate )
    {
        if ( candidate == null )
        {
            throw new NullPointerException( "Candidate is null.");
        }
        synchronized( candidatesLock )
        {
            int pos = goodCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from good...
                goodCandidatesList.remove( pos );
            }
            pos = mediumCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from medium...
                mediumCandidatesList.remove( pos );
            }
            
            // ...and add to bad.
            if ( !badCandidatesList.contains( candidate ) )
            {
                badCandidatesList.add( candidate );
                //logger.debug( "Moving candidate to bad list: {}", 
                    //candidate.getHostAddress() );
                candidate.addToCandidateLog("Moving candidate to bad list.");
            }
        }
        candidate.setStatus( CandidateStatus.BAD );
    }
    
    /**
     * Makes the given download candidate a ignored download candidate. This operation
     * will NOT stop a running download of this candidate.
     * the candidate is not looked at when searching for possible candidates in 
     * the bad list.
     * 
     * @param candidate the candidate to make bad.
     */
    public void markCandidateIgnored( SWDownloadCandidate candidate, String reason )
    {
        if ( candidate == null )
        {
            throw new NullPointerException( "Candidate is null.");
        }
        synchronized( candidatesLock )
        {
            int pos = goodCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from good...
                goodCandidatesList.remove( pos );
            }
            pos = mediumCandidatesList.indexOf( candidate );
            if ( pos >= 0 )
            {
                // remove from medium...
                mediumCandidatesList.remove( pos );
            }
            
            // ...and add to bad.
            if ( !badCandidatesList.contains( candidate ) )
            {
                badCandidatesList.add( candidate );
                //logger.debug( "Moving candidate to bad list: {}", 
                   // candidate.getHostAddress() );
                candidate.addToCandidateLog("Moving candidate to bad list (ignoring).");
            }
        }
        candidate.setStatus( CandidateStatus.IGNORED, -1, reason );
    }
    
    public void onCandidateStatusChange( String topic, 
        final ChangeEvent event )
    {
        // Valid transfer status:
        // CONNECTING, PUSH_REQUEST, REMOTLY_QUEUED
        // REQUESTING, DOWNLOADING
        SWDownloadCandidate candidate = (SWDownloadCandidate)event.getSource();
        if ( this != candidate.getDownloadFile() )
        {// not for me...
            return;
        }
        switch( (CandidateStatus)event.getOldValue() )
        {
            case CONNECTING:
            case PUSH_REQUEST:
            case REMOTLY_QUEUED:
            case ALLOCATING_SEGMENT:
            case REQUESTING:
            case DOWNLOADING:
                synchronized( candidatesLock )
                {
                    transferCandidatesList.remove(candidate);
                }
                break;
            case WAITING:
            case IGNORED:
            case RANGE_UNAVAILABLE:
            case BAD:
            case CONNECTION_FAILED:
            case BUSY:
                // ignore...
        }
        
        switch( (CandidateStatus)event.getNewValue() )
        {
            case CONNECTING:
            case PUSH_REQUEST:
            case REMOTLY_QUEUED:
            case ALLOCATING_SEGMENT:
            case REQUESTING:
            case DOWNLOADING:
                synchronized( candidatesLock )
                {
                    transferCandidatesList.add(candidate);
                }
                break;
            case WAITING:
            case IGNORED:
            case RANGE_UNAVAILABLE:
            case BAD:
            case CONNECTION_FAILED:
            case BUSY:
                // ignore...
        }
    }
    
    /**
     * Makes the given download candidate a bad download candidate. This operation
     * will NOT stop a running download of this candidate.
     * 
     * @param candidate the candidate to make bad.
     */
    public void addBadAltLoc( SWDownloadCandidate candidate )
    {
        URN candidateURN = candidate.getResourceURN();
        if ( candidateURN != null && fileURN != null )
        {
            
            AlternateLocation altLoc = new AlternateLocation(
                candidate.getHostAddress(), candidateURN );
            
            // remove good alt loc    
            goodAltLocContainer.removeAlternateLocation( altLoc );
            
            // add bad alt loc
            badAltLocContainer.addAlternateLocation( altLoc );            
        }
        //logger.debug( "Adding bad alt loc: {}", candidate.getHostAddress() );
    }
    
    /**
     * Makes the given download candidate a good download candidate.
     * 
     * @param candidate the candidate to make good.
     */
    public void addGoodAltLoc( SWDownloadCandidate candidate )
    {
        URN candidateURN = candidate.getResourceURN();
        if ( candidateURN != null && fileURN != null )
        {
            AlternateLocation altLoc = new AlternateLocation(
                candidate.getHostAddress(), candidateURN );
            
            // remove bad alt loc    
            badAltLocContainer.removeAlternateLocation( altLoc );
            
            // add good alt loc
            goodAltLocContainer.addAlternateLocation( altLoc );            
        }
        //logger.debug( "Adding good alt loc: {}",
          //  candidate.getHostAddress() );
    }
    
    /**
     * Returns the bandwidth controller of this download
     * @return the bandwidth controller of this download
     */
    public BandwidthController getBandwidthController()
    {
        return bandwidthController;
    }
    
    public void setDownloadThrottlingRate( int speedInBytes )
    {
        bandwidthController.setThrottlingRate(speedInBytes);
    }
    
    public long getDownloadThrottlingRate(  )
    {
        return bandwidthController.getThrottlingRate();
    }
    
    /**
     * Returns the transfer speed from the bandwidth controller of this download.
     * @return the short term transfer avg.
     */
    public long getTransferSpeed()
    {
        return bandwidthController.getShortTransferAvg().getAverage();
    }

    /**
     * Returns the number of available candidates
     */
    public int getCandidatesCount()
    {
        return allCandidatesList.size();
    }
    
    public int getDownloadingCandidatesCount()
    {
        updateCandidateWorkerCounts();
        return downloadingCandidateCount;
    }
    
    public int getQueuedCandidatesCount()
    {
        return queuedCandidatesSet.size();
    }
    
    public int getConnectingCandidatesCount()
    {
        updateCandidateWorkerCounts();
        return connectingCandidateCount;
    }
    
    public int getAllocatingSegmentCandidatesCount()
    {
        updateCandidateWorkerCounts();
        return allocatingSegmentCandidateCount;
    }
    
    private void updateCandidateWorkerCounts()
    {
        // to save performance search in transfer list for 
        // downloading candidates.
        synchronized( candidatesLock )
        {
            long now = System.currentTimeMillis();
            if ( lastCandidateWorkerCountUpdate +
                 CANDIDATE_WORKER_COUNT_TIMEOUT > now )
            {
                return;
            }
            int downloadingCount = 0;
            int queuedCount = 0;
            int connectingCount = 0;
            int allocatingSegmentCount = 0;
            for ( SWDownloadCandidate candidate : transferCandidatesList )
            {
                CandidateStatus candStatus = candidate.getStatus();
                switch ( candStatus )
                {
                case DOWNLOADING:
                    downloadingCount ++;
                    break;
                case REMOTLY_QUEUED:
                    queuedCount ++;
                    break;
                case CONNECTING:
                    connectingCount ++;
                    break;
                case ALLOCATING_SEGMENT:
                    allocatingSegmentCount ++;
                    break;
                }
            }
            downloadingCandidateCount = downloadingCount;
            connectingCandidateCount = connectingCount;
            allocatingSegmentCandidateCount = allocatingSegmentCount;
            // calculation could take a while.. therefore we don't use cached
            // time.
            lastCandidateWorkerCountUpdate = System.currentTimeMillis();
        }
    }
    
    /**
     * The number of candidates in the good candidate list.
     * @return number of good candidates
     */
    public int getGoodCandidateCount()
    {
        return goodCandidatesList.size();
    }
    
    /**
     * The number of candidates in the bad candidate list.
     * @return number of bad candidates
     */
    public int getBadCandidateCount()
    {
        return badCandidatesList.size();
    }

    /**
     * Gets the candidate at the given position. Or null if the index is not
     * available.
     */
    public SWDownloadCandidate getCandidate( int index )
    {
        if ( index < 0 || index >= allCandidatesList.size() )
        {
            return null;
        }
        return allCandidatesList.get( index );
    }

    /**
     * Returns the container of all known good alternate download locations or null
     * if the download has no valid file urn.
     * @return the container of all known good alternate download locations or null
     * if the download has no valid file urn.
     */
    public AltLocContainer getGoodAltLocContainer()
    {
        return goodAltLocContainer;
    }
    
    /**
     * Returns the container of all known bad alternate download locations or null
     * if the download has no valid file urn.
     * @return the container of all known bad alternate download locations or null
     * if the download has no valid file urn.
     */
    public AltLocContainer getBadAltLocContainer()
    {
        return badAltLocContainer;
    }
    
    public int getTransferCandidateCount()
    {
        return transferCandidatesList.size();
    }
    
    public SWDownloadCandidate getTransferCandidate( int index )
    {
        if ( index < 0 || index >= transferCandidatesList.size() )
        {
            return null;
        }
        return transferCandidatesList.get( index );
    }

    public URN getFileURN()
    {
        return fileURN;
    }

    public Date getCreatedDate()
    {
        return createdDate;
    }
    
    protected void setCreatedDate( Date createdDate )
    {
        this.createdDate = createdDate; 
    }

    public Date getDownloadedDate()
    {
        return modifiedDate;
    }
    
    protected void setDownloadedDate( Date modifiedDate )
    {
        this.modifiedDate = modifiedDate; 
    }
    
    public MemoryFile getMemoryFile()
    {
        return memoryFile;
    }
    
    public ThexVerificationData getThexVerificationData()
    {
        return thexVerificationData;
    }

    public HTTPRangeSet createAvailableRangeSet()
    {
        HTTPRangeSet rangeSet = new HTTPRangeSet();
        List<DownloadScope> finishedScopes = memoryFile.getFinishedScopeListCopy();
        for( DownloadScope scope : finishedScopes )
        {
            rangeSet.addRange( scope.getStart(),
                    scope.getEnd() );
        }
        return rangeSet;
    }

    /**
     * The research settings.
     */
    public ResearchSetting getResearchSetting()
    {
        return researchSetting;
    }

    /**
     * This method is used when a user triggers a search from the user interface
     * it should not be used for any automatic searching! Automatic searching
     * is done vie the ResearchService class.
     */
    public void startSearchForCandidates()
    {
        if ( isFileCompletedOrMoved() )
        {
            return;
        }
        researchSetting.stopSearch();
        // user triggered search with default timeout
        researchSetting.startSearch( );
    }

    public void setStatus( int newStatus )
    {
        // dont care for same status
        if ( status == newStatus )
        {
            return;
        }
        // lock downloadCandidates since verifyStatus changes status with
        // downloadCandidate lock.
        synchronized( candidatesLock )
        {
            //logger.debug("DownloadFile Status: {} ({}).",
//                SWDownloadInfo.getDownloadFileStatusString(newStatus),
//                newStatus );
            switch( newStatus )
            {
                case STATUS_FILE_COMPLETED:
                    // count the completed download
                    SimpleStatisticProvider provider = (SimpleStatisticProvider)
                        Servent.getInstance().getStatisticsService().getStatisticProvider(
                        StatisticsManager.SESSION_DOWNLOAD_COUNT_PROVIDER );
                    provider.increment( 1 );
                    // stop possible running search...
                    researchSetting.stopSearch();
                case STATUS_FILE_WAITING:
                case STATUS_FILE_STOPPED:
                    downloadStopNotify();
                    break;
                case STATUS_FILE_DOWNLOADING:
                    // only trigger if the status is not already set to downloading
                    downloadStartNotify();
                    break;
                case STATUS_FILE_COMPLETED_MOVED:
                    eventService.publish( PhexEventTopics.Download_File_Completed, this );
                    break;
            }
            status = newStatus;
        }
    }

    /**
     * Returns the current status of the download file.
     */
    public int getStatus()
    {
        return status;
    }

    public boolean isAbleToBeAllocated()
    {
        return !isDownloadStopped() &&
               !isFileCompletedOrMoved() &&
               workerCount <= DownloadPrefs.MaxWorkerPerDownload.get().intValue();
    }

    /**
     * Raises the number of downloading worker for this file by one.
     */
    public void incrementWorkerCount()
    {
        workerCount ++;
    }

    /**
     * Lowers the number of downloading worker for this file by one
     */
    public void decrementWorkerCount()
    {
        workerCount --;
    }

    /**
     * The methods checks all download candidates if there is still a download
     * going on or if the download was completed.
     * The status is set appropriately. If the download is completed the status
     * is set to STATUS_FILE_COMPLETED.
     */
    public void verifyStatus()
    {    
        // access to memoryFile should be outside of a candidatesLock,
        // otherwise a deadlock situation can occure.
        if ( !isFileCompletedOrMoved()
            && memoryFile.getDownloadedLength() == getTotalDataSize() )
        {
            // trigger data write down to disk... this might take a while
            // and will also adjust the status in case file really turns
            // out to be complete
            memoryFile.requestBufferWriting();
            return;
        }
        synchronized( candidatesLock )
        {
            SWDownloadCandidate candidate;
            Iterator<SWDownloadCandidate> iterator = 
                allocatedCandidateWorkerMap.keySet().iterator();
            CandidateStatus highestStatus = CandidateStatus.WAITING;
            while ( iterator.hasNext() && highestStatus != CandidateStatus.DOWNLOADING)
            {
                candidate = iterator.next();
                switch ( candidate.getStatus() )
                {
                    case DOWNLOADING:
                    {
                        highestStatus = CandidateStatus.DOWNLOADING;
                        break;
                    }
                    case REMOTLY_QUEUED:
                    {
                        highestStatus = CandidateStatus.REMOTLY_QUEUED;
                        break;
                    }
                }
            }

            // when we are here no download is running...
            // we dont need to set status if file is stopped or completed
            if ( !isDownloadStopped() && !isFileCompletedOrMoved() )
            {
                switch ( highestStatus )
                {
                    case REMOTLY_QUEUED:
                        setStatus( STATUS_FILE_QUEUED );
                        break;
                    case DOWNLOADING:
                        setStatus( STATUS_FILE_DOWNLOADING );
                        break;
                    default:
                        setStatus( STATUS_FILE_WAITING );
                        break;
                }
            }
        }
    }

    public boolean isFileCompleted()
    {
        return status == STATUS_FILE_COMPLETED;
    }
    
    public boolean isFileCompletedMoved()
    {
        return status == STATUS_FILE_COMPLETED_MOVED;
    }
    
    public boolean isFileCompletedOrMoved()
    {
        return status == STATUS_FILE_COMPLETED_MOVED ||
               status == STATUS_FILE_COMPLETED;
    }

    public boolean isDownloadInProgress()
    {
        return status == STATUS_FILE_DOWNLOADING;
    }

    public boolean isDownloadStopped()
    {
        return status == STATUS_FILE_STOPPED;
    }
    
    /**
     * The incomplete file of the download. A single incomplete file is used
     * for each download file.
     * 
     * @throws FileHandlingException 
     */
    public ManagedFile getIncompleteDownloadFile() 
        throws ManagedFileException, FileHandlingException
    {
        initIncompleteFile();
        return incompleteManagedFile;
    }
    
    /**
     * Returns true if the file is likely to be streamable during download.
     * @return true if the file is likely to be streamable during download,
     *         false otherwise.
     */
    public boolean isDestinationStreamable()
    {
        return isDestStreamable;
    }
    
    /**
     * Recalculate prefix, suffix & filetype values.
     * To be called when the destinationFile is updated.
     */
    private void updateDestinationStreamable()
    {
        isDestStreamable = MediaType.getStreamableMediaType().isFilenameOf( 
            getFileName() );
    }
    
    /**
     * The destination directory of the finished download. In case it is null
     * the default download directory is used to store the finished download
     * file. In case the destination directory is changed by the user this
     * field contains the new destination directory.
     */
    public File getDestinationDirectory()
    {
        return destinationDirectory;
    }

    /**
     * Sets the new destination direcotry of the finished download. This can 
     * be null in which case the default download directory will be used.
     * @param directory the new download directory or null.
     */
    public void setDestinationDirectory( File directory )
    {
        destinationDirectory = directory;
    }
    
    /**
     * Returns the file name of this SWDownloadFile.
     */
    public String getFileName( )
    {
        return fileName;
    }
    
    /**
     * Sets the new name of the file. This will not change the name of the incomplete
     * file. Only the possible name of the file after completion, in case it is 
     * not already completed.
     * @param fileName the new file name.
     */
    public void setFileName( String fileName )
    {
        if ( StringUtils.isEmpty( fileName ) )
        {
            throw new IllegalArgumentException( "Empty file name not allowed: " + fileName );
        }
        this.fileName = fileName;
        updateDestinationStreamable();
    }
    
    /**
     * Returns the destination file. It is build using a configured destination
     * directory or the default destination directory and the file name.
     * @return the destination file for this download file.
     */
    public File getDestinationFile()
    {
        if ( destinationDirectory == null )
        {
            return new File( DownloadPrefs.DestinationDirectory.get(), fileName );
        }
        else
        {
            return new File( destinationDirectory, fileName );
        }
    }
    
    /**
     * Returns a file that is ready for preview. If the download is still running
     * a copy of the incomplete file is made and returned, if the download is
     * complete the complete file is returned, if the file cant be previewed 
     * null is returned.
     * @return a file for preview, or null.
     */
    public File getPreviewFile()
    {
        if ( isFileCompletedOrMoved() )
        {
            return getDestinationFile();
        }
        
        if ( !memoryFile.isFileBeginningAvailable() )
        {
            return null;
        }
        
        long previewLength = memoryFile.getFileBeginningScopeLength();
        
        StringBuffer fullFileNameBuf = new StringBuffer();
        fullFileNameBuf.append( DownloadPrefs.IncompleteDirectory.get() );
        fullFileNameBuf.append( File.separatorChar );
        fullFileNameBuf.append( "PREVIEW" );
        fullFileNameBuf.append( "-" );
        fullFileNameBuf.append( fileName );
        File previewFile = new File( fullFileNameBuf.toString() );
        previewFile.deleteOnExit();
        
        try
        {
            FileUtils.copyFile(incompleteManagedFile.getFile(), previewFile, previewLength);
            return previewFile;
        }
        catch ( IOException exp )
        {
            return null;
        }
    }
    
    /**
     * Returns whether a preview of this file is already possible.
     * @return true if a file preview is possible, false otherwise.
     */
    public boolean isPreviewPossible()
    {
        if ( isFileCompletedOrMoved() )
        {
            return true;
        }
        
        if ( !memoryFile.isFileBeginningAvailable() )
        {
            return false;
        }
        
        // TODO2 validate file extensions if they are previewable...
        
        return true;
    }

    /**
     * Moves a completed download file to the destination file.
     */
    public synchronized void moveToDestinationFile()
    {
        if ( isFileCompletedMoved() )
        {// somebody else did it already before me...
            return;
        }

        // this is an assertion... go crazy if fails...
        if ( memoryFile.getMissingLength() > 0 && status == STATUS_FILE_COMPLETED)
        {
            throw new RuntimeException( "There should be no missing length (found " 
                + memoryFile.getMissingLength() + 
                ") and the download must be completed to move to destination file '" 
                + fileName + "'" );
        }
            
        File destFile = getDestinationFile();
        try
        {
            // make sure the incomplete file is closed, and nobody interrupts me
            // during my rename operation...
            getIncompleteDownloadFile().acquireFileLock();
            try
            {
                getIncompleteDownloadFile().closeFile();

                // find a free file spot...
                int tryCount = 0;
                while( destFile.exists() )
                {
                    tryCount ++;
                    StringBuffer newName = new StringBuffer();
                    newName.append( destFile.getParent() );
                    newName.append( File.separatorChar );
                    newName.append( '(' );
                    newName.append( tryCount );
                    newName.append( ") " );
                    newName.append( fileName );
                    destFile = new File( newName.toString()  );
                }
                //logger.debug( "Renaming final segment from {} to {}.",
//                    getIncompleteDownloadFile().getAbsolutePath(),
//                    destFile.getAbsoluteFile() );
                getIncompleteDownloadFile().renameFile(destFile);
                setStatus(STATUS_FILE_COMPLETED_MOVED);

                //auto remove download file if set
                if ( DownloadPrefs.AutoRemoveCompleted.get().booleanValue() )
                {
                    downloadService.removeDownloadFile( this );
                    // removeDownloadFile triggers save...
                }
                else
                {
                    downloadService.notifyDownloadListChange();
                }
            }
            finally
            {
                getIncompleteDownloadFile().releaseFileLock();
            }
        }
        catch ( FileHandlingException exp )
        {
            //logger.warn( "Failed renaming from {} to {}.", 
//                incompleteManagedFile.getAbsolutePath(),
//                destFile.getAbsolutePath() );
            //logger.error( exp.toString(), exp);
        }
        catch ( ManagedFileException exp )
        {
            if ( exp.getCause() instanceof InterruptedException )
            { // the thread was interrupted and requested to stop, most likley
              // by user request.
                //logger.debug( exp.toString() );
            }
            else if ( exp.getCause() instanceof FileHandlingException )
            {
                //logger.warn( "Failed renaming from {} to {}.", 
//                    incompleteManagedFile.getAbsolutePath(),
//                    destFile.getAbsolutePath() );
                //logger.error( exp.toString(), exp);
            }
            else
            {
                //logger.error( exp.toString(), exp);
            }
        }
    }


    /**
     * The progress of the download. Its calculated from the total file size
     * compared to the transfered file size.
     * @return the progress of the download or -1 if it can't be determined.
     */
    public Integer getProgress()
    {
        int percentage;
        if ( isFileCompletedOrMoved() )
        {
            percentage = 100;
        }
        else
        {
            long tmpTransferDataSize = fileSize;
            if ( tmpTransferDataSize == UNKNOWN_FILE_SIZE 
              || tmpTransferDataSize == 0 )
            {
                percentage = -1;
            }
            else
            {
                percentage = (int)( getTransferredDataSize() * 100L / tmpTransferDataSize );
            }
        }

        if ( currentProgress.intValue() != percentage )
        {
            // only create new object if necessary
            currentProgress = Integer.valueOf( percentage );
        }
        return currentProgress;
    }

    public void startDownload()
    {
        setStatus( STATUS_FILE_WAITING );
        verifyStatus();
        downloadService.notifyWaitingWorkers();
    }
    
    /**
     * Stops a possible running download from the given candidate. The method
     * blocks until its worker completely stopped.
     * @param candidate the candidate to stop the possibly running download from.
     */
    public void stopDownload( SWDownloadCandidate candidate )
    {
        SWDownloadWorker worker;
        synchronized ( candidatesLock )
        {
            worker = allocatedCandidateWorkerMap.get( candidate );
        }
        if ( worker != null )
        {
            worker.stopWorker();
            worker.waitTillFinished();
        }
    }

    /**
     * Method call blocks until all workers are stopped.
     */
    public void stopDownload()
    {
        setStatus( STATUS_FILE_STOPPED );        
        stopAllWorkers( true );
    }
    
    /**
     * Stops all active workers of this download.
     * If waitTillFinished is true the method blocks till all workers are completely
     * stopped.
     * 
     * @param waitTillFinished if true the method blocks till all workers are 
     *                         completely stopped.
     */
    private void stopAllWorkers( boolean waitTillFinished )
    {
        SWDownloadWorker[] workers;
        synchronized ( candidatesLock )
        {
            Collection<SWDownloadWorker> workerColl = allocatedCandidateWorkerMap.values();
            workers = new SWDownloadWorker[ workerColl.size() ];
            workerColl.toArray( workers );
            
            for (int i = 0; i < workers.length; i++)
            {
                SWDownloadWorker worker = workers[i];            
                worker.stopWorker();
            }
        }
        if ( !waitTillFinished )
        {
            return;
        }
        for (int i = 0; i < workers.length; i++)
        {
            SWDownloadWorker worker = workers[i];
            if ( worker.isInsideCriticalSection() )
            {
                worker.waitTillFinished();
            }
        }
    }

    /**
     * Removes incomplete download file from disk of a stopped download file.
     * In case segments are still allocated by a worker the method blocks until
     * all workers are stopped. 
     */
    public void removeIncompleteDownloadFile()
    {
        if ( isFileCompletedOrMoved() )
        {
            return;
        }
        if ( status != STATUS_FILE_STOPPED )
        {
            //logger.error( "Can't clean temp files of not stopped download file.");
            return;
        }
        
        stopAllWorkers( true );

        if ( incompleteManagedFile != null )
        {
            try
            {
                incompleteManagedFile.deleteFile();
            }
            catch ( ManagedFileException exp )
            {
                //logger.error( exp.toString(), exp);
            }
        }
    }

    /**
     * Indicate that the download is just starting.
     * Triggered internally when status changes to STATUS_FILE_DOWNLOADING.
     */
    private void downloadStartNotify( )
    {
        transferStartTime = System.currentTimeMillis();
        modifiedDate = new Date( transferStartTime );
        transferStopTime = 0;
    }

    /**
     * Indicate that the download is no longer running.
     * Triggered internally when status is set to STATUS_FILE_COMPLETED or
     * STATUS_FILE_QUEUED.
     */
    private void downloadStopNotify( )
    {
        // Ignore nested calls.
        if( transferStopTime == 0 )
        {
            transferStopTime = System.currentTimeMillis();
            if ( status == STATUS_FILE_DOWNLOADING )
            {// only update if the status is currently switched from downloading to stopped..
                modifiedDate = new Date( transferStopTime );
            }
        }
        downloadService.notifyDownloadListChange();
        
        // make sure incomplete file is available/can be inited to prevent
        // race condition in initIncompleteFile()
        if ( incompleteManagedFile != null )
        {
            try
            {
                getIncompleteDownloadFile().closeFile();
            }
            catch ( FileHandlingException exp )
            {
                //logger.error( exp.toString(), exp);
            }
            catch ( ManagedFileException exp )
            {
                if ( exp.getCause() instanceof InterruptedException )
                { // the thread was interrupted and requested to stop, most likely
                  // by user request.
                    //logger.debug( exp.toString() );
                }
                else
                {
                    //logger.error( exp.toString(), exp);
                }
            }
        }
    }
    
    public RatedDownloadScopeList getRatedScopeList()
    {
        return memoryFile.getRatedScopeList( );
    }
    
    public void rateDownloadScopeList( RatedDownloadScopeList ratedScopeList)
    {
        long oldestConnectTime = System.currentTimeMillis() - BAD_CANDIDATE_STATUS_TIMEOUT;
        synchronized( candidatesLock )
        {
            for ( SWDownloadCandidate candidate : goodCandidatesList )
            {
                if ( candidate.getLastConnectionTime() == 0 ||
                     candidate.getLastConnectionTime() < oldestConnectTime )
                {
                    continue;
                }
                DownloadScopeList availableScopeList = candidate.getAvailableScopeList();
                if ( availableScopeList == null )
                {
                    availableScopeList = new DownloadScopeList();
                    availableScopeList.add( new DownloadScope( 0, fileSize - 1)  );
                }
                ratedScopeList.rateDownloadScopeList( availableScopeList, 
                    candidate.getSpeed() );
            }
        }
    }
    
    /**
     * Used to check if a scope is allocateable. This check is done early
     * before a connection to a candidate is opened. The actual allocation happens
     * after the connection to the candidate is established. Though it can happen
     * that all scopes are already allocated until then.
     * @param candidateScopeList the ranges that are wanted for this download if
     *        set to null all ranges are allowed.
     * @return true if there is a scope available. false otherwise.
     */
    public boolean isScopeAllocateable( DownloadScopeList candidateScopeList,
        boolean isCandidateScopeComplete )
    {
        boolean includeBlocked = true;
        if ( allocatingSegmentCandidateCount > 0 )
        {
            return false;
            //includeBlocked = false;
        }
        boolean state = memoryFile.isMissingScopeAllocateable(candidateScopeList, 
            isCandidateScopeComplete, includeBlocked );
        
        return state;
    }
    
    /**
     * Used to allocate and reserve a download segment for a download worker.
     */
    public DownloadScope allocateDownloadScope( SWDownloadCandidate candidate )
    {
        DownloadScope result = null;
        boolean retry;
        do
        {
            retry = false;
            // ignore wanted range set if file size is unknown
            if ( fileSize != UNKNOWN_FILE_SIZE )
            {
                result = allocateSegmentForRangeSet( candidate );
                //logger.debug( "Allocated: {}", result );
            }
            else
            {
                long preferredSize = candidate.getPreferredSegmentSize();
                result = memoryFile.allocateMissingScope(preferredSize);
                //logger.debug( "Allocated: {}", result );
            }
            
            /*
            TODO2 segment hijacking like we done in the old download concept is
            not what we want anymore for the new concept. Instead we like to
            double download certain slow or left over download scopes to raise
            overall download speed.
            */
        } while (retry);

        return result;
    }
    
    
    private DownloadScope allocateSegmentForRangeSet( SWDownloadCandidate candidate )
    {
        assert fileSize != UNKNOWN_FILE_SIZE : 
            "Cant allocate segment for range set with unknown end.";
        
        DownloadScopeList candidateScopeList = candidate.getAvailableScopeList();
        boolean isCandidateScopeComplete = candidate.isAvailableScopeComplete();
        long preferredSize = candidate.getPreferredSegmentSize();
        
        //logger.debug( "allocateSegmentForRangeSet() size: {}", preferredSize);
        
        if( candidateScopeList == null )
        {
            // create scope covering the whole file.
            candidateScopeList = new DownloadScopeList();
            candidateScopeList.add( new DownloadScope( 0, fileSize - 1 ) );
        }
        
        return memoryFile.allocateMissingScopeForCandidate( candidateScopeList, 
            isCandidateScopeComplete, preferredSize );
    }
    
    /**
     * Releases a allocated download segment.
     */
    public void releaseDownloadScope( DownloadScope downloadScope, 
        long transferredSize, SWDownloadCandidate downloadCandidate )
    {
        memoryFile.releaseAllocatedScope( downloadScope, transferredSize, downloadCandidate );
    }
    
    public boolean addAndValidateQueuedCandidate( SWDownloadCandidate candidate )
    {        
        int maxWorkers = Math.min(
            DownloadPrefs.MaxTotalDownloadWorker.get().intValue(),
            DownloadPrefs.MaxWorkerPerDownload.get().intValue() );
        int maxQueuedWorkers = (int)Math.max( Math.floor( 
            maxWorkers - (maxWorkers*0.2) ), 1 );
        synchronized ( candidatesLock )
        {
            int queuedCount = queuedCandidatesSet.size();
            if ( queuedCount < maxQueuedWorkers )
            {
                candidate.addToCandidateLog("Accept queued candidate (" + 
                    queuedCount +"/"+maxQueuedWorkers + ")");
                queuedCandidatesSet.add(candidate);
                return true;
            }
            
            if ( queuedCandidatesSet.contains(candidate) )
            {
                return true;
            }
            
            // find a bad slot to drop...
            SWDownloadCandidate highestCandidate = null;
            int highestPos = -1;
            for ( SWDownloadCandidate altCandidate : queuedCandidatesSet )
            {
                int altPos = altCandidate.getXQueueParameters().getPosition();
                if ( altPos > highestPos )
                {
                    highestCandidate = altCandidate;
                    highestPos = altPos;
                }
            }
            
            int candidatePos = candidate.getXQueueParameters().getPosition();
            if ( highestCandidate != null && highestPos > candidatePos )
            {
                highestCandidate.addToCandidateLog(
                    "Drop queued candidate - new alternative: " + 
                    highestPos +" - " + candidatePos + ")");
                // stay busy 1 minute for each queue position but max 15 minutes.
                highestCandidate.setStatus( CandidateStatus.BUSY, 
                    Math.min( 15, candidatePos) * 60 );
                SWDownloadWorker worker = allocatedCandidateWorkerMap.get(
                    highestCandidate);
                if ( worker != null )
                {
                    worker.stopWorker();
                }
                queuedCandidatesSet.add(candidate);
                return true;
            }
            else
            {
                candidate.addToCandidateLog(
                    "Drop queued candidate - existing alternative: " + 
                    candidatePos +" - " + highestPos );
                candidate.setStatus( CandidateStatus.BUSY, Math.min( 15, candidatePos) * 60 );
                return false;
            }
        }
    }
    
    public void removeQueuedCandidate( SWDownloadCandidate candidate )
    { 
        synchronized ( candidatesLock )
        {
            queuedCandidatesSet.remove(candidate);
        }
    }

    private void initAltLocContainers()
    {
        goodAltLocContainer = new AltLocContainer( fileURN );
        badAltLocContainer = new AltLocContainer( fileURN );
    }
    
    /**
     * Creates the incomplete file object.
     * The filename contains the localFilename of the download file and the
     * segment code. The first segment of the file should not modify the
     * original file extension. This allows applications to open the file.
     * All other segments should modify the extension since it should not be
     * possible to open a segment that is not the first one.
     * For performance reasons the segment keeps hold to its File reference.
     * @throws FileHandlingException 
     * @throws ManagedFileException 
     */
    private void initIncompleteFile()
        throws FileHandlingException, ManagedFileException
    {
        if ( incompleteManagedFile != null )
        {
            return;
        }
        try
        {
            incompleteManagedFile = Phex.getFileManager().getReadWriteManagedFile( 
                createIncompleteFile( fileName ) );
        }
        catch ( FileHandlingException exp )
        {
            String filename = exp.getFileName();
            Throwable cause = exp.getCause();
            String errorStr = cause != null ? cause.toString() : "Unknown";
            stopDownload();
            Environment.getInstance().fireDisplayUserMessage(
                UserMessageListener.SegmentCreateIncompleteFileFailed, 
                new String[] {filename, errorStr});
            throw exp;
        }
    }
    
    public static File createIncompleteFile( String fileName ) throws FileHandlingException
    {
        int tryCount = 0;
        File tryFile;
        boolean succ;
        IOException lastExp = null;
        do
        {
            StringBuffer incFileNameBuf = new StringBuffer();
            incFileNameBuf.append( "INCOMPLETE" );
            if ( tryCount > 0 )
            {
                incFileNameBuf.append( '(' );
                incFileNameBuf.append( String.valueOf( tryCount ) );
                incFileNameBuf.append( ')' );
            }
            incFileNameBuf.append( "-" );
            incFileNameBuf.append( fileName );
            String incFileName = FileUtils.convertToLocalSystemFilename( 
                incFileNameBuf.toString() );

            tryFile = new File( DownloadPrefs.IncompleteDirectory.get(), incFileName );
            tryCount ++;
            try
            {
                FileUtils.forceMkdir( tryFile.getParentFile() );
                succ = tryFile.createNewFile();
            }
            catch (IOException exp)
            {
                lastExp = exp;
                succ = false;
            }
        }
        while ( !succ && tryCount < 50 );
        if ( lastExp != null )
        {
            //logger.error( tryFile.getAbsolutePath() );
            //logger.error( lastExp.toString(), lastExp );
        }
        if ( !succ )
        {
            //logger.error( "Tryied {} times to create a segment file. Giving up.", tryCount );
            throw new FileHandlingException( FileHandlingException.CREATE_FILE_FAILED,
                tryFile.getAbsolutePath(), lastExp );
        }
        
        return tryFile;
    }
    
    ////////START XML Handling support ///////////

    private void createDownloadCandidates( DDownloadFile dFile )
    {
        synchronized( candidatesLock )
        {
            long before24h = System.currentTimeMillis() - DateUtils.MILLIS_PER_DAY;
            // clean candidates
            allCandidatesList.clear();
            goodCandidatesList.clear();
            mediumCandidatesList.clear();
            badCandidatesList.clear();

            // add xml definitions...
            DDownloadCandidate dCandidate;
            SWDownloadCandidate candidate;
            Iterator<DDownloadCandidate> iterator = dFile.getCandidateList().
                getSubElementList().iterator();
            while( iterator.hasNext() )
            {
                try
                {
                    dCandidate = iterator.next();
                    candidate = new SWDownloadCandidate( dCandidate, this,
                        eventService, downloadService.getCandidateLogBuffer() );
                    //logger.debug( "Adding download candidate {}", candidate );
                    
                    int pos = allCandidatesList.size();
                    allCandidatesList.add( candidate );
                    fireDownloadCandidateAdded( candidate, pos );
                    if ( dCandidate.getConnectionFailedRepetition() >= 
                        BAD_CANDIDATE_CONNECTION_TRIES )
                    {
                        badCandidatesList.add( candidate );
                    }
                    else if ( dCandidate.getLastConnectionTime() > before24h )
                    {
                        goodCandidatesList.add( candidate );
                    }
                    else
                    {
                        mediumCandidatesList.add( candidate );
                    }
                }
                catch ( MalformedDestAddressException exp )
                {
                    //logger.warn( exp.toString(), exp );
                }
                catch ( Exception exp )
                {// catch all exception in case we have an error in the XML
                    //logger.error( "Error loading a download candidate from XML.", exp );
                }
            }
            
            Collections.sort(goodCandidatesList, new InitialCandidatesComparator());
            Collections.sort(mediumCandidatesList, new InitialCandidatesComparator());
        }
    }
    
    /**
     * Creates the DElement representation of this object to serialize it 
     * into XML.
     * @return the DElement representation of this object
     */
    public DDownloadFile createDDownloadFile()
    {
        DDownloadFile dFile = new DDownloadFile();
        dFile.setLocalFileName( fileName );

        // in case incomplete file failed to initialize correctly, this could be
        // null and should not prevent us from saving the download...
        if ( incompleteManagedFile != null )
        {
            dFile.setIncompleteFileName( incompleteManagedFile.getAbsolutePath() );
        }
        if ( destinationDirectory != null )
        {
            dFile.setDestinationDirectory( destinationDirectory.getAbsolutePath() );
        }
        dFile.setFileSize( fileSize );
        dFile.setSearchTerm( researchSetting.getSearchTerm() );
        dFile.setCreationTime( createdDate.getTime() );
        dFile.setModificationTime( modifiedDate.getTime() );
        if ( fileURN != null )
        {
            dFile.setFileURN( fileURN.getAsString() );
        }
        dFile.setStatus( status );
        synchronized ( candidatesLock )
        {
            List<DDownloadCandidate> list = dFile.getCandidateList().getSubElementList();
            for( SWDownloadCandidate candidate : goodCandidatesList )
            {
                DDownloadCandidate xjbCandidate = candidate.createDDownloadCandidate();
                list.add( xjbCandidate );
            }
            for( SWDownloadCandidate candidate : mediumCandidatesList )
            {
                DDownloadCandidate xjbCandidate = candidate.createDDownloadCandidate();
                list.add( xjbCandidate );
            }
            for( SWDownloadCandidate candidate : badCandidatesList )
            {
                if ( candidate.getStatus() == CandidateStatus.IGNORED )
                {
                    continue;
                }
                DDownloadCandidate xjbCandidate = candidate.createDDownloadCandidate();
                list.add( xjbCandidate );
            }
        }
        
        memoryFile.createXJBFinishedScopes( dFile );
        return dFile;
    }
    ////////END XML Hanlding support ///////////
    
    //////// START TransferDataProvider Interface ///////////

    /**
     * Not implemented... uses own transfer rate calculation
     */
    public void setTransferRateTimestamp( long timestamp )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Not implemented... uses own transfer rate calculation
     */
    public int getShortTermTransferRate()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the long term data transfer rate in bytes. This is the rate of
     * the transfer since the last start of the transfer. This means after a
     * transfer was interrupted and is resumed again the calculation restarts.
     */
    public int getLongTermTransferRate()
    {
        return (int)getTransferSpeed();
    }

    /**
     * Return the data transfer status.
     * It can be TRANSFER_RUNNING, TRANSFER_NOT_RUNNING, TRANSFER_COMPLETED,
     * TRANSFER_ERROR.
     */
    public short getDataTransferStatus()
    {
        switch ( status )
        {
        case STATUS_FILE_DOWNLOADING:
            return TRANSFER_RUNNING;
        case STATUS_FILE_COMPLETED:
        case STATUS_FILE_COMPLETED_MOVED:
            return TRANSFER_COMPLETED;
        default:
            return TRANSFER_NOT_RUNNING;
        }
    }

    /**
     * Return the size of data that is attempting to be transfered. This is
     * NOT necessarily the full size of the file as could be the case during
     * a download resumption.
     */
    public long getTransferDataSize()
    {
        return getTotalDataSize();
    }

    /**
     * This is the total size of the file. Even if its not importend
     * for the transfer itself.
     * Dont change the meaning of this it is way importent for researching and
     * adding candidates!!
     */
    public long getTotalDataSize()
    {
        return fileSize;
    }

    /**
     * Defines the length already downloaded.
     */
    public long getTransferredDataSize()
    {
        return memoryFile.getDownloadedLength();
    }
    
    //////// END TransferDataProvider Interface ///////////

    ///////////////////// START event handling methods ////////////////////////

    private void fireDownloadCandidateAdded( SWDownloadCandidate candidate, int position )
    {
        eventService.publish( PhexEventTopics.Download_Candidate,
            new ContainerEvent( Type.ADDED, candidate, this, position ) );
    }

    ///////////////////// END event handling methods ////////////////////////
    
    private class CandidateAllocator
    {
        private final List<SWDownloadCandidate> candidatesList;
        private int allocationIndex;
        
        public CandidateAllocator(List<SWDownloadCandidate> candidatesList)
        {
            super();
            this.candidatesList = candidatesList;
            allocationIndex = 0;
        }

        public SWDownloadCandidate allocate( SWDownloadWorker worker, 
            AddressCounter ipCounter )
        {
            SWDownloadCandidate candidate = null;
            synchronized( candidatesLock )
            {
                int numCandidates = candidatesList.size();
                
                // return quickly if there are no candidates
                if ( numCandidates == 0 )
                {
                    return null;
                }

                // sanity check on persistent index, because since the last call to 
                // this method, the number of candidates may have decreased, leaving 
                // the persistent index out of range.
                if ( allocationIndex >= numCandidates )
                {
                    allocationIndex = 0;
                }

                // Iterate over candidates to find the next available
                for (int i=0; i < numCandidates; i++)
                {
                    // currentIndex holds the index of the candidate that will be
                    // checked for availability
                    int currentIndex = i + allocationIndex;
                    if (currentIndex >= numCandidates)
                    {
                        currentIndex -= numCandidates;
                    }
                        
                    candidate = candidatesList.get( currentIndex );
                    if ( !candidate.isAbleToBeAllocated() )
                    {
                        continue;
                    }
                    if ( allocatedCandidateWorkerMap.containsKey( candidate ) )
                    {
                        continue;
                    }
                    boolean ipSucc = ipCounter.validateAndCountAddress( 
                        candidate.getHostAddress() );
                    if ( ipSucc )
                    {
                        //logger.debug( "Allocating good candidate {} from {}.", candidate, 
                         //   worker );
                        candidate.addToCandidateLog("Allocating as good candidate.");
                        // Sets the segment to be allocated by a worker.
                        allocatedCandidateWorkerMap.put( candidate, worker );
                        allocationIndex = currentIndex + 1;
                        return candidate;
                    }
                    else
                    {
                        candidate.addToCandidateLog( 
                            "Max downloads for candidate IP already reached." );
                    }
                }
            }

            // No valid candidate found
            // Don't bother updating the persistent index, because it probably
            // doesn't matter where we begin the search from on the next call.
            return null;
        }
    }
    
    private static class InitialCandidatesComparator 
        implements Comparator<SWDownloadCandidate>
    {
        public int compare( SWDownloadCandidate candidate1, SWDownloadCandidate candidate2 )
        {
            if ( candidate1 == candidate2 || candidate1.equals(candidate2) )
            {
                return 0;
            }
            long diff = candidate1.getLastConnectionTime() 
                - candidate2.getLastConnectionTime();
            if ( diff < 0 )
            {
                return 1;
            }
            else if ( diff > 0 )
            {
                return -1;
            }
            else
            {
                return candidate1.hashCode() - candidate2.hashCode();
            }
        }
    }

	@Override
	public void onEvent(String topic, Object event) {
		// TODO Auto-generated method stub
		 if(topic.compareTo(PhexEventTopics.Download_Candidate_Status)==0)
		 	onCandidateStatusChange(  topic, 
		         (ChangeEvent) event );
		
	}
}
