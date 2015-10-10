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
package phex.host;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import phex.common.Environment;
import phex.common.Phex;
import phex.common.QueryRoutingTable;
import phex.common.address.DestAddress;
import phex.connection.ConnectionClosedException;
import phex.connection.MessageQueue;
import phex.event.PhexEventTopics;
import phex.io.buffer.ByteBuffer;
import phex.msg.GUID;
import phex.msg.Message;
import phex.msg.QueryMsg;
import phex.msg.QueryResponseMsg;
import phex.msg.vendor.CapabilitiesVMsg;
import phex.msg.vendor.MessagesSupportedVMsg;
import phex.net.connection.Connection;
import phex.prefs.core.SecurityPrefs;
import phex.query.DynamicQueryConstants;
import phex.utils.GnutellaInputStream;
import phex.utils.GnutellaOutputStream;
import phex.utils.Localizer;

/**
 * <p>A Gnutella host, or servent together with operating statistics and IO.</p>
 */
public class Host
{
    private static final double MESSAGE_QUALITY_WEIGHT = 0.9;
    private static final int SENT_WINDOW_LENGTH = 10000; // 10 seconds
    private static final double SENT_QUALITY_THRESHOLD = 0.7;

    //private static final Logger logger = LoggerFactory.getLogger( Host.class );
    
    public enum Type
    {
        OUTGOING,
        INCOMING,
        LOCAL
    }
    
    private static final int MAX_SEND_QUEUE = 400;

    private static final int DROP_PACKAGE_RATIO = 70;

    /**
     * The time after which a query routing table can be updated in milliseconds.
     */
    private static final long QUERY_ROUTING_UPDATE_TIME = 5 * 60 * 1000; // 5 minutes

    /**
     * The time after which a connection is stable.
     */
    private static final int STABLE_CONNECTION_TIME = 60 * 1000; // 60 seconds

    /**
     * Normal connection type.
     */
    public static final byte CONNECTION_NORMAL = 0;

    /**
     * Connection type as me as a leaf and the host as a ultrapeer.
     */
    public static final byte CONNECTION_LEAF_UP = 1;

    /**
     * Connection type from ultrapeer to ultrapeer.
     */
    public static final byte CONNECTION_UP_UP = 2;

    /**
     * Connection type representing me as a ultrapeer and the host as a leaf.
     */
    public static final byte CONNECTION_UP_LEAF = 3;

    private DestAddress hostAddress;

    private Connection connection;

    private HostStatus status;

    private String lastStatusMsg = "";

    private long statusTime = 0;

    private Type type;

    
    /**
     * Total number of messages received from this host,
     * including dropped.
     */
    private volatile int receivedMsgCount;
    
    /**
     * Number of received messages that where dropped.
     */
    private volatile int receivedDropMsgCount;
    
    /**
     * Current received message quality.
     */
    private volatile double receivedQuality = 1.0;

    
    /**
     * Number of messages send to this host, 
     * excluding queued and dropped messages.
     */
    private volatile int sentMsgCount;
    
    /**
     * Number of messages that where dropped.
     */
    private volatile int sentDropMsgCount;
    
    /**
     * Number of messages dropped inside the current sent window.
     */
    private volatile int sentWindowDropMsgCount;
    
    /**
     * Number of messages send inside the current sent window.
     */
    private volatile int sentWindowGoodMsgCount;
    
    /**
     * Start timestamp of the current sent window.
     */
    private volatile long sentWindowStartTime;
    
    /**
     * Current sent message quality.
     */
    private volatile double sentQuality = 1.0;
    

    private long fileCount = -1;

    private long shareSize = -1;

    private String vendor;

    private boolean vendorChecked = false;
    
    private boolean isPhexVendor;
    
    /**
     * The servent id we found from {@link QueryResponseMsg}.
     */
    private GUID serventId;

