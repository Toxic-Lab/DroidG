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
 *  --- CVS Information ---
 *  $Id$
 */
package phex.upload;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import phex.common.AddressCounter;
import phex.common.Environment;
import phex.common.Phex;
import phex.common.address.DestAddress;
import phex.common.bandwidth.BandwidthController;
import phex.common.log.LogBuffer;
import phex.event.ContainerEvent;
import phex.event.PhexEventTopics;
import phex.event.ContainerEvent.Type;
import phex.http.HTTPRequest;
import phex.net.connection.Connection;
import phex.prefs.core.UploadPrefs;
import phex.servent.Servent;

public class UploadManager
{   
    private final Servent servent;
    
    private final AddressCounter uploadIPCounter;

    private final List<UploadState> uploadStateList;

    private final List<UploadState> queuedStateList;
    
    private LogBuffer uploadStateLogBuffer;

    public UploadManager( Servent servent )
    {
        this.servent = servent;
        uploadStateList = new ArrayList<UploadState>();
        queuedStateList = new ArrayList<UploadState>();
        uploadIPCounter = new AddressCounter( 
            UploadPrefs.MaxUploadsPerIP.get().intValue(), false );
        if ( UploadPrefs.UploadStateLogBufferSize.get().intValue() > 0 )
        {
            uploadStateLogBuffer = new LogBuffer( UploadPrefs.UploadStateLogBufferSize.get().intValue() );
        }

        Environment.getInstance().scheduleTimerTask(
            new CleanUploadStateTimer(), CleanUploadStateTimer.TIMER_PERIOD,
            CleanUploadStateTimer.TIMER_PERIOD );
    }
    
    public BandwidthController getUploadBandwidthController()
    {
        return servent.getBandwidthService().getUploadBandwidthController();
    }

    public void handleUploadRequest( Connection connection, HTTPRequest httpRequest)
    {
        UploadEngine uploadEngine = new UploadEngine( connection, httpRequest, 
            this, servent.getSharedFilesService() );
        uploadEngine.startUpload();
    }

    /**
     * Returns true if all upload slots are filled.
     */
    public boolean isHostBusy()
    {
        if ( getUploadingCount() >= UploadPrefs.MaxParallelUploads.get().intValue() ) 
        { return true; }
        return false;
    }

    /**
     * Returns true if all queue slots are filled.
     */
    public boolean isQueueLimitReached()
    {
        if ( queuedStateList.size() >= UploadPrefs.MaxQueueSize.get().intValue() ) 
        { return true; }
        return false;
    }
    
    public boolean validateAndCountAddress( DestAddress address )
    {
        synchronized (uploadIPCounter)
        {
            // update count...
            uploadIPCounter.setMaxCount( UploadPrefs.MaxUploadsPerIP.get().intValue() );
            return uploadIPCounter.validateAndCountAddress( address );
        }
    }

    public void releaseUploadAddress( DestAddress address )
    {
        synchronized (uploadIPCounter)
        {
            uploadIPCounter.relaseAddress( address );
        }
    }
    
    public LogBuffer getUploadStateLogBuffer()
    {
        return uploadStateLogBuffer;
    }

    ///////////////////// Collection access methods ////////////////////////////

    public void addUploadState(UploadState uploadState)
    {
        synchronized (uploadStateList)
        {
            int position = uploadStateList.size();
            uploadStateList.add( uploadState );
            fireUploadStateAdded( uploadState, position );
        }
    }
    
    public boolean containsUploadState( UploadState uploadState )
    {
        synchronized (uploadStateList)
        {
            return uploadStateList.contains(uploadState);
        }
    }

    /**
     * Returns the number of all files in the upload list. Also with state
     * completed and aborted.
     */
    public int getUploadListSize()
    {
        synchronized (uploadStateList)
        {
            return uploadStateList.size();
        }
    }

    /**
     * Returns only the number of files that are currently getting uploaded.
     * TODO it's better to maintain the number of files in an attribute...
     */
    public int getUploadingCount()
    {
        int count = 0;
        synchronized (uploadStateList)
        {
            for ( UploadState state : uploadStateList )
            {
                if ( state.isUploadRunning() )
                {
                    count++;
                }
            }
        }
        return count;
    }

