/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
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
 *  Created on 01.04.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.download;

import java.io.IOException;
import java.net.SocketException;

import phex.common.address.DestAddress;
import phex.common.bandwidth.BandwidthController;
import phex.connection.ConnectionFailedException;
import phex.download.swarming.SWDownloadCandidate;
import phex.download.swarming.SWDownloadCandidate.CandidateStatus;
import phex.net.connection.Connection;
import phex.net.connection.SocketFactory;
import phex.net.repres.SocketFacade;

public class DownloadConnection extends Connection
{
    private SWDownloadCandidate candidate;
    
    public DownloadConnection( SWDownloadCandidate candidate )
    {
        this.candidate = candidate;
    }
    
    public DownloadConnection( SWDownloadCandidate candidate, SocketFacade socket )
    {
        this( candidate );
        this.socket = socket;
        BandwidthController bwCont = candidate.getDownloadFile().getBandwidthController();
        setBandwidthController( bwCont );
        
        candidate.addToCandidateLog( "Connected successfully to " 
            + candidate.getHostAddress() + "." );
        candidate.setLastConnectionTime( System.currentTimeMillis() );
//        //NLogger.debug( DownloadConnection.class, 
//            "Download Engine @" + Integer.toHexString(hashCode()) 
//            + " connected successfully to " + candidate.getHostAddress() + ".");
    }
    
    /**
     * Connects to a download candidate.
     * @param timeout
     * @throws IOException
     */
    public void connect( int timeout )
        throws IOException
    {
        assert socket == null;
        
        DestAddress address = candidate.getHostAddress();
        
        try
        {
            candidate.addToCandidateLog( "Wait for connect slot " + address.getHostName() + ":"
                + address.getPort() );
            //NLogger.debug( DownloadConnection.class,
//                "Wait for connect slot " + address.getHostName() + ":"
//                + address.getPort() );
            
            Runnable acquireCallback = new Runnable() {
                public void run()
                {
                    DestAddress candAddress = candidate.getHostAddress();
                    candidate.addToCandidateLog( "Connecting to " + candAddress.getHostName() + ":"
                        + candAddress.getPort() );
                    //NLogger.debug( DownloadConnection.class,
//                        "Connecting to " + candAddress.getHostName() + ":"
//                        + candAddress.getPort() );
                    candidate.setStatus(CandidateStatus.CONNECTING);
                }
            };
            socket = SocketFactory.connect( address, timeout,
                acquireCallback );
        }
        catch ( SocketException exp )
        {// indicates a general communication error while connecting
            throw new ConnectionFailedException( exp.getMessage() );
        }

        BandwidthController bwCont = candidate.getDownloadFile().getBandwidthController();
        setBandwidthController( bwCont );
        
        candidate.addToCandidateLog( "Connected successfully to " 
            + candidate.getHostAddress() + "." );
        candidate.setLastConnectionTime( System.currentTimeMillis() );
        //NLogger.debug( DownloadConnection.class, 
//            "Download Engine @" + Integer.toHexString(hashCode()) 
//            + " connected successfully to " + candidate.getHostAddress() + ".");
    }
}
