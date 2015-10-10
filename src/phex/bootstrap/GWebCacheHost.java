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
package phex.bootstrap;

import java.io.IOException;
import java.net.URL;

import phex.common.address.AddressUtils;
import phex.connection.ProtocolNotSupportedException;

public class GWebCacheHost extends BootstrapHost
{
    private static final long RECONNECT_PENALTY = 1000 * 60 * 60 * 4; // 4 hours
    
    private URL url;
        
    public GWebCacheHost( URL url, boolean isPhexCache )
        throws IOException
    {
        super( isPhexCache );
        
        if ( url == null )
        {
            throw new NullPointerException( "Null url given.");
        }
        
        // we only support http protocol urls.
        if ( !url.getProtocol().equals( "http" ) )
        {
            throw new ProtocolNotSupportedException(
                "Only http URLs are supported for a GWebCacheConnection" );
        }
        if ( url.getPort() == 80 )
        {
            // rebuild url without port
            url = new URL( url.getProtocol(), url.getHost(), -1, url.getFile() );
        }

        this.url = url;
    }

    public URL getUrl()
    {
        return url;
    }
    
    public String getHostDomain()
    {
        String host = url.getHost();
        if ( AddressUtils.isIPHostName(host) )
        {
            return host;
        }
        int topLevelIdx = host.lastIndexOf( '.' );
        int domainIdx = host.lastIndexOf('.', topLevelIdx - 1 );
        if ( domainIdx != -1 )
        {
            return host.substring(domainIdx+1);
        }
        return host;
    }    
    
    public void countConnectionAttempt( boolean isFailed )
    {
        lastRequestTime = System.currentTimeMillis();
        if ( isFailed )
        {
            incFailedInRowCount();
        }
        else
        {
            resetFailedInRowCount();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getReconnectPenalty()
    {
        return RECONNECT_PENALTY;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj ) 
        {
            return true;
        }
        
        if ( !(obj instanceof GWebCacheHost) )
        {
            return false;
        }
        
        GWebCacheHost gwc = (GWebCacheHost) obj;
        return url.getHost().equals( gwc.getUrl().getHost() );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return 17 * 31 + url.getHost().hashCode();
    }   
}