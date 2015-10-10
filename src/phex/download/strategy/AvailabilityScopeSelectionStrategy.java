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

import phex.download.DownloadScope;
import phex.download.DownloadScopeList;
import phex.download.RatedDownloadScopeList;
import phex.download.swarming.SWDownloadFile;

/**
 * This scope selection strategy tries to prefer a scope that has the lowest
 * availability among the download candidates.
 */
public class AvailabilityScopeSelectionStrategy implements ScopeSelectionStrategy
{
    public DownloadScope selectDownloadScope( SWDownloadFile downloadFile,
        DownloadScopeList wantedScopeList, long preferredSize )
    {
        RatedDownloadScopeList ratedScopeList = downloadFile.getRatedScopeList();
        ratedScopeList.retainAll( wantedScopeList );
        ratedScopeList.prepareRating();
        
        if( ratedScopeList.isRatingFruitful() )
        {
            DownloadScope bestScope = ratedScopeList.getBestRated();
            if ( bestScope.getLength() > preferredSize )
            {
                bestScope = new DownloadScope( 
                    bestScope.getStart(), bestScope.getStart() + preferredSize - 1 );
            }
            return bestScope;
        }
        return null;
    }
}
