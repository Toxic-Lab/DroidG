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
package phex.query;

import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.host.HostManager;
import phex.servent.Servent;

public class ResearchService
{
    private final SwarmingManager downloadService;
    private final ResearchServiceConfig researchConfig;

    public ResearchService( ResearchServiceConfig config, SwarmingManager downloadService )
    {
        this.downloadService = downloadService;
        researchConfig = config;
    }

    public void startResearchSequence()
    {
        if ( !researchConfig.isResearchEnabled() )
        {
            // research service disabled
            return;
        }
        for ( int i = downloadService.getDownloadFileCount(); i >= 0; i-- )
        {
            SWDownloadFile file = downloadService.getDownloadFile( i );
            if ( file != null )
            {
                performResearchOnDownload( file );
            }
        }
    }

    private void performResearchOnDownload( SWDownloadFile file )
    {
/* We are now adjusting the search interval depending on the
   number of candidates...
        if ( file.getRemoteCandidatesCount() >=
            researchConfig.getMinRemoteCandidatesCount() )
        {
            // we have a certain amount of candidates available.
            return;
        }
*/

        if ( file.isDownloadInProgress() || file.isFileCompletedOrMoved()
          || file.isDownloadStopped() )
        {
            // we don't search when download is in progress, completed or stopped
            return;
        }

        performResearchOnSetting( file.getResearchSetting(), file );
    }

    /**
     * Unfortunately we need the DownloadFile also for better optimization.
     */
    private void performResearchOnSetting( ResearchSetting setting,
        SWDownloadFile downladFile )
    {
        HostManager hostMgr = Servent.getInstance().getHostService();
        if ( hostMgr.getNetworkHostsContainer().getTotalConnectionCount() == 0 )
        {
            // there is no network connection available to send the search
            return;
        }

        // current timestamp for performance reason..
        long currentTime = System.currentTimeMillis();
        if ( isSearchRunning( setting, currentTime ) )
        {
            // there is a valid search still running...
            return;
        }

        if ( !isTimeForResearch( downladFile, setting, currentTime ) )
        {
            return;
        }
        setting.startSearch( /*researchConfig.getResearchTimeout()*/ );
    }

    private boolean isTimeForResearch( SWDownloadFile downladFile,
        ResearchSetting setting, long currentTime )
    {
        long lastResearchStartTime = setting.getLastResearchStartTime();
        int noNewResultsCount = setting.getNoNewResultsCount();
        long researchInterval = researchConfig.getResearchIntervalMillis();
        int candidatesCount = downladFile.getCandidatesCount();
        int candidatesThreshold = researchConfig.getCandidatesIntervalThreshold();
        int totalResearchCount = setting.getTotalResearchCount();
        long researchPenalty = researchConfig.getResearchTimePenalty();

        long thisResearchInterval = researchInterval +
              // for each no result add one interval
              ( researchInterval * noNewResultsCount ) +
              // for each candidate over the threshold add a penalty
              ( researchPenalty * Math.max( 0, candidatesCount - candidatesThreshold ) ) +
              // for each total try add one penalty
              ( researchPenalty * totalResearchCount );
        thisResearchInterval = Math.min( thisResearchInterval,
            researchConfig.getMaxResearchIntervalMillis() );
        long nextResearchTime = lastResearchStartTime + thisResearchInterval;

        //System.out.println( "Interval: " + thisResearchInterval + "  "  + downladFile.getFilename()
        //    + " " + (nextResearchTime - currentTime) + " ... " + noNewResultsCount + "  " +
        //    totalResearchCount + "  " + candidatesCount );

        if ( currentTime > nextResearchTime )
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Checks if a search is running and if a running search is still valid.
     */
    private boolean isSearchRunning( ResearchSetting setting, long currentTime )
    {
        if ( !setting.isSearchRunning() )
        {
            return false;
        }
        /*
        // timeout check is moved to a more general place for all searches
        long lastResearchStartTime = setting.getLastResearchStartTime();
        long researchTimeoutTime =
            lastResearchStartTime + researchConfig.getResearchTimeout();
        if ( currentTime > researchTimeoutTime )
        {
            setting.stopSearch();
            return false;
        }*/
        return true;
    }
}