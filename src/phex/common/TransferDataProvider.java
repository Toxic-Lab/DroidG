/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2009 Phex Development Group
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
package phex.common;

/**
 * This interface is used to define a standard set of method for all kind
 * of classes that provide information of transfered data like UploadFile and
 * DownloadFile.
 */
 // TODO integrate common transfer status for transfer files
public interface TransferDataProvider
{
    /**
     * 365 days in seconds...
     */
    static final int INFINITY_ETA_INT = 365 * 24 * 60 * 60;
    
    static final short TRANSFER_RUNNING = 10;
    static final short TRANSFER_NOT_RUNNING = 11;
    static final short TRANSFER_COMPLETED = 12;
    static final short TRANSFER_ERROR = 13;

    /**
     * Returns the size of the data that already has been transfered.
     */
    long getTransferredDataSize();

    /**
     * Return the size of data that is attempting to be transfered. This is
     * NOT necessarily the full size of the file as could be the case during
     * a download resumption.
     */
    long getTransferDataSize();

    /**
     * This is the total size of the available data. Even if its not importend
     * for the transfer itself.
     */
    long getTotalDataSize();

    /**
     * Return the data transfer status.
     * It can be TRANSFER_RUNNING, TRANSFER_NOT_RUNNING, TRANSFER_COMPLETED,
     * TRANSFER_ERROR.
     */
    short getDataTransferStatus();

    /**
     * Returns the long term data transfer rate in bytes. This is the rate of
     * the transfer since the last start of the transfer. This means after a
     * transfer was interrupted and is resumed again the calculation restarts.
     */
    int getLongTermTransferRate();

    /**
     * Returns the short term data transfer rate in bytes. The rate should depend
     * on the transfer rate timestamp.
     * An implementing class should provide an implementation like this
     * together with the setTransferRateTimestamp() method:
     *
     * <code>
     * public int getShortTermTransferRate()
     * {
     *    if ( transferRateTimestamp != 0 )
     *    {
     *       double sec = (System.currentTimeMillis() - transferRateTimestamp) / 1000;
     *       // don't drop transfer rate to 0 if we just have a new timestamp and
     *       // no bytes transfered
     *       if ( ( transferRateBytes > 0 || sec > 1 ) && sec != 0)
     *       {
     *           transferRate = (int) ( transferRateBytes / sec );
     *       }
     *    }
     *    return transferRate;
     * }
     * </code>
     */
    int getShortTermTransferRate();

    /**
     * To be able to provide a constantly valid short term data transfer rate
     * the time to calculate the data rate from needs to be updated.
     * If you want your timestamp to be updated regularly you need to register
     * your TransferDataProvider at the TransferRateService.
     * An implementing class should provide an implementation like this
     * together with the getDataTransferRate() method:
     *
     * <code>
     * /**
     *  * holds the timestamp.
     *  *\/
     * private long transferRateTimestamp;
     * /**
     *  * holds the transfered bytes since the last timestamp.
     *  *\/
     * private int transferRateBytes;
     * public void setTransferRateTimestamp( long timestamp )
     * {
     *    transferRateTimestamp = timestamp;
     *    transferRateBytes = 0;
     * }
     * </code>
     */
    void setTransferRateTimestamp( long timestamp );
}
