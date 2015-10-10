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

import phex.common.URN;
import phex.download.RemoteFile;
import phex.download.swarming.SwarmingManager;
import phex.query.Search;
import phex.servent.Servent;
import phex.share.SharedFilesService;
import phex.utils.StringUtils;
import phex.xml.sax.rules.DConsequence;
import phex.xml.sax.rules.DDownloadFileConsequence;

public class DownloadFileConsequence implements Consequence
{
    public static final DownloadFileConsequence INSTANCE = 
        new DownloadFileConsequence();
    private static final DDownloadFileConsequence DELEMENT = 
        new DDownloadFileConsequence();

    public void invoke( Search search, final RemoteFile remoteFile, Servent servent )
    {
        URN urn = remoteFile.getURN();
        if ( urn == null )
        {
            // don't do automated stuff with files without an URN,
            // to dangerous and unsecure.
            return;
        }
        SwarmingManager swarmingMgr = servent.getDownloadService();
        if ( swarmingMgr.isURNDownloaded(urn) )
        {
            // file already downloading...
            return;
        }
        SharedFilesService fileService = servent.getSharedFilesService();
        if ( fileService.isURNShared( urn ) )
        {
            // file already shared... don't download...
            return;
        }
        
        remoteFile.setInDownloadQueue( true );
        RemoteFile dfile = new RemoteFile( remoteFile );
        String searchTerm = StringUtils.createNaturalSearchTerm( dfile.getFilename() );
        swarmingMgr.addFileToDownload( dfile, dfile.getFilename(), searchTerm );        
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