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

import phex.common.TransferDataProvider;
import phex.common.bandwidth.TransferAverage;

/**
 * Defines a download segment. A download segment is one piece of the download file
 * the segment describes the start position of the segment and the length. Also
 * the segment has a link to it's following segement. This link is used to be able
 * to merge following segments after donwload. Also the files that correspond
 * to these segments will be merged once we have a sequence of downloaded segments.
 */
public class SWDownloadSegment 
    implements TransferDataProvider, SWDownloadConstants
{
    /**
     * The start offset of the download scope, inclusive.
     * Expected: start <= end
     */
    private long start;
    
    private long length;
    
    /**
     * Defines the bytes already downloaded.
     */
    private long transferredDataSize;

    /**
     * Used to store the current progress.
     */
    private Integer currentProgress;

    /**
     * The download file that this segment belongs to.
     */
    private SWDownloadFile downloadFile;

    /**
     * Transfer start time
     */
    private long transferStartTime;

    /**
     * Transfer stop time
     */
    private long transferStopTime;
    
    private TransferAverage transferAverage;
        
    /**
     * 
     * @param aDownloadFile
     * @param aStartPos
     * @param aLength
     */
    public SWDownloadSegment( SWDownloadFile aDownloadFile, long aStartPos, long aLength )
    {
        start = aStartPos;
        length = aLength;
        downloadFile = aDownloadFile;
        transferredDataSize = 0;
        currentProgress = Integer.valueOf( 0 );
        transferAverage = new TransferAverage( 1000, 6 );
    }

    /**
     * Returns the start position of the transfer. The start position depends on
     * the segment start position plus the already transferred data size.
     * During a transfer this start position will move.
     */
    public long getTransferStartPosition()
    {
        return start + transferredDataSize;
    }

    /**
     * Returns the stop position of the segment, inclusive.
     */
    public long getEnd()
    {
        if ( length == UNKNOWN_FILE_SIZE )
        {
            return -1;
        }
        return start + length - 1;
    }
    
    /**
     * Returns the start position of the segment, inclusive.
     */
    public long getStart()
    {
        return start;
    }

    /**
     * Returns the length that is left to download.
     */
    public long getTransferDataSizeLeft()
    {
        if ( length == UNKNOWN_FILE_SIZE )
        {
            return -1;
        }
        return Math.max( 0, length - transferredDataSize );
    }

    /**
     * Sets the size of the data that has been transferred.
     */
    public void setTransferredDataSize( long size )
    {
        if ( size < transferredDataSize )
        {
            throw new IllegalArgumentException( "Transfered data size is not allowed to go down!");
        }
        if ( length > UNKNOWN_FILE_SIZE && size > length )
        {
            throw new IllegalArgumentException( "Transfered data size is not to grow beyond segments size!");
        }
            
        long diff = size - transferredDataSize;
        transferAverage.addValue(diff);
        transferredDataSize = size;
    }

    /**
     * Indicate that the download is just starting.
     */
    public void downloadStartNotify()
    {
        transferStartTime = System.currentTimeMillis();
        transferStopTime = 0;
    }

    /**
     * Indicate that the download is no longer running.
     */
    public void downloadStopNotify()
    {
        // Ignore nested calls.
        if( transferStopTime == 0 )
        {
            transferStopTime = System.currentTimeMillis();
        }
    }

    /**
     * Returns the progress in percent. If status == completed will always be 100%.
     */
    public Integer getProgress()
    {
        int percentage;
        long transferDataSize = getTransferDataSize();
        if ( transferDataSize > 0 )
        {
            percentage = (int)( getTransferredDataSize() * 100L / transferDataSize );
        }
        else
        {
            percentage = 0;
        }
        

        if ( currentProgress.intValue() != percentage )
        {
            // only create new object if necessary
            currentProgress = Integer.valueOf( percentage );
        }

        return currentProgress;
    }
        
    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( getClass().getName() );
        buffer.append( "[start: " );
        buffer.append( start );
        buffer.append( ", so far: " );
        buffer.append( transferredDataSize );
        buffer.append( " of " );
        buffer.append( length );
        buffer.append( "]@" );
        buffer.append( hashCode() );
        buffer.append( "\n" );
        return buffer.toString();
    }
    
    private void validateTransferredDataSize()
    {
        if ( length != UNKNOWN_FILE_SIZE &&
             transferredDataSize > length )
        {
//            NLogger.error( SWDownloadFile.class, 
//                "Transferred data size above transfer data size: " + toString() );
        }
    }
    
    /**
     * Returns the transfer speed from the bandwidth controller of this download.
     * @return
     */
    public long getTransferSpeed()
    {
        if ( transferStopTime > 0 && (transferStopTime - transferStartTime) < 1000 )
        { // transfer was faster then 1 sec.. we likely dont have speed data collected.
          // we return the full transferred data size
            return transferredDataSize;
        }
        return transferAverage.getAverage();
    }
    
    //////// START TransferDataProvider Interface ///////////
    
    /**
     * Returns the data size that has already be transferred.
     */
    public long getTransferredDataSize()
    {
        return transferredDataSize;
    }

    /**
     * This is the total size of the available data. Even if its not importend
     * for the transfer itself.
     */
    public long getTotalDataSize()
    {
        return getTransferDataSize();
    }
    
    /**
     * Returns the length of the segment.
     */
    public long getTransferDataSize()
    {
        return length;
    }
    
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
        if ( transferStartTime != 0 && transferStopTime == 0 )
        {
            return TRANSFER_RUNNING;
        }
        else
        {
            return TRANSFER_NOT_RUNNING;
        }
    }
    
    //////// END TransferDataProvider Interface ///////////
}
