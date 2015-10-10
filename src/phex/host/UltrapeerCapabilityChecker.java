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
 */
package phex.host;

import java.util.TimerTask;

import org.apache.commons.lang.time.DateUtils;

import phex.common.Environment;
import phex.common.LongObj;
import phex.common.bandwidth.BandwidthController;
import phex.prefs.core.BandwidthPrefs;
import phex.prefs.core.ConnectionPrefs;
import phex.servent.Servent;
import phex.statistic.StatisticProviderConstants;
import phex.statistic.StatisticsManager;
import phex.statistic.UptimeStatisticProvider;

// TODO could be LifeCycle..
public class UltrapeerCapabilityChecker extends TimerTask
{
    //private static Logger logger = LoggerFactory.getLogger( 
    //    UltrapeerCapabilityChecker.class );
    /**
     * The time period in millis to wait between checks.
     */
    private static final long TIMER_PERIOD = 10 * 1000;
    
    private static final long ONE_HOUR = 60 * 60 * 1000;
    private static final long HALF_HOUR = 30 * 60 * 1000;

    private final Servent servent;
    private final StatisticsManager statisticsService;
    private final boolean isUltrapeerOS;
    
    private volatile boolean isUltrapeerCapable;
    private volatile boolean isPerfectUltrapeer;
    

    public UltrapeerCapabilityChecker( Servent servent, 
        StatisticsManager statisticsService )
    {
        if ( servent == null )
        {
            throw new NullPointerException( "servent missing." );
        }
        if ( statisticsService == null )
        {
            throw new NullPointerException( "statisticsService missing." );
        }
        this.servent = servent;
        this.statisticsService = statisticsService;
        
        Environment env = Environment.getInstance();
        isUltrapeerOS = env.isUltrapeerOS();

        env.scheduleTimerTask( this, 0, TIMER_PERIOD );
    }

    /**
     * Provided run implementation of TimerTask.
     */
    @Override
    public void run()
    {
        checkIfUltrapeerCapable();
    }

    private void checkIfUltrapeerCapable()
    {
        UptimeStatisticProvider uptimeProvider = (UptimeStatisticProvider)statisticsService.
            getStatisticProvider( StatisticProviderConstants.UPTIME_PROVIDER );
        if ( uptimeProvider == null )
        {// we can't measure uptime... -> we are not capable...
            isUltrapeerCapable = false;
            return;
        }
        
        boolean isCapable =
            // the first check if we are allowed to become a ultrapeer at all...
            // if not we don't need to continue checking...
            ConnectionPrefs.AllowToBecomeUP.get().booleanValue() &&
            // host should not be firewalled.
            !servent.isFirewalled() &&
            // host should provide a Ultrapeer capable OS
            isUltrapeerOS &&
            // the connection speed should be more then single ISDN
            BandwidthPrefs.NetworkSpeedKbps.get().intValue() > 64 &&
            // also we should provide at least 10KB network bandwidth
            BandwidthPrefs.MaxNetworkBandwidth.get().intValue() > 10 * 1024 &&
            // and at least 14KB total bandwidth (because network bandwidth might
            // be set to unlimited)
            BandwidthPrefs.MaxTotalBandwidth.get().intValue() > 14 * 1024 &&
            // the current uptime should be at least 60 minutes or 30 minutes in avg.
            ( ((LongObj)uptimeProvider.getValue()).getValue() > ONE_HOUR ||
              ((LongObj)uptimeProvider.getAverageValue()).getValue() > HALF_HOUR );
        
//        if ( //logger.isTraceEnabled() && !isCapable )
//        {
//            logTraceUltrapeerCapable( uptimeProvider );
//        }

        isUltrapeerCapable = isCapable;
        
        if ( isUltrapeerCapable && !servent.isUltrapeer() )
        {
            long now = System.currentTimeMillis();
            
            long lastQueryTime = servent.getQueryService().getLastQueryTime();
            
            BandwidthController upBandCont = servent.getBandwidthService().getUploadBandwidthController();
            long upAvg = upBandCont.getLongTransferAvg().getAverage();

            boolean isPerfect =
                // last query is 5 minutes ago
                now - lastQueryTime > 5 * DateUtils.MILLIS_PER_MINUTE &&
                upAvg < 2 * 1024;
                
            isPerfectUltrapeer = isPerfect;
            servent.upgradeToUltrapeer();
        }
        else
        {
            isPerfectUltrapeer = false;
        }
        //logger.debug( "UP capable: {}, perfect: {}", isUltrapeerCapable, isPerfectUltrapeer );
    }

    private void logTraceUltrapeerCapable(UptimeStatisticProvider uptimeProvider)
    {
        if ( !ConnectionPrefs.AllowToBecomeUP.get().booleanValue() )
        {
            //logger.trace( "Not allowed to become UP." );
        }
        if ( servent.isFirewalled() )
        {
            //logger.trace( "Servent is firewalled." );
        }
        if ( !isUltrapeerOS )
        {
            //logger.trace( "No ultrapeer OS." );
        }
        if ( BandwidthPrefs.NetworkSpeedKbps.get().intValue() <= 64 )
        {
            //logger.trace( "Not enough network speed" );
        }
        if ( BandwidthPrefs.MaxNetworkBandwidth.get().intValue() <= 10 * 1024 )
        {
            //logger.trace( "Not enough max network bandwidth" );
        }
        if ( BandwidthPrefs.MaxTotalBandwidth.get().intValue() <= 14 * 1024 )
        {
            //logger.trace( "Not enough max total bandwidth" );
        }
        if ( ((LongObj)uptimeProvider.getValue()).getValue() <= ONE_HOUR ||
            ((LongObj)uptimeProvider.getAverageValue()).getValue() <= HALF_HOUR )
        {
            //logger.trace( "Not enough current or avg uptime." );
        }
    }

    public boolean isUltrapeerCapable()
    {
        return isUltrapeerCapable;
    }
    
    public boolean isPerfectUltrapeer()
    {
        return isPerfectUltrapeer;
    }
}