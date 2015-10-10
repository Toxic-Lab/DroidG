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
package phex.share;

import java.io.File;
import java.util.List;

import phex.common.AltLocContainer;
import phex.common.AlternateLocation;
import phex.common.URN;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.http.Range;
import phex.http.Range.RangeAvailability;
import phex.net.repres.PresentationManager;
import phex.thex.FileHashCalculationHandler;
import phex.thex.ShareFileThexData;
import phex.xml.sax.share.DAlternateLocation;
import phex.xml.sax.share.DSharedFile;

public class ShareFile extends SharedResource
{
    /**
     * The unique file index;
     */
    private int fileIndex;

    /**
     * The file size ( file.length() ). Buffered because of performance reasons.
     */
    private long fileSize;

    /**
     * The urn of the file.
     */
    private URN urn;
    
    /**
     * The THEX data of the shared files.
     */
    private ShareFileThexData thexData;

    /**
     * The number of times the file was searched for.
     */
    private int searchCount;

    /**
     * The number of times the file was requested for upload.
     */
    private int uploadCount;

    /**
     * A ArrayList of AlternateLocations of the share file.
     */
    private AltLocContainer alternateLocations;
    
    /**
     * The if possible network wide creation time of this file.
     * Since this object is stored in a map in SharedFilesService it makes
     * sense to hold it as an Object.
     */
    private Long networkCreateTime;

    /**
     * Creates a new ShareFile with its backed file object.
     * @param aFile the backed file object.
     */
    public ShareFile( File aFile )
    {
        super( aFile );

        fileSize = systemFile.length();
        searchCount = 0;
        uploadCount = 0;
        
        networkCreateTime = Long.valueOf( aFile.lastModified() );
    }

    /**
     * Called by subclass to initialize.
     */
    protected ShareFile(long aFileSize)
    {
        fileSize = aFileSize;
    }

    /**
     * Returns the file urn.
     * @return the file urn.
     */
    public URN getURN()
    {
        return urn;
    }

    /**
     * @param urn The urn to set.
     */
    public void setURN(URN urn)
    {
        this.urn = urn;
    }
    
    /**
     * Returns the sha1 nss value of the urn if available.
     * @return the sha1 nss value of the urn if available.
     */
    public String getSha1()
    {
        if (urn == null || !urn.isSha1Nid())
        {
            return "";
        }
        return urn.getNamespaceSpecificString();
    }

    /**
     * Returns the unique file index.
     * @return the unique file index.
     */
    public int getFileIndex()
    {
        return fileIndex;
    }

    /**
     * Sets the file index. It must be unique over all ShareFile object
     * @param index the unique file index.
     */
    public void setFileIndex(int index)
    {
        fileIndex = index;
    }

    /**
     * Returns the thex data if they are already available otherwise it schedules
     * calculation in case hashCalcQueue is not null. Null is returned until
     * the calcualtion results are available.
     * @param hashCalcQueue if not null the thex calculation will be queued with
     *    it if SharedFileThexData is not yet available.  
     * @return the thex data if already available, null otherwise.
     */
    public ShareFileThexData getThexData( FileHashCalculationHandler hashCalcQueue )
    {
        if ( thexData == null && urn != null && hashCalcQueue != null && fileSize > 0)
        {// if there is no thex data and we have already calculated SHA1 urn,
         // schedule a calculation worker. 
            hashCalcQueue.queueThexCalculation( this );
        }
        return thexData;
    }
    
    public void setThexData( ShareFileThexData thexData )
    {
        this.thexData = thexData;
    }
    
    public Long getNetworkCreateTime()
    {
        return networkCreateTime;
    }

    /**
     * Returns the file size.
     * @return the file size.
     */
    public long getFileSize()
    {
        return fileSize;
    }

    /**
     * Checks if the requested range is satisfiable.
     * @param range the requested range.
     * @return true if the requested range is satisfiable, false otherwise.
     */
    public RangeAvailability getRangeAvailableStatus(Range range)
    {
        long startOffset = range.getStartOffset( fileSize );
        if (startOffset < 0 || startOffset >= fileSize )
        {
            return RangeAvailability.RANGE_NOT_SATISFIABLE;
        }
        else
        {
            return RangeAvailability.RANGE_AVAILABLE;
        }
    }

