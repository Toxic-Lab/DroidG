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

import org.apache.commons.collections.set.ListOrderedSet;

import phex.download.RemoteFile;
import phex.query.Search;
import phex.xml.sax.rules.DCondition;
import phex.xml.sax.rules.DFilenameCondition;

/**
 * Filters all files matching the expression.
 */
public class FilenameCondition implements Condition
{
    private ListOrderedSet/*<String>*/ terms;
    
    /**
     * @param expression
     * @param case1
     */
    public FilenameCondition( )
    {
        terms = new ListOrderedSet();
    }
    
    /**
     * Deep copy constructor.
     * @param condition
     */
    public FilenameCondition( FilenameCondition condition )
    {
        this();
        update( condition );
    }
    
    public synchronized void update( FilenameCondition condition )
    {
        terms.clear();
        // we simply add the string.. since they are not mutable. 
        terms.addAll(condition.terms);
    }
    
    
    public synchronized int getTermsCount()
    {
        return terms.size();
    }
    
    public synchronized Set<String> getTerms()
    {
        return Collections.unmodifiableSet(terms);
    }
    
    /**
     * Returns a unmodifiable list of the ranges.
     * @return
     */
    public synchronized List<String> getTermsList()
    {
        return terms.asList();
    }
    
    public synchronized FilenameCondition addTerm( String term )
    {
        term = term.toLowerCase();
        terms.add( term );
        return this;
    }
    
    public synchronized void removeTerm( String term )
    {
        terms.remove(term);
    }

    public synchronized boolean isMatched( Search search, RemoteFile remoteFile )
    {
        String filename = remoteFile.getFilename();
        filename = filename.toLowerCase();
        
        Iterator<String> iterator = terms.iterator();
        while( iterator.hasNext() )
        {
            String term = iterator.next();
            if ( filename.indexOf( term ) != -1 )
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Validates if this condition is completly edited and ready for storage or 
     * requires further modifications.
     * @return true if complet false otherwise.
     */
    public synchronized boolean isComplete()
    {
        return getTermsCount() > 0;
    }
    
    @Override
    public synchronized Object clone()
    {
        try
        {
            FilenameCondition clone = (FilenameCondition) super.clone();
            clone.terms = new ListOrderedSet();
            clone.terms.addAll( terms );
            return clone;
        }
        catch (CloneNotSupportedException exp)
        {
            throw new InternalError();
        }
    }

    public synchronized DCondition createDCondition()
    {
        DFilenameCondition dCond = new DFilenameCondition();
        List<String> newList = new ArrayList<String>( terms );
        dCond.setTerms(newList);
        return dCond;
    }
}
