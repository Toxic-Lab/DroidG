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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import phex.common.AlternateLocation;
import phex.common.URN;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.MalformedDestAddressException;
import phex.common.log.LogBuffer;
import phex.common.log.LogRecord;
import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.RemoteFile;
import phex.event.ChangeEvent;
import phex.event.PhexEventService;
import phex.event.PhexEventTopics;
import phex.http.HTTPRangeSet;
import phex.http.Range;
import phex.http.XQueueParameters;
import phex.msg.GUID;
import phex.net.repres.PresentationManager;
import phex.prefs.core.DownloadPrefs;
import phex.query.QueryHitHost;
import phex.utils.URLCodecUtils;
import phex.utils.URLUtil;
import phex.xml.XMLUtils;
import phex.xml.sax.downloads.DDownloadCandidate;

/**
 * A representation of a download candidate. A download candidate contains all
 * information required for a given endpoint that can offer us data for download.
 * <p>
 * 
 */
public class SWDownloadCandidate implements SWDownloadConstants
{
   // private static final Logger logger = LoggerFactory.getLogger( SWDownloadCandidate.class );
    
    
    /**
     * The value of the candidate status constants are used for sorting
     * therefore status values should be kept in a reasonable order.
     */
    public enum CandidateStatus
    {
        // Indicating the candidate is ignored from further processing
        IGNORED,
        // Indicating the candidate is ignored from further processing
        BAD,
        // Indicating the candidate is waiting to be processed.
        WAITING,
        // Indicating the last connecting try to the candidate failed.
        CONNECTION_FAILED,
        // Indicating the candidate is busy.
        BUSY,
        // Indicating the last requested range of the candidate was unavailable.
        RANGE_UNAVAILABLE,
        // Indicating the candidate is connecting
        CONNECTING,
        // Indicating the candidate is connecting with a push request.
        PUSH_REQUEST,
        // Indicating the candidate is looking for a segment.
        ALLOCATING_SEGMENT,
        // Indicating the candidate is remotely queued.
        REMOTLY_QUEUED,
        // Indicating the candidate is requesting a segment
        REQUESTING,
        // Indicating the candidate is downloading
        DOWNLOADING
    }
    
    public enum ThexStatus
    {
        // Indicating the candidate has not yet succeeded or failed to perform 
        // a thex request.
        OPEN,
        // Indicating the candidate has succeeded to perform a thex request.
        SUCCEDED,
        // Indicating the candidate failed to perform a thex request.
        FAILED
    }
    
    /**
     * The download file that this candidate belongs to.
     */
    private final SWDownloadFile downloadFile;
    
    /**
     * A {@link LogBuffer} to add candidate log messages.
     * This can be null and also a shared instance across multiple 
     * candidate.
     */
    private final LogBuffer candidateLogBuffer;
    
    /**
     * The event service to inform of status changes.
     */
    private final PhexEventService eventService;
    
    /**
     * The time this download candidate was first added to the list of download
     * candidates of the corresponding download file. This is the first creation
     * time of the SWDownloadCandidate object.
     */
    //private Date firstSeenDate;
    
    /**
     * The time a connection to this download candidate could be established
     * successful.
     */
    private long lastConnectionTime;
        
    /**
     * The number of failed connection tries to this download candidate, since
     * the last successful connection.
     */
    private int failedConnectionTries;
    
    /** 
     * The GUID of the client used for push requests.
     */
    private GUID guid;

    /**
     * The file index of the file at the download candidate.
     * This is the old style identifier for candidates without
     * urn.
     */
    private long fileIndex;
    
    /**
     * The resource URN of the file. If known this will be used
     * for download requests.
     */
    private URN resourceURN;
    
    /**
     * A complete download URI to use. This is necessary for standard
     * uri downloads like in http urls.
     */
    private URI downloadURI;
    
    /**
     * The thex request status for this candidate.
     */
    private ThexStatus thexStatus;
    
    /**
     * The thex uri string, expected without host (it's supposed to be intended
     * for this candidate). The value is initialized during the download
     * handshake.
     */
    private String thexUri;
    
    /**
     * The thex root string. Initialized during the download handshake.
     */
    private String thexRoot;

    /**
     * The name of the file at the download candidate.
     */
    private String fileName;

    /**
     * Rate at which the last segment was transferred
     */
    private int lastTransferRateBPS;

    /**
     * The host address of the download candidate.
     */
    private DestAddress hostAddress;

    /**
     * The status of the candidate.
     */
    private CandidateStatus status;
    
    /**
     * A possible status reason.
     */
    private String statusReason;

    /**
     * The last error status of the download to track status changes.
     */
    private CandidateStatus errorStatus;

    /**
     * The time after which the current status times out and things continue.
     */
    private long statusTimeout;

    /**
     * Counts the number of times the status keeps repeating.
     */
    private int errorStatusRepetition;

    /**
     * The vendor of the client running at the candidate.
     */
    private String vendor;
    
    private boolean isG2FeatureAdded;

