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
package phex.msg;

import java.util.Collection;

import phex.bootstrap.UdpHostCacheContainer;
import phex.common.PhexVersion;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.host.CaughtHost;
import phex.host.CaughtHostsContainer;
import phex.host.Host;
import phex.host.NetworkHostsContainer;
import phex.security.PhexSecurityManager;
import phex.utils.IOUtil;

public class PongFactory
{
    private static final int ULTRAPEER_MAJOR_VERSION_NUMBER = 0;
    private static final int ULTRAPEER_MINOR_VERSION_NUMBER = 1;
    
    //private static final Logger logger = LoggerFactory.getLogger( PongFactory.class );
    /**
     * Vendor code GGEP extension in GUESS format
     */
    private static final byte[] GGEP_VENDOR_CODE = new byte[5];
    static
    {
        // add vendor code 'PHEX'
        GGEP_VENDOR_CODE[ 0 ] = (byte) 0x50;
        GGEP_VENDOR_CODE[ 1 ] = (byte) 0x48;
        GGEP_VENDOR_CODE[ 2 ] = (byte) 0x45;
        GGEP_VENDOR_CODE[ 3 ] = (byte) 0x58;
        GGEP_VENDOR_CODE[4] = IOUtil.serializeGUESSVersionFormat(
            PhexVersion.getMajorVersion(), PhexVersion.getMinorVersion() );
    }
    
    private final PhexSecurityManager securityService;
    
    private final NetworkHostsContainer netHostsContainer;
    
    private final CaughtHostsContainer caughtHostsContainer;
    
    private final UdpHostCacheContainer uhcContainer;
    
    public PongFactory( NetworkHostsContainer netHostsContainer, 
        CaughtHostsContainer caughtHostsContainer, PhexSecurityManager securityService )
    {
        this( netHostsContainer, caughtHostsContainer, null, securityService );
    }
    
    public PongFactory( NetworkHostsContainer netHostsContainer, 
        CaughtHostsContainer caughtHostsContainer, UdpHostCacheContainer uhcContainer,
        PhexSecurityManager securityService )
    {
        if ( securityService == null )
        {
            throw new NullPointerException( "securityService is null");
        }
        this.securityService = securityService;
        
        if ( netHostsContainer == null )
        {
            throw new NullPointerException( "netHostsContainer is null");
        }
        this.netHostsContainer = netHostsContainer;
        
        if ( caughtHostsContainer == null )
        {
            throw new NullPointerException( "caughtHostsContainer is null");
        }
        this.caughtHostsContainer = caughtHostsContainer;
        
        // uhcContainer can be null
        this.uhcContainer = uhcContainer;
    }
    
    /**
     * Creates a udp pong message from a given byte array.
     * 
     * @param bytesMsg the binary message.
     * @param fromHost the host the message was received from.
     * @return the created {@link PongMsg}.
     * @throws InvalidMessageException
     */
    public PongMsg createUdpPongMsg( byte[] bytesMsg, Host fromHost ) 
        throws InvalidMessageException
    {
        MsgHeader msgHdr = MsgHeader.createMsgHeader( bytesMsg, 0 );
        return createUdpPongMsg( msgHdr, bytesMsg, MsgHeader.DATA_LENGTH, fromHost );
    }
    
