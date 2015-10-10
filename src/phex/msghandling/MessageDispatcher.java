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
package phex.msghandling;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import phex.common.Environment;
import phex.common.QueryRoutingTable;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.common.format.NumberFormatUtils;
import phex.host.Host;
import phex.host.HostManager;
import phex.io.buffer.ByteBuffer;
import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.Message;
import phex.msg.MsgHeader;
import phex.msg.PingMsg;
import phex.msg.PongFactory;
import phex.msg.PongMsg;
import phex.msg.PushRequestMsg;
import phex.msg.QueryMsg;
import phex.msg.QueryResponseMsg;
import phex.msg.QueryResponseRecord;
import phex.msg.RouteTableUpdateMsg;
import phex.msg.vendor.CapabilitiesVMsg;
import phex.msg.vendor.HopsFlowVMsg;
import phex.msg.vendor.MessagesSupportedVMsg;
import phex.msg.vendor.PushProxyAcknowledgementVMsg;
import phex.msg.vendor.PushProxyRequestVMsg;
import phex.msg.vendor.TCPConnectBackRedirectVMsg;
import phex.msg.vendor.TCPConnectBackVMsg;
import phex.msg.vendor.VendorMsg;
import phex.net.connection.Connection;
import phex.net.connection.ConnectionFactory;
import phex.prefs.core.BandwidthPrefs;
import phex.prefs.core.MessagePrefs;
import phex.security.AccessType;
import phex.security.PhexSecurityManager;
import phex.servent.Servent;
import phex.share.QueryResultSearchEngine;
import phex.share.ShareFile;
import phex.share.SharedFilesService;
import phex.statistic.SimpleStatisticProvider;
import phex.statistic.StatisticProvider;
import phex.statistic.StatisticsManager;
import phex.upload.PushWorker;
import phex.utils.StringUtils;

class MessageDispatcher
{
//    private static final Logger logger = LoggerFactory.getLogger( 
//        MessageDispatcher.class );
    
    private final Servent servent;
    private final MessageRouting msgRouting;
    private final Map<Class<? extends Message>, MessageSubscriber<? extends Message>> messageSubscribers;
    private final Map<Class<? extends Message>, UdpMessageSubscriber<? extends Message>> udpMessageSubscribers;
    private final PongFactory pongFactory;
    private final SharedFilesService sharedFilesService;
    private final HostManager hostMgr;
    private final PhexSecurityManager securityService;
    
    // for stats
    private SimpleStatisticProvider pingMsgInCounter;
    private SimpleStatisticProvider pongMsgInCounter;
    private SimpleStatisticProvider queryMsgInCounter;
    private SimpleStatisticProvider queryHitMsgInCounter;
    private SimpleStatisticProvider pushMsgInCounter;
    private SimpleStatisticProvider dropedMsgInCounter;
    private SimpleStatisticProvider totalInMsgCounter;
    private StatisticProvider uptimeStatsProvider;
    
    public MessageDispatcher( Servent servent, MessageRouting msgRouting, 
        PongFactory pongFactory  )
    {
        this.servent = servent;
        this.msgRouting = msgRouting;
        this.pongFactory = pongFactory;
        messageSubscribers = new HashMap<Class<? extends Message>, MessageSubscriber<? extends Message>>();
        udpMessageSubscribers = new HashMap<Class<? extends Message>, UdpMessageSubscriber<? extends Message>>();
        hostMgr = servent.getHostService();
        sharedFilesService = servent.getSharedFilesService();
        securityService = servent.getSecurityService();
    }
    
