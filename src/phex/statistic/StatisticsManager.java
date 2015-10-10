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
package phex.statistic;

import java.util.HashMap;

import phex.common.AbstractLifeCycle;
import phex.common.HorizonTracker;
import phex.common.LongObj;
import phex.common.bandwidth.BandwidthManager;
import phex.msg.PongMsg;
import phex.prefs.core.StatisticPrefs;
import phex.servent.Servent;
import phex.statistic.HorizonStatisticProvider.Type;

public class StatisticsManager extends AbstractLifeCycle implements StatisticProviderConstants
{
    private HashMap<String, StatisticProvider> statisticProviderMap;

    public StatisticsManager()
    {
        statisticProviderMap = new HashMap<String, StatisticProvider>();
        
        registerStatisticProvider( UPTIME_PROVIDER,
            new UptimeStatisticProvider() );
        registerStatisticProvider( DAILY_UPTIME_PROVIDER,
            new DailyUptimeStatisticProvider() );
        
        
        
        initializeMsgCountStats();
        initializeUpDownloadStats();
    }

    public void registerStatisticProvider( String name, StatisticProvider provider )
    {
        statisticProviderMap.put( name, provider );
    }

    public StatisticProvider getStatisticProvider( String name )
    {
        return statisticProviderMap.get( name );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void doStart()
    {
        Servent servent = Servent.getInstance();
        BandwidthManager manager = servent.getBandwidthService();
        
        registerStatisticProvider( TOTAL_BANDWIDTH_PROVIDER,
            new TransferAverageStatisticProvider( manager.getServentBandwidthController() ) );
        registerStatisticProvider( NETWORK_BANDWIDTH_PROVIDER,
            new TransferAverageStatisticProvider( manager.getNetworkBandwidthController() ) );
        registerStatisticProvider( DOWNLOAD_BANDWIDTH_PROVIDER,
            new TransferAverageStatisticProvider( manager.getDownloadBandwidthController() ) );
        registerStatisticProvider( UPLOAD_BANDWIDTH_PROVIDER,
            new TransferAverageStatisticProvider( manager.getUploadBandwidthController() ) );
        
        
        HorizonTracker horizonTracker = new HorizonTracker();
        servent.getMessageService().addMessageSubscriber( PongMsg.class, horizonTracker );
        
        registerStatisticProvider( HORIZON_HOST_COUNT_PROVIDER,
            new HorizonStatisticProvider( Type.HOST_COUNT, horizonTracker ) );
        registerStatisticProvider( HORIZON_FILE_COUNT_PROVIDER,
            new HorizonStatisticProvider( Type.FILE_COUNT, horizonTracker ) );
        registerStatisticProvider( HORIZON_FILE_SIZE_PROVIDER,
            new HorizonStatisticProvider( Type.FILE_SIZE, horizonTracker ) );
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    public void doStop()
    {
        UptimeStatisticProvider uptimeProvider = (UptimeStatisticProvider)
            getStatisticProvider( UPTIME_PROVIDER );
        uptimeProvider.saveUptimeStats();
        
        DailyUptimeStatisticProvider dailyUptimeProvider =
            (DailyUptimeStatisticProvider)getStatisticProvider( DAILY_UPTIME_PROVIDER );
        dailyUptimeProvider.shutdown();
        
        SimpleStatisticProvider totalDownloadCount = (SimpleStatisticProvider) getStatisticProvider(
            TOTAL_DOWNLOAD_COUNT_PROVIDER);
        SimpleStatisticProvider totalUploadCount = (SimpleStatisticProvider) getStatisticProvider(
            TOTAL_UPLOAD_COUNT_PROVIDER);
        StatisticPrefs.TotalDownloadCount.set( Integer.valueOf( 
            (int)((LongObj)totalDownloadCount.getValue()).value ) );
        StatisticPrefs.TotalUploadCount.set( Integer.valueOf(
            (int)((LongObj)totalUploadCount.getValue()).value ) );
    }
    
    private void initializeMsgCountStats()
    {
        SimpleStatisticProvider totalInMsgCounter = new SimpleStatisticProvider();
        StatisticProvider pingMsgInCounter = new ChainedSimpleStatisticProvider(
            totalInMsgCounter);
        StatisticProvider pongMsgInCounter = new ChainedSimpleStatisticProvider(
            totalInMsgCounter);
        StatisticProvider pushMsgInCounter = new ChainedSimpleStatisticProvider(
            totalInMsgCounter);
        StatisticProvider queryMsgInCounter = new ChainedSimpleStatisticProvider(
            totalInMsgCounter);
        StatisticProvider queryHitMsgInCounter = new ChainedSimpleStatisticProvider(
            totalInMsgCounter);

        registerStatisticProvider(TOTALMSG_IN_PROVIDER, totalInMsgCounter);
        registerStatisticProvider(PINGMSG_IN_PROVIDER, pingMsgInCounter);
        registerStatisticProvider(PONGMSG_IN_PROVIDER, pongMsgInCounter);
        registerStatisticProvider(PUSHMSG_IN_PROVIDER, pushMsgInCounter);
        registerStatisticProvider(QUERYMSG_IN_PROVIDER, queryMsgInCounter);
        registerStatisticProvider(QUERYHITMSG_IN_PROVIDER, queryHitMsgInCounter);

        SimpleStatisticProvider totalOutMsgCounter = new SimpleStatisticProvider();
        StatisticProvider pingMsgOutCounter = new ChainedSimpleStatisticProvider(
            totalOutMsgCounter);
        StatisticProvider pongMsgOutCounter = new ChainedSimpleStatisticProvider(
            totalOutMsgCounter);
        StatisticProvider pushMsgOutCounter = new ChainedSimpleStatisticProvider(
            totalOutMsgCounter);
        StatisticProvider queryMsgOutCounter = new ChainedSimpleStatisticProvider(
            totalOutMsgCounter);
        StatisticProvider queryHitMsgOutCounter = new ChainedSimpleStatisticProvider(
            totalOutMsgCounter);

        registerStatisticProvider(TOTALMSG_OUT_PROVIDER, totalOutMsgCounter);
        registerStatisticProvider(PINGMSG_OUT_PROVIDER, pingMsgOutCounter);
        registerStatisticProvider(PONGMSG_OUT_PROVIDER, pongMsgOutCounter);
        registerStatisticProvider(PUSHMSG_OUT_PROVIDER, pushMsgOutCounter);
        registerStatisticProvider(QUERYMSG_OUT_PROVIDER, queryMsgOutCounter);
        registerStatisticProvider(QUERYHITMSG_OUT_PROVIDER,
            queryHitMsgOutCounter);

        SimpleStatisticProvider dropedMsgTotalCounter = new SimpleStatisticProvider();
        StatisticProvider dropedMsgInCounter = new ChainedSimpleStatisticProvider(
            dropedMsgTotalCounter);
        StatisticProvider dropedMsgOutCounter = new ChainedSimpleStatisticProvider(
            dropedMsgTotalCounter);

        registerStatisticProvider(DROPEDMSG_TOTAL_PROVIDER,
            dropedMsgTotalCounter);
        registerStatisticProvider(DROPEDMSG_IN_PROVIDER, dropedMsgInCounter);
        registerStatisticProvider(DROPEDMSG_OUT_PROVIDER, dropedMsgOutCounter);
        
    }
    
    private void initializeUpDownloadStats( )
    {
        SimpleStatisticProvider totalUploadCount = new SimpleStatisticProvider();
        totalUploadCount.setValue( StatisticPrefs.TotalUploadCount.get().intValue() );
        StatisticProvider sessionUploadCount = new ChainedSimpleStatisticProvider( 
            totalUploadCount );
        registerStatisticProvider(SESSION_UPLOAD_COUNT_PROVIDER,
            sessionUploadCount);
        registerStatisticProvider(TOTAL_UPLOAD_COUNT_PROVIDER, totalUploadCount);
        
        SimpleStatisticProvider totalDownloadCount = new SimpleStatisticProvider();
        totalDownloadCount.setValue( StatisticPrefs.TotalDownloadCount.get().intValue() );
        StatisticProvider sessionDownloadCount = new ChainedSimpleStatisticProvider(
            totalDownloadCount );
        registerStatisticProvider(SESSION_DOWNLOAD_COUNT_PROVIDER,
            sessionDownloadCount);
        registerStatisticProvider(TOTAL_DOWNLOAD_COUNT_PROVIDER,
            totalDownloadCount);
        
        registerStatisticProvider(PUSH_DOWNLOAD_ATTEMPTS_PROVIDER,
            new SimpleStatisticProvider());
        registerStatisticProvider(PUSH_DOWNLOAD_SUCESS_PROVIDER,
            new SimpleStatisticProvider());
        registerStatisticProvider(PUSH_DOWNLOAD_FAILURE_PROVIDER,
            new SimpleStatisticProvider());

        registerStatisticProvider(PUSH_DLDPUSHPROXY_ATTEMPTS_PROVIDER,
            new SimpleStatisticProvider());
        registerStatisticProvider(PUSH_DLDPUSHPROXY_SUCESS_PROVIDER,
            new SimpleStatisticProvider());

        registerStatisticProvider(PUSH_UPLOAD_ATTEMPTS_PROVIDER,
            new SimpleStatisticProvider());
        registerStatisticProvider(PUSH_UPLOAD_SUCESS_PROVIDER,
            new SimpleStatisticProvider());
        registerStatisticProvider(PUSH_UPLOAD_FAILURE_PROVIDER,
            new SimpleStatisticProvider());
    }
}