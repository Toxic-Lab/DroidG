/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2009 Phex Development Group
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
package phex.msghandling;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.xsocket.IDataSource;

import phex.common.address.DestAddress;
import phex.host.HostManager;
import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.Message;
import phex.msg.MessageProcessor;
import phex.msg.MsgHeader;
import phex.msg.PingMsg;
import phex.msg.PongFactory;
import phex.msg.PongMsg;
import phex.msg.QueryResponseMsg;
import phex.msg.vendor.MessageAcknowledgementVMsg;
import phex.msg.vendor.OOBReplyCountVMsg;
import phex.msg.vendor.UdpHeadPingVMsg;
import phex.msg.vendor.VendorMsg;
import phex.net.UdpDataHandler;
import phex.net.UdpService;
import phex.prefs.core.MessagePrefs;
import phex.security.AccessType;
import phex.security.PhexSecurityManager;
import phex.servent.Servent;
import phex.share.SharedFilesService;
import phex.statistic.StatisticProvider;
import phex.statistic.StatisticsManager;
import phex.udp.UdpGuidRoutingTable;

public class UdpMessageDataHandler implements UdpDataHandler
{
//    private static final Logger logger = LoggerFactory.getLogger( UdpMessageDataHandler.class );
    
    private static final int UDP_PING_PERIOD = 1000 * 3 * 10;
    
    private final Servent servent;
    private final StatisticsManager statsService;
    private final SharedFilesService sharedFilesService;
    private final PongFactory pongFactory;
    private final PhexSecurityManager securityService;
    private final HostManager hostService;
    private final MessageService messageService;
    private final UdpService udpService;
    
    private final UdpGuidRoutingTable pingRoutingTable;
    

    
    public UdpMessageDataHandler(Servent servent,
        StatisticsManager statsService, SharedFilesService sharedFilesService,
        PongFactory pongFactory, PhexSecurityManager securityService,
        HostManager hostService, MessageService messageService,
        UdpService udpService )
    {
        super();
        this.servent = servent;
        this.statsService = statsService;
        this.sharedFilesService = sharedFilesService;
        this.pongFactory = pongFactory;
        this.securityService = securityService;
        this.hostService = hostService;
        this.messageService = messageService;
        this.udpService = udpService;
        
        //create the routing table with a lifetime of the 
        //udp send ping time interval....so a pong will only be accepted if
        //it comes within the period between 0 to 2 * lifetime
        pingRoutingTable = new UdpGuidRoutingTable( UDP_PING_PERIOD );
    }
    

    public void handleUdpData(IDataSource dataSource, DestAddress orgin )
    {
        try
        {
            ByteBuffer headerBuffer = ByteBuffer.allocate( MsgHeader.DATA_LENGTH );
            dataSource.read( headerBuffer );
            headerBuffer.flip();
            MsgHeader msgHeader = MessageProcessor.parseMessageHeader( headerBuffer );
            
            int length = msgHeader.getDataLength();
            if ( length < 0 )
            {
                throw new IOException( "Negative body size. Drop." );
            }
            else if ( length > MessagePrefs.MaxLength.get().intValue() )
            {
                throw new IOException("Packet too big ("+length+"). Drop.");
            }
            
            ByteBuffer bodyBuffer = ByteBuffer.allocate( length );
            dataSource.read( bodyBuffer );
            bodyBuffer.flip();
            
            try
            {
                Message msg = MessageProcessor.createMessageFromBody( msgHeader, bodyBuffer.array(), 
                    securityService );
                msg.setUdpMsg( true );
                AccessType access = securityService.controlHostAddressAccess( orgin );
                if ( access == AccessType.ACCESS_STRONGLY_DENIED )
                {
                    // drop message
                    dropMessage( msg, orgin, "IP access strongly denied." );
                    return;
                }
                
                msgHeader.countHop();
             
                // Now check the payload field and take appropriate action
                switch( msgHeader.getPayload() )
                {
                    case MsgHeader.PING_PAYLOAD :
                        handlePing( (PingMsg)msg, orgin );
                        break;
                    case MsgHeader.PONG_PAYLOAD :
                        handlePong( (PongMsg)msg, orgin );
                        break;
                    case MsgHeader.QUERY_HIT_PAYLOAD:
                        handleQueryResponse( (QueryResponseMsg)msg, orgin );
                        break;
                    case MsgHeader.VENDOR_MESSAGE_PAYLOAD:
                    case MsgHeader.STANDARD_VENDOR_MESSAGE_PAYLOAD:
                        handleVendorMessage( (VendorMsg)msg, orgin );
                        break;
                    default:
                        //logger.debug( "Rcv unrecognized Msg from: {}", orgin );
                        break;
                }                
            }
            catch ( InvalidMessageException exp )
            {
                //logger.warn( exp.toString(), exp );
            }
        }
        catch ( IOException exp )
        {
            //logger.warn( exp.toString(), exp );
        }
    }

