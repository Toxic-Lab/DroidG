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

import java.util.HashMap;

import phex.common.address.DestAddress;
import phex.msg.InvalidMessageException;
import phex.msg.QueryResponseMsg;
import phex.msg.vendor.OOBReplyCountVMsg;
import phex.servent.Servent;
import phex.utils.IOUtil;
import phex.utils.RandomUtils;

public abstract class QuerySearch extends Search
{
//    private static final Logger logger = LoggerFactory
//        .getLogger( QuerySearch.class );

    private HashMap<Long, OOBQueryToken> queryTokens;
    private volatile int totalExpectedResults;

    public QuerySearch(Servent servent)
    {
        super( servent );
    }
    
    /**
     * Tries a very basic calculation about the search progress.
     * @return the search progress between 0 and 100
     */
    public int getProgress()
    {
        if ( searchProgress == null )
        {
            return 0;
        }
        if ( searchProgress.isSearchFinished() )
        {
            return 100;
        }
        
        int progress = searchProgress.getProgress();
        //logger.debug( "Search progress: {}", Integer.valueOf( progress ) );
        if ( queryEngine != null )
        {
            int qeProgress = queryEngine.getProgress();
            //logger.debug( "Query engine progress: {}", Integer.valueOf( qeProgress ) );
            progress = Math.max( progress, qeProgress );
        }
        //logger.debug( "QuerySearch progress: {}", Integer.valueOf( progress ) );
        return progress;
    }
    

    protected byte[] generateSecurityToken(int expectedResults,
        DestAddress address)
    {
        byte[] secToken = RandomUtils.getBytes( 8 );
        long sec = IOUtil.deserializeLong( secToken, 0 );
        if ( queryTokens == null )
        {
            queryTokens = new HashMap<Long, OOBQueryToken>();
        }
        queryTokens.put( Long.valueOf( sec ), 
            new OOBQueryToken(address, expectedResults ) );
        totalExpectedResults += expectedResults;
        return secToken;
    }

    protected boolean isValid(QueryResponseMsg msg,
        DestAddress recAddress) throws InvalidMessageException
    {
        if ( queryTokens == null )
        {
            //logger.debug( "Drop OOB response no tokens given yet." );
            return false;
        }
        byte[] securityToken = msg.getSecurityToken();
        if ( securityToken == null )
        {
            //logger.debug( "Drop OOB response msg has no tokens." );
            return false;
        }
        if ( securityToken.length != 8 )
        {
            //logger.debug( "Drop OOB response wrong tokens length." );
            return false;
        }
        long sec = IOUtil.deserializeLong( securityToken, 0 );
        Long secObj = Long.valueOf( sec );
        OOBQueryToken token = queryTokens.get( secObj );
        if ( token == null )
        {
            //logger.debug( "Drop OOB response no tokens found." );
            return false;
        }
        if ( !token.isAcceptableResponse( msg, recAddress ) )
        {
            return false;
        }
        
        totalExpectedResults -= msg.getRecordCount();
        
        if ( token.isResponseReceived() )
        {
            queryTokens.remove( secObj );
        }
        return true;
    }

    public void processOOBReplyCountResponse(OOBReplyCountVMsg msg,
        DestAddress sourceAddress)
    {
        if ( isSearchFinished() )
        {
            return;
        }
        
        if ( !msg.getHeader().getMsgID().equals(
            queryMsg.getHeader().getMsgID() ) )
        {
            return;
        }

        //logger.debug( "Handling oob message from {}.", sourceAddress );
        
        int resultOpen = searchProgress.getDesiredResultsCount() - 
            searchProgress.getReceivedResultsCount();
        // never request more expected results then 4 times as much as still needed
        if ( totalExpectedResults > resultOpen * 4 )
        {
            //logger.debug( "Already expeting enough results: {}/{}", 
            //    totalExpectedResults, resultOpen );
            return;
        }

        //logger.debug( "Prepare UDP response for {}.", sourceAddress );
        byte[] secToken = generateSecurityToken( msg.getResultCount(),
            sourceAddress );
        servent.getMessageService().sendUdpMessageAcknowledgementVMsg(
            msg.getHeader().getMsgID(), msg.getResultCount(), secToken,
            sourceAddress );
    }

    public void processOOBResponse(QueryResponseMsg msg,
        DestAddress sourceAddress) throws InvalidMessageException
    {
        // check if it is a response for this query?
        if ( !msg.getHeader().getMsgID().equals(
            queryMsg.getHeader().getMsgID() ) )
        {
            return;
        }

        if ( !isValid( msg, sourceAddress ) )
        {
            return;
        }

        // validate IP addresses
        if ( !msg.getDestAddress().getIpAddress().equals(
            sourceAddress.getIpAddress() ) )
        {
            if ( msg.getPushNeededFlag() == QHDFlag.QHD_TRUE_FLAG
                || !msg.getDestAddress().isSiteLocalAddress() )
            {
                msg.setDestAddressFromOOB( sourceAddress );
            }
            else
            {
                // should we drop? likely spam.. but not sure...
            }
        }
        processResponse( msg );
    }

    private static class OOBQueryToken
    {
        private DestAddress expectedAddress;

        private int expectedResults;

        private int receivedResults;
        
        
        public OOBQueryToken( DestAddress expectedAddress,
            int expectedResults )
        {
            this.expectedAddress = expectedAddress;
            this.expectedResults = expectedResults;
            this.receivedResults = 0;
        }
        
        public boolean isAcceptableResponse( QueryResponseMsg msg, DestAddress sourceAddress )
        {
            if ( !expectedAddress.equals( sourceAddress ) )
            {
                // coming from wrong address...
                //logger.debug( "Drop OOB response from invalid address." );
                return false;
            }
            int responses = msg.getRecordCount();
            if ( receivedResults + responses > expectedResults )
            {
                // too many results...
                //logger.debug( "Drop OOB response too many responses." );
                return false;
            }
            receivedResults += responses;
            return true;
        }
        
        public boolean isResponseReceived()
        {
            return receivedResults >= expectedResults;
        }
    }
}