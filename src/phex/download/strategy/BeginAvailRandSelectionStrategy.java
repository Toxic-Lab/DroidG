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
 *  Created on 20.09.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.download.strategy;

import java.util.Random;

import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.swarming.SWDownloadFile;

public class BeginAvailRandSelectionStrategy implements ScopeSelectionStrategy
{
    private final Random random;
    private final ScopeSelectionStrategy beginStrategy;
    private final ScopeSelectionStrategy availRandStrategy;
    
    protected BeginAvailRandSelectionStrategy(
        PrefereBeginingScopeSelectionStrategy beginStrategy,
        AvailRandSelectionStrategy availRandStrategy)
    {
        this.random = new Random();
        this.beginStrategy = beginStrategy;
        this.availRandStrategy = availRandStrategy;
    }

    public DownloadScope selectDownloadScope( SWDownloadFile downloadFile,
        DownloadScopeList wantedScopeList, long preferredSize )
    {
        DownloadScope scope = null;
        // choose beginning by 50%
        boolean useBegin = random.nextBoolean( );
        if ( useBegin )
        {
            scope = beginStrategy.selectDownloadScope(
                downloadFile, wantedScopeList, preferredSize );
        }
        if ( scope == null )
        {
            scope = availRandStrategy.selectDownloadScope(
                downloadFile, wantedScopeList, preferredSize);
        }
        return scope;
    }
}
