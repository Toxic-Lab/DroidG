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

import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.time.DateUtils;

import phex.common.ThreadTracking;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.prefs.core.DownloadPrefs;

/**
 * Extra thread that is responsible to write buffered download data to
 * disk regulary.
 */
public class DownloadDataWriter implements Runnable
{
    private Thread thread;
    private boolean isShutingDown;
    private long lastCompleteWrite;
    private boolean isWriteCycleRequested;
    private SwarmingManager swarmingMgr;
    
    public DownloadDataWriter( SwarmingManager downloadService )
    {
        swarmingMgr = downloadService;
    }
    
    public void start()
    {
        isShutingDown = false;
        thread = new Thread( ThreadTracking.rootThreadGroup, this,
            "DownloadDataWriter" );
        thread.setDaemon( true );
        thread.start();
    }
    
    /**
     * This call performs a shutdown of the download data writer thread.
     * The call blocks until the DownloadDataWriter thread died.
     */
    public void shutdown()
    {
        isShutingDown = true;
        // trigger thread cycle
        triggerWriteCycle();
        try
        {
            thread.join();
        }
        catch ( InterruptedException exp )
        {
            //NLogger.error( DownloadDataWriter.class, exp, exp );
            Thread.currentThread().interrupt();
        }
        
        // perform a last write operation to ensure all data is on disk
        writeDownloadData();
    }

    public void run()
    {
        while ( !isShutingDown )
        {
            try
            {
                do
                {
                    writeDownloadData();
                }
                while ( isWriteCycleRequested );
                // loop around write cycles as long as data is filling...
                // this is necessary for very fast downloads..
                waitForNotify();
            }
            catch (Throwable th)
            {
                //NLogger.error( DownloadDataWriter.class, th, th );
            }
        }
    }
    
    private synchronized void waitForNotify()
    {
        //NLogger.debug(DownloadDataWriter.class, "Waiting..." );
        try
        {
            wait( 5000 );
        }
        catch (InterruptedException exp)
        {
            //NLogger.error( DownloadDataWriter.class, exp, exp );
        }
        //NLogger.debug(DownloadDataWriter.class, "Woke..." );
    }
    
    public synchronized void triggerWriteCycle()
    {
        //NLogger.debug(DownloadDataWriter.class, "Triggering write cycle." );
        isWriteCycleRequested = true;
        notifyAll();
    }
    
    private void writeDownloadData()
    {
        if ( !swarmingMgr.isDownloadActive() && !isWriteCycleRequested )
        {
            return;
        }
        
        long bufferedDataWritten = 0;
        long totalBufferedSize = 0;
        boolean performCompleteWrite = false;
        if ( isShutingDown || isWriteCycleRequested || 
             lastCompleteWrite + DateUtils.MILLIS_PER_MINUTE < System.currentTimeMillis() )
        {
            //NLogger.debug(DownloadDataWriter.class, "Time for complete write cycle." );
            isWriteCycleRequested = false;
            performCompleteWrite = true;
        }
        
        // write limit is 90% of configured max.
        int maxPerDownloadBuffer = DownloadPrefs.MaxWriteBufferPerDownload.get().intValue();
        maxPerDownloadBuffer = (int)(maxPerDownloadBuffer * 0.9);
        
        List<SWDownloadFile> downloadList = swarmingMgr.getDownloadFileListCopy();
        ListIterator<SWDownloadFile> iterator = downloadList.listIterator();
        while( iterator.hasNext() )
        {
            SWDownloadFile downloadFile = iterator.next();
            MemoryFile memoryFile = downloadFile.getMemoryFile();
            long bufferedSize = memoryFile.getBufferedDataLength();
            totalBufferedSize += bufferedSize;
            
            if ( performCompleteWrite || memoryFile.isBufferWritingRequested() ||
                 bufferedSize >= maxPerDownloadBuffer )
            {
                //NLogger.debug(DownloadDataWriter.class,
                 //   "Trigger buffer write for " + downloadFile + 
                 //   ", amount: " + bufferedSize );
                memoryFile.writeBuffersToDisk();
                bufferedDataWritten += bufferedSize;
                // remove from buffer since not needed anymore in possible
                // following complete write cycle
                iterator.remove();
            }
        }
        
        //NLogger.debug(DownloadDataWriter.class,
         //   "Total buffered data was: " + totalBufferedSize );
        
        // write limit is 90% of configured max.
        int maxTotalBuffer = DownloadPrefs.MaxTotalDownloadWriteBuffer.get().intValue();
        maxTotalBuffer = (int)(maxTotalBuffer * 0.9);
        
        // if we have not already written everything but have a high total buffer
        // size, we write down the complete remaining download buffers to disk.
        if ( !performCompleteWrite && totalBufferedSize >= maxTotalBuffer )
        {
            performCompleteWrite = true;
            iterator = downloadList.listIterator();
            while( iterator.hasNext() )
            {
                SWDownloadFile downloadFile = iterator.next();
                MemoryFile memoryFile = downloadFile.getMemoryFile();
                long bufferedSize = memoryFile.getBufferedDataLength();
                //NLogger.debug(DownloadDataWriter.class,
//                    "Trigger buffer write for " + downloadFile + 
//                    ", amount: " + bufferedSize );
                memoryFile.writeBuffersToDisk();
                bufferedDataWritten += bufferedSize;
            }
        }
        if ( performCompleteWrite )
        {
            lastCompleteWrite = System.currentTimeMillis();
        }
        if ( bufferedDataWritten > 0 )
        {
            swarmingMgr.notifyDownloadListChange();
        }
    }
}