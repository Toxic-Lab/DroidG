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
package phex.chat;

import java.io.IOException;
import java.util.Locale;

import phex.common.Environment;
import phex.common.Phex;
import phex.common.address.DestAddress;
//import phex.common.log.NLogger;
import phex.event.PhexEventService;
import phex.event.PhexEventTopics;
import phex.io.buffer.ByteBuffer;
import phex.net.connection.Connection;
import phex.net.connection.ConnectionFactory;
import phex.prefs.core.NetworkPrefs;
import phex.utils.GnutellaInputStream;

/**
 *
 */
public final class ChatEngine
{
    private final ChatService chatService;
    private final boolean isOutgoingConnection;
    
    private GnutellaInputStream chatReader;
    private Connection connection;
    private String chatNick;

    private boolean useEncodedStr;
    
    /**
     * The host address of the chat connection.
     */
    private final DestAddress hostAddress;

    /**
     * For incoming chat requests
     * 
     * @throws IOException if an IO error occures.
     */
    ChatEngine( ChatService chatService, Connection connection )
        throws IOException
    {
        this.useEncodedStr = true;
        this.chatService = chatService;
        this.connection = connection;
        hostAddress = connection.getSocket().getRemoteAddress();
        chatReader = connection.getInputStream();

        chatNick = hostAddress.getFullHostName();
        finalizeHandshake();
        isOutgoingConnection = false;
    }

    /**
     * For outgoing chat requests. We need to connect to the host address first.
     */
    ChatEngine( ChatService chatService, DestAddress aHostAddress )
    {
        this.useEncodedStr = true;
        this.chatService = chatService;
        hostAddress = aHostAddress;
        isOutgoingConnection = true;
        chatNick = hostAddress.getFullHostName();
    }

    public void startChat()
    {
        ChatReadWorker worker = new ChatReadWorker( Phex.getEventService() );
        Environment.getInstance().executeOnThreadPool( worker,
            "ChatReadWorker-" + Integer.toHexString(worker.hashCode()));
    }

    public void stopChat()
    {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
        chatService.chatClosed( this );
    }

    public boolean isConnected()
    {
        return connection != null;
    }

    /**
     * Returns the host address the engine is connected to
     */
    public DestAddress getHostAddress()
    {
        return hostAddress;
    }

    public String getChatNick()
    {
        return chatNick;
    }

    /**
     * Sends a chat message to the connected servent
     * @param message the message to send
     */
    public void sendChatMessage( String message )
    {
        if ( connection == null )
        {
            chatService.fireChatConnectionFailed( ChatEngine.this );
        }
        try
        {
            if (useEncodedStr)
            {
                byte[] base64Str = org.bouncycastle.util.encoders.Base64.encode(message.getBytes());
                connection.write( ByteBuffer.wrap(  (base64Str + "\n").getBytes() ) );
            }
            else
            {
                connection.write( ByteBuffer.wrap(  (message + "\n").getBytes() ) );                
            }
        }
        catch ( IOException exp )
        {
            //NLogger.warn( ChatEngine.class, exp, exp );
            stopChat();
        }
    }

    private void finalizeHandshake()
        throws IOException
    {
        connection.getSocket().setSoTimeout( NetworkPrefs.TcpRWTimeout.get().intValue() );

        // read the header that have been left in the stream after accepting the
        // connection.
        String line;
        String upLine;
        boolean foundPhexEncoded = false;
        do
        {
            line = chatReader.readLine();
            //NLogger.debug( ChatEngine.class, "Read Chat header: " + line );
            if ( line == null )
            {
                throw new IOException( "No handshake response from chat partner." );
            }
            upLine = line.toUpperCase( Locale.US );
            if ( upLine.startsWith( "X-NICKNAME:" ) )
            {
                chatNick = line.substring(11).trim();
               // NLogger.debug( ChatEngine.class, "Chat Nick: " + chatNick );
            }
            if ( upLine.startsWith("X-PHEX-ENCODED:") )
            {
                foundPhexEncoded = true;
                if ( upLine.equals("X-PHEX-ENCODED: TRUE") )
                {
                    useEncodedStr = true;
                }
                else
                {
                    useEncodedStr = false;
                }
            }
        }
        while ( line.length() > 0 );

        if (!foundPhexEncoded)
        {
            useEncodedStr = false;
        }
        
        // we respond with "CHAT/0.1 200 OK\r\n\r\n" to finish the handshake.
        //NLogger.debug( ChatEngine.class, "Sending: CHAT/0.1 200 OK");
        if (foundPhexEncoded)
        {
            connection.write( ByteBuffer.wrap( ("CHAT/0.1 200 OK\r\n" +
                "User-Agent: " + Phex.getFullPhexVendor() + "\r\n" +
                //"X-Nickname: " + StrUtil.getAppNameVersion() + "\r\n" +
                "X-Phex-Encoded: true" + "\r\n" +
                "\r\n").getBytes() ) );
        }
        else
        {
            connection.write( ByteBuffer.wrap( ("CHAT/0.1 200 OK\r\n" +
                "User-Agent: " + Phex.getFullPhexVendor() + "\r\n" +
                //"X-Nickname: " + StrUtil.getAppNameVersion() + "\r\n" +
                "\r\n").getBytes() ) );
        }

        // assume we read the final "CHAT/0.1 200 OK" followed by possible headers.
        do
        {
            line = chatReader.readLine();
            //NLogger.debug( ChatEngine.class, "Read Chat response: " + line );
            if ( line == null )
            {
                throw new IOException( "No handshake response from chat partner." );
            }
        }
        while ( line.length() > 0 );

        connection.getSocket().setSoTimeout( 0 );
        // chat connection open notification will be fired through the chat manager
    }

