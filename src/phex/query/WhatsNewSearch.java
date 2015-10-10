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

import java.util.ArrayList;

import phex.common.URN;
import phex.common.address.DestAddress;
import phex.download.RemoteFile;
import phex.msg.InvalidMessageException;
import phex.msg.QueryFactory;
import phex.msg.QueryResponseMsg;
import phex.msg.QueryResponseRecord;
import phex.msg.vendor.OOBReplyCountVMsg;
import phex.servent.Servent;
import phex.utils.RandomUtils;


public class WhatsNewSearch extends QuerySearch
{
    public WhatsNewSearch( QueryFactory queryFactory, Servent servent )
    {
        super( servent );
        queryMsg = queryFactory.createWhatsNewQuery();
    }
    
    @Override
    public void processResponse( QueryResponseMsg msg )
        throws InvalidMessageException
    {
        //we like to receive results even if the query was stopped already.
        
        // check if it is a response for this query?
        if (!msg.getHeader().getMsgID().equals( queryMsg.getHeader().getMsgID()))
        {
            return;
        }

        // remoteHost.log("Got response to my query.  " + msg);
        
        QueryHitHost qhHost = QueryHitHost.createFrom( msg );
        RemoteFile rfile;
        QueryResponseRecord[] records = msg.getMsgRecords();
        ArrayList<RemoteFile> newHitList = new ArrayList<RemoteFile>( records.length );
        for (int i = 0; i < records.length; i++)
        {
            if ( !isResponseRecordValid( records[i] ) )
            {// skip record.
                continue;
            }
            synchronized( this )
            {   
                long fileSize = records[i].getFileSize();
                String filename = records[i].getFilename();
                URN urn = records[i].getURN();
                int fileIndex = records[i].getFileIndex();
                String metaData = records[i].getMetaData();
                
                // find duplicate from same host...
                RemoteFile availableHit = searchResultHolder.findQueryHit( qhHost, urn, filename,
                    fileSize, fileIndex );
                
                if ( availableHit != null )
                {
                    // update availableHit
                    availableHit.updateQueryHitHost( qhHost );
                    availableHit.setMetaData( metaData );
                }
                else
                {
                    String pathInfo = records[i].getPathInfo();
                    rfile = new RemoteFile( qhHost, fileIndex, filename, pathInfo,
                        fileSize, urn, metaData, (short)100 );
                    searchResultHolder.addQueryHit( rfile );
                    newHitList.add( rfile );
                }
                // handle possible AlternateLocations
                DestAddress[] alternateLocations = records[i].getAlternateLocations();
                if ( urn != null && alternateLocations != null)
                {
                    for ( int j = 0; j < alternateLocations.length; j++ )
                    {
                        // find duplicate from same host...
                        QueryHitHost qhh = new QueryHitHost( null, alternateLocations[j], -1 );
                        
                        availableHit = searchResultHolder.findQueryHit( qhHost, 
                            urn, filename, fileSize, fileIndex );
                        if ( availableHit != null )
                        {
                            // update availableHit
                            availableHit.updateQueryHitHost( qhHost );
                            availableHit.setMetaData( metaData );
                        }
                        else
                        {
                            rfile = new RemoteFile( qhh, -1, filename, "", 
                                fileSize, urn, metaData, (short)100 );
                            searchResultHolder.addQueryHit( rfile );
                            newHitList.add( rfile );
                        }
                    }
                }
            }
        }
        // if something was added...
        if ( newHitList.size() > 0 )
        {
            RemoteFile[] newHits = new RemoteFile[ newHitList.size() ];
            newHitList.toArray( newHits );
            fireSearchHitsAdded( newHits );
        }
    }
        
    @Override
    public String toString()
    {
        return "[WhatsNewSearch," + "@" + Integer.toHexString(hashCode()) + "]";
    }
}