    private void handlePing( PingMsg pingMsg, DestAddress orgin ) throws IOException
    {
        MsgHeader header = pingMsg.getHeader();
        if ( header.getHopsTaken() > 1 )
        {
            dropMessage( pingMsg, orgin, "Udp Ping traveled more then 1 hop." );
            return;
        }
        //logger.debug( "Rcv UDP PingMsg {} from {}", pingMsg, orgin );

        // respond to ping
        StatisticProvider uptimeProvider = statsService.getStatisticProvider(
            StatisticsManager.DAILY_UPTIME_PROVIDER );
        int avgDailyUptime = ((Integer)uptimeProvider.getValue()).intValue();
        int shareFileCount = sharedFilesService.getFileCount();
        int shareFileSize = sharedFilesService.getTotalFileSizeInKb();
        
        DestAddress localAddress = servent.getLocalAddress();
        boolean isUdpHostCache = servent.isUdpHostCache();
        
        PongMsg pong = pongFactory.createUdpPongMsg( pingMsg, localAddress, isUdpHostCache,
            avgDailyUptime, shareFileCount, shareFileSize, servent.isUltrapeer() );
        
        udpService.sendDatagram( pong.getbytes(), orgin );
    }

    
    private void handlePong( PongMsg pongMsg, DestAddress orgin )
    {
        //logger.debug( "Rcv PongMsg {} from: {}", pongMsg, orgin );
        MsgHeader header = pongMsg.getHeader();
        if ( header.getHopsTaken() > 1 )
        {
            dropMessage( pongMsg, orgin, "Udp Pong traveled more then 1 hop." );
            return;
        }
        
        // first check if we had sent a ping to receive a pong
        GUID guid = header.getMsgID();
        DestAddress address = pingRoutingTable.getAndRemoveRouting( guid ); 
        if( address == null )
        {
            // did not find routing for this pong
            //logger.warn( "Recieved not requested UDP Pong from {}", orgin );
            return;
        }
        
        // thought of comparing the address in the table to the pong packet's address
        // but since its udp the packet can come from any interface of the packet's host
        // so just be happy that u sent a ping with the same guid
        
        DestAddress pongAddress = pongMsg.getPongAddress();
        // Security checking makes no sense for UDP pong, since we already know
        // from the routing check above that we requested the pong with a ping
        // it can be expected to be fine.
        
        boolean isNew = hostService.catchHosts( pongMsg );
        if ( isNew )
        {
            messageService.addPongToCache( pongMsg );
        }
    }
    
    private void handleQueryResponse( QueryResponseMsg msg, DestAddress orgin ) 
        throws InvalidMessageException
    {
        messageService.dispatchToUdpSubscribers( msg, orgin );
    }
    
    private void handleVendorMessage(VendorMsg msg, DestAddress orgin) throws InvalidMessageException
    {
        if ( msg instanceof OOBReplyCountVMsg )
        {
            handleOOBReplyCountVMsg( (OOBReplyCountVMsg)msg, orgin );
        }
        else if ( msg instanceof UdpHeadPingVMsg )
        {
            UdpHeadPingVMsg headPing = (UdpHeadPingVMsg) msg;
            // likely spam msg...
            //logger.warn( "Possible UdpHeadPing spam from {}: Features: {}, URN: {}, GUID: {}", 
//                new Object[] { orgin, headPing.getFeatures(),
//                headPing.getUrn().getAsString(), headPing.getGuid() } );
        }
    }

    private void handleOOBReplyCountVMsg(OOBReplyCountVMsg msg,
        DestAddress orgin) throws InvalidMessageException
    {
        messageService.dispatchToUdpSubscribers( msg, orgin );
    }
    
    private void dropMessage( Message msg, DestAddress orgin, String reason )
    {
        //logger.info( "Dropping UDP message: {} from {}.", reason, orgin );
//        if ( //logger.isDebugEnabled( ) )
//        {
//            //logger.debug( "Header: [" + msg.getHeader().toString() + "] - Message: [" +
//                msg.toString() + "].");
//        }
        // TODO should we count dropping udp? currently we dont
        // fromHost.incDropCount();
        // MessageCountStatistic.dropedMsgInCounter.increment( 1 );
    }

    public void sendUdpPing( PingMsg pingMsg, DestAddress destination )
    {
        GUID guid = pingMsg.getHeader().getMsgID();
        if( ! (pingRoutingTable.checkAndAddRouting( guid, destination ) ) )
        {
            //could not add to routing table
            //logger.warn( "Ping with duplicate guid not sent {} for message: {}", 
//                guid, pingMsg );
            return;
        }
        //logger.debug( "guid: {} successfully added to routing table for udp ping: \n {}", guid, pingMsg );
        
        byte[] data = pingMsg.getBytes();
        try
        {
            udpService.sendDatagram( data, destination );
        }
        catch ( IOException exp )
        {
            //logger.warn( exp.toString(), exp );
        }
    }
    
    public void sendMessageAcknowledgementVMsg( MessageAcknowledgementVMsg respMsg, 
        DestAddress destination )
    {
        phex.io.buffer.ByteBuffer headerBuf = respMsg.createHeaderBuffer();
        phex.io.buffer.ByteBuffer messageBuf = respMsg.createMessageBuffer();
        int len = headerBuf.remaining();
        byte[] data = new byte[ len + messageBuf.remaining() ];
        headerBuf.get( data, 0, len );
        messageBuf.get( data, len, messageBuf.remaining() );
        try
        {
            udpService.sendDatagram( data, destination );
        }
        catch ( IOException exp )
        {
            //logger.warn( exp.toString(), exp );
        }
    }
}