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

import java.util.ArrayList;
import java.util.List;

import phex.common.address.DestAddress;
import phex.download.swarming.SWDownloadCandidate;
import phex.msg.GUID;
import phex.net.repres.SocketFacade;
import phex.servent.Servent;
import phex.statistic.SimpleStatisticProvider;
import phex.statistic.StatisticsManager;

public class PushHandler
{
    /**
     * This is a stand alone global class responsible for push handling.
     */
    private static PushHandler singleton = new PushHandler();

    /**
     * A list is used instead of a map since this will never contain many
     * entries. And it's hard to create a useful key since the file index might
     * change.
     */
    private ArrayList<PushRequestSleeper> pushSleeperList;

    private PushHandler()
    {
        pushSleeperList = new ArrayList<PushRequestSleeper>(5);
    }

    public static void handleIncommingGIV(SocketFacade aGivenSocket, GUID givenGUID,
        String givenFileName)
    {
        singleton.internalHandleIncommingGIV(aGivenSocket, givenGUID,
            givenFileName);
    }

    public static SocketFacade requestSocketViaPush( Servent servent,
        SWDownloadCandidate downloadCandidate )
    {
        if ( downloadCandidate.getGUID() == null ) { return null; }
        return singleton.internalRequestSocketViaPush( servent,
            downloadCandidate.getGUID(),
            downloadCandidate.getFileIndex(), 
            downloadCandidate.getPushProxyAddresses() );
    }

    /**
     *
     * @param aClientGUID
     * @param aFileIndex
     * @param aFileName
     * @return Returns null if push request fails.
     */
    public static SocketFacade requestSocketViaPush( Servent servent, 
        GUID aClientGUID, long aFileIndex )
    {
        return singleton.internalRequestSocketViaPush( servent,
            aClientGUID, aFileIndex, null);
    }

    public static void unregisterPushRequestSleeper(PushRequestSleeper sleeper)
    {
        singleton.internalUnregisterPushRequestSleeper(sleeper);
    }

    private void internalHandleIncommingGIV(SocketFacade aGivenSocket,
        GUID givenGUID, String givenFileName)
    {
        //NLogger.debug( PushHandler.class, 
        //    "Handle incomming GIV response: " + givenFileName);
        
        // to prevent deadlocks with SWDownloadWorker inside PushRequestSleeper
        // create a copy of the pushSleeperList.
        List<PushRequestSleeper> sleeperList;
        synchronized (pushSleeperList)
        {
            sleeperList = new ArrayList<PushRequestSleeper>( pushSleeperList );
        }
        for ( PushRequestSleeper sleeper : sleeperList )
        {
            boolean succ = sleeper.acceptGIVConnection(aGivenSocket, givenGUID);
            if ( succ )
            {
                //NLogger.debug( PushHandler.class, 
                 //   "Accepted GIV response: " + givenFileName);
                return;
            }
        }        
        //NLogger.debug( PushHandler.class, 
          //  "No Push request for GIV found: " + givenFileName);
    }

    private SocketFacade internalRequestSocketViaPush(Servent servent,
        GUID aClientGUID, long aFileIndex, DestAddress[] pushProxyAddresses )
    {
        //NLogger.debug( PushHandler.class, "Perform PUSH request..." );
        
        ((SimpleStatisticProvider)servent.getStatisticsService().getStatisticProvider(
            StatisticsManager.PUSH_DOWNLOAD_ATTEMPTS_PROVIDER)).increment(1);
        PushRequestSleeper pushSleeper = new PushRequestSleeper(servent, 
            aClientGUID, aFileIndex, pushProxyAddresses );
        synchronized (pushSleeperList)
        {
            pushSleeperList.add(pushSleeper);
        }
        SocketFacade socket = pushSleeper.requestSocketViaPush();
        if ( socket == null )
        {
            //NLogger.debug( PushHandler.class, "PUSH request failed." );
            ((SimpleStatisticProvider)servent.getStatisticsService().getStatisticProvider(
                StatisticsManager.PUSH_DOWNLOAD_FAILURE_PROVIDER)).increment(1);
        }
        else
        {
            //NLogger.debug( PushHandler.class, "PUSH request successful." );
            ((SimpleStatisticProvider)servent.getStatisticsService().getStatisticProvider(
                StatisticsManager.PUSH_DOWNLOAD_SUCESS_PROVIDER)).increment(1);
        }
        return socket;
    }

    private void internalUnregisterPushRequestSleeper(PushRequestSleeper sleeper)
    {
        synchronized (pushSleeperList)
        {
            pushSleeperList.remove(sleeper);
        }
    }
}