    /**
     * Defines if a push is needed for this candidate.
     */
    private boolean isPushNeeded;
    
    /**
     * The addresses of push proxies of this host or null
     * if not available.
     */
    private DestAddress[] pushProxyAddresses;

    /**
     * Defines if the candidate supports chat connections.
     */
    private boolean isChatSupported;

    /**
     * The available range set of the candidate file.
     */
    private DownloadScopeList availableScopeList;
    
    /**
     * Indicates if it assumed that the available scope list is complete.
     */
    private boolean isAvailableScopeComplete;

    /**
     * The time the available range was last updated.
     */
    private long availableRangeSetTime = 0;

    /**
     * The download segment that is currently associated by this download
     * candidate or null if no association exists.
     */
    private SWDownloadSegment downloadSegment;

    /**
     * The parameters that are available in case the candidate is remotely queuing
     * our download request. This is referenced for information purpose only.
     */
    private XQueueParameters xQueueParameters;
    
    /**
     * This is a Map holding all AltLocs already send to this candidate during
     * this session. It is used to make sure the same AltLocs are not send twice
     * to the same candidate. The list is lazy initialized on first access.
     */
    private Set<AlternateLocation> sendAltLocSet;
    
    /**
     * The total amount of data downloaded from this candidate.
     */
    private long totalDownloadSize;
    
    private SWDownloadCandidate( SWDownloadFile downloadFile,
        PhexEventService eventService, LogBuffer candidateLogBuffer )
    {
        if ( downloadFile == null )
        {
            throw new NullPointerException( "downloadFile is null." );
        }
        this.downloadFile = downloadFile;
        
        if ( eventService == null )
        {
            throw new NullPointerException( "event service is null." );
        }
        this.eventService = eventService;
        
        // can be null
        this.candidateLogBuffer = candidateLogBuffer;
        
        availableScopeList = null;
        lastTransferRateBPS = 0;
        totalDownloadSize = 0;
        lastConnectionTime = 0;
    }
    
    public SWDownloadCandidate( RemoteFile remoteFile,
        SWDownloadFile downloadFile, PhexEventService eventService,
        LogBuffer candidateLogBuffer )
    {
        this( downloadFile, eventService, candidateLogBuffer );

        fileIndex = remoteFile.getFileIndex();
        fileName = remoteFile.getFilename();
        resourceURN = remoteFile.getURN();
        guid = remoteFile.getRemoteClientID();
        QueryHitHost qhHost = remoteFile.getQueryHitHost();
        vendor = qhHost.getVendor();
        isPushNeeded = qhHost.isPushNeeded();
        hostAddress = remoteFile.getHostAddress();
        isChatSupported = qhHost.isChatSupported();
        pushProxyAddresses = qhHost.getPushProxyAddresses();
        status = CandidateStatus.WAITING;
        thexStatus = ThexStatus.OPEN;
    }

    public SWDownloadCandidate( DestAddress aHostAddress, long aFileIndex,
        String aFileName, URN aResourceURN, SWDownloadFile downloadFile,
        PhexEventService eventService, LogBuffer candidateLogBuffer )
    {
        this( downloadFile, eventService, candidateLogBuffer );

        fileIndex = aFileIndex;
        fileName = aFileName;
        resourceURN = aResourceURN;
        guid = null;
        vendor = null;
        isPushNeeded = false;
        // assume chat is supported but we dont know...
        isChatSupported = true;
        hostAddress = aHostAddress;
        status = CandidateStatus.WAITING;
        thexStatus = ThexStatus.OPEN;
        
        /*setAvailableRangeSet(new HTTPRangeSet(0, downloadFile.getTotalDataSize())); */
    }
    
    public SWDownloadCandidate(DestAddress address, URI downloadUri,
        SWDownloadFile downloadFile, PhexEventService eventService,
        LogBuffer candidateLogBuffer )
        throws URIException
    {
        this( downloadFile, eventService, candidateLogBuffer );

        fileName = URLUtil.getPathQueryFromUri( downloadUri );
        this.downloadURI = downloadUri;
        resourceURN = URLUtil.getQueryURN( downloadUri );
        guid = null;
        vendor = null;
        isPushNeeded = false;
        // assume chat is supported but we dont know...
        isChatSupported = true;
        hostAddress = address;
        status = CandidateStatus.WAITING;
        thexStatus = ThexStatus.OPEN;
    }

