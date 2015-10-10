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

import phex.common.URN;
import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadFile;
import phex.event.EventHandler;
import phex.event.PhexEventService;
import phex.event.PhexEventTopics;
import phex.rules.Rule;
import phex.rules.condition.FileSizeCondition;
import phex.rules.condition.NotCondition;
import phex.rules.consequence.RemoveFromSearchConsequence;
import phex.servent.Servent;

public class ResearchSetting implements EventHandler
{
    private final BackgroundSearchContainer searchContainer;
    
    private long lastResearchStartTime;

    /**
     * The count of research that didn't return any new results.
     */
    private int noNewResultsCount;
    private int totalResearchCount;

    /**
     * The term to search for.
     */
    private String searchTerm;

    private RuleFilteredSearch ruledSearch;

    /**
     * When the search has new results. This flag is true.
     */
    private boolean hasNewSearchResults;


    // Since currently the only one who uses the research setting is the
    // download file this solution is ok... later we need to find a different
    // way
    private final SWDownloadFile downloadFile;
    
    private final Servent servent;
    
    private final PhexEventService eventService;

    public ResearchSetting( SWDownloadFile file, QueryManager queryService, 
        PhexEventService eventService, Servent servent )
    {
        this.servent = servent;
        this.eventService = eventService;
        downloadFile = file;
        searchContainer = queryService.getBackgroundSearchContainer();
        eventService.register( this , new String[]{PhexEventTopics.Search_Data});
    }

    public long getLastResearchStartTime()
    {
        return lastResearchStartTime;
    }

    public void setLastResearchStartTime( long time )
    {
        lastResearchStartTime = time;
    }

    public int getNoNewResultsCount()
    {
        return noNewResultsCount;
    }

    public String getSearchTerm()
    {
        return searchTerm;
    }

    public void setSearchTerm( String term )
    {
        searchTerm = term;
    }

    public String getSHA1()
    {
        URN searchURN = downloadFile.getFileURN();
        if ( searchURN == null || !searchURN.isSha1Nid() )
        {
            return "";
        }
        return searchURN.getNamespaceSpecificString();
    }

    public void startSearch( )
    {
        if ( ruledSearch != null && !ruledSearch.isSearchFinished() )
        {
            return;
        }

        if ( searchTerm.length() < DynamicQueryConstants.MIN_SEARCH_TERM_LENGTH &&
            downloadFile.getFileURN() == null)
        {
            return;
        }
        hasNewSearchResults = false;
        
        // Since Limewire is not adding urns to QRT anymore URN queries even with
        // string turn out to not work very good.. therefore we are not trying
        // urn queries if we have a decent search term available.
        URN queryURN = null;
        if ( searchTerm.length() < DynamicQueryConstants.MIN_SEARCH_TERM_LENGTH 
             && downloadFile.getFileURN() != null )
        {
            queryURN = downloadFile.getFileURN();
        }
        Search search = searchContainer.createSearch( searchTerm, queryURN );
        
        Rule rule = new Rule();
        rule.addConsequence(RemoveFromSearchConsequence.INSTANCE);
        rule.addCondition( new NotCondition( new FileSizeCondition( 
            downloadFile.getTotalDataSize(), 
            downloadFile.getTotalDataSize() ) ) );
        ruledSearch = new RuleFilteredSearch( search, rule, servent, eventService );
        
        DefaultSearchProgress searchProgress = DefaultSearchProgress.createForForMeProgress( 
            queryURN != null );
        ruledSearch.startSearching( searchProgress );

        totalResearchCount ++;
        long currentTime = System.currentTimeMillis();
        lastResearchStartTime = currentTime;
    }

    public int getTotalResearchCount()
    {
        return totalResearchCount;
    }

    public void stopSearch()
    {
        if ( ruledSearch == null || ruledSearch.isSearchFinished() )
        {
            return;
        }
        ruledSearch.stopSearching();
    }

    public int getSearchHitCount()
    {
        return ruledSearch.getResultCount();
    }
    
    public int getSearchProgress()
    {
        return ruledSearch.getProgress();
    }

    public boolean isSearchRunning()
    {
        if ( ruledSearch == null )
        {
            return false;
        }
        return !ruledSearch.isSearchFinished();
    }
    
    public void onSearchDataEvent( String topic, final SearchDataEvent event )
    {
        if ( ruledSearch != event.getSource() )
        {
            return;
        }
        
        // after search has stopped check if we found any thing.
        if ( event.getType() == SearchDataEvent.SEARCH_STOPED )
        {
            if ( hasNewSearchResults == false )
            {   // no new results...
                noNewResultsCount ++;
            }
            else
            {
                noNewResultsCount = 0;
            }
        }
        
        if ( event.getType() == SearchDataEvent.SEARCH_HITS_ADDED )
        {
            // Adds a file from a background search to the candidates list.
            RemoteFile[] files = event.getSearchData();
            for ( int i = 0; i < files.length; i++ )
            {
                boolean isAdded = downloadFile.addDownloadCandidate( files[i] );
                if ( isAdded )
                {
                    hasNewSearchResults = true;
                }
            }
        }
    }

	@Override
	public void onEvent(String topic, Object event) {
		// TODO Auto-generated method stub
    	if(topic.compareTo(PhexEventTopics.Search_Data)== 0) 
    		onSearchDataEvent(  topic,  (SearchDataEvent) event );
	}
}