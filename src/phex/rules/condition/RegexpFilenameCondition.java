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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import phex.download.RemoteFile;
import phex.query.Search;
import phex.xml.sax.rules.DCondition;

/**
 * Filters all files matching the expression.
 */
public class RegexpFilenameCondition implements Condition
{
    // TODO should we extends this from filenamecondition and use some of its logic?
    
    /**
     * The expression to trigger the filter on
     */
    private String expression;
    
    /**
     * If not null indicates that a regular expression is used to filter.
     */
    private Pattern filterPattern;
        
    /**
     * @param expression
     * @param case1
     */
    public RegexpFilenameCondition( String expression, boolean ignoreCase )
    {
        this.expression = expression;
        int flags = 0;
        if ( ignoreCase )
        {
            flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        filterPattern = Pattern.compile( expression, flags );
    }

    public boolean isMatched( Search search, RemoteFile remoteFile )
    {
        String filename = remoteFile.getFilename();        
        Matcher m = filterPattern.matcher( filename );
        if ( m.matches() )
        {
            return true;
        }
        return false;
    }

    public boolean isComplete()
    {// TODO implement together with list handling
        
        return true;
    }
    
    @Override
    public Object clone()
    {
        throw new UnsupportedOperationException();
    }
    
    public synchronized DCondition createDCondition()
    {
        throw new UnsupportedOperationException("Not yet implemented.");
    }
}
