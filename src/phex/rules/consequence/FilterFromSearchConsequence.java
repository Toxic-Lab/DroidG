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
 *  Created on 18.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.rules.consequence;

import phex.download.RemoteFile;
import phex.query.Search;
import phex.servent.Servent;
import phex.xml.sax.rules.DConsequence;
import phex.xml.sax.rules.DFilterFromSearchConsequence;

public class FilterFromSearchConsequence implements Consequence
{
    public static final FilterFromSearchConsequence INSTANCE = 
        new FilterFromSearchConsequence();
    private static final DFilterFromSearchConsequence DELEMENT = 
        new DFilterFromSearchConsequence();

    public void invoke( Search search, RemoteFile remoteFile, Servent servent )
    {
        remoteFile.setFilteredHidden(true);
    }

    @Override
    public Object clone()
    {
        // there is only one instance...
        return INSTANCE;
    }

    public DConsequence createDConsequence()
    {
        return DELEMENT;
    }
}
