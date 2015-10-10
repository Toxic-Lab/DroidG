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
import phex.xml.sax.rules.DCondition;

/**
 * This is a OR concatenation search filter. When one filter in this filter
 * matches the filter gets triggered.
 *
 */
public class OrConcatCondition extends ConcatCondition
{
    public synchronized boolean isMatched( Search search, RemoteFile remoteFile )
    {
        if ( filterList.size() == 0 )
        {
            return true;
        }
        
        boolean isMatched = false;
        for( Condition filter : filterList )
        {
            if ( filter.isMatched( search, remoteFile ) )
            {
                isMatched = true;
                break;
            }
        }
        return isMatched;
    }
    
    public synchronized DCondition createDCondition()
    {
        throw new UnsupportedOperationException("Not yet implemented.");
    }
}
