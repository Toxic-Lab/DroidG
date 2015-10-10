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

import java.util.List;

import phex.download.RemoteFile;
import phex.query.Search;
import phex.xml.sax.rules.DAndConcatCondition;
import phex.xml.sax.rules.DCondition;

/**
 * This is a AND concatenation search filter. All filters in this filter
 * must match the filter to get it triggered.
 *
 */
public class AndConcatCondition extends ConcatCondition
{
    public synchronized boolean isMatched( Search search, RemoteFile remoteFile )
    {
        if ( filterList.size() == 0 )
        {
            return true;
        }
        
        boolean isMatched = true;
        for( Condition filter : filterList )
        {
            boolean condMatched = filter.isMatched( search, remoteFile );
            if ( !condMatched )
            {
                isMatched = false;
                break;
            }
        }
        return isMatched;
    }
    
    public synchronized DCondition createDCondition()
    {
        DAndConcatCondition dCond = new DAndConcatCondition();
        List<DCondition> dList = dCond.getSubElementList();
        for( Condition cond : filterList )
        {
            dList.add( cond.createDCondition() );
        }
        return dCond;
    }
}