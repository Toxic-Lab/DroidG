/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2007 Phex Development Group
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

import phex.common.AddressCounter;
import phex.common.Phex;
import phex.common.address.DestAddress;
import phex.common.bandwidth.BandwidthController;
//import phex.common.log.NLogger;
import phex.event.PhexEventTopics;
import phex.net.connection.Connection;
import phex.prefs.core.NetworkPrefs;
import phex.servent.Servent;

public class ChatService
{
    private final Servent servent;
    
    /**
     * A Set containing all 
     */
    private final AddressCounter addressCounter;
    
    public ChatService( Servent servent )
    {
        this.servent = servent;
        addressCounter = new AddressCounter( 1, false );
    }
    
    public BandwidthController getChatBandwidthController()
    {
        // chat will use bandwidth from network. 
        return servent.getBandwidthService().getNetworkBandwidthController();
    }

    /**
     * Opens a new chat connection to start a instant message chat.
     */
    public void openChat( DestAddress hostAddress )
    {
        // initialize the chat engine that reads and send the chat data
        // for a new HostAddress.
        ChatEngine chatEngine = new ChatEngine( this, hostAddress );
        chatEngine.startChat();
        fireChatConnectionOpened( chatEngine );
    }
    
    /**
     * Called from ChatEngine.stopChat() to notify of a closed chat session.
     * @param chatEngine
     */
    public void chatClosed( ChatEngine chatEngine )
    {
        addressCounter.relaseAddress( chatEngine.getHostAddress() );
        fireChatConnectionFailed( chatEngine );
    }

    /**
     * Accepts a connection to start a instant message chat.
     */
    public void acceptChat( Connection connection )
    {
        if ( !NetworkPrefs.AllowChatConnection.get().booleanValue() )
        {
            connection.disconnect();
            return;
        }
        
        DestAddress hostAddress = connection.getSocket().getRemoteAddress();
        
        // validate if we already have a chat connection with this host..
        if ( !addressCounter.validateAndCountAddress( hostAddress ) )
        {
            // we dont like to have more then one chat connection to a single
            // host...
            connection.disconnect();
            return;
        }

        // initialize the chat engine that reads and send the chat data
        // over the connected socket.
        try
        {
            ChatEngine chatEngine = new ChatEngine( this, connection );
            chatEngine.startChat();
            fireChatConnectionOpened( chatEngine );
        }
        catch ( IOException exp )
        {
           // NLogger.debug( ChatService.class, exp, exp );
            connection.disconnect();
            return;
        }
    }

    ///////////////////// START event handling methods ////////////////////////

    /**
     * Fires if a new chat connection was opened.
     */
    public void fireChatConnectionOpened( final ChatEngine chatEngine )
    {
        Phex.getEventService().publish( PhexEventTopics.Chat_Update,
            new ChatEvent( ChatEvent.Type.OPENED, chatEngine, null ) );
    }

    /**
     * Fires a event if a chat connection was failed to opened or a opened chat
     * connection was closed.
     */
    public void fireChatConnectionFailed( final ChatEngine chatEngine )
    {
        Phex.getEventService().publish( PhexEventTopics.Chat_Update,
            new ChatEvent( ChatEvent.Type.FAILED, chatEngine, null ) );
    }
    ///////////////////// END event handling methods ////////////////////////
}