    public UploadState getUploadStateAt(int index)
    {
        synchronized (uploadStateList)
        {
            if ( index < 0 || index >= uploadStateList.size() ) { return null; }
            return uploadStateList.get( index );
        }
    }

    public UploadState[] getUploadStatesAt(int[] indices)
    {
        synchronized (uploadStateList)
        {
            int length = indices.length;
            UploadState[] states = new UploadState[length];
            int listSize = uploadStateList.size();
            for (int i = 0; i < length; i++)
            {
                if ( indices[i] < 0 || indices[i] >= listSize )
                {
                    states[i] = null;
                }
                else
                {
                    states[i] = uploadStateList.get( indices[i] );
                }
            }
            return states;
        }
    }

    public void removeUploadState(UploadState state)
    {
        state.stopUpload();
        synchronized (uploadStateList)
        {
            int idx = uploadStateList.indexOf( state );
            if ( idx != -1 )
            {
                uploadStateList.remove( idx );
                fireUploadStateRemoved( state, idx );
            }
        }

        synchronized (queuedStateList)
        {
            int idx = queuedStateList.indexOf( state );
            if ( idx != -1 )
            {
                queuedStateList.remove( idx );
                //fireQueuedFileRemoved( idx );
            }
        }
    }

    /**
     * Removes uploads that are in a ready for cleanup state.
     */
    public void cleanUploadStateList()
    {
        synchronized (uploadStateList)
        {
            for (int i = uploadStateList.size() - 1; i >= 0; i--)
            {
                UploadState state = uploadStateList.get( i );
                if ( state.isReadyForCleanup() )
                {
                    uploadStateList.remove( i );
                    fireUploadStateRemoved( state, i );
                }
            }
        }
    }

    private class CleanUploadStateTimer extends TimerTask
    {
        private static final long TIMER_PERIOD = 1000 * 10;

        @Override
        public void run()
        {
            try
            {
                if ( UploadPrefs.AutoRemoveCompleted.get().booleanValue() )
                {
                    cleanUploadStateList();
                }
            }
            catch ( Throwable th )
            {
                //NLogger.error( CleanUploadStateTimer.class, th, th );
            }
        }
    }

    ////////////////////// queue collection methods ////////////////////////////

    public int addQueuedUpload(UploadState uploadState)
    {
        int position;
        synchronized (queuedStateList)
        {
            position = queuedStateList.size();
            queuedStateList.add( uploadState );
        }
        //dumpQueueInfo();
        return position;
    }

    public void removeQueuedUpload(UploadState uploadState)
    {
        int position;
        synchronized (queuedStateList)
        {
            position = queuedStateList.indexOf( uploadState );
            if ( position != -1 )
            {
                queuedStateList.remove( position );
            }
        }
        //dumpQueueInfo();
    }

    public int getQueuedPosition(UploadState state)
    {
        synchronized (queuedStateList)
        {
            return queuedStateList.indexOf( state );
        }
    }

    /**
     * Returns the number of all files in the upload queue list.
     */
    public int getUploadQueueSize()
    {
        synchronized (queuedStateList)
        {
            return queuedStateList.size();
        }
    }

    /*public void dumpQueueInfo()
     {
     System.out.println( "---------------------------------" );
     synchronized( queuedStateList )
     {
     Iterator iterator = queuedStateList.iterator();
     while( iterator.hasNext() )
     {
     Object obj = iterator.next();
     System.out.println( obj );
     }
     }
     System.out.println( "---------------------------------" );
     }*/

    ///////////////////// START event handling methods /////////////////////////

    private void fireUploadStateAdded( UploadState uploadState, int position )
    {
        Phex.getEventService().publish( PhexEventTopics.Upload_State,
            new ContainerEvent( Type.ADDED, uploadState, this, position ) );
    }

    private void fireUploadStateRemoved( UploadState uploadState, int position )
    {
        Phex.getEventService().publish( PhexEventTopics.Upload_State,
            new ContainerEvent( Type.REMOVED, uploadState, this, position ) );
    }

    ///////////////////// END event handling methods ////////////////////////
}