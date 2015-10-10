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
package phex.query;

import java.io.File;
import java.util.TimerTask;

import phex.common.AbstractLifeCycle;
import phex.common.Environment;
import phex.common.Phex;
import phex.event.EventHandler;
import phex.event.PhexEventTopics;
import phex.host.Host;
import phex.msg.QueryFactory;
import phex.msg.QueryMsg;
import phex.msg.QueryResponseMsg;
import phex.msg.vendor.OOBReplyCountVMsg;
import phex.msghandling.MessageService;
import phex.rules.SearchFilterRules;
import phex.servent.Servent;

public class QueryManager extends AbstractLifeCycle implements EventHandler
{
    private final Servent servent;
    private final MessageService msgService;
    private final SearchContainer searchContainer;
    private final BackgroundSearchContainer backgroundSearchContainer;
    private final SearchFilterRules searchFilterRules;
    private final DynamicQueryWorker dynamicQueryWorker;
    private final QueryFactory queryFactory;
    
    /**
     * The last time a query was send.
     */
    private volatile long lastQueryTime;

    public QueryManager( MessageService msgService, Servent servent )
    {
        this.servent = servent;
        this.msgService = msgService;
        queryFactory = new QueryFactory( servent );
        searchContainer = new SearchContainer( queryFactory, servent );
        msgService.addMessageSubscriber( QueryResponseMsg.class, 
            searchContainer );
        msgService.addUdpMessageSubscriber( OOBReplyCountVMsg.class, 
            searchContainer );
        msgService.addUdpMessageSubscriber( QueryResponseMsg.class, 
            searchContainer );
        
        backgroundSearchContainer = new BackgroundSearchContainer( queryFactory, 
            servent );
        msgService.addMessageSubscriber( QueryResponseMsg.class, 
            backgroundSearchContainer );
        
        File filterFile = servent.getGnutellaNetwork().getSearchFilterFile();
        searchFilterRules = new SearchFilterRules( filterFile );
        //researchService = new ResearchService( new ResearchServiceConfig() );
        dynamicQueryWorker = new DynamicQueryWorker();
        
        Phex.getEventService().register( this , new String[]{PhexEventTopics.Host_Disconnect});
    }

    @Override
    protected void doStart()
    {
        searchFilterRules.load();
        dynamicQueryWorker.startQueryWorker();
        Environment.getInstance().scheduleTimerTask( 
            new ExpiredSearchCheckTimer(), ExpiredSearchCheckTimer.TIMER_PERIOD,
            ExpiredSearchCheckTimer.TIMER_PERIOD );
    }
    
    @Override
    public void doStop()
    {
        searchFilterRules.save();
    }
    
    public void onHostDisconnectEvent( String topic, Host host )
    {
        removeHostQueries( host );
    }

    public SearchContainer getSearchContainer()
    {
        return searchContainer;
    }

    public BackgroundSearchContainer getBackgroundSearchContainer()
    {
        return backgroundSearchContainer;
    }

/*    public ResearchService getResearchService()
    {
        return researchService;
    }
*/
    
    /**
     * Removes all running queries for this host.
     * @param host the host to remove its queries for.
     */
    public void removeHostQueries( Host host )
    {
        if ( host.isUltrapeerLeafConnection() )
        {
            dynamicQueryWorker.removeDynamicQuerysForHost( host );
        }
    }
    
    /**
     * Sends a dynamic query using the dynamic query engine.
     * @param query the query to send.
     */
    public DynamicQueryEngine sendDynamicQuery( QueryMsg query, Host sourceHost,
        SearchProgress searchProgress )
    {
        DynamicQueryEngine engine = new DynamicQueryEngine( query, sourceHost,
            searchProgress, servent.getHostService().getNetworkHostsContainer(), 
            msgService );
        dynamicQueryWorker.addDynamicQueryEngine( engine );
        return engine;
    }
    
    /**
     * Sends a query for this host, usually initiated by the user.
     * @param queryMsg the query to send.
     * @return the possible dynamic query engine used, or null if no
     *         dynamic query is initiated.
     */
    public DynamicQueryEngine sendMyQuery( QueryMsg queryMsg,
        SearchProgress searchProgress )
    {
        lastQueryTime = System.currentTimeMillis();
        msgService.updateMyQueryRouting( queryMsg );
        searchProgress.searchStarted();
        
        if ( servent.isUltrapeer() )
        {
            return sendDynamicQuery( queryMsg, Host.LOCAL_HOST, searchProgress );
        }
        else
        {
            msgService.forwardMyQueryToUltrapeers( queryMsg );
            return null;
        }
    }
    
    public long getLastQueryTime()
    {
        return lastQueryTime;
    }

    public SearchFilterRules getSearchFilterRules()
    {
        return searchFilterRules;
    }
    
    /**
     * Stops all searches where the timeout has passed.
     */
    private class ExpiredSearchCheckTimer extends TimerTask
    {

        public static final long TIMER_PERIOD = 5000;

        /**
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run()
        {
            // Stops all searches where the timeout has passed.
            long currentTime = System.currentTimeMillis();
            searchContainer.stopExpiredSearches( currentTime );
            backgroundSearchContainer.stopExpiredSearches( currentTime );
        }
    }

	@Override
	public void onEvent(String topic, Object event) {
		// TODO Auto-generated method stub
    	if(topic.compareTo(PhexEventTopics.Host_Disconnect)== 0) 
    		onHostDisconnectEvent(  topic, (Host) event );
	}
}
