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
package phex.rules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimerTask;

import phex.common.Environment;
import phex.common.PhexVersion;
import phex.event.UserMessageListener;
import phex.rules.condition.AndConcatCondition;
import phex.rules.condition.Condition;
import phex.rules.consequence.Consequence;
import phex.xml.sax.DPhex;
import phex.xml.sax.DSubElementList;
import phex.xml.sax.XMLBuilder;
import phex.xml.sax.parser.rules.SearchRuleListHandler;
import phex.xml.sax.rules.DAndConcatCondition;
import phex.xml.sax.rules.DCondition;
import phex.xml.sax.rules.DConsequence;
import phex.xml.sax.rules.DConsequencesList;
import phex.xml.sax.rules.DSearchRule;

public class SearchFilterRules
{
    private final File rulesFile;
    
    private boolean hasChangedSinceLastSave;

    private List<Rule> searchFilterRules;

    public SearchFilterRules( File rulesFile )
    {
        this.rulesFile = rulesFile;
        
        searchFilterRules = new ArrayList<Rule>();
        searchFilterRules.add( DefaultSearchFilterRules.ADULT_FILTER_RULE );
        searchFilterRules.add( DefaultSearchFilterRules.SCAM_FILE_FILTER_RULE ); 
        searchFilterRules.add( DefaultSearchFilterRules.SPAM_FILE_FILTER_RULE ); 
        searchFilterRules.add( DefaultSearchFilterRules.NASTY_FILE_FILTER_RULE ); 
        hasChangedSinceLastSave = false;

        Environment.getInstance().scheduleTimerTask( new SaveFavoritesTimer(),
            SaveFavoritesTimer.TIMER_PERIOD, SaveFavoritesTimer.TIMER_PERIOD );
    }

    /**
     * Sets the rule list.
     * @param newList
     */
    public synchronized void setRuleList( List<Rule> newList )
    {
        searchFilterRules = newList;
        hasChangedSinceLastSave = true;
    }

    /**
     * Returns a unmodifiable list of search rules.
     */
    public synchronized List<Rule> getAsList()
    {
        return Collections.unmodifiableList( searchFilterRules );
    }
    
    /**
     * Returns a unmodifiable list of permanently enabled 
     * search rules.
     */
    public synchronized List<Rule> getPermanentList()
    {
        List<Rule> list = new ArrayList<Rule>();
        for ( Rule rule : searchFilterRules )
        {
            if ( rule.isPermanentlyEnabled() )
            {
                list.add(rule);
            }
        }
        return list;
    }

    /**
     * Returns the number of available search filters.
     * @return the number of available search filters.
     */
    public synchronized int getCount()
    {
        return searchFilterRules.size();
    }

    /**
     * Returns the search filter at index.
     * @param index the index
     * @return the search filter at index.
     */
    public synchronized Rule getRuleAt( int index )
    {
        if ( index < 0 || index >= searchFilterRules.size() )
        {
            return null;
        }
        return searchFilterRules.get( index );
    }
    
    /**
     * Returns the search filter with given id.
     * @param id the id
     * @return the search filter with id or null if not found.
     */
    public synchronized Rule getRuleById( String id )
    {
        for ( Rule rule : searchFilterRules )
        {
            if ( id.equals( rule.getId() ) )
            {
                return rule;
            }
        }
        return null;
    }

