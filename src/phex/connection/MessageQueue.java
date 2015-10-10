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
package phex.connection;

import java.io.IOException;

import phex.host.Host;
import phex.msg.Message;
import phex.msg.MsgHeader;
import phex.servent.Servent;
import phex.statistic.SimpleStatisticProvider;
import phex.statistic.StatisticProviderConstants;
import phex.statistic.StatisticsManager;

/**
 * This queue handles the flow control algorithm described in the
 * 'SACHRIFC: Simple Flow Control for Gnutella' proposal by Limewire.
 *
 */
public class MessageQueue
{
    /**
     * The max size of a queue.
     */
    private static final int MAX_QUEUE_SIZE = 100;

    /**
     * The long message timeout ( 10 sec ).
     */
    private static final int LONG_TIMEOUT = 10000;

    /**
     * The short message timeout ( 5 sec ).
     */
    private static final int SHORT_TIMEOUT = 5000;

    /**
     * The number of used prioritys
     */
    private static final int PRIORITY_COUNT = 8;

    /**
     * The priority for urgent requests like keep alive pings.
     */
    private static final int PRIORITY_URGENT = 0;
    private static final int PRIORITY_MY_QUERY = 1;
    private static final int PRIORITY_PUSH = 2;
    private static final int PRIORITY_QUERY_HIT = 3;
    private static final int PRIORITY_QUERY = 4;
    private static final int PRIORITY_PONG = 5;
    private static final int PRIORITY_PING = 6;
    /**
     * The priority for all other messages like for the QRP.
     */
    private static final int PRIORITY_OTHER = 7;

    /**
     * The host this queue belongs to.
     */
    private Host host;

    /**
     * The array of FlowControlQueues.
     */
    private FlowControlQueue flowControlQueue[];
    private int queuedCount;


    /**
     * The  priority of the last message added.
     */
    private int lastPriorityAdded;

    public MessageQueue( Host host )
    {
        this.host = host;
        // burst rates are taken from Limewire.

        flowControlQueue = new FlowControlQueue[ PRIORITY_COUNT ];
        // no timeout and lifo queue.
        flowControlQueue[ PRIORITY_URGENT ] = new FlowControlQueue( 1,
            Integer.MAX_VALUE, MAX_QUEUE_SIZE, true );
            
        // long timeout and lifo queue
        // TODO could be priority based.
        flowControlQueue[ PRIORITY_MY_QUERY ] = new FlowControlQueue( 10,
            LONG_TIMEOUT, MAX_QUEUE_SIZE, true );

        // long timeout and lifo queue
        // TODO could be priority based.
        flowControlQueue[ PRIORITY_PUSH ] = new FlowControlQueue( 6,
            LONG_TIMEOUT, MAX_QUEUE_SIZE, true );

        // long timeout and lifo queue
        // TODO could be priority based.
        flowControlQueue[ PRIORITY_QUERY_HIT ] = new FlowControlQueue( 6,
            LONG_TIMEOUT, MAX_QUEUE_SIZE, true );

        // short timeout and lifo queue
        // TODO could be priority based.
        flowControlQueue[ PRIORITY_QUERY ] = new FlowControlQueue( 3,
            SHORT_TIMEOUT, MAX_QUEUE_SIZE, true );

        // short timeout and lifo queue
        // TODO could be priority based.
        flowControlQueue[ PRIORITY_PONG ] = new FlowControlQueue( 1,
            SHORT_TIMEOUT, MAX_QUEUE_SIZE, true );

        // short timeout and lifo queue
        // TODO could be priority based.
        flowControlQueue[ PRIORITY_PING ] = new FlowControlQueue( 1,
            SHORT_TIMEOUT, MAX_QUEUE_SIZE, true );

        // no timeout and fifo queue to maintain QRP message order
        flowControlQueue[ PRIORITY_OTHER ] = new FlowControlQueue( 1,
            Integer.MAX_VALUE, MAX_QUEUE_SIZE, false );
    }

    public void addMessage( Message msg )
    {
        int priority = calculatePriority( msg );
        synchronized( this )
        {
            //Logger.logMessage( Logger.FINEST, Logger.NETWORK,
            //    "Adding message to queue: " + msg );

            flowControlQueue[ priority ].addMessage( msg );
            int tmpDropCount = flowControlQueue[ priority ].getAndResetDropCount();
            // count sent drop
            host.incSentDropCount( tmpDropCount );
            
            StatisticsManager statMgr = Servent.getInstance().getStatisticsService();
            ((SimpleStatisticProvider) statMgr.getStatisticProvider(
                StatisticProviderConstants.DROPEDMSG_OUT_PROVIDER)).increment( tmpDropCount );
            // update queuedCount.
            queuedCount += 1 - tmpDropCount;
            lastPriorityAdded = priority;
        }
    }

    public void sendQueuedMessages()
        throws IOException
    {
        FlowControlQueue queue;
        Message msg;
        boolean isQueueEmpty = false;
        int i, start;
        i = start = lastPriorityAdded;
        do
        {
            queue = flowControlQueue[i];
            queue.initNewMessageBurst();
            while ( true )
            {
                synchronized( this )
                {
                    msg = queue.removeMessage();
                    //Logger.logMessage( Logger.FINEST, Logger.NETWORK,
                    //    "Getting message from queue: " + msg );
                    int tmpDropCount = queue.getAndResetDropCount();
                    // count sent drop
                    host.incSentDropCount( tmpDropCount );

                    StatisticsManager statMgr = Servent.getInstance().getStatisticsService();
                    ((SimpleStatisticProvider) statMgr.getStatisticProvider(
                        StatisticProviderConstants.DROPEDMSG_OUT_PROVIDER)).increment( tmpDropCount );
                    if ( msg == null )
                    {
                        queuedCount -= tmpDropCount;
                    }
                    else
                    {
                        queuedCount -= 1 + tmpDropCount;
                    }

                    if ( queuedCount == 0 )
                    {
                        isQueueEmpty = true;
                    }

                    if ( msg == null )
                    {
                        break;
                    }
                }
                host.sendMessage( msg );
            }
            if ( isQueueEmpty )
            {// break if queue is empty
                break;
            }
            // go to next queue and cycle if necessary
            i = ( i + 1 ) % PRIORITY_COUNT;
        }
        // stop after going through one message burst.
        while( i != start );
        host.flushOutputStream();
    }

    public int getQueuedMessageCount()
    {
        synchronized( this )
        {
            return queuedCount;
        }
    }

    private int calculatePriority( Message msg )
    {
        MsgHeader header = msg.getHeader();
        int messageCode = header.getPayload();

        switch( messageCode )
        {
            case MsgHeader.PING_PAYLOAD:
                if ( header.getHopsTaken() == 0 && header.getTTL() <= 2 )
                {
                    return PRIORITY_URGENT;
                }
                return PRIORITY_PING;
            case MsgHeader.PONG_PAYLOAD:
                if ( header.getHopsTaken() == 0 && header.getTTL() <= 2 )
                {
                    return PRIORITY_URGENT;
                }
                return PRIORITY_PONG;
            case MsgHeader.PUSH_PAYLOAD:
                return PRIORITY_PUSH;
            case MsgHeader.QUERY_HIT_PAYLOAD:
                return PRIORITY_QUERY_HIT;
            case MsgHeader.QUERY_PAYLOAD:
                // query send from me...
                if ( header.getHopsTaken() == 0 )
                {
                    return PRIORITY_MY_QUERY;
                }
                return PRIORITY_QUERY;
            default:
                // like QRP messages.
                return PRIORITY_OTHER;
        }
    }
}