    public SWDownloadCandidate( DDownloadCandidate dCandidate,
        SWDownloadFile downloadFile, PhexEventService eventService,
        LogBuffer candidateLogBuffer ) 
        throws MalformedDestAddressException
    {
        this( downloadFile, eventService, candidateLogBuffer );

        fileIndex = dCandidate.getFileIndex();
        fileName = dCandidate.getFileName();
        
        
        /* setAvailableRangeSet(new HTTPRangeSet(0, downloadFile.getTotalDataSize())); */

        String guidHexStr = dCandidate.getGuid();
        if ( guidHexStr != null )
        {
            guid = new GUID( guidHexStr );
        }
        String downloadUriStr = dCandidate.getDownloadUri();
        if ( downloadUriStr != null )
        {
            try
            {
                downloadURI = new URI( downloadUriStr, true );
            }
            catch ( URIException exp )
            {
                //logger.warn( "Malformed URI in: " + downloadFile.toString() +
//                    " - " + downloadUriStr + " - " + this.toString(),
//                    exp );
                // continue anyway.. download candidate might still be useful
            }
        }
        
        String resourceUrnStr = dCandidate.getResourceUrn();
        if ( resourceUrnStr != null )
        {
            resourceURN = new URN( resourceUrnStr );
        }
        else
        {
            // No resource urn was found there could be two reasons, the 
            // candidate uses a download uri or the download file is from 
            // release 3.0.2 or earlier. If no download uri is available
            // we use the resource urn of the download file to work around
            // this shortcoming and support pre 3.0.2 download lists as 
            // good as possible. Basically this means we don't support 
            // file index downloads anymore.. but do we really need them 
            // nowadays..
            if ( downloadURI == null )
            {
                resourceURN = downloadFile.getFileURN();
            }
        }
        
        vendor = dCandidate.getVendor();
        isPushNeeded = dCandidate.isPushNeeded();
        isChatSupported = dCandidate.isChatSupported();
        
        if ( dCandidate.isSetLastConnectionTime() )
        {
            lastConnectionTime = dCandidate.getLastConnectionTime();
        }
        else
        {
            lastConnectionTime = 0;
        }

        try
        {
            hostAddress = PresentationManager.getInstance().createHostAddress(
                dCandidate.getRemoteHost(), DefaultDestAddress.DEFAULT_PORT );
        }
        catch ( MalformedDestAddressException exp )
        {
            //logger.warn( "Malformed host address in: " + downloadFile.toString() +
            //    " - " + dCandidate.getRemoteHost() + " - " + this.toString(), exp );
            throw exp;
        }
        
        if ( dCandidate.getConnectionFailedRepetition() > 0 )
        {
            errorStatus = CandidateStatus.CONNECTION_FAILED;
            status = CandidateStatus.CONNECTION_FAILED;
            errorStatusRepetition = dCandidate.getConnectionFailedRepetition();
            failedConnectionTries = errorStatusRepetition;
        }
        else
        {
            status = CandidateStatus.WAITING;
        }
        
        // thex status is reset on startup.
        thexStatus = ThexStatus.OPEN;
    }
    
    

    /**
     * Returns the url necessary for the download request.
     * @return the download url.
     */
    public String getDownloadRequestUrl()
    {
        String requestUrl;
        if ( downloadURI != null )
        {
            try
            {
                // Don't use whole uri.. only file and query part..!?
                requestUrl = URLUtil.getPathQueryFromUri( downloadURI );
                return requestUrl;
            }
            catch (URIException e)
            {// failed to use uri.. try other request urls..
                //logger.warn( e.toString(), e );
            }
        }
        
        if ( resourceURN != null )
        {
            requestUrl = URLUtil.buildName2ResourceURL( resourceURN );
        }
        else
        {
            // build standard old style gnutella request.
            String fileIndexStr = String.valueOf( fileIndex );
            StringBuffer urlBuffer = new StringBuffer( 6 + fileIndexStr.length()
                + fileName.length() );
            urlBuffer.append( "/get/" );
            urlBuffer.append( fileIndexStr );
            urlBuffer.append( '/' );            
            urlBuffer.append( URLCodecUtils.encodeURL( fileName ) );
            requestUrl = urlBuffer.toString();
        }
        return requestUrl;
    }
    
    public SWDownloadFile getDownloadFile()
    {
        return downloadFile;
    }
    
    /**
     * Returns the average transfer speed of the last transfer,
     * or 0 if no transfer has finished yet.
     * @return the average transfer speed of the last transfer,
     * or 0 if no transfer has finished yet
     */
    public long getSpeed()
    {
        return lastTransferRateBPS;
    }

    /**
     * Returns the HostAddress of the download candidate
     */
    public DestAddress getHostAddress()
    {
        return hostAddress;
    }

    /**
     * Returns the name of the file at the download candidate.
     */
    public String getFileName()
    {
        return fileName;
    }

    /**
     * Returns the resource URN of the file at the download candidate.
     */
    public URN getResourceURN()
    {
        return resourceURN;
    }
    
    /**
     * Returns the GUID of the candidate for push requests.
     */
    public GUID getGUID()
    {
        return guid;
    }

    /**
     * Returns the file index of the file at the download candidate.
     */
    public long getFileIndex()
    {
        return fileIndex;
    }
    
    public long getTotalDownloadSize()
    {
        return totalDownloadSize;
    }
    
    public void incTotalDownloadSize( int val )
    {
        totalDownloadSize += val;
    }

    /**
     * Returns the time in millis until the the status timesout.
     */
    public long getStatusTimeLeft()
    {
        long timeLeft = statusTimeout - System.currentTimeMillis();
        if ( timeLeft < 0 )
        {
            timeLeft = 0;
        }
        return timeLeft;
    }

