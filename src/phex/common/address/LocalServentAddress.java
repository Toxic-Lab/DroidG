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
package phex.common.address;

import phex.event.ChangeEvent;
import phex.event.PhexEventService;
import phex.event.PhexEventTopics;
import phex.net.repres.PresentationManager;
import phex.net.server.Server;
import phex.prefs.core.ProxyPrefs;

public class LocalServentAddress implements DestAddress
{
    /**
     * The server this local address applies for.
     */
    private final Server server;
    
    /**
     * The event service to use to publish address changes.
     */
    private final PhexEventService eventService;
    
    /**
     * A possibly forced address from the user, 
     * it's null in case no forced address is set.
     */
    private DestAddress forcedAddress;
    
    /**
     * The determined local address of this node, only
     * used if forcedAddress is not null.
     */
    private DestAddress localAddress;
    
    
    public LocalServentAddress( Server server, PhexEventService eventService )
    {
       this.server = server;
       this.eventService = eventService;
       localAddress = PresentationManager.getInstance().createHostAddress( 
           IpAddress.LOCAL_HOST_IP, server.getListeningLocalPort() );
    }
    
    /**
     * Updates the local address in case there is no forced ip set.
     */
    public void updateLocalAddress( DestAddress updateAddress )
    {
        if ( forcedAddress != null )
        {
            // we have a forced address the local address has no value.
            return;
        }
        // we should only compare the IP before updating.. 
        // the port is usually different..
        if ( localAddress == null || !localAddress.getIpAddress().equals( 
            updateAddress.getIpAddress() ) )
        {
            localAddress = PresentationManager.getInstance().createHostAddress(
                updateAddress.getIpAddress(), server.getListeningLocalPort() );
            fireNetworkIPChanged( localAddress );
        }
    }

    /**
     * Sets the forced IP in the configuration. This call is not saving the
     * configuration!
     */
    public void setForcedHostIP( IpAddress forcedHostIP )
    {
        PresentationManager presentationMgr = PresentationManager.getInstance();
        if ( forcedHostIP == null )
        {// clear forcedHostIP and init localAddress
            forcedAddress = null;
            ProxyPrefs.ForcedIp.set( "" );
            IpAddress hostIP = server.resolveLocalHostIP();
            int port = server.getListeningLocalPort();
            DestAddress address = presentationMgr.createHostAddress( 
                hostIP, port );
            updateLocalAddress( address );
            return;
        }
        if ( !forcedHostIP.isValidIP() )
        { 
            throw new IllegalArgumentException( 
                "Invalid IP " + forcedHostIP );
        }
        
        ProxyPrefs.ForcedIp.set( forcedHostIP.getFormatedString() );
        
        forcedAddress = presentationMgr.createHostAddress( forcedHostIP, 
            server.getListeningLocalPort() );
        fireNetworkIPChanged( forcedAddress );
    }
    
    private void fireNetworkIPChanged( DestAddress newAddress )
    {
        eventService.publish( PhexEventTopics.Servent_LocalAddress, 
            new ChangeEvent( this, null, newAddress ) );
    }

    
    protected DestAddress getEffectiveAddress()
    {
        return forcedAddress != null ? forcedAddress : localAddress;
    }
    
    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof DestAddress )
        {
            return equals( (DestAddress) obj );
        }
        return false;
    }
    
    @Override
    public int hashCode()
    {
        return getEffectiveAddress().hashCode();
    }
    
    //////////////////// Delegate methods ///////////////////////////
    
    public boolean equals(DestAddress address)
    {
        return getEffectiveAddress().equals( address );
    }

    public boolean equals(byte[] ipAddress, int port)
    {
        return getEffectiveAddress().equals( ipAddress, port );
    }

    public String getCountryCode()
    {
        return getEffectiveAddress().getCountryCode();
    }


    public String getFullHostName()
    {
        return getEffectiveAddress().getFullHostName();
    }


    public String getHostName()
    {
        return getEffectiveAddress().getHostName();
    }


    public IpAddress getIpAddress()
    {
        return getEffectiveAddress().getIpAddress();
    }


    public int getPort()
    {
        return getEffectiveAddress().getPort();
    }


    public boolean isIpHostName()
    {
        return getEffectiveAddress().isIpHostName();
    }


    public boolean isLocalHost( DestAddress localAddress )
    {
        return getEffectiveAddress().isLocalHost( localAddress );
    }


    public boolean isSiteLocalAddress()
    {
        return getEffectiveAddress().isSiteLocalAddress();
    }


    public boolean isValidAddress()
    {
        return getEffectiveAddress().isValidAddress();
    }
}
