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

import phex.download.RemoteFile;
import phex.query.Search;
import phex.xml.sax.rules.*;

/**
 * Applies a NOT condition to the given search filter. That means that the 
 * filter results of the given search filter is reversed.
 */
public class NotCondition implements Condition
{
    private Condition filter;
    
    public NotCondition( Condition filter )
    {
        this.filter = filter;
    }
    
    public Condition getContainedCondition()
    {
        return filter;
    }

    public boolean isMatched( Search search, RemoteFile remoteFile )
    {
        return !filter.isMatched(search, remoteFile);
    }

    public boolean isComplete()
    {
        return filter != null;
    }
    
    @Override
    public Object clone()
    {
        try
        {
            NotCondition clone = (NotCondition)super.clone();
            clone.filter = (Condition) filter.clone();            
            return clone;
        }
        catch (CloneNotSupportedException e) 
        { 
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }
    
    public synchronized DCondition createDCondition()
    {
        DNotCondition dCond = new DNotCondition();
        dCond.setDCondition( filter.createDCondition() );
        return dCond;
    }
    
    @Override
    public String toString()
    {
        return super.toString() + "[filter: " + filter.toString() + "]";
    }
}
