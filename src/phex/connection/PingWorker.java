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
package phex.connection;

import phex.common.ThreadTracking;
import phex.host.Host;
import phex.servent.Servent;

/**
 * This class is responsible to ping the neighborhood and find new hosts for the
 * PongCache and for the CaughtHostsContainer.
 */
public class PingWorker implements Runnable
{
    private static final int SLEEP_TIME = 5000;
    private final Servent servent;
    
    public PingWorker( Servent servent )
    {
        this.servent = servent;
    }
    
    public void start()
    {
        Thread thread = new Thread( ThreadTracking.rootThreadGroup, this,
            "PingWorker-" + Integer.toHexString( hashCode() ) );
        thread.setPriority( Thread.NORM_PRIORITY );
        thread.setDaemon( true );
        thread.start();
    }
    
    public void run()
    {
        
        while( true )
        {
            // Sleep some time...
            try
            {
                Thread.sleep( SLEEP_TIME );
            }
            catch (InterruptedException e)
            {
            }
            
            if( servent.isUltrapeer() )
            {
                Host[] hosts = servent.getHostService().getUltrapeerConnections();
                // TODO only forward to a selected amount (75%) of node if there are more
                // then 4-5 node.
                servent.getMessageService().pingHosts( (byte)3, hosts );
            }
        }
    }
}