    public PongMsg createUdpPongMsg( MsgHeader msgHdr, byte[] data, int offset, Host fromHost ) 
        throws InvalidMessageException 
    {
        if( msgHdr.getDataLength() < PongMsg.MIN_PONG_DATA_LEN )
        {
             throw new InvalidMessageException( " Could not create Msg Body while trying to" +
                    " create udp pong Msg"
                    );
        }
        
        byte[] body = MessageProcessor.createBody( msgHdr, data, offset );
        if ( body == null )
        {
            throw new InvalidMessageException( " Could not create Msg Body while trying to" +
                    " create udp pong Msg"
                    );
        }
        
        return new PongMsg( msgHdr, body, securityService );  
    }
    
    
    /**
     * Create a pong response message for a given ping
     * 
     * @param ping the ping to create the pong for.
     * @param localAddress the local address of the host.
     * @param isUdpHostCache indicates if the host is a udp host cache.
     * @param avgDailyUptime the daily uptime of the host.
     * @param sharedFileCount the number of files shared.
     * @param sharedFileSize the total size of the shared files in KB
     * @param isUltrapeer indicates if this pong is coming from an ultrapeer.
     * @return the new PongMsg
     */
    public PongMsg createUdpPongMsg( PingMsg ping, DestAddress localAddress,
        boolean isUdpHostCache, int avgDailyUptime, int sharedFileCount,
        int sharedFileSize, boolean isUltrapeer )
    {        
        GGEPBlock ggepBlock = createMyGGEPBlock(avgDailyUptime, isUltrapeer);
        
        byte[] scpByte = ping.getScpByte();
        if ( scpByte != null )
        {
            Collection<CaughtHost> ipPortPairs = null;
            if ( scpByte.length > 0 && (scpByte[0] & PingMsg.UDP_SCP_MASK) == PingMsg.UDP_SCP_ULTRAPEER )
            {
                ipPortPairs = caughtHostsContainer.getFreeUltrapeerSlotHosts();
            }
            else
            {
                ipPortPairs = caughtHostsContainer.getFreeLeafSlotHosts();
            }
            addUdpPongGGEPExt( localAddress, isUdpHostCache, ipPortPairs, 
                ggepBlock );
        }
        
        
        IpAddress ipAddress = localAddress.getIpAddress();
        if( ipAddress == null )
        {
            throw new IllegalArgumentException( "Can't accept null ip." );
        }
        
        // Construct pingResponse msg.  Copy the original ping's GUID.
        MsgHeader newHeader = new MsgHeader( ping.getHeader().getMsgID(),
                MsgHeader.PONG_PAYLOAD, (byte)1, (byte)0, 0 );
        PongMsg udpPong = new PongMsg( newHeader, localAddress, 
            sharedFileCount, sharedFileSize, isUltrapeer, ggepBlock );
        //logger.info( "Created udp pong in response to ping: {}", udpPong );
        return udpPong;
    }
    
    
    /**
     * @param sharedFileCount the number of files shared
     * @param sharedFileSize the total size of the shared files in KB
     * @return the new PongMsg
     */
    public PongMsg createMyOutgoingPong( GUID msgId, DestAddress localAddress,
        byte ttl, int sharedFileCount, int sharedFileSize, boolean isUltrapeer, 
        int avgDailyUptime, boolean isGgepSupported )
    {
        GGEPBlock ggepBlock = null;
        if ( isGgepSupported )
        {
            ggepBlock = createMyGGEPBlock( avgDailyUptime, isUltrapeer );
        }
        
        IpAddress localIp = localAddress.getIpAddress();
        IpAddress pongIp;
        if ( localIp == null )
        {
            pongIp = IpAddress.UNSET_IP;
            // in case we have a unset ip address we need to use the Phex.EXTDEST
            // GGEP extension to specify our pong destination.
            if ( isGgepSupported )
            {
                addPhexExtendedDestinationGGEP( localAddress, ggepBlock );
            }
        }
        else
        {
            pongIp = new IpAddress( localIp.getHostIP() );
        }
        
        MsgHeader header = new MsgHeader( msgId, MsgHeader.PONG_PAYLOAD,
            ttl, (byte)0, 0 );
        DestAddress pongAddress = new DefaultDestAddress( pongIp, localAddress.getPort() );
        PongMsg pong = new PongMsg( header, pongAddress, sharedFileCount, 
            sharedFileSize, isUltrapeer, ggepBlock );
        return pong;
    }
    
    public PongMsg createOtherLeafsOutgoingPong( GUID msgId, byte ttl, 
        byte hops, DestAddress address )
    {
        MsgHeader header = new MsgHeader( msgId, MsgHeader.PONG_PAYLOAD,
            ttl, hops, 0 );
        GGEPBlock ggepBlock = null;
        
        IpAddress ip = address.getIpAddress();
        IpAddress pongIp;
        if ( ip == null )
        {
            pongIp = IpAddress.UNSET_IP;
            // in case we have a unset ip address we need to use the Phex.EXTDEST
            // GGEP extension to specify our pong destination.
            ggepBlock = new GGEPBlock( false );
            addPhexExtendedDestinationGGEP( address, ggepBlock );
        }
        else
        {
            pongIp = new IpAddress( ip.getHostIP() );
        }
        DestAddress pongAddress = new DefaultDestAddress( pongIp, address.getPort() );
        PongMsg pong = new PongMsg( header, pongAddress, 0, 0, false, ggepBlock );
        return pong;
    }
    
