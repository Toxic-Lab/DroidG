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
package phex.download.strategy;

import java.util.Random;

import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.swarming.SWDownloadFile;

/**
 * This download strategy first analysis if there is a useful scope by 
 * availability. If no scope was found and the file is streamable there is a 
 * 50% chance that the beginning is selected otherwise a random segment is chosen.
 */
public class AvailBeginRandSelectionStrategy implements ScopeSelectionStrategy
{
    private final Random random;
    private final ScopeSelectionStrategy availStrategy;
    private final ScopeSelectionStrategy beginStrategy;
    private final ScopeSelectionStrategy randStrategy;
    
    protected AvailBeginRandSelectionStrategy(
        AvailabilityScopeSelectionStrategy availStrategy,
        PrefereBeginingScopeSelectionStrategy beginStrategy,
        RandomScopeSelectionStrategy randStrategy )
    {
        this.random = new Random();
        this.availStrategy = availStrategy;
        this.beginStrategy = beginStrategy;
        this.randStrategy = randStrategy;
    }


    public DownloadScope selectDownloadScope( SWDownloadFile downloadFile,
        DownloadScopeList wantedScopeList, long preferredSize )
    {
        DownloadScope scope = availStrategy.selectDownloadScope(
            downloadFile, wantedScopeList, preferredSize);
        
        if ( scope == null && downloadFile.isDestinationStreamable() )
        {
            // choose beginning by 50%
            boolean useBegin = random.nextBoolean( );
            if ( useBegin )
            {
                scope = beginStrategy.selectDownloadScope(downloadFile, 
                    wantedScopeList, preferredSize);
            }
        }
        
        if ( scope == null )
        {
            scope = randStrategy.selectDownloadScope(
                downloadFile, wantedScopeList, preferredSize);
        }
        return scope;
    }
}
