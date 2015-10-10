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
package phex.query;

import phex.common.URN;
import phex.download.RemoteFile;
import phex.download.swarming.SWDownloadFile;
import phex.download.swarming.SwarmingManager;
import phex.host.Host;
import phex.msg.InvalidMessageException;
import phex.msg.QueryResponseMsg;
import phex.msg.QueryResponseRecord;
import phex.msghandling.MessageSubscriber;
import phex.prefs.core.ConnectionPrefs;
import phex.security.AccessType;
import phex.security.PhexSecurityManager;

/**
 * This class monitors incoming {@link QueryResponseMsg} and tries to find 
 * corresponding {@link SWDownloadFile} to add new candidates.
 */
public class DownloadCandidateSnoop implements MessageSubscriber<QueryResponseMsg>
{   
    private final SwarmingManager swarmingMgr;
    private final PhexSecurityManager securityService;

    public DownloadCandidateSnoop( SwarmingManager swarmingMgr, PhexSecurityManager securityService )
    {
        this.swarmingMgr = swarmingMgr;
        this.securityService = securityService;
    }
    
    /**
     * Checks incoming {@link QueryResponseMsg} against {@link SWDownloadFile}
     * to find possible candidates.
     */
    public void onMessage(QueryResponseMsg message, Host sourceHost)
    {
        if( !ConnectionPrefs.EnableQueryHitSnooping.get().booleanValue() )
        {
            return;
        }
        
        try
        {
            QueryHitHost qhHost = null;
            QueryResponseRecord[] records = message.getMsgRecords();
            for (QueryResponseRecord rec : records)
            {            
                if ( !isResponseRecordValid( rec ) )
                {// skip record.
                    continue;
                }
                SWDownloadFile swdlf = swarmingMgr.getDownloadFile( rec.getFileSize(),
                    rec.getURN() );
                if ( swdlf == null )
                {
                    continue;
                }
                
                if ( qhHost == null )
                {
                    qhHost = QueryHitHost.createFrom( message );
                }
                // add record as candidate...
                RemoteFile rFile = new RemoteFile( qhHost, rec.getFileIndex(),
                    rec.getFilename(), rec.getPathInfo(), rec.getFileSize(),
                    rec.getURN(), rec.getMetaData(), (short) -1 );
                swdlf.addDownloadCandidate(rFile);
            }
        }
        catch (InvalidMessageException e)
        {
            // message is invalid...
            return;
        }
    }
    
    /**
     * Used to check if the record is valid. In this case a 
     * security check is done on the record URN.
     * @param record
     * @return true if valid, false otherwise.
     */
    private boolean isResponseRecordValid( QueryResponseRecord record )
    {
        // REWORK maybe we should if we should move the altloc security check
        // from GGEPExtension.parseAltExtensionData to here to?
        URN urn = record.getURN();
        if ( urn != null && securityService.controlUrnAccess( urn ) != AccessType.ACCESS_GRANTED )
        {
            //NLogger.debug( DownloadCandidateSnoop.class, "Record contains blocked URN: " + urn.getAsString() );
            return false;
        }
        return true;
    }
}