    public synchronized void save( )
    {
        if ( !hasChangedSinceLastSave || rulesFile == null )
        {
            return;
        }

        //NLogger.debug( SearchFilterRules.class, "Saving search filters." );
        try
        {
            DPhex dPhex = new DPhex();
            dPhex.setPhexVersion( PhexVersion.getFullVersion() );

            DSubElementList<DSearchRule> dList = new DSubElementList<DSearchRule>(
                SearchRuleListHandler.THIS_TAG_NAME );
            dPhex.setSearchRuleList( dList );

            List<DSearchRule> searchRuleList = dList.getSubElementList();
            for( Rule rule : searchFilterRules )
            {
                DSearchRule dRule = new DSearchRule();
                dRule.setId( rule.getId() );
                dRule.setName( rule.getName() );				
                dRule.setDescription( rule.getDescription() );  
                dRule.setNotes( rule.getNotes() );
                dRule.setPermanentlyEnabled( rule.isPermanentlyEnabled() );

                DAndConcatCondition andCondition = new DAndConcatCondition();
                List<DCondition> dCondList = andCondition.getSubElementList();
                for ( Condition cond : rule.getConditions() )
                {
                    DCondition dCond = cond.createDCondition();
                    dCondList.add( dCond );
                }
                dRule.setAndConcatCondition( andCondition );

                DConsequencesList dConsequ = new DConsequencesList();
                List<DConsequence> dConsequList = dConsequ.getSubElementList();
                for( Consequence consequ : rule.getConsequences() )
                {
                    DConsequence dCond = consequ.createDConsequence();
                    dConsequList.add( dCond );
                }
                dRule.setConsequencesList( dConsequ );

                searchRuleList.add( dRule );
            }
            XMLBuilder.saveToFile( rulesFile, dPhex );
            hasChangedSinceLastSave = false;
        }
        catch (IOException exp)
        {
            // TODO during close this message is never displayed since application
            // will exit too fast. A solution to delay exit process in case 
            // SlideInWindows are open needs to be found.
            //NLogger.error( SearchFilterRules.class, exp, exp );
            Environment.getInstance().fireDisplayUserMessage(
                UserMessageListener.FavoritesSettingsSaveFailed, new String[]
                { exp.toString() } );
        }
    }

    public synchronized void load( )
    {
        if ( rulesFile == null || !rulesFile.exists() )
        {
            return;
        }
        
        //NLogger.debug( SearchFilterRules.class, "Loading search filters." );
        DPhex dPhex;
        try
        {
            dPhex = XMLBuilder.loadDPhexFromFile( rulesFile );
            if ( dPhex == null )
            {
                //NLogger.debug( SearchFilterRules.class, "No DPhex found." );
                return;
            }
            DSubElementList<DSearchRule> dRuleList = dPhex.getSearchRuleList();
            if ( dRuleList == null )
            {
                //NLogger.warn( SearchFilterRules.class,
                //    "No DSearchRule list found." );
                return;
            }

            List<Rule> newRules = new ArrayList<Rule>();
            for ( DSearchRule dRule : dRuleList.getSubElementList() )
            {
                Rule rule = null;
                boolean isNew = false;
                String id = dRule.getId();
                if ( id != null )
                {
                    // try to find existing.
                    rule = getRuleById( id );
                }
                if ( rule == null )
                {
                    isNew = true;
                    rule = new Rule();
                }

                if ( !rule.isDefaultRule() )
                {
                    String name = dRule.getName();
                    rule.setName( name );
                    rule.setNotes( dRule.getNotes());
                    DAndConcatCondition dCondition = dRule.getAndConcatCondition();
                    AndConcatCondition andCond = (AndConcatCondition) dCondition
                        .createCondition();
    
                    for ( Condition cond : andCond.getConditions() )
                    {
                        rule.addCondition( cond );
                    }
    
                    DConsequencesList consequencesList = dRule.getConsequencesList();
                    for ( DConsequence dConsequ : consequencesList.getSubElementList()  )
                    {
                        Consequence conseq = dConsequ.createConsequence();
                        rule.addConsequence( conseq );
                    }
                }
                if ( dRule.isHasPermanentlyEnabled() )
                {
                    rule.setPermanentlyEnabled( dRule.isPermanentlyEnabled() );
                }
                if ( isNew )
                {
                    newRules.add( rule );
                }
            }
            searchFilterRules.addAll(newRules);
        }
        catch (IOException exp)
        {
            //NLogger.error( SearchFilterRules.class, exp, exp );
            Environment.getInstance().fireDisplayUserMessage(
                UserMessageListener.FavoritesSettingsLoadFailed, new String[]
                { exp.toString() } );
            return;
        }
    }

    ////////////////////// START inner classes //////////////////////////

    private class SaveRunner implements Runnable
    {
        public void run()
        {
            save();
        }
    }

    private class SaveFavoritesTimer extends TimerTask
    {
        // once per minute
        public static final long TIMER_PERIOD = 1000 * 60;

        @Override
        public void run()
        {
            try
            {
                // trigger the save inside a background job
                Environment.getInstance().executeOnThreadPool( new SaveRunner(),
                    "SaveSearchFilterRules" );
            }
            catch (Throwable th)
            {
                //NLogger.error( SearchFilterRules.class, th, th );
            }
        }
    }
}