    private void connectOutgoingChat()
        throws IOException
    {
        boolean foundPhexEncoded = false;
        
        //NLogger.debug( ChatEngine.class, "Connect outgoing to: " + hostAddress );

        connection = ConnectionFactory.createConnection( hostAddress,
            chatService.getChatBandwidthController() );
        
        // initialize the chat connection handshake
        // First send "CHAT CONNECT/0.1\r\n" and header data
        // only header currently is user agent.
        String message = "CHAT CONNECT/0.1\r\n" +
            "User-Agent: " + Phex.getFullPhexVendor() + "\r\n" +
            //"X-Nickname: " + StrUtil.getAppNameVersion() + "\r\n" +
            "X-Phex-Encoded: true" + "\r\n" +
            "\r\n";
       // NLogger.debug( ChatEngine.class, "Sending: " + message );
        connection.write( ByteBuffer.wrap( message.getBytes() ) );

        chatReader = connection.getInputStream();

        // assume we read "CHAT/0.1 200 OK" and header data
        // TODO: check string and status, don't check for version since it might
        //       change only for "CHAT" and "200"
        String line;
        String upLine;
        do
        {
            line = chatReader.readLine();
            //NLogger.debug( ChatEngine.class, "Read Chat header: " + line );
            if ( line == null )
            {
                throw new IOException( "No handshake response from chat partner." );
            }
            upLine = line.toUpperCase( Locale.US );
            if ( upLine.startsWith( "X-NICKNAME:" ) )
            {
                chatNick = line.substring(11).trim();
            }
            if ( upLine.startsWith("X-PHEX-ENCODED:") )
            {
                foundPhexEncoded = true;
                if ( upLine.equals("X-PHEX-ENCODED: TRUE") )
                {
                    useEncodedStr = true;
                }
                else
                {
                    useEncodedStr = false;
                }
            }
        }
        while ( line.length() > 0 );

        if (!foundPhexEncoded)
        {
            useEncodedStr = false;
        }
        
        // we respond with "CHAT/0.1 200 OK\r\n\r\n" to finish the handshake.
        connection.write( ByteBuffer.wrap( ("CHAT/0.1 200 OK\r\n" +
            "User-Agent: " + Phex.getFullPhexVendor() + "\r\n\r\n" ).getBytes() ) );
        connection.getSocket().setSoTimeout( 0 );
        // chat connection open notification will be fired through the chat manager
    }

    /**
     * Hide the reading thread from the engine implementation.
     */
    private class ChatReadWorker implements Runnable
    {
        private PhexEventService eventService;
        
        ChatReadWorker( PhexEventService eventService )
        {
            this.eventService = eventService;
        }
        
        public void run()
        {
            if ( isOutgoingConnection )
            {
                try
                {
                    connectOutgoingChat();
                }
                catch ( IOException exp )
                {
                    stopChat();
                    return;
                }
            }
            String str;
            while ( true )
            {
                try
                {
                    str = chatReader.readLine();
                    if ( str == null )
                    {
                        throw new IOException( "Remote host diconnected chat." );
                    }
                    if ( str.length() == 0 )
                    {
                        continue;
                    }
                   // NLogger.debug( ChatEngine.class, "Reading chat message: " + str );

                    if (useEncodedStr)
                    {
                        byte[] base64Buf = org.bouncycastle.util.encoders.Base64.decode(str);
                        str = new String(base64Buf);
                    }
                    eventService.publish( PhexEventTopics.Chat_Update,
                        new ChatEvent( ChatEvent.Type.MSG_REC, ChatEngine.this, str ) );
                }
                catch ( IOException exp )
                {
                   // NLogger.debug( ChatEngine.class, exp, exp);
                    stopChat();
                    break;
                }
            }
        }
    }
}