    /**
     * Returns the current status of the candidate.
     */
    public CandidateStatus getStatus()
    {
        return status;
    }
    
    public String getStatusReason()
    {
        return statusReason;
    }
    
    /**
     * Indicates how often the last error status repeated. The last error status
     * must must not be the current status, but is the current status in case the
     * current status is a error status.
     * @return how often the last error status repeated
     */
    /*public int getErrorStatusRepetition()
    {
        return errorStatusRepetition;
    }*/
    
    /**
     * Returns the number of failed connection tries since the last successful
     * connection.
     * @return the number of failed connection tries since the last successful
     * connection.
     */
    public int getFailedConnectionTries()
    {
        return failedConnectionTries;
    }

    /**
     * The download candidate vendor.
     * @return the vendor name
     */
    public String getVendor()
    {
        return vendor;
    }

    public void setVendor( String aVendor )
    {
        if ( vendor == null || !vendor.equals( aVendor ) )
        {
            // verify characters - this is used to remove invalid xml characters
            for ( int i = 0; i < aVendor.length(); i++ )
            {
                if ( !XMLUtils.isXmlChar( aVendor.charAt( i ) ) )
                {
                    return;
                }
            }
            vendor = aVendor;
            addToCandidateLog( "Set vendor to: " + vendor );
        }
    }
    
    
    
    public boolean isG2FeatureAdded()
    {
        return isG2FeatureAdded;
    }
    
    public void setG2FeatureAdded( boolean state )
    {
        isG2FeatureAdded = state;
    }

    public void updateXQueueParameters( XQueueParameters newXQueueParameters )
    {
        if ( xQueueParameters == null )
        {
            xQueueParameters = newXQueueParameters;
        }
        else
        {
            xQueueParameters.update( newXQueueParameters );
        }
    }

    public XQueueParameters getXQueueParameters()
    {
        return xQueueParameters;
    }

    public boolean isPushNeeded()
    {
        return isPushNeeded;
    }
    
    /**
     * Returns the array of push proxies of this connection
     * or null if there are no available.
     * @return an array of push proxies or null
     */
    public DestAddress[] getPushProxyAddresses()
    {
        return pushProxyAddresses;
    }
    
    public void setPushProxyAddresses( DestAddress[] addresses )
    {
        pushProxyAddresses = addresses;
    }

    public long getLastConnectionTime()
    {
        return lastConnectionTime;
    }

    public void setLastConnectionTime( long lastConnectionTime )
    {
        this.lastConnectionTime = lastConnectionTime;
    }

    public boolean isChatSupported()
    {
        return isChatSupported;
    }

    public void setChatSupported( boolean state )
    {
        isChatSupported = state;
    }
    
    /**
     * Returns true if the candidate is remotly queued, false otherwise.
     * @return true is the candidate is remotly queued, false otherwise.
     */
    public boolean isRemotlyQueued()
    {
        return status == CandidateStatus.REMOTLY_QUEUED;
    }
    
    /**
     * Returns true if the candidate is busy or remotly queued, false otherwise.
     * @return true is the candidate is busy or remotly queued, false otherwise.
     */
    public boolean isBusyOrQueued()
    {
        return status == CandidateStatus.BUSY || status == CandidateStatus.REMOTLY_QUEUED;
    }
    
    /**
     * Returns true if the candidate range is unavailable, false otherwise.
     * @return true is the candidate range is unavailable, false otherwise.
     */
    public boolean isRangeUnavailable()
    {
        return status == CandidateStatus.RANGE_UNAVAILABLE;
    }
    
    /**
     * Returns true if the candidate is downloading, false otherwise.
     * @return true is the candidate is downloading, false otherwise.
     */
    public boolean isDownloading()
    {
        return status == CandidateStatus.DOWNLOADING;
    }
    
    public boolean isThexSupported()
    {
        return thexUri != null && thexRoot != null && thexStatus == ThexStatus.OPEN;
    }
    
    public String getThexUri()
    {
        return thexUri;
    }
    
    public String getThexRoot()
    {
        return thexRoot;
    }
    
    public void setThexUriRoot( String thexUri, String thexRoot )
    {
        this.thexUri = thexUri;
        this.thexRoot = thexRoot;
    }
    
    public void setThexStatus( ThexStatus thexStatus )
    {
        this.thexStatus = thexStatus;
    }
    
    /**
     * Returns the list of alt locs already send to this connection. 
     * @return the list of alt locs already send to this connection.
     */
    public Set<AlternateLocation> getSendAltLocsSet()
    {
        if ( sendAltLocSet == null )
        {// TODO2 use something like a LRUMap. But current LRUMap uses maxSize
         // as initial hash size. This would be much to big in most cases!
         // Currently this HashSet has no size boundry. We would need our own
         // LRUMap implementation with a low initial size and a different max size.
            sendAltLocSet = new HashSet<AlternateLocation>();
        }
        return sendAltLocSet;
    }

