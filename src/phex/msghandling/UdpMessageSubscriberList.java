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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import phex.common.address.DestAddress;
import phex.msg.InvalidMessageException;
import phex.msg.Message;

public class UdpMessageSubscriberList<E extends Message> implements UdpMessageSubscriber<E>
{
    private List<UdpMessageSubscriber<E>> subscriberList;
    
    UdpMessageSubscriberList()
    {
        subscriberList = new CopyOnWriteArrayList<UdpMessageSubscriber<E>>( );
    }
    
    UdpMessageSubscriberList( UdpMessageSubscriber<E> subscriber1,
        UdpMessageSubscriber<E> subscriber2 )
    {
        this( );
        addSubscribers( subscriber1, subscriber2 );
    }

    public void onUdpMessage(E message, DestAddress sourceAddress) 
        throws InvalidMessageException
    {
        for ( UdpMessageSubscriber<E> messageSubscriber : subscriberList )
        {
            messageSubscriber.onUdpMessage(message, sourceAddress );
        }
    }
    
    public void addSubscriber( UdpMessageSubscriber<E> subscriber )
    {
        subscriberList.add( subscriber );
    }
    
    public void addSubscribers( UdpMessageSubscriber<E>... subscriberArr )
    {
        subscriberList.addAll( Arrays.asList( subscriberArr ) );
    }
    
    public void removeSubscriber( UdpMessageSubscriber<E> subscriber )
    {
        subscriberList.remove( subscriber );
    }
}