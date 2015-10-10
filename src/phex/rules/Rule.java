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
 *  Created on 16.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.rules;

import java.util.*;

import phex.download.RemoteFile;
import phex.query.Search;
import phex.rules.condition.AndConcatCondition;
import phex.rules.condition.Condition;
import phex.rules.consequence.Consequence;
import phex.servent.Servent;

public class Rule implements Cloneable
{
    private String name;
    
    private String description;
    
    private String notes;
    
    /**
     * A internal id mostly only used for Phex default rules to identify there
     * settings.
     */
    private String id;
    
    private boolean isPermanentlyEnabled;
    
    /**
     * A default rule indicates that this rule is delivered by Phex. The user
     * changeability of this rule is limited. Only the following fields are 
     * allowed to be changed:
     * - isPermanentlyEnabled
     * - consequences
     * If the user likes to be able to change the conditions, it is recommended
     * to copy the rule and modify it afterwards.
     */
    private boolean isDefaultRule;
    
    private AndConcatCondition ruleCondition;
    private List<Consequence> consequences;
    
    public Rule()
    {
        ruleCondition = new AndConcatCondition();
        consequences = new ArrayList<Consequence>();
    }
    
    public boolean isDefaultRule()
    {
        return isDefaultRule;
    }

    public void setDefaultRule( boolean isDefaultRule )
    {
        this.isDefaultRule = isDefaultRule;
    }
    
    public boolean isPermanentlyEnabled()
    {
        return isPermanentlyEnabled;
    }

    public void setPermanentlyEnabled( boolean isPermanentlyEnabled )
    {
        this.isPermanentlyEnabled = isPermanentlyEnabled;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }
    
    public String getNotes()
    {
        return notes;
    }

    public void setNotes( String notes )
    {
        this.notes = notes;
    }
    
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }
    
    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    /**
     * Returns a unmodifiable list of the containing conditions.
     * @return
     */
    public List<Condition> getConditions()
    {
        return ruleCondition.getConditions();
    }
    
    public void clearConditions()
    {
        ruleCondition.clearConditions();
    }

    public Rule addCondition( Condition condition )
    {
        ruleCondition.addCondition(condition);
        return this;
    }
    
    public Rule removeCondition( Condition condition )
    {
        ruleCondition.removeCondition( condition );
        return this;
    }
    
    /**
     * Returns a unmodifiable list of the containing consequences.
     * @return
     */
    public List<Consequence> getConsequences()
    {
        return Collections.unmodifiableList(consequences);
    }
    
    public void addConsequence( Consequence consequence )
    {
        consequences.add(consequence);
    }
    
    public Rule removeConsequence( Consequence consequence )
    {
        consequences.remove( consequence );
        return this;
    }
    
    public void process( Search search, RemoteFile[] remoteFiles, Servent servent )
    {
        for ( int i = 0; i < remoteFiles.length; i++ )
        {
            // we stop further process if already marked for remove...
            // TODO we might also support a consequence 'stop further processing'
            // in the future.
            if( remoteFiles[i].isFilteredRemoved() )
            {
                continue;
            }
            
            boolean isMatched = ruleCondition.isMatched(search, remoteFiles[i]);
            if ( !isMatched )
            {
                continue;
            }
            
            for ( Consequence conseq : consequences )
            {
                conseq.invoke( search, remoteFiles[i], servent );
            }
        }
    }
    
    @Override
    public Object clone()
    {
        try
        {
            Rule clone = (Rule) super.clone();
            clone.ruleCondition = (AndConcatCondition) ruleCondition.clone();
            
            clone.consequences = new ArrayList<Consequence>();
            for ( Consequence conseq : consequences )
            {
                clone.consequences.add( (Consequence)conseq.clone() );
            }
            return clone;
        }
        catch ( CloneNotSupportedException exp )
        {
            throw new InternalError();
        }
    }
    
    @Override
    public String toString()
    {
        return super.toString() + "[Condition: " + ruleCondition.toString() + "]";
    }
}