    // temporary workaround until stats are reworked.
    protected void initStats( StatisticsManager statsMgr )
    {
        // for stats
        pingMsgInCounter = (SimpleStatisticProvider) statsMgr.getStatisticProvider( 
            StatisticsManager.PINGMSG_IN_PROVIDER );
        pongMsgInCounter = (SimpleStatisticProvider) statsMgr.getStatisticProvider( 
            StatisticsManager.PONGMSG_IN_PROVIDER );
        queryMsgInCounter = (SimpleStatisticProvider) statsMgr.getStatisticProvider( 
            StatisticsManager.QUERYMSG_IN_PROVIDER );
        queryHitMsgInCounter = (SimpleStatisticProvider) statsMgr.getStatisticProvider( 
            StatisticsManager.QUERYHITMSG_IN_PROVIDER );
        pushMsgInCounter = (SimpleStatisticProvider) statsMgr.getStatisticProvider( 
            StatisticsManager.PUSHMSG_IN_PROVIDER );
        dropedMsgInCounter = (SimpleStatisticProvider) statsMgr.getStatisticProvider( 
            StatisticsManager.DROPEDMSG_IN_PROVIDER );
        totalInMsgCounter = (SimpleStatisticProvider) statsMgr.getStatisticProvider( 
            StatisticsManager.TOTALMSG_IN_PROVIDER );
        uptimeStatsProvider = statsMgr.getStatisticProvider(
            StatisticsManager.DAILY_UPTIME_PROVIDER );
    }
    
//  Messy auto-detecting of generic type... causes to much trouble with 
//  MessageSubscribers that are capable to handle multiple Message types.
//    @SuppressWarnings("unchecked")
//    public <T extends Message> void addMessageSubscriber( MessageSubscriber<T> subscriber )
//    {        
//        Class<? extends MessageSubscriber> clazz = subscriber.getClass();
//        Type[] genInterfaces = clazz.getGenericInterfaces();
//        for ( int i = 0; i < genInterfaces.length; i++ )
//        {
//            if ( !(genInterfaces[i] instanceof ParameterizedType) )
//            {
//                continue;
//            }
//            ParameterizedType parType = (ParameterizedType) genInterfaces[i];
//            if ( !parType.getRawType().equals( MessageSubscriber.class ) )
//            {
//                continue;
//            }
//            Type[] typeArgs = parType.getActualTypeArguments();
//            if ( typeArgs.length != 1 )
//            {
//                throw new IllegalArgumentException( 
//                    "MessageSubscriber has invalid number of generic type arguments: " + typeArgs.length );
//            }
//            Class<T> messageClass = (Class<T>) typeArgs[0];
//            if ( !Message.class.isAssignableFrom( messageClass ) )
//            {
//                throw new IllegalArgumentException( 
//                    "MessageSubscriber generic type is not a Message: " + messageClass );
//            }
//            
//            addMessageSubscriber( messageClass, subscriber );
//        }
//    }
        
    public <T extends Message> void addMessageSubscriber( Class<T> clazz, MessageSubscriber<T> subscriber )
    {
        //logger.debug( "Adding MessageSubscriber {} for type {}.", subscriber, clazz );
        MessageSubscriber<T> registeredSubscriber = (MessageSubscriber<T>)messageSubscribers.get( clazz );
        if ( registeredSubscriber == null )
        {
            messageSubscribers.put( clazz, subscriber );
        }
        else if ( registeredSubscriber instanceof MessageSubscriberList )
        {
            ((MessageSubscriberList<T>)registeredSubscriber).addSubscriber( subscriber );
        }
        else
        {
            MessageSubscriberList<T> list = new MessageSubscriberList<T>( 
                registeredSubscriber, subscriber );
            messageSubscribers.put( clazz, list );
        }
    }
    
    public <T extends Message> void removeMessageSubscriber( Class<T> clazz, MessageSubscriber<T> subscriber )
    {
        //logger.debug( "Removing MessageSubscriber {} for type {}.", subscriber, clazz );
        MessageSubscriber<T> registeredSubscriber = (MessageSubscriber<T>)messageSubscribers.get( clazz );

        if ( registeredSubscriber instanceof MessageSubscriberList )
        {
            ((MessageSubscriberList<T>)registeredSubscriber).removeSubscriber( subscriber );
        }
        else
        { 
            // either we don't have a subscriber or this is the only registered one
            messageSubscribers.remove( clazz );
        }
    }
    
    @SuppressWarnings("unchecked")
    private void dispatchToSubscribers( Message message, Host sourceHost )
        throws InvalidMessageException
    {
        MessageSubscriber messageSubscriber = messageSubscribers.get( 
            message.getClass() );
        messageSubscriber.onMessage(message, sourceHost);
    }
    