    /**
     * Returns the container of all known alternate download locations.
     * @return the container of all known alternate download locations.
     */
    public AltLocContainer getAltLocContainer()
    {
        if (alternateLocations == null && urn != null )
        {// initialize when first time requested.
            alternateLocations = new AltLocContainer(urn);
        }
        return alternateLocations;
    }
    
    public int getAltLocCount()
    {
        if ( alternateLocations == null )
        {
            return 0;
        }
        else
        {
            return alternateLocations.getSize();
        }
    }

    /**
     * Returns the number of times the file was searched for.
     * @return the number of times the file was searched for.
     */
    public int getSearchCount()
    {
        return searchCount;
    }

    /**
     * Increments the search counter by one.
     */
    public void incSearchCount()
    {
        searchCount++;
    }

    /**
     * Returns the number of times the file was uploaded.
     * @return the number of times the file was uploaded.
     */
    public int getUploadCount()
    {
        return uploadCount;
    }

    /**
     * Increments the upload counter by one.
     */
    public void incUploadCount()
    {
        uploadCount++;
    }

    /**
     * Updates the searchCount, uploadCount and urn from the cached XMLSharedFile
     * object that is used to make ShareFile data persistend.
     * @param dFile the cached XJBSharedFile
     * object that is used to make ShareFile data persistend.
     */
    public void updateFromCache( DSharedFile dFile )
    {
        searchCount = dFile.getHitCount();
        uploadCount = dFile.getUploadCount();
        urn = new URN("urn:sha1:" + dFile.getSha1());
        networkCreateTime = Long.valueOf( dFile.getCreationTime() );
        
        String rootHash = dFile.getThexRootHash();
        if ( rootHash != null )
        {
            String xjbLowestLevelNodes = dFile.getThexLowestLevelNodes();
            int depth = dFile.getThexTreeDepth();
            if ( thexData == null )
            {
                thexData = new ShareFileThexData( rootHash, xjbLowestLevelNodes, 
                    depth );
            }
            else
            {
                thexData.updateFromCache( rootHash, xjbLowestLevelNodes, depth );
            }
        }
        List<DAlternateLocation> list = dFile.getAltLocList();
        for ( DAlternateLocation dAltLoc : list )
        {
            try
            {
                String hostAddress = dAltLoc.getHostAddress();
                String altLocUrn = dAltLoc.getUrn();
                if (altLocUrn != null)
                {
                    DestAddress address = PresentationManager.getInstance().
                        createHostAddress( hostAddress, DefaultDestAddress.DEFAULT_PORT );
                    AlternateLocation altLoc = new AlternateLocation( address,
                        new URN(altLocUrn) );
                    getAltLocContainer().addAlternateLocation(altLoc);
                }
            }
            catch (Exception exp)
            {
//                NLogger.error( ShareFile.class, 
//                    "AlternateLocation skipped due to error.", exp );
            }
        }
    }

    public DSharedFile createDSharedFile()
    {
        DSharedFile dFile = new DSharedFile();
        dFile.setFileName(systemFile.getAbsolutePath());
        dFile.setSha1( getSha1() );
        if ( thexData != null )
        {
            dFile.setThexTreeDepth( thexData.getTreeDepth() );
            dFile.setThexRootHash( thexData.getRootHash() );
            dFile.setThexLowestLevelNodes( thexData.getXJBLowestLevelNodes() );
        }
        dFile.setLastModified(systemFile.lastModified());
        dFile.setHitCount( searchCount );
        dFile.setUploadCount( uploadCount );
        dFile.setCreationTime( networkCreateTime.longValue() );

        if (alternateLocations != null)
        {
            alternateLocations.createDAlternateLocationList(
                dFile.getAltLocList());
        }
        return dFile;
    }

    public String toString()
    {
        return super.toString() + " " + getFileName() + "  " + fileIndex;
    }
    
    public void clearSearchCounters()    
    {   
        searchCount=0;
    }

    public void clearUploadCounters()
    {
        uploadCount=0;
    }
}