    public PongMsg createFromCachePong( GUID newGuid, byte newTTL, PongMsg pongMsg,
        boolean isGgepSupported )
    {
        MsgHeader header = new MsgHeader( newGuid, MsgHeader.PONG_PAYLOAD,
            newTTL, (byte)0, 0 );
        PongMsg pong = new PongMsg( header, pongMsg.getBody(), securityService );
        if ( !isGgepSupported )
        {
            pong.stripGgepBlock();
        }
        return pong;
    }
    
    private GGEPBlock createMyGGEPBlock( int avgDailyUptime,
        boolean isUltrapeer )
    {
        GGEPBlock ggepBlock = new GGEPBlock( false );
        // add daily avg. uptime.
        if ( avgDailyUptime > 0 )
        {
            ggepBlock.addExtension( GGEPBlock.AVARAGE_DAILY_UPTIME, avgDailyUptime );
        }
        
        // add UP GGEP extension.
        if ( isUltrapeer )
        {
            byte[] upExtension = new byte[3];
            upExtension[0] = IOUtil.serializeGUESSVersionFormat(
                ULTRAPEER_MAJOR_VERSION_NUMBER,
                ULTRAPEER_MINOR_VERSION_NUMBER ); 

            upExtension[1] = (byte) netHostsContainer.getOpenLeafSlotsCount();
            upExtension[2] = (byte) netHostsContainer.getOpenUltrapeerSlotsCount();
            
            ggepBlock.addExtension( GGEPBlock.ULTRAPEER_ID, upExtension );
        }
        
        // add vendor info
        ggepBlock.addExtension( GGEPBlock.VENDOR_CODE_ID, GGEP_VENDOR_CODE );
        return ggepBlock;
    }
    
    private void addUdpPongGGEPExt( DestAddress localAddress, 
        boolean isUdpHostCache, Collection<CaughtHost> ipPortPairs,
        GGEPBlock ggepBlock )
    {
        // add ip port info if asked for
        if( ipPortPairs != null )
        {
            byte[] ipPortData = packIpPortData( ipPortPairs );
            if( ipPortData.length >= 6 )
            {
                ggepBlock.addExtension( GGEPBlock.UDP_HOST_CACHE_IPP, ipPortData );
            }
        }
        
        // if this host is a udp host cache
        if( isUdpHostCache )
        {
            byte[] data;
            
            // check if we have dns name
            if( localAddress.isIpHostName() )
            {
                data = new byte[0];
            }
            else
            {
                data = localAddress.getHostName().getBytes();
            }
            // now add the ggep extension udphc
            ggepBlock.addExtension( GGEPBlock.UDP_HOST_CACHE_UDPHC, data );
            //logger.debug( "UDP HOST CACHE extension added to outgoing pongs" );
        }
        
        // providing a uhcContainer is optional
        if ( uhcContainer != null )
        {
            // if we want Packed Host Caches the data should be added in compressed form
            String packedCacheString = uhcContainer.createPackedHostCaches();
            if( packedCacheString.length() > 0 )
            {
                byte[] data = IOUtil.deflate( packedCacheString.getBytes() );
                ggepBlock.addExtension( GGEPBlock.UDP_HOST_CACHE_PHC, data );
                //logger.debug( "PACKED HOST CACHE extension added to outgoing pongs.");
            }
        }
    }
    
    private static void addPhexExtendedDestinationGGEP( DestAddress address,
        GGEPBlock ggepBlock )
    {
        // TODO1 this is totally experimental and needs to be optimized
        // to use correct byte encoding! It can be used to transfer an destination
        // address info in case there is no IP address used for communication.
        // Like the case in I2P
        ggepBlock.addExtension( GGEPBlock.PHEX_EXTENDED_DESTINATION, 
            address.getHostName().getBytes() );
    }
    
    /**
     * packs ip port data into a data array
     * @param ipPortCollection
     * @return ip port byte array 
     */    
    private static byte[] packIpPortData( Collection<CaughtHost> ipPortCollection )
    {
        final int FIELD_SIZE = 6;
        byte[] data = new byte[ipPortCollection.size() * FIELD_SIZE];
        int offset = 0;
        
        for( CaughtHost host : ipPortCollection ) 
        {
            DestAddress address = host.getHostAddress();
            byte[] addr = address.getIpAddress().getHostIP();
            int port = address.getPort();
            System.arraycopy(addr, 0, data, offset, 4);
            offset += 4;
            IOUtil.serializeShortLE( (short)port, data, offset);
            offset += 2;
        }
        return data;    
    }
}