    /**
     * The maxTTL this connection accepts for dynamic queries.
     * It is provide through the handshake header X-Max-TTL and used
     * for the dynamic query proposal.
     * This header indicates that we should not send fresh queries to
     * this connection with TTLs higher than the X-Max-TTL. If we are
     * routing traffic from other Ultrapeers, the X-Max-TTL is irrelevant.
     * The X-Max-TTL MUST NOT exceed 4, as any TTL above 4 indicates a client
     * is allowing too much query traffic on the network. This header is
     * particularly useful for compatibility with future clients that may
     * choose to have higher degrees but that would prefer lower TTL traffic
     * from their neighbors. For example, if future clients connect to
     * 200 Ultrapeers, they could use the X-Max-TTL header to indicate to
     * today's clients that they will not accept TTLs above 2. A typical
     * initial value for X-Max-TTL is 3.
     */
    private byte maxTTL;
    
    /**
     * The max hops value to use for queries coming from a hops flow vendor
     * message.
     */
    private byte hopsFlowLimit;

    /**
     * The intra ultrapeer connection this connection holds.
     * It is provide through the handshake header X-Degree and used
     * for the dynamic query proposal.
     * The X-Degree header simply indicates the number of Ultrapeer
     * connections this nodes attempts to maintain. Clients supporting
     * the dynamic query proposal must have X-Degrees of at least 15, 
     * and higher values are preferable.
     */
    private int ultrapeerDegree;

    /**
     * Defines if the host supports QRP. This is only important for Ultrapeer
     * connections.
     */
    private boolean isQueryRoutingSupported;

    /**
     * Defines if the host supports Ultrapeer QRP. This is only important for
     * Ultrapeer connections.
     */
    private boolean isUPQueryRoutingSupported;

    /**
     * Defines if the host supports dynamic query.
     */
    private boolean isDynamicQuerySupported;

    /**
     * Marks the last time the local query routing table was sent to this
     * host. Only for leaf-ultrapeers connection a query routing table is send.
     * @see #isQRTableUpdateRequired()
     */
    private long lastQRTableSentTime;

    /**
     * The QR table that was last sent to this host on lastQRTableSentTime. This
     * table is needed to send patch updates to the host.
     */
    private QueryRoutingTable lastSentQRTable;

    /**
     * The QR table that was last received from this host. This
     * table is needed to determine which querys to send to this host.
     */
    private QueryRoutingTable lastReceivedQRTable;

    /**
     * Defines the connection type we have with this host. Possible values are
     * CONNECTION_NORMAL
     * CONNECTION_LEAF_ULTRAPEER
     * PEER_LEAF
     */
    private byte connectionType;

    /**
     * A SACHRIFC message queue implementation.
     */
    private MessageQueue messageQueue;
    private SendEngine sendEngine;
    
    private boolean isVendorMessageSupported;
    private boolean isGgepSupported;
    
    private MessagesSupportedVMsg supportedVMsgs;
    private CapabilitiesVMsg capabilitiesVMsgs;    
    
    /**
     * The PushProxy host address of this host. It is received from a
     * PushProxyAcknowledgement vendor message.
     */
    private DestAddress pushProxyAddress;

    /**
     * Create a new Host with type OUTGOING.
     */
    private Host()
    {
        connection = null;
        status = HostStatus.NOT_CONNECTED;
        type = Type.OUTGOING;
        connectionType = CONNECTION_NORMAL;
        isQueryRoutingSupported = false;
        isUPQueryRoutingSupported = false;
        isDynamicQuerySupported = false;
        isVendorMessageSupported = false;
        isGgepSupported = false;
        receivedMsgCount = 0;
        sentMsgCount = 0;
        sentDropMsgCount = 0;
        receivedDropMsgCount = 0;
        maxTTL = DynamicQueryConstants.DEFAULT_MAX_TTL;
        hopsFlowLimit = -1;
    }

    /**
     * <p>Create a new Host for a HostAddress that will default type
     * OUTGOING.</p>
     *
     * @param address  the HostAddress this Host will communicate with
     */
    public Host(DestAddress address)
    {
        this();
        hostAddress = address;
    }

    public Host(DestAddress address, Connection connection)
    {
        this();
        setHostAddress( address );
        setConnection( connection );
    }
    