    /**
     * Sets the available range set.
     * @param newRangeSet the available range set, must not be null.
     */
    public void setAvailableRangeSet(HTTPRangeSet newRangeSet)
    {
        if ( newRangeSet == null )
        {
            // not only clear existing scope list but set it to null to give 
            // free memory
            availableScopeList = null;
            return;
        }
        
        if ( !processAvailableRangeSet( newRangeSet ) )
        {
            return;
        }
        
        if ( newRangeSet.size() == 0 )
        {
            // not only clear existing scope list but set it to null to give 
            // free memory
            availableScopeList = null;
            return;
        }
        
        // we always initialize a new available scope list here instead of 
        // clearing and reusing the existing scope list to prevent concurrent
        // modification exceptions during the many access operations to the list
        // after calls to getAvailableScopeList()
        // Also we lock the write operation to prevent giving out a incomplete
        // availableScopeList
        synchronized( this )
        {
            DownloadScopeList newScopeList = new DownloadScopeList();
            addHttpRangeSet( newScopeList, newRangeSet );
            availableScopeList = newScopeList;
            availableRangeSetTime = System.currentTimeMillis();
            isAvailableScopeComplete = true;
        }
        //logger.debug( "Added new rangeset for {}: ", downloadFile.getFileName(),
        //    newRangeSet );
    }
    
    /**
     * Sets the available range set.
     * @param rangeSetToAdd the available range set, must not be null
     */
    public void addToAvailableRangeSet( HTTPRangeSet rangeSetToAdd, long availableSize )
    {
        if ( rangeSetToAdd == null )
        {
            throw new NullPointerException();
        }
        
        if ( !processAvailableRangeSet( rangeSetToAdd ) )
        {
            return;
        }
        
        if ( rangeSetToAdd.size() == 0 )
        {
            // nothing to add...
            return;
        }
        
        // we always initialize a new available scope list here instead of 
        // clearing and reusing the existing scope list to prevent concurrent
        // modification exceptions during the many access operations to the list
        // after calls to getAvailableScopeList()
        // Also we lock the write operation to prevent giving out a incomplete
        // availableScopeList
        synchronized( this )
        {
            DownloadScopeList newScopeList = new DownloadScopeList();
            // add all old scopes...
            if ( availableScopeList != null )
            {
                newScopeList.addAll( availableScopeList );
            }
            addHttpRangeSet( newScopeList, rangeSetToAdd );
            availableScopeList = newScopeList;
            availableRangeSetTime = System.currentTimeMillis();
            
            isAvailableScopeComplete = availableScopeList.getAggregatedLength() >= availableSize;
        }
        

        //logger.debug( "Added additional rangeset for {}: {}", downloadFile.getFileName(),
         //   rangeSetToAdd );
    }
    
    /**
     * Used to check if the range set should be processed further or 
     * should be ignored.
     * @param rangeSet
     * @return true to process, false to ignore.
     */
    private boolean processAvailableRangeSet( HTTPRangeSet rangeSet )
    {
        // don't do anything if we have no range set and the scope list is
        // uninitialized
        if ( rangeSet.size() == 0 && availableScopeList == null )
        {
            return false;
        }
        long fileSize = downloadFile.getTotalDataSize();
        if ( fileSize == SWDownloadConstants.UNKNOWN_FILE_SIZE )
        {// we can't handle available range set without knowing
         // file size...
            return false;
        }
        return true;
    }
    
    /**
     * Helper method to add a {@link HTTPRangeSet} to a {@link DownloadScopeList}.
     * @param scopeList the scope list to add the ranges to.
     * @param httpRangeSet the range set to add to the scope list.
     */
    private void addHttpRangeSet( DownloadScopeList scopeList, HTTPRangeSet httpRangeSet )
    {
        long fileSize = downloadFile.getTotalDataSize();
        Iterator<Range> iterator = httpRangeSet.getIterator();
        while ( iterator.hasNext() )
        {
            Range range = iterator.next();
            long start = range.getStartOffset( fileSize );
            long end = range.getEndOffset( fileSize );
            if ( end < start )
            {// this is an invalid range... skip it
                //logger.warn( "Invalid range: " +range.buildHTTPRangeString() + " - "
                //    + start + " - " + end + " - " + fileSize + " - " + vendor );
                continue;
            }
            DownloadScope scope = new DownloadScope( start, end );
            scopeList.add( scope );
        }
    }

    /**
     * Returns the available range set or null if not set.
     * @return the available range set or null if not set.
     */
    public DownloadScopeList getAvailableScopeList( )
    {
        if ( System.currentTimeMillis() >
               availableRangeSetTime + AVAILABLE_RANGE_SET_TIMEOUT 
            && isAvailableScopeComplete )
        {
            setAvailableRangeSet( null );
        }
        // We lock the access to prevent giving out a currently written to
        // availableScopeList
        synchronized( this )
        {
            return availableScopeList;
        }
    }
    