    public <T extends Message> void addUdpMessageSubscriber( Class<? extends Message> clazz, UdpMessageSubscriber<T> subscriber )
    {
        //logger.debug( "Adding MessageSubscriber {} for type {}.", subscriber, clazz );
        UdpMessageSubscriber<T> registeredSubscriber = (UdpMessageSubscriber<T>)udpMessageSubscribers.get( clazz );
        if ( registeredSubscriber == null )
        {
            udpMessageSubscribers.put( clazz, subscriber );
        }
        else if ( registeredSubscriber instanceof UdpMessageSubscriberList )
        {
            ((UdpMessageSubscriberList<T>)registeredSubscriber).addSubscriber( subscriber );
        }
        else
        {
            UdpMessageSubscriberList<T> list = new UdpMessageSubscriberList<T>( 
                registeredSubscriber, subscriber );
            udpMessageSubscribers.put( clazz, list );
        }
    }
    
    public <T extends Message> void removeUdpMessageSubscriber( Class<T> clazz, UdpMessageSubscriber<T> subscriber )
    {
        //logger.debug( "Removing MessageSubscriber {} for type {}.", subscriber, clazz );
        UdpMessageSubscriber<T> registeredSubscriber = (UdpMessageSubscriber<T>)udpMessageSubscribers.get( clazz );

        if ( registeredSubscriber instanceof UdpMessageSubscriberList )
        {
            ((UdpMessageSubscriberList<T>)registeredSubscriber).removeSubscriber( subscriber );
        }
        else
        { 
            // either we don't have a subscriber or this is the only registered one
            udpMessageSubscribers.remove( clazz );
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void dispatchToUdpSubscribers( Message message, DestAddress sourceAddress )
        throws InvalidMessageException
    {
        UdpMessageSubscriber messageSubscriber = udpMessageSubscribers.get( 
            message.getClass() );
        messageSubscriber.onUdpMessage( message, sourceAddress );
    }
    
    public void handlePing( PingMsg pingMsg, Host sourceHost )
    {
//        if ( //logger.isDebugEnabled( ) )
//        {
//            //logger.debug( "Received Ping: " + pingMsg.toString() + " - " 
//            //    + pingMsg.getHeader().toString());
//        }

        // count ping statistic
        pingMsgInCounter.increment( 1 );

        MsgHeader header = pingMsg.getHeader();

        // Bearshare seems to massively send pings with ttl 6 hops 1 and empty 
        // GUID. We currently drop them all since they are duplicates...
        // if ( header.getMsgID().equals(GUID.EMPTY_GUID) )
        
        // See if I have seen this Ping before.  Drop msg if duplicate.
        if ( !msgRouting.checkAndAddToPingRoutingTable( header.getMsgID(),
                sourceHost ) )
        {
            dropMessage( pingMsg, "Dropping already seen ping", sourceHost );
            return;
        }
        
        respondToPing( pingMsg, sourceHost );
    }
    
    /**
     * Respond to ping using PongCache
     * @param pingMsg
     * @param sourceHost
     */
    private void respondToPing( PingMsg pingMsg, Host sourceHost )
    {
        MsgHeader header = pingMsg.getHeader();
        // to reduce the incoming connection attempts of other clients
        // only response to ping a when we have free incoming slots or this
        // ping has a original TTL ( current TTL + hops ) of 2.
        byte ttl = header.getTTL();
        byte hops = header.getHopsTaken();
        if ( ( ttl + hops > 2 ) && !hostMgr.areIncommingSlotsAdvertised() )
        {
            return;
        }

        // For crawler pings (hops==1, ttl=1) we have a special treatment...
        // We reply with all our leaf connections... in case we have them as a
        // ultrapeer...
        if ( hops == 1 && ttl == 1)
        {// crawler ping
            // respond with leaf nodes pongs, already "hoped" one step. (ttl=1,hops=1)
            Host[] leafs = hostMgr.getNetworkHostsContainer().getLeafConnections();
            for ( int i = 0; i < leafs.length; i++ )
            {
                DestAddress ha = leafs[i].getHostAddress();
                PongMsg pong = pongFactory.createOtherLeafsOutgoingPong( header.getMsgID(),
                    (byte)1, (byte)1, ha );
                sourceHost.queueMessageToSend( pong );                
            }
        }

        // send back my own pong
        byte newTTL = hops++;
        if ( ( hops + ttl ) <= 2)
        {
            newTTL = 1;
        }
        
        int avgDailyUptime = ((Integer)uptimeStatsProvider.getValue()).intValue();
        int shareFileCount = sharedFilesService.getFileCount();
        int shareFileSize = sharedFilesService.getTotalFileSizeInKb();
        
        // Get my host:port for InitResponse.
        PongMsg pong = pongFactory.createMyOutgoingPong( header.getMsgID(), 
            servent.getLocalAddress(), newTTL, shareFileCount, shareFileSize,
            servent.isUltrapeer(), avgDailyUptime, sourceHost.isGgepSupported() );
        sourceHost.queueMessageToSend( pong );
        
        // send pongs from pong cache
        DestAddress orginAddress = sourceHost.getHostAddress();
        IpAddress ip = orginAddress.getIpAddress();
        if ( ip == null )
        {
            return;
        }
        GUID guid = header.getMsgID();
        List<PongMsg> pongs = servent.getMessageService().getCachedPongs();
        for( PongMsg pMsg : pongs )
        {
            if( ip.equals( pMsg.getPongAddress().getIpAddress() ) )
            {
                continue;
            }
            PongMsg pongCpy = pongFactory.createFromCachePong(
                guid, newTTL, pMsg, sourceHost.isGgepSupported() );
            sourceHost.queueMessageToSend( pongCpy );
        }
    }
    
    /**
     * 
     */
    public void handlePong( PongMsg msg, Host sourceHost )
    {
//        if ( //logger.isDebugEnabled( ) )
//        {
//            //logger.debug( "Received Pong: " + msg.getDebugString() 
//                + " - " + msg.getHeader().toString());
//        }

        // count pong statistic
        pongMsgInCounter.increment( 1 );

        DestAddress pongAddress = msg.getPongAddress();
        AccessType access = securityService.controlHostAddressAccess( pongAddress );
        if ( access == AccessType.ACCESS_STRONGLY_DENIED )
        {
            // drop message
            dropMessage( msg, "IP access strongly denied.", sourceHost );
            return;
        }
        
        try
        {
            dispatchToSubscribers( msg, sourceHost );
        }
        catch ( InvalidMessageException exp )
        {// drop invalid message
            dropMessage( msg, exp.getMessage(), sourceHost);
            return;
        }

        // add address to host catcher...
        if ( access == AccessType.ACCESS_GRANTED )
        {
            boolean isNew = hostMgr.catchHosts( msg );
            if ( isNew )
            {
                servent.getMessageService().addPongToCache( msg );
            }
        }

        byte hopsTaken = msg.getHeader().getHopsTaken();
        
        // check if this is the response to my Ping message
        if ( hopsTaken == 1 )
        {
            DestAddress connectedAddress = sourceHost.getHostAddress();
            IpAddress connectedIP = connectedAddress.getIpAddress();
            if ( connectedIP.equals( pongAddress.getIpAddress() ) )
            {
                sourceHost.setFileCount( msg.getFileCount() );
                sourceHost.setTotalFileSize( msg.getFileSizeInKB() );
                // I guess that a hops == 1 with equal ip address is a pong from my
                // direct neighbor, therefore I also update the obviously wrong port.
                int connectedPort = connectedAddress.getPort();
                int pongPort = pongAddress.getPort();
                if ( connectedPort != pongPort )
                {
                    sourceHost.setHostAddress( new DefaultDestAddress( 
                        connectedAddress.getIpAddress(), pongPort ) );
                }
            }
        }
        
        if ( msg.getHeader().getTTL() > 0 )
        {
            msgRouting.routePongMessage( msg );
        }
    }
    
    public void handleQuery( QueryMsg msg, Host sourceHost )
    {
//        if ( //logger.isDebugEnabled( ) )
//        {
//            //logger.debug( "Received Query: " + msg.toString() + " - " 
//                + msg.getHeader().toString());
//        }
        
        // count query statistic
        queryMsgInCounter.increment( 1 );

        MsgHeader header = msg.getHeader();

        // See if I have seen this Query before. Drop msg if duplicate.
        // This drop is done even though this could be an extension of a 
        // probe query. Only Limewire is doing this extension of a probe
        // query currently and as stated by themselves the efficiency of it
        // is doubtful. 
        if ( !msgRouting.checkAndAddToQueryRoutingTable( header.getMsgID(),
                sourceHost ) )
        {
            dropMessage( msg, "Drop already seen query", sourceHost );
            return;
        }
        
        // a leaf is not supposed to forward me queries not coming from itself.
        if ( sourceHost.isUltrapeerLeafConnection() && header.getHopsTaken() > 2 )
        {
            dropMessage( msg, "Drop query from leaf with hops > 2.", sourceHost );
        }
        
        if ( MessagePrefs.DropIndexQueries.get().booleanValue() && 
             QueryResultSearchEngine.INDEX_QUERY_STRING.equals( msg.getSearchString() ) )
        {
            dropMessage( msg, "Drop index query.", sourceHost );
        }

        // logging a msg can be very expensive!
        //mRemoteHost.log( //logger.FINEST, "Received Msg: " + msg + " Hex: " +
        //    HexConverter.toHexString( body ) + " Data: " + new String( body) );

        try
        {
            dispatchToSubscribers( msg, sourceHost );
        }
        catch ( InvalidMessageException exp )
        {// drop invalid message
            dropMessage( msg, exp.getMessage(), sourceHost);
            return;
        }
        
        // Search the shared file database and get groups of shared files.
        List<ShareFile> resultFiles = sharedFilesService.handleQuery( msg );
        if ( resultFiles == null || resultFiles.size() == 0)
        {
            return;
        }
        respondToQuery( header, resultFiles, sourceHost );
    }

    private void respondToQuery( MsgHeader header, List<ShareFile> resultFiles, 
        Host sourceHost )
    {
        // Construct QueryResponse msg.  Copy the original Init's GUID.
        // TTL expansion on query hits doesn't matter very much so it doesn't
        // hurt us to give query hits a TTL boost.
        // Bearshare sets QueryHit TTL to 10
        // gtk-gnutella sets QueryHit TTL to (hops + 5)
        MsgHeader newHeader = new MsgHeader( header.getMsgID(),
            MsgHeader.QUERY_HIT_PAYLOAD,
            // Will take as many hops to get back.
            // hops + 1 decided in gdf 2002-12-04
            (byte)(header.getHopsTaken() + 1),
            //(byte)(Math.min( 10, header.getHopsTaken() + 5 ) ),
            (byte)0, 0 );

        int resultCount = resultFiles.size();
        if ( resultCount > 255 )
        {
            resultFiles.subList( 0, 255 );
            resultCount = resultFiles.size();
        }
        assert resultCount < 255;
        
        QueryResponseRecord[] records = new QueryResponseRecord[ resultCount ];
        QueryResponseRecord record;
        int recPos = 0;
        for( ShareFile shareFile : resultFiles )
        {
            record = QueryResponseRecord.createFromShareFile( shareFile,
                servent.getLocalAddress() );
            records[ recPos ] = record;
            recPos ++;
        }

        DestAddress hostAddress = servent.getLocalAddress();
        GUID serventGuid = servent.getServentGuid();
        
        QueryResponseMsg response = new QueryResponseMsg(
            newHeader, serventGuid, hostAddress,
            Math.round( BandwidthPrefs.MaxUploadBandwidth.get().floatValue() / NumberFormatUtils.ONE_KB ),
            records, hostMgr.getNetworkHostsContainer().getPushProxies(),
            !servent.isFirewalled(), servent.isUploadLimitReached() );

        sourceHost.queueMessageToSend( response );
    }
    
    /**
     * Be aware that QueryResponseMsg from BrowseHostConnections are also handle
     * through here. MessageSubscribers should honor this accordingly.
     * @param queryResponseMsg the QueryResponseMsg to handle
     * @param sourceHost the source host, can be null in case of BrowseHostConnection 
     *        results.
     */
    public void handleQueryResponse( QueryResponseMsg queryResponseMsg, Host sourceHost )
    {
        // Logging is expensive...
//        if ( //logger.isLevelLogged( //logger.FINEST ) )
//        {
//            //logger.logMessage( //logger.FINEST, //logger.NETWORK,
//                connectedHost, "Received QueryResponse: " + queryResponseMsg + " - " +
//                queryResponseMsg.toString() );
//        }
        
        // count query hit statistic
        queryHitMsgInCounter.increment( 1 );
        
        MsgHeader header = queryResponseMsg.getHeader();
        
        // validate remote client id
        GUID respServentId = queryResponseMsg.getRemoteServentID();
        if ( respServentId.equals( servent.getServentGuid() ) )
        {
            dropMessage( queryResponseMsg, "My query response should never reach me.", sourceHost);
            return;
        }
        if ( respServentId.equals( header.getMsgID() ) )
        {
            dropMessage( queryResponseMsg, "Message id equals servent id.", sourceHost);
            return;
        }
        if ( respServentId.equals( GUID.EMPTY_GUID ) )
        {
            dropMessage( queryResponseMsg, "Servent id is empty.", sourceHost);
            return;
        }

        DestAddress queryAddress = queryResponseMsg.getDestAddress();
        AccessType access = securityService.controlHostAddressAccess( queryAddress );
        if ( access == AccessType.ACCESS_STRONGLY_DENIED )
        {
            // drop message
            dropMessage( queryResponseMsg, "IP access strongly denied.", sourceHost );
            return;
        }

        
        if ( access == AccessType.ACCESS_GRANTED )
        {
            try
            {
                // MessageSubscribers should honor that QueryResponseMsg from 
                // BrowseHostConnections are also going through here.
                dispatchToSubscribers( queryResponseMsg, sourceHost );
            }
            catch ( InvalidMessageException exp )
            {// drop invalid message
                dropMessage(queryResponseMsg, exp.getMessage(), sourceHost);
                return;
            }
        }
        
        // byte hopsTaken = header.getHopsTaken();
        // check if this is from a direct neighbor
        // if ( hopsTaken == 1 )
        // {
        // GUID sourceServentId = sourceHost.getServentId();
        // if ( sourceServentId == null )
        // {// learn neighbor serventId from QueryResponseMsg.
        // sourceHost.setServentId( respServentId );
        // }
        // else if ( !sourceServentId.equals( respServentId ) )
        // {// host responds with different serventId then before..
        // // drop servent..
        // sourceHost.setStatus( HostStatus.ERROR, "Switching servent id: " +
        // sourceServentId.toHexString() + " - " + respServentId );
        // }
        //            // these logics can be improved.. see GTKG search.c update_neighbour_info()
        //        }
        
        if ( header.getTTL() > 0 )
        {
            try
            {
                msgRouting.routeQueryResponse( queryResponseMsg, sourceHost );
            }
            catch ( InvalidMessageException exp )
            {// drop invalid message
                dropMessage(queryResponseMsg, exp.getMessage(), sourceHost);
                return;
            }
        }
    }
    
    public void handleRouteTableUpdate( RouteTableUpdateMsg message, Host sourceHost )
    {
        // no specific stats so count to total
        totalInMsgCounter.increment( 1 );
        if ( !(sourceHost.isQueryRoutingSupported() ||
                sourceHost.isUPQueryRoutingSupported()) )
        {
            dropMessage( message, "QRP not supported from host.", sourceHost);
            return;
        }

        QueryRoutingTable qrTable = sourceHost.getLastReceivedRoutingTable();
        if ( qrTable == null )
        {
            // create new table... TODO3 maybe makes not much sense because we might
            // recreate table. maybe there is a way to initialize the QRT lazy
            qrTable = new QueryRoutingTable();
            sourceHost.setLastReceivedRoutingTable( qrTable );
        }
        try
        {
            qrTable.updateRouteTable( message, sourceHost );
            if ( sourceHost.isUltrapeerLeafConnection() )
            {// in case this is a leaf connection, we need to update our
             // local query routing table. This needs to be done since
             // have our leaves QRT aggregated our QRT and are checking
             // during a query against our QRT if leaves might have a hit.
                servent.getMessageService().triggerQueryRoutingTableUpdate();
            }
        }
        catch ( InvalidMessageException exp )
        {// drop message
            dropMessage( message, "Invalid QRT update message.", sourceHost );
        }
    }
    
    public void handleVendorMessage( VendorMsg vendorMsg, Host sourceHost )
    {
//        if ( //logger.isDebugEnabled( ) )
//        {
//            //logger.debug( "Received VendorMsg: " + vendorMsg.toString() 
//                + " - " + vendorMsg.getHeader().toString());
//        }
        
        if ( vendorMsg instanceof MessagesSupportedVMsg )
        {
            handleMessagesSupportedVMsg( (MessagesSupportedVMsg)vendorMsg, sourceHost );
        }
        else if ( vendorMsg instanceof TCPConnectBackVMsg )
        {
            handleTCPConnectBackVMsg( (TCPConnectBackVMsg)vendorMsg, sourceHost );
        }
        else if ( vendorMsg instanceof TCPConnectBackRedirectVMsg )
        {
            handleTCPConnectBackRedirectVMsg((TCPConnectBackRedirectVMsg)vendorMsg, sourceHost );
        }
        else if ( vendorMsg instanceof PushProxyRequestVMsg )
        {
            handlePushProxyRequestVMsg( (PushProxyRequestVMsg)vendorMsg, sourceHost );
        }
        else if ( vendorMsg instanceof PushProxyAcknowledgementVMsg )
        {
            handlePushProxyAcknowledgementVMsg( (PushProxyAcknowledgementVMsg)vendorMsg, sourceHost );
        }
        else if ( vendorMsg instanceof HopsFlowVMsg )
        {
            handleHopsFlowVMsg( (HopsFlowVMsg)vendorMsg, sourceHost );
        }
        else if ( vendorMsg instanceof CapabilitiesVMsg )
        {
            handleCapabilitiesVMsg( (CapabilitiesVMsg)vendorMsg, sourceHost );
        }
    }
    
    private void handleMessagesSupportedVMsg(MessagesSupportedVMsg msg, Host sourceHost )
    {
        sourceHost.setSupportedVMsgs( msg );
        
        // if push proxy is supported request it..
        boolean isFirewalled = servent.isFirewalled();
        // if we are a leave or are firewalled and connected to a ultrapeer
        // and the connection supports push proxy.
        if ( ( sourceHost.isLeafUltrapeerConnection() ||
             ( isFirewalled && sourceHost.isUltrapeer() ) ) 
          && sourceHost.isPushProxySupported() )
        {
            PushProxyRequestVMsg pprmsg = new PushProxyRequestVMsg(
                servent.getServentGuid() );
            // TODO2 remove this once Limewire support PPR v2
            if ( sourceHost.getVendor() != null &&
                    sourceHost.getVendor().indexOf( "LimeWire" ) != -1 )
            {
                pprmsg.setVersion( 1 );
            }
            sourceHost.queueMessageToSend( pprmsg );
        }
        if ( isFirewalled && 
             servent.getMessageService().isTCPRedirectAllowed() &&
             sourceHost.isTCPConnectBackSupported()  )
        {
            DestAddress localAddress = servent.getLocalAddress();
            VendorMsg tcpConnectBack = new TCPConnectBackVMsg( localAddress.getPort() );
            sourceHost.queueMessageToSend( tcpConnectBack );
            servent.getMessageService().incNumberOfTCPRedirectsSent();
        }
    }
    
    private void handleCapabilitiesVMsg(CapabilitiesVMsg msg, Host sourceHost )
    {
        sourceHost.setCapabilitiesVMsgs( msg );
    }

    /**
     * @param msg
     */
    private void handleTCPConnectBackVMsg(TCPConnectBackVMsg msg, Host sourceHost )
    {
        int port = msg.getPort();
        DestAddress address = sourceHost.getHostAddress();
        if ( address.getPort() != port )
        {
            address = new DefaultDestAddress( address.getHostName(), port );
        }
        VendorMsg redirectMsg = new TCPConnectBackRedirectVMsg( address );
        
        Host[] hosts = hostMgr.getNetworkHostsContainer().getUltrapeerConnections();
        int sentCount = 0;
        for ( int i = 0; sentCount <= 5 && i < hosts.length; i++ )
        {
            if ( sourceHost == hosts[i] )
            {
                // skip sending redirect to my host.
                continue;
            }
            if ( hosts[i].isTCPConnectBackRedirectSupported() )
            {
                hosts[i].queueMessageToSend( redirectMsg );
                sentCount ++;
            }
        }
    }
    
    private void handleTCPConnectBackRedirectVMsg( TCPConnectBackRedirectVMsg msg, Host sourceHost )
    {
        final DestAddress address = msg.getAddress();
        Runnable connectBackRunner = new Runnable()
        {
            public void run()
            {
                Connection connection = null;
                try
                {
                    DestAddress connectBackAddress = new DefaultDestAddress( address.getHostName(),
                        address.getPort() );
                    connection = ConnectionFactory.createConnection( 
                        connectBackAddress, 2000, servent.getBandwidthService().getNetworkBandwidthController() );
                    connection.write( ByteBuffer.wrap( StringUtils.getBytesInUsAscii( "\n\n" ) ) );
                    connection.flush();
                }
                catch ( IOException exp )
                { // failed.. don't care..
                }
                finally
                {
                    if (connection != null)
                    {
                        connection.disconnect();
                    }
                }
            }
        };
        Environment.getInstance().executeOnThreadPool( connectBackRunner, "TCPConnectBackJob");
    }
    
    private void handlePushProxyRequestVMsg( PushProxyRequestVMsg pprvmsg, Host sourceHost )
    {
        if ( !sourceHost.isUltrapeerLeafConnection() ) 
        {
            return;
        }
        DestAddress localAddress = servent.getLocalAddress();
        // PP only works if we have a valid IP to use in the PPAck message.
        if( localAddress.getIpAddress() == null )
        {
            //logger.warn( "Local address has no IP to use for PPAck." );
            return;
        }
        GUID requestGUID = pprvmsg.getHeader().getMsgID();        
        PushProxyAcknowledgementVMsg ppavmsg = 
            new PushProxyAcknowledgementVMsg( localAddress,
            requestGUID );
        sourceHost.queueMessageToSend( ppavmsg );
        
        msgRouting.addToPushRoutingTable( requestGUID,
                sourceHost );            
    }
    
    private void handlePushProxyAcknowledgementVMsg( PushProxyAcknowledgementVMsg ppavmsg, Host sourceHost )
    {
        // the candidate is able to be a push proxy if the ack contains my guid.
        if ( servent.getServentGuid().equals( ppavmsg.getHeader().getMsgID() ) )
        {
            sourceHost.setPushProxyAddress( ppavmsg.getHostAddress() );
        }
    }
    
    private void handleHopsFlowVMsg( HopsFlowVMsg hopsFlowVMsg, Host sourceHost )
    {
        byte hopsFlowValue = hopsFlowVMsg.getHopsValue();
        sourceHost.setHopsFlowLimit(hopsFlowValue);
    }
    
    public void handlePushRequest( PushRequestMsg msg, Host sourceHost )
    {
        // count push statistic
        pushMsgInCounter.increment( 1 );

        // logging a msg can be very expensive! the toString() calls are bad
        //mRemoteHost.log( //logger.FINEST, "Received Msg: " + msg);

        AccessType access = securityService.controlHostAddressAccess(
            msg.getRequestAddress() );
        if ( access == AccessType.ACCESS_STRONGLY_DENIED )
        {
            // drop message
            dropMessage( msg, "IP access strongly denied.", sourceHost );
            return;
        }

        if ( servent.getServentGuid().equals(msg.getClientGUID() ) )
        {
            if ( access == AccessType.ACCESS_GRANTED )
            {
                new PushWorker( msg, servent.getUploadService() );
            }
            return;
        }
        
        if ( msg.getHeader().getTTL() > 0 )
        {
            servent.getMessageService().routePushMessage( msg );
        }
    }

    private void dropMessage( Message msg, String reason, Host sourceHost )
    {
        //logger.info( "Dropping message: {} from: {}", reason, sourceHost );
//        if ( //logger.isDebugEnabled( ) )
//        {
//            //logger.debug( "Header: [" + msg.getHeader().toString() + "] - Message: [" +
//                msg.toString() + "].");
//        }
        if ( sourceHost != null )
        {
          sourceHost.incReceivedDropCount();
        }
        dropedMsgInCounter.increment( 1 );
    }
    
    public void dropMessage( MsgHeader header, byte[] body, String reason, Host sourceHost )
    {
        //logger.info( "Dropping message: {} from: {}", reason, sourceHost );
//        if ( //logger.isDebugEnabled( ) )
//        {
//            //logger.debug( "Header: [" + header.toString() + "] - Body: [" +
//                HexConverter.toHexString( body, 0, header.getDataLength() ) + "]." );
//        }
        if ( sourceHost != null )
        {
          sourceHost.incReceivedDropCount();
        }
        dropedMsgInCounter.increment( 1 );
    }
}