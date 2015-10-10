/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Phex Development Group
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
 *  Created on 14.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.rules.condition;

import java.util.*;

/**
 * This is a AND concatenation search filter. All filters in this filter
 * must match the filter to get it triggered.
 *
 */
public abstract class ConcatCondition implements Condition, Cloneable
{
    protected List<Condition> filterList;
    
    public ConcatCondition()
    {
        filterList = new ArrayList<Condition>();
    }
    
    public int getConditionCount()
    {
        return filterList.size();
    }
    
    /**
     * Returns a unmodifiable list of the containing conditions.
     * @return
     */
    public List<Condition> getConditions()
    {
        return Collections.unmodifiableList(filterList);
    }
    
    public synchronized void clearConditions()
    {
        filterList.clear();
    }
    
    public synchronized ConcatCondition addCondition( Condition filter )
    {
        filterList.add( filter );
        return this;
    }
    
    public synchronized void removeCondition( Condition filter )
    {
        filterList.remove( filter );
    }
    
    public boolean isComplete()
    {
        return true;
    }
    
    @Override
    public Object clone()
    {
        try
        {
            ConcatCondition clone = (ConcatCondition)super.clone();
            clone.filterList = new ArrayList<Condition>();
            
            for ( Condition condition : filterList )
            {
                clone.filterList.add( (Condition)condition.clone() );
            }
            return clone;
        }
        catch (CloneNotSupportedException e) 
        { 
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }
    
    @Override
    public String toString()
    {
        return super.toString() + "[List: " + filterList.toString() + "]";
    }
}