    public boolean isAvailableScopeComplete()
    {
        synchronized( this )
        {
            return isAvailableScopeComplete;
        }
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int PRIME = 31;
        int result = 17;
        result = PRIME * result + ((hostAddress == null) ? 0 : hostAddress.hashCode());
        return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        final SWDownloadCandidate other = (SWDownloadCandidate) obj;
        if ( hostAddress == null )
        {
            if ( other.hostAddress != null ) return false;
        }
        else if ( !hostAddress.equals( other.hostAddress ) ) return false;
        return true;
    }

    /**
     * Sets the status of the candidate and fulfills the required actions
     * necessary for that status. E.g. setting the statusTime.
     * @param newStatus the status to set from 
     *        SWDownloadConstants.STATUS_CANDIDATE_*
     */
    public void setStatus( CandidateStatus newStatus )
    {
        setStatus( newStatus, -1, null );
    }
    
    /**
     * Sets the status of the candidate and fulfills the required actions
     * necessary for that status. E.g. setting the statusTime.
     * @param newStatus the status to set from 
     *        SWDownloadConstants.STATUS_CANDIDATE_*
     * @param statusSeconds the time in seconds the status should last.
     */
    public void setStatus( CandidateStatus newStatus, int statusSeconds )
    {
        setStatus( newStatus, statusSeconds, null );
    }
    
    /**
     * Sets the status of the candidate and fulfills the required actions
     * necessary for that status. E.g. setting the statusTime.
     * @param newStatus the status to set from 
     *        SWDownloadConstants.STATUS_CANDIDATE_*
     * @param statusSeconds the time in seconds the status should last only
     *        used for REMOTLY_QUEUED.
     * @param aStatusReason a status reason to be displayed to the user.
     */
    public void setStatus( CandidateStatus newStatus, int statusSeconds, String aStatusReason )
    {
        // Don't care for same status
        if ( status == newStatus )
        {
            return;
        }
        CandidateStatus oldStatus = status;
        status = newStatus;
        
//        if ( //logger.isWarnEnabled() && System.currentTimeMillis() < statusTimeout )
//        {
//            //logger.warn( "Status timeout has not passed yet." );
//        }
        
        long newStatusTimeout;
        statusTimeout = newStatusTimeout = System.currentTimeMillis();
        switch( status )
        {
            case BAD:
                newStatusTimeout += BAD_CANDIDATE_STATUS_TIMEOUT;
                break;
            case IGNORED:
                newStatusTimeout = Long.MAX_VALUE;
                break;
            case CONNECTING:
                //connectionTries ++;
                break;
            case CONNECTION_FAILED:
                failedConnectionTries ++;
                if ( failedConnectionTries >= IGNORE_CANDIDATE_CONNECTION_TRIES )
                {// we have tried long enough to connect to this candidate, ignore
                 // it for the future (causes delete after session).
                    downloadFile.markCandidateIgnored( this, 
                        "CandidateStatusReason_ConnectionFailed" );
                    // markCandidateIgnored updates the statusTimeout 
                    // and statusReason, this value is reset here to not 
                    // overwrite it...
                    newStatusTimeout = statusTimeout;
                    aStatusReason = statusReason;
                }
                else if ( failedConnectionTries >= BAD_CANDIDATE_CONNECTION_TRIES )
                {
                    // we dont remove candidates but put them into a bad list.
                    // once we see a new X-Alt the candidate is valid again.
                    // candidates might go into the bad list quickly.
                    // when no "good" candidates are avaiable we might also try additional
                    // bad list connects, every 3 hours or so...
                    downloadFile.markCandidateBad( this );
                    // markCandidateBad updates the statusTimeout, this value is
                    // reset here to not overwrite it...
                    newStatusTimeout = statusTimeout;
                }
                else
                {
                    downloadFile.markCandidateMedium( this );
                    newStatusTimeout += calculateConnectionFailedTimeout();
                }
                break;
            case ALLOCATING_SEGMENT:
            case REQUESTING:
                failedConnectionTries = 0;
                break;
            case BUSY:
            case RANGE_UNAVAILABLE:
            case REMOTLY_QUEUED:
                failedConnectionTries = 0;
                if ( statusSeconds > 0 )
                {
                    newStatusTimeout += statusSeconds * 1000L;
                }
                else
                {
                    newStatusTimeout += determineErrorStatusTimeout( status );
                }
                break;
            case PUSH_REQUEST:
                newStatusTimeout += DownloadPrefs.PushRequestTimeout.get().intValue();
                break;
            case DOWNLOADING:
                // clear the current error status... use the waiting status for this
                errorStatus = CandidateStatus.WAITING;
                failedConnectionTries = 0;
                break;
            
        }
        this.statusReason = aStatusReason;
        
        //logger.debug( "Setting status from " + oldStatus + " to " + newStatus 
//            + " and raise timeout from " + statusTimeout + " to " + newStatusTimeout + "(" + 
//            (newStatusTimeout-statusTimeout) + ") Reason:" + aStatusReason + ".");
        addToCandidateLog( "Setting status to " 
            + SWDownloadInfo.getDownloadCandidateStatusString(this) 
            + " and raise timeout from " + statusTimeout + " to " 
            + newStatusTimeout + "(" + (newStatusTimeout-statusTimeout) 
            + ") Reason:" + aStatusReason + ". OldStatus: " + oldStatus);
        
        statusTimeout = newStatusTimeout;
        
        fireCandidateStatusChange( oldStatus, newStatus );
    }
     
