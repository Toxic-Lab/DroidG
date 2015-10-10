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
package phex.upload;

import java.util.HashSet;
import java.util.Set;

import phex.common.AlternateLocation;
import phex.common.TransferDataProvider;
import phex.common.URN;
import phex.common.address.DestAddress;
import phex.common.bandwidth.TransferAverage;
import phex.common.log.LogRecord;
import phex.prefs.core.UploadPrefs;


public class UploadState implements TransferDataProvider
{
    /**
     * The upload manager.
     */
    private final UploadManager uploadManager;

    /**
     * Defines the length already uploaded.
     */
    private long transferredDataSize;

    /**
     * Used to store the current progress.
     */
    private int currentProgress;

    /**
     * The status of the upload
     */
    private UploadStatus status;

    /**
     * The host address that request the upload.
     */
    private DestAddress hostAddress;

    /**
     * The vendor that requests the upload.
     */
    private String vendor;

    private String fileName;
    private URN fileURN;

    /**
     * The upload engine working on this upload or null if not available.
     */
    private UploadEngine uploadEngine;

    /*
     * Total data sent previously with the same connection
     */
    private long previousSegmentsSize;
    
    private TransferAverage transferAverage;
    
    /**
     * This is a List holding all AltLocs already send to this connection during
     * this session. It is used to make sure the same AltLocs are not send twice
     * to the same connection.
     */
    private Set<AlternateLocation> sendAltLocSet;

    /**
     * This is used to create a upload state object that is used for displaying
     * it in the upload queue.
     * @param hostAddress the host address of the host.
     * @param vendor the vendor string of the host.
     */
    public UploadState( DestAddress hostAddress, String vendor, UploadManager uploadManager )
    {
        this( hostAddress, vendor, null, null, -1, uploadManager );
    }

    public UploadState( DestAddress hostAddress, String vendor,
        String fileName, URN fileURN, long contentLength, UploadManager uploadManager )
    {
        this.uploadManager = uploadManager;
        transferredDataSize = 0;
        previousSegmentsSize = 0;
        currentProgress = 0;
        sendAltLocSet = new HashSet<AlternateLocation>();

        this.hostAddress = hostAddress;
        this.vendor = vendor;
        this.fileName = fileName;
        this.fileURN = fileURN;
        transferLength = contentLength;
        status = UploadStatus.ACCEPTING_REQUEST;
        
        transferAverage = new TransferAverage( 1000, 10 );
    }

    public void update( String fileName, URN fileURN, long contentLength )
    {
        this.fileName = fileName;
        this.fileURN = fileURN;
        transferLength = contentLength;
    }
    
    public Set<AlternateLocation> getSendAltLocSet()
    {
        return sendAltLocSet;
    }

    public String getVendor()
    {
        return vendor;
    }
    
    public void setVendor( String vendor )
    {
        this.vendor = vendor;
    }

    public String getFileName()
    {
        return fileName;
    }
    
    public void setFileName( String fileName )
    {
        this.fileName = fileName;
    }

    public URN getFileURN()
    {
        return fileURN;
    }

    public DestAddress getHostAddress()
    {
        return hostAddress;
    }
    
    public void setHostAddress( DestAddress hostAddress )
    {
        this.hostAddress = hostAddress;
    }

    public UploadStatus getStatus()
    {
        return status;
    }
    
    public void addToUploadLog( String message )
    {
        if ( UploadPrefs.UploadStateLogBufferSize.get().intValue() > 0 )
        {
            LogRecord record = new LogRecord( this, message );
            uploadManager.getUploadStateLogBuffer().addLogRecord( record );
        }
    }

    public void setStatus( UploadStatus newStatus )
    {
        // dont care for same status
        if ( status == newStatus )
        {
            return;
        }
/*        NLogger.debug( UploadState.class, "UploadState Status "
            + newStatus );*/
        addToUploadLog( "UploadState Status " + newStatus );

        this.status = newStatus;
    }

    public boolean isUploadRunning()
    {
        return status == UploadStatus.HANDSHAKE ||
               status == UploadStatus.UPLOADING_THEX ||
               status == UploadStatus.UPLOADING_DATA;
    }
    
    public boolean isReadyForCleanup()
    {
        return status == UploadStatus.COMPLETED ||
               status == UploadStatus.ABORTED;
    }

    public void setUploadEngine( UploadEngine uploadEngine )
    {
        this.uploadEngine = uploadEngine;
    }

    public void setTransferredDataSize( long aTransferredSize )
    {
        long diff = aTransferredSize - transferredDataSize;
        if ( diff < 0 ) // a new block is being transferred
        {
            previousSegmentsSize += transferredDataSize;
            transferAverage.addValue(aTransferredSize);
        } 
        else
        {
            transferAverage.addValue(diff);
        }
        transferredDataSize = aTransferredSize;
    }

    /**
     * Returns the progress in percent. If mStatus == sCompleted will always be 100%.
     */
    public int getProgress()
    {
        if( status == UploadStatus.COMPLETED )
        {
            currentProgress = 100;
        }
        else
        {
            long toTransfer = getTransferDataSize();
            currentProgress = (int)(getTransferredDataSize() * 100L / (toTransfer == 0L ? 1L : toTransfer));
        }
        return currentProgress;
    }

    public void stopUpload()
    {
        if ( uploadEngine != null )
        {
            uploadEngine.stopUpload();
        }
        setStatus( UploadStatus.ABORTED );
    }
    
    /**
     * Returns the transfer speed from the bandwidth controller of this upload.
     * @return the transfer.
     */
    public long getTransferSpeed()
    {
        return transferAverage.getAverage();
    }

    ////////////////////// TransferDataProvider Interface //////////////////////

    private long transferLength;

    public long getTransferDataSize()
    {
        return transferLength + previousSegmentsSize;
    }

    /**
     * Indicate how much of the file has been uploaded on this transfer.
     */
    public long getTransferredDataSize()
    {
        return transferredDataSize + previousSegmentsSize;
    }

    /**
     * This is the total size of the available data. Even if its not important
     * for the transfer itself.
     */
    public long getTotalDataSize()
    {
        // in case of upload this is the same as the size that is transfered.
        return getTransferDataSize();
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
        switch ( status )
        {
            case HANDSHAKE:
            case UPLOADING_DATA:
            case UPLOADING_THEX:
                return TransferDataProvider.TRANSFER_RUNNING;
            case ABORTED:
                return TransferDataProvider.TRANSFER_ERROR;
            case COMPLETED:
                return TransferDataProvider.TRANSFER_COMPLETED;
            default:
                return TransferDataProvider.TRANSFER_NOT_RUNNING;
        }
    }
}