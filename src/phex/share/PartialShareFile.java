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
import java.util.Iterator;

import phex.common.AltLocContainer;
import phex.common.FileHandlingException;
import phex.common.URN;
import phex.common.file.ManagedFileException;
import phex.download.swarming.SWDownloadFile;
import phex.http.HTTPRangeSet;
import phex.http.Range;
import phex.http.Range.RangeAvailability;
import phex.xml.sax.share.DSharedFile;
 
public class PartialShareFile extends ShareFile
{
    private SWDownloadFile swDownloadFile;
    private HTTPRangeSet availableRangeSet;
    private File partialFile;
    private long fileStartOffset;

    public PartialShareFile( SWDownloadFile downloadFile )
    {
        super( downloadFile.getTotalDataSize() );
        swDownloadFile = downloadFile;
        availableRangeSet = swDownloadFile.createAvailableRangeSet();
    }

    /**
     * Returns the file urn.
     * @return the file urn.
     */
    public URN getURN()
    {
        return swDownloadFile.getFileURN();
    }

    /**
     * Returns the sha1 nss value of the urn if available.
     * @return the sha1 nss value of the urn if available.
     */
    public String getSha1()
    {
        URN urn = swDownloadFile.getFileURN();
        if ( urn == null || !urn.isSha1Nid() )
        {
            return "";
        }
        return urn.getNamespaceSpecificString();
    }

    /**
     * Returns the file name without path information.
     * @return the file name without path information.
     */
    public String getFileName()
    {
        return swDownloadFile.getFileName();
    }

    /**
     * Returns the container of all known alternate download locations.
     * @return the container of all known alternate download locations.
     */
    public AltLocContainer getAltLocContainer()
    {
        return swDownloadFile.getGoodAltLocContainer();
    }

    /**
     * Checks if the requested range is satisfiable.
     * @param range the requested range.
     * @return true if the requested range is satisfiable, false otherwise.
     */
    public RangeAvailability getRangeAvailableStatus( Range requestedRange )
    {
        Range availableRange;
        long fileSize = getFileSize();
        Iterator<Range> iterator = availableRangeSet.getIterator();
        while ( iterator.hasNext() )
        {
            availableRange = iterator.next();
            if ( availableRange.isRangeSatisfiable( requestedRange, fileSize ) )
            {
                return RangeAvailability.RANGE_AVAILABLE;
            }
        }

        // no partial segment found check if range is not satisfiable or not available.
        long startOffset = requestedRange.getStartOffset( fileSize );
        if ( startOffset < 0 || startOffset >= fileSize )
        {
            return RangeAvailability.RANGE_NOT_SATISFIABLE;
        }
        else
        {
            return RangeAvailability.RANGE_NOT_AVAILABLE;
        }
    }

    /**
     * Returns the backed file object.
     * @return the backed file object.
     */
    public File getSystemFile()
    {
        return partialFile;
    }

    public String toString()
    {
        return super.toString() + " - Backed SWDownloadFile: "
            + swDownloadFile.toString();
    }

    /**
     * Finds a fitting part for the requested range and also adjustes the
     * requested range accordingly to fit the part.
     * @param requestedRange the range to find the fitting part for and to adjust.
     */
    public void findFittingPartForRange( Range requestedRange )
    {
        Range availableRange;
        Iterator iterator = availableRangeSet.getIterator();
        long fileSize = getFileSize();
        while ( iterator.hasNext() )
        {
            availableRange = (Range)iterator.next();
            if ( availableRange.isRangeSatisfiable( requestedRange, fileSize ) )
            {
                try
                {
                    partialFile = swDownloadFile.getIncompleteDownloadFile().getFile();
                    long startOffset = requestedRange.getStartOffset( fileSize );
                    long endOffset = Math.min( requestedRange.getEndOffset( fileSize ),
                        availableRange.getEndOffset( fileSize ) );
                    requestedRange.update( startOffset, endOffset );
                    fileStartOffset = startOffset;
                    return;
                }
                catch ( ManagedFileException exp )
                {
                    //NLogger.error( PartialShareFile.class, exp );
                }
                catch ( FileHandlingException exp )
                {
                    //NLogger.error( PartialShareFile.class, exp );
                }
            }
        }
    }

    public long getFileStartOffset()
    {
        return fileStartOffset;
    }

    public String buildXAvailableRangesString()
    {
        return availableRangeSet.buildXAvailableRangesString();
    }

    /**
     * This method is not supported.
     */
    public int getFileIndex()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported.
     */
    public void setFileIndex( int index)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported.
     */
    public Integer getSearchCountObject()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported.
     */
    public int getSearchCount()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported.
     */
    public void incSearchCount( )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported.
     */
    public Integer getUploadCountObject()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported.
     */
    public int getUploadCount()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported.
     */
    public void incUploadCount( )
    {// nothing to increment...
    }

    /**
     * This method is not supported.
     */
    public char[] getSearchCompareTerm()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported.
     */
    public void updateFromCache( DSharedFile dFile )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported.
     */
    public void calculateURN()
    {
        throw new UnsupportedOperationException();
    }
}