    /**
     * Calculates the timeout between the last failed connection and the next
     * connection try. 
     * @return the number of millies to wait till the status expires.
     */
    private long calculateConnectionFailedTimeout()
    {   
        // Once we are over BAD_CANDIDATE_CONNECTION_TRIES we dont call this 
        // method anymore and use BAD_CANDIDATE_STATUS_TIMEOUT instead.
        
        // When CONNECTION_FAILED_STEP_TIME time is 2 it gives a sequence of
        // tries: 1, 2,  3,|  4,  5,  6,   7,   8,   9,  10
        //    to: 2, 4,  8,| 16, 32, 64, 128, 128, 128, 128
        //   to2: 2, 6, 12,| 22, 40, 74, 140, 142, 144, 146 
        // 
        // to1 - would cause the values to double on each repetition.
        //       CONNECTION_FAILED_STEP_TIME * (long)Math.pow( 2, 
        //           Math.min( failedConnectionTries - 1, 7 ) );
        // to2 - would add a raising penalty to to1 on each repetition
        //       CONNECTION_FAILED_STEP_TIME * (long)Math.pow( 2,
        //           Math.min( failedConnectionTries - 1, 7 ) ) 
        //           + (failedConnectionTries - 1) * 2;
        //
        // We use to2 currently:
        
        // Java 6 - Math.scalb() might perform faster.
        return CONNECTION_FAILED_STEP_TIME * (long)Math.pow( 2,
            Math.min( failedConnectionTries - 1, 7 ) ) 
            + (failedConnectionTries - 1) * 2;
    }

    /**
     * Maintains error status tracking. This is needed to track repeting errors
     * that will be handled by flexible status timeouts.
     * @returns the livetime of the status.
     */
    private long determineErrorStatusTimeout( CandidateStatus aErrorStatus )
    {
        if ( errorStatus == aErrorStatus )
        {
            errorStatusRepetition ++;
        }
        else
        {
            errorStatus = aErrorStatus;
            errorStatusRepetition = 0;
        }
        switch( errorStatus )
        {
            case BUSY:
                // we can add here a step thing for each retry with a top limit
                // and a shorter start sleep time but currently we keep it like this.
                return HOST_BUSY_SLEEP_TIME;
                    /*( statusRepetition + 1 ) * */
            case RANGE_UNAVAILABLE:
                // when step time is 1 it gives a sequence of
                // 1, 2, 4, 8, 16, 32...
                // this would cause the values to double on each repetition.
                
                // Java 6 - Math.scalb() might perform faster.
                return RANGE_UNAVAILABLE_STEP_TIME * (long)Math.pow( 2, errorStatusRepetition );
            case REMOTLY_QUEUED:
                if ( xQueueParameters == null )
                {
                    return 0;
                }
                else
                {
                    return xQueueParameters.getRequestSleepTime();
                }
            default:
                //logger.warn( "Unknown error status: {}", errorStatus );
                return 0;
        }
    }

    /**
     * Manually forces a connection retry, when the candidate is in a
     * valid state.
     * The method should only be called from user triggered GUI action.
     */
    public void manualConnectionRetry()
    {
        if ( status != CandidateStatus.BUSY &&
             status != CandidateStatus.CONNECTION_FAILED &&
             status != CandidateStatus.RANGE_UNAVAILABLE &&
             status != CandidateStatus.BAD &&
             status != CandidateStatus.IGNORED )
        {
            return;
        }
        setStatus( CandidateStatus.WAITING );
    }
        
    /**
     * Returns if the candidate is able to be allocated. To be allocated a
     * candidate must not have a worker assigned and the nextRetryTime must be
     * passed.
     */
    public boolean isAbleToBeAllocated( )
    {
        // Do not allow allocation if it's too slow!
        if (lastTransferRateBPS < DownloadPrefs.CandidateMinAllowedTransferRate.get().intValue()
            && lastTransferRateBPS > 0)
        {
            addToCandidateLog( "Refusing candidate allocation as last transfer rate was only " 
                + lastTransferRateBPS + " bps");
            //logger.debug( "Refusing candidate allocation as last transfer rate was only {} bps.",
            //    lastTransferRateBPS );
            return false;
        }
        long currentTime = System.currentTimeMillis();
        return statusTimeout <= currentTime;
    }

    public void associateDownloadSegment( SWDownloadSegment aSegment )
    {
        downloadSegment = aSegment;
    }