    public void setHostAddress( DestAddress hostAddress )
    {
        this.hostAddress = hostAddress;
    }

    public DestAddress getHostAddress()
    {
        return hostAddress;
    }

    public void setConnection(Connection connection)
    {
        this.connection = connection;
        receivedMsgCount = 0;
        sentMsgCount = 0;
        sentDropMsgCount = 0;
        receivedDropMsgCount = 0;
    }

    /**
     * @return Returns the connection.
     */
    public Connection getConnection()
    {
        return connection;
    }
    
    /**
     * @deprecated
     */
    @Deprecated
    public GnutellaInputStream getInputStream() throws IOException
    {
        if ( connection == null ) { throw new ConnectionClosedException(
            "Connection already closed"); }
        return connection.getInputStream();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public GnutellaOutputStream getOutputStream() throws IOException
    {
        if ( connection == null ) { throw new ConnectionClosedException(
            "Connection already closed"); }
        return connection.getOutputStream();
    }

    public void activateInputInflation() throws IOException
    {
        getInputStream().activateInputInflation();
    }

    public void activateOutputDeflation() throws IOException
    {
        getOutputStream().activateOutputDeflation();
    }

    public void setVendor(String aVendor)
    {
        vendor = aVendor;
        isPhexVendor = Phex.isPhexVendor( vendor );
    }

    public String getVendor()
    {
        return vendor;
    }
    
    public boolean isPhex()
    {
        return isPhexVendor;
    }

    /**
     * @return the serventId
     */
    public GUID getServentId()
    {
        return serventId;
    }

    /**
     * @param serventId the serventId to set
     */
    public void setServentId(GUID serventId)
    {
        this.serventId = serventId;
    }

    public HostStatus getStatus()
    {
        return status;
    }
    
    public String getLastStatusMsg()
    {
        return lastStatusMsg;
    }

    public void setStatus( HostStatus status)
    {
        setStatus(status, null, System.currentTimeMillis());
    }

    public void setStatus( HostStatus status, long statusTime)
    {
        setStatus(status, null, statusTime);
    }

    public void setStatus( HostStatus status, String msg)
    {
        setStatus(status, msg, System.currentTimeMillis());
    }

    public void setStatus( HostStatus status, String msg, long statusTime)
    {
        if ( this.status == status && lastStatusMsg != null
            && lastStatusMsg.equals(msg) ) { return; }
        this.status = status;
        lastStatusMsg = msg;
        this.statusTime = statusTime;
    }

    /**
     * Checks if a connection status is stable. A stable connection
     * is a connection that last over STABLE_CONNECTION_TIME seconds.
     */
    public boolean isConnectionStable()
    {
        return status == HostStatus.CONNECTED
            && getConnectionUpTime( ) > STABLE_CONNECTION_TIME;
    }

    /**
     * Returns the number of millis the connection is up.
     */
    public long getConnectionUpTime( )
    {
        if ( status == HostStatus.CONNECTED )
        {
            return System.currentTimeMillis() - statusTime;
        }
        else
        {
            return 0;
        }
    }

    public boolean isErrorStatusExpired( long currentTime, long expiryDelay )
    {
        if ( status == HostStatus.ERROR || status == HostStatus.DISCONNECTED )
        {
            if ( currentTime - statusTime > expiryDelay ) { return true; }
        }
        return false;
    }

    public Type getType()
    {
        return type;
    }

    public void setType( Type aType )
    {
        this.type = aType;
    }

    public boolean isIncomming()
    {
        return type.equals( Type.INCOMING );
    }
    
    public void setVendorMessageSupported( boolean state )
    {
        this.isVendorMessageSupported = state;
    }
    
    public boolean isVendorMessageSupported()
    {
        return isVendorMessageSupported;
    }
    
    public void setGgepSupported( boolean state )
    {
        this.isGgepSupported = state;
    }
    
    public boolean isGgepSupported()
    {
        return isGgepSupported;
    }
    
    public void setCapabilitiesVMsgs( CapabilitiesVMsg capabilitiesVMsgs )
    {
        this.capabilitiesVMsgs = capabilitiesVMsgs;
    }
    
    public boolean isFeatureSearchSupported()
    {
        return capabilitiesVMsgs != null && capabilitiesVMsgs.isFeatureSearchSupported();
    }
    
    public void setSupportedVMsgs( MessagesSupportedVMsg supportedVMsgs )
    {
        this.supportedVMsgs = supportedVMsgs;
    }
    
    public boolean isTCPConnectBackSupported()
    {
        return supportedVMsgs != null && supportedVMsgs.isTCPConnectBackSupported();
    }
    
    public boolean isTCPConnectBackRedirectSupported()
    {
        return supportedVMsgs != null && supportedVMsgs.isTCPConnectBackRedirectSupported();
    }
    
    public boolean isPushProxySupported()
    {
        return supportedVMsgs != null && supportedVMsgs.isPushProxySupported();
    }
    
    public boolean isHopsFlowSupported()
    {
        return supportedVMsgs != null && supportedVMsgs.isHopsFlowSupported();
    }
    
    /**
     * Returns the PushProxy host address of this host if received 
     * from a PushProxyAcknowledgement vendor message. Null otherwise.
     */
    public DestAddress getPushProxyAddress()
    {
        return pushProxyAddress;
    }
    
    /**
     * Sets the PushProxy host address of this host. It must be received 
     * from a PushProxyAcknowledgement vendor message.
     */
    public void setPushProxyAddress( DestAddress address )
    {
        pushProxyAddress = address;
    }

    public int getReceivedCount()
    {
        return receivedMsgCount;
    }
    
    public void incReceivedCount()
    {
        receivedMsgCount++;
        calcReceivedQuality( false );
    }

    public int getReceivedDropCount()
    {
        return receivedDropMsgCount;
    }
    
    public void incReceivedDropCount()
    {
        receivedDropMsgCount++;
        calcReceivedQuality( true );
    }
    
    public double getReceivedQuality()
    {
        return receivedQuality;
    }
    
    public int getSentCount()
    {
        return sentMsgCount;
    }
    
    public void incSentCount()
    {
        sentMsgCount++;
        calcSentQuality( false, 1 );
    }
    
    public int getSentDropCount()
    {
        return sentDropMsgCount;
    }
    
    public void incSentDropCount( int val )
    {
        if ( val > 0 )
        {// ensure no quality calculation in case nothing was dropped.
            sentDropMsgCount += val;
            calcSentQuality( true, val );
        }
    }
    
    public double getSentQuality()
    {
        return sentQuality;
    }
    
    private void calcSentQuality( boolean isDrop, int count )
    {
        if ( isDrop )
        {
            sentWindowDropMsgCount += count;
        }
        else
        {
            sentWindowGoodMsgCount += count;
        }
        long now = System.currentTimeMillis();
        if ( now - sentWindowStartTime > SENT_WINDOW_LENGTH )
        {
            int msgCount = sentWindowGoodMsgCount + sentWindowDropMsgCount;
            double val = 1.0 - (sentWindowDropMsgCount / Math.max( msgCount, 1.0 ));
            sentQuality = sentQuality * MESSAGE_QUALITY_WEIGHT + val 
                * ( 1.0 - MESSAGE_QUALITY_WEIGHT );
            sentWindowGoodMsgCount = sentWindowDropMsgCount = 0;
            sentWindowStartTime = now;
            
            if ( sentQuality < SENT_QUALITY_THRESHOLD && isConnectionStable() )
            {
                setStatus( HostStatus.ERROR, Localizer.getString( "TooManyDroppedPackets" ), now );
                disconnect();
            }
        }
    }
    
    private void calcReceivedQuality( boolean isDrop )
    {
        int val = isDrop ? 0 : 1;
        receivedQuality = receivedQuality * MESSAGE_QUALITY_WEIGHT + val 
            * ( 1.0 - MESSAGE_QUALITY_WEIGHT );
    }

    public long getFileCount()
    {
        return fileCount;
    }

    public void setFileCount(long fileCount)
    {
        this.fileCount = fileCount;
    }

    /**
     * Returns total size in kBytes.
     */
    public long getTotalSize()
    {
        return shareSize;
    }

    public void setTotalFileSize(long shareSize)
    {
        this.shareSize = shareSize;
    }

    /**
     * Returns the maxTTL this connection accepts.
     * @return the maxTTL this connection accepts.
     * @see #maxTTL
     */
    public byte getMaxTTL()
    {
        return maxTTL;
    }

    /**
     * Sets the maxTTL this connection accepts.
     * @param maxTTL the new maxTTL.
     * @see #maxTTL
     */
    public void setMaxTTL(byte maxTTL)
    {
        this.maxTTL = maxTTL;
    }
    
    /**
     * Returns the max hops value to use for queries comming from a hops flow vendor
     * message, or -1 if not set.
     **/
    public byte getHopsFlowLimit()
    {
        return hopsFlowLimit;
    }
    
    /**
     * Sets the max hops value to use for queries coming from a hops flow vendor
     * message, or -1 to reset.
     * @param hopsFlowLimit
     */
    public void setHopsFlowLimit(byte hopsFlowLimit)
    {
        this.hopsFlowLimit = hopsFlowLimit;
    }
    
    /**
     * Returns the ultrapeer connection degree.
     * @return the ultrapeer connection degree.
     * @see #ultrapeerDegree
     */
    public int getUltrapeerDegree()
    {
        return ultrapeerDegree;
    }

    /**
     * Sets the ultrapeer connection degree of this connection.
     * @param degree the new ultrapeer connection degree.
     * @see #ultrapeerDegree
     */
    public void setUltrapeerDegree(int degree)
    {
        ultrapeerDegree = degree;
    }

    public boolean isConnected()
    {
        return connection != null;
    }

    public void disconnect()
    {
        if ( connection != null )
        {
            if ( status != HostStatus.ERROR )
            {
                setStatus( HostStatus.DISCONNECTED );
            }
            connection.disconnect();
            connection = null;
        }
        Phex.getEventService().publish( PhexEventTopics.Host_Disconnect, this );
    }

    public int getSendQueueLength()
    {
        if ( messageQueue == null )
        {
            return 0;
        }
        else
        {
            return messageQueue.getQueuedMessageCount();
        }
    }

    public boolean isSendQueueTooLong()
    {
        if ( messageQueue == null ) { return false; }
        return (messageQueue.getQueuedMessageCount() >= MAX_SEND_QUEUE - 1);
    }

    public boolean isSendQueueInRed()
    {
        if ( messageQueue == null ) { return false; }
        return (messageQueue.getQueuedMessageCount() >= MAX_SEND_QUEUE * 3 / 4);
    }

    public boolean isNoVendorDisconnectApplying()
    {
        if ( !SecurityPrefs.DisconnectNoVendorHosts.get().booleanValue() )
        {
            return false;
        }
            
        // Already checked?  Short-circuit out (no need to recalculate len & delta-time)
        // Possible issue if user toggles the config setting while connected to
        // an unwanted host--it will not disconnect because it already passed the test
        if ( vendorChecked ) { return false; }
        // The vendor string might not be there immediately because of
        // handshaking, but will certainly be there when the status is HOST_CONNECTED.
        if ( status != HostStatus.CONNECTED ) { return false; }

        String normalizedVendorString = this.vendor;
        if ( normalizedVendorString == null )
        {
            normalizedVendorString = "";
        }
        else
        {
            normalizedVendorString = normalizedVendorString.trim();
        }

        if ( normalizedVendorString.length() == 0 )
        {
            return true;
        }
        else
        {
            vendorChecked = true;
            return false;
        }
    }

    public boolean isFreeloader(long currentTime)
    {
// freeloaders are no real problem...       
//        // never count a ultrapeer as freeloader...
//        if ( isUltrapeer() ) { return false; }
//        long timeDelta = getConnectionUpTime(currentTime);
//        // We can only really tell after initial handshaing is complete, 10
//        // seconds should be a good delay.
//        if ( timeDelta >= DISCONNECT_POLICY_THRESHOLD )
//        {
//            if ( ServiceManager.sCfg.freeloaderFiles > 0
//                && fileCount < ServiceManager.sCfg.freeloaderFiles ) { return true; }
//            if ( ServiceManager.sCfg.freeloaderShareSize > 0
//                && (shareSize / 1024) < ServiceManager.sCfg.freeloaderShareSize ) { return true; }
//        }
        return false;
    }

    /**
     * Indicates that this is a ultrapeer and I am a leaf. The
     * connection type in this case is CONNECTION_LEAF_UP.
     * @return true if this is a ultrapeer and I am a leaf, false otherwise.
     */
    public boolean isLeafUltrapeerConnection()
    {
        return connectionType == CONNECTION_LEAF_UP;
    }

    /**
     * Indicates that this is a ultrapeer in general without paying attention to
     * my relationship to this ultrapeer. The connection type in this case can
     * be CONNECTION_LEAF_UP or CONNECTION_UP_UP.
     * @return true if this is a ultrapeer, false otherwise.
     */
    public boolean isUltrapeer()
    {
        return connectionType == CONNECTION_LEAF_UP
            || connectionType == CONNECTION_UP_UP;
    }

    /**
     * Indicates that this is a leaf and I am its ultrapeer. The
     * connection type in this case is CONNECTION_UP_LEAF.
     * @return true if this is a leaf and I am its ultrapeer, false otherwise.
     */
    public boolean isUltrapeerLeafConnection()
    {
        return connectionType == CONNECTION_UP_LEAF;
    }

    /**
     * Sets the connection type of the host. The connection can be of type:
     * CONNECTION_NORMAL
     * CONNECTION_LEAF_UP
     * CONNECTION_UP_UP
     * CONNECTION_UP_LEAF
     * @param connectionType the connection type of the host.
     */
    public void setConnectionType(byte connectionType)
    {
        this.connectionType = connectionType;
    }

    @Override
    public String toString()
    {
        return "Host" + "[" + hostAddress.getHostName() + ":"
            + hostAddress.getPort() + "," + vendor + ",State=" + status + "]";
    }

    ////////////////////////START MessageQueue implementation///////////////////

    /**
     * Sends a message over the output stream but is not flushing the output
     * stream. This needs to be done by the caller.
     * @param message the message to send
     * @throws IOException when a send error occurs
     */
    public void sendMessage( Message message ) throws IOException
    {
//        if ( logger.isDebugEnabled( ) )
//        {
//            logger.debug( "Sending message: " + message + " - " 
//                + message.getHeader().toString());
//        }
        ByteBuffer headerBuf = message.createHeaderBuffer();
        ByteBuffer messageBuf = message.createMessageBuffer();
        if ( !isConnected() )
        {
            throw new ConnectionClosedException(
                "Connection is already closed");
        }
        connection.write( headerBuf );
        if ( !isConnected() )
        {
            throw new ConnectionClosedException(
                "Connection is already closed");
        }
        connection.write( messageBuf );
        incSentCount();
//        if ( logger.isDebugEnabled( ) )
//        {
//            logger.debug( "Message send: " + message + " - " + message.getHeader().toString());
//        }
    }

    public void flushOutputStream() throws IOException
    {
        if ( isConnected() )
        {
            connection.flush();
            //Logger.logMessage( Logger.FINEST, Logger.NETWORK,
            //    "Messages flushed" );
        }
    }

    public void queueMessageToSend(Message message)
    {
        // before queuing a query check hops flow limit...
        if (    hopsFlowLimit > -1
             && message instanceof QueryMsg
             && message.getHeader().getHopsTaken() >= hopsFlowLimit )
        {// don't send query!
            return;
        }
        
        //logger.debug( "Queuing message: {}", message );
        initMessageQueue();
        synchronized (messageQueue)
        {
            messageQueue.addMessage(message);
            sendEngine.dispatch();
        }
    }
    
    private class SendEngine implements Runnable
    {
        private AtomicBoolean isRunning = new AtomicBoolean(false);
        
        public void dispatch( )
        {
            boolean result = isRunning.compareAndSet( false, true );
            if ( result )
            {
                String jobName = "SendEngine-" + Integer.toHexString(sendEngine.hashCode());
                Environment.getInstance().executeOnThreadPool( sendEngine,
                    jobName);
            }
        }
        
        public void run()
        {
            do
            {
                try
                {
                    messageQueue.sendQueuedMessages();
                }
                catch (IOException exp)
                {
                    if ( isConnected() )
                    {
                        setStatus( HostStatus.ERROR, exp.getMessage() );
                        disconnect();
                    }
                }
            }
            while( checkForRepeat() );
        }

        private boolean checkForRepeat()
        {
            synchronized (messageQueue)
            {
                if ( isConnected() && messageQueue.getQueuedMessageCount() > 0 )
                {
                    return true;
                }
                else
                {
                    boolean result = isRunning.compareAndSet( true, false );
                    if ( !result )
                    {
                        throw new RuntimeException( "Invalid state." );
                    }
                    return false;
                }
            }
        }
    }

    /**
     * This method is here to make sure the message queue is only generated
     * when it is actually necessary. Generating it in the constructor
     * uses up a big amount of memory for every known Host. Together with the
     * message queue its SendEngine is initialized.
     */
    private void initMessageQueue()
    {
        if ( messageQueue != null ) { return; }
        // Create a queue with max-size, dropping the oldest msg when max reached.
        messageQueue = new MessageQueue(this);
        sendEngine = new SendEngine();
    }

    ////////////////////////END MessageQueue implementation/////////////////////

    ////////////////////////START QRP implementation////////////////////////////

    public boolean isQRTableUpdateRequired()
    {
        return System.currentTimeMillis() > lastQRTableSentTime
            + QUERY_ROUTING_UPDATE_TIME;
    }

    public QueryRoutingTable getLastSentRoutingTable()
    {
        return lastSentQRTable;
    }

    public void setLastSentRoutingTable(QueryRoutingTable routingTable)
    {
        lastSentQRTable = routingTable;
        lastQRTableSentTime = System.currentTimeMillis();
    }

    public QueryRoutingTable getLastReceivedRoutingTable()
    {
        return lastReceivedQRTable;
    }

    public void setLastReceivedRoutingTable(QueryRoutingTable routingTable)
    {
        lastReceivedQRTable = routingTable;
    }

    public boolean isQueryRoutingSupported()
    {
        return isQueryRoutingSupported;
    }

    public void setQueryRoutingSupported(boolean state)
    {
        isQueryRoutingSupported = state;
    }

    public boolean isUPQueryRoutingSupported()
    {
        return isUPQueryRoutingSupported;
    }

    public void setUPQueryRoutingSupported(boolean state)
    {
        isUPQueryRoutingSupported = state;
    }

    /**
     * Returns if the host supports dynamic query.
     * @return true if dynamic query is supported, false otherwise.
     */
    public boolean isDynamicQuerySupported()
    {
        return isDynamicQuerySupported;
    }

    /**
     * Sets if the hosts supports dynamic query.
     * @param state true if dynamic query is supported, false otherwise.
     */
    public void setDynamicQuerySupported(boolean state)
    {
        isDynamicQuerySupported = state;
    }

    //////////////////////////END QRP implementation////////////////////////////

    public static final LocalHost LOCAL_HOST;
    static
    {
        LOCAL_HOST = new LocalHost();
    }

    public static class LocalHost extends Host
    {
        LocalHost()
        {
            super();
        }
        
        @Override
        public void setHostAddress( DestAddress hostAddress )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public DestAddress getHostAddress()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isConnected()
        {
            // return true to suite routing table..
            return true;
        }

        @Override
        public Type getType()
        {
            return Type.LOCAL;
        }
        
        @Override
        public String toString()
        {
            return "LOCAL_HOST";
        }
    }
}
