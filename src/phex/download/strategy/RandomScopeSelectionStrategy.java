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
 *  Created on 16.09.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.download.strategy;

import java.util.Random;

import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.swarming.SWDownloadFile;

/**
 * This scope selection strategy tries to preferre a scope that has the lowest
 * availability among the download candidates.
 */
public class RandomScopeSelectionStrategy implements ScopeSelectionStrategy
{
    private static final Random random = new Random();
    
    public DownloadScope selectDownloadScope( SWDownloadFile downloadFile,
        DownloadScopeList wantedScopeList, long preferredSize )
    {
        int size = wantedScopeList.size();
        int pos = random.nextInt( size );
        
        DownloadScope bestScope = wantedScopeList.getScopeAt( pos );
        if ( bestScope.getLength() > preferredSize )
        {
            if ( downloadFile.getMemoryFile().getDownloadedFragmentCount() > 15 )
            {
                // Try to reduce scope fragmenting by selecting the bestScope
                // beginning when we have more then 15 finished fragments. This 
                // might not continue a finished fragment because of candidate 
                // availability but raises the chance.
                bestScope = new DownloadScope( 
                    bestScope.getStart(), bestScope.getStart() + preferredSize - 1 );
            }
            else
            {
                int parts = (int)Math.floor( 
                    (double)bestScope.getLength() / (double)preferredSize );
                int startPart = random.nextInt( parts );
                long startPos = Math.min( 
                    bestScope.getStart() + startPart*preferredSize, 
                    bestScope.getEnd() - preferredSize + 1 );
                bestScope = new DownloadScope( startPos, 
                    startPos + preferredSize - 1 );
            }
        }
        return bestScope;
    }
}