    /**
     * Returns the preferred (ie: largest) segment size to use for this candidate.
     * This will be DEFAULT_SEGMENT_SIZE initially, but will then be calculated so that if
     * the transfer rate for the previous segment were maintained, the next segment
     * would take DEFAULT_SEGMENT_TIME seconds.
     * The value MAXIMUM_ALLOWED_SEGMENT_SIZE is respected.
     */
    public long getPreferredSegmentSize()
    {
        long result;
        // No previous segment has been transferred
        if (lastTransferRateBPS == 0)
        {
            result = DownloadPrefs.SegmentInitialSize.get().intValue();
        }
        else
        {
            // default is rate * seconds, but not lower then a segmentMultiple.
            result = Math.max( lastTransferRateBPS * 
                DownloadPrefs.SegmentTransferTargetTime.get().intValue(),
                DownloadPrefs.SegmentMultiple.get().intValue() );
            // round the result up to the next multiple of segmentMultiple
            long remainder = (-result) % DownloadPrefs.SegmentMultiple.get().intValue();
            result += remainder;

            // Too fast (ie: segment would be bigger than allowed
            if (result > DownloadPrefs.SegmentMaximumSize.get().intValue() )
            {
                result = DownloadPrefs.SegmentMaximumSize.get().intValue();
            }
        }
        if ( result < 1 )
        {
            //logger.warn( "Preferred size looks strange. bps=" + lastTransferRateBPS 
//                + " and stt=" + DownloadPrefs.SegmentTransferTargetTime.get().intValue()
//                + " res " + result
//                + " res1 " + (lastTransferRateBPS * DownloadPrefs.SegmentTransferTargetTime.get().intValue())
//                + " res2 " + ((-result) % DownloadPrefs.SegmentMultiple.get().intValue() ) );
            result = DownloadPrefs.SegmentInitialSize.get().intValue();
        }
        //logger.debug( "Preferred segment size is {}", result);
        return result;
    }

    public void releaseDownloadSegment()
    {
        if ( downloadSegment != null )
        {
            lastTransferRateBPS = downloadSegment.getLongTermTransferRate();
            downloadSegment = null;
        }
    }

    /**
     * Provides the caller with the currently associated segment or null if no
     * association is available.
     * @return the associated segment or null.
     */
    public SWDownloadSegment getDownloadSegment()
    {
        return downloadSegment;
    }

    /**
     * Creates the DElement representation of this object to serialize it 
     * into XML.
     * @return the DElement representation of this object
     */
    public DDownloadCandidate createDDownloadCandidate()
    {
        DDownloadCandidate dCandidate = new DDownloadCandidate();
        dCandidate.setFileIndex( fileIndex );
        dCandidate.setFileName( fileName );
        if ( guid != null )
        {
            dCandidate.setGuid( guid.toHexString() );
        }
        if ( downloadURI != null )
        {
            dCandidate.setDownloadUri( downloadURI.getEscapedURI() );
        }
        // we need to store the resource urn since not all candidates
        // have one, and we need to identify them.
        if ( resourceURN != null )
        {
            dCandidate.setResourceUrn( resourceURN.getAsString() );
        }
        dCandidate.setPushNeeded( isPushNeeded );
        dCandidate.setChatSupported( isChatSupported );
        dCandidate.setRemoteHost( hostAddress.getFullHostName() );
        dCandidate.setVendor( vendor );
        if ( lastConnectionTime > 0 )
        {
            dCandidate.setLastConnectionTime( lastConnectionTime );
        }
        
        // also maintain count how often a connection was failed in a row...
        //if ( failedConnectionTries >= BAD_CANDIDATE_CONNECTION_TRIES )
        if ( failedConnectionTries > 0 )
        {
            dCandidate.setConnectionFailedRepetition( failedConnectionTries );
        }
        return dCandidate;
    }

    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer( "[Candidate: ");
        if ( vendor != null )
        {
            buffer.append( vendor );
            buffer.append( ',' );
        }
        buffer.append( "Adr:" );
        buffer.append( hostAddress );
        buffer.append( " ->" );
        buffer.append( super.toString() );
        buffer.append( "]" );
        return buffer.toString();
    }
    
    public void addToCandidateLog( String message )
    {
        if ( candidateLogBuffer != null )
        {
            LogRecord record = new LogRecord( this, message );
            candidateLogBuffer.addLogRecord( record );
        }
    }
    
    public void addToCandidateLog( Throwable th )
    {
        if ( candidateLogBuffer != null )
        {
            StackTraceElement[] stackTrace = th.getStackTrace();
            if ( stackTrace == null )
            {
                return;
            }
            for ( int i = 0; i < 2 && i < stackTrace.length; i++ )
            {
                LogRecord record = new LogRecord( this, stackTrace[i].toString() );
                candidateLogBuffer.addLogRecord( record );
            }
        }
    }
    
    
    ///////////////////// START event handling methods ////////////////////////

    private void fireCandidateStatusChange( CandidateStatus oldStatus, CandidateStatus newStatus )
    {
        eventService.publish( PhexEventTopics.Download_Candidate_Status,
            new ChangeEvent( this, oldStatus, newStatus ) );
    }

    ///////////////////// END event handling methods ////////////////////////
}