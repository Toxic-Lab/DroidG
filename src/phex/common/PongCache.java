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
package phex.common;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import phex.common.collections.PriorityQueue;
import phex.msg.PongMsg;
import phex.servent.Servent;

/**
 * A simple cache of pongs to reduce network bandwidth.
 */
public class PongCache
{    
    private static final int PONGS_PER_HOP = 1;
    private static final int MAX_HOPS = 6;
    
    private static final int EXPIRE_TIME_MILLIS = 60000;
    
    private final PriorityQueue pongQueue;
    
    private final Servent servent;
    
    public PongCache( Servent servent )
    {
        this.servent = servent;
        int[] capacities = new int[MAX_HOPS];
        Arrays.fill(capacities, PONGS_PER_HOP);
        pongQueue = new PriorityQueue( capacities );
    }

    public List<PongMsg> getPongs( )
    {
        synchronized( pongQueue )
        { 
            List<PongMsg> pongList = new LinkedList<PongMsg>();
            List<PongMsg> removeList = null;
            
            long now = System.currentTimeMillis();
            Iterator<PongMsg> iterator = pongQueue.iterator();
            while( iterator.hasNext() )
            {
                PongMsg pong = iterator.next();
                if( now - pong.getCreationTime() > EXPIRE_TIME_MILLIS )
                {
                    if(removeList == null) 
                    {
                        removeList = new LinkedList<PongMsg>();
                    }
                    removeList.add( pong );
                }
                else
                {
                    pongList.add( pong );
                }
            }
            removePongs( removeList );
            return pongList;
        }
    }

    private void removePongs( List<PongMsg> pongList )
    {
        if ( pongList == null )
        {
            return;
        }
        Iterator<PongMsg> iterator = pongList.iterator();
        while(iterator.hasNext()) 
        {
            PongMsg pong = iterator.next();
            pongQueue.removeFromAll( pong );
        }
    }                             

    public void addPong( PongMsg pong )
    {
        // we only store ultrapeer pongs
        if( !pong.isUltrapeerMarked() )
        {
            return;      
        }
        
        // no caching needed if we are no ultrapeer
        if( !servent.isUltrapeer() )
        {
            return;
        }

        // reduce by already counted hop..
        int hops = pong.getHeader().getHopsTaken()-1;
        
        // ignore high hops
        if( hops >= MAX_HOPS )   
        {
            return;
        }

        synchronized( pongQueue )
        {
            pongQueue.addToHead( pong, hops );
        }
    }
}