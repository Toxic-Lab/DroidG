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
package phex.host;

import java.util.concurrent.atomic.AtomicInteger;

import phex.common.address.DestAddress;
import phex.utils.StringUtils;

/**
 * 
 */
public class CaughtHost
{
    private static AtomicInteger UNIQUE_ID_PROVIDER = new AtomicInteger(0);
    
    /**
     * A unique id of a host, it's main use is to provide 
     * distinction in CaughtHostComparator.
     */
    private final int uniqueId;
    
    /**
     * The address of the host.
     */
    private final DestAddress hostAddress;
    
    private long lastFailedConnection;
    private long lastSuccessfulConnection;
    private int avgDailyUptime;
    private String vendor;
    private int vendorVersionMajor;
    private int vendorVersionMinor;
    private boolean isUltrapeer;
    
    /**
     * @param address
     */
    public CaughtHost( DestAddress address  )
    {
        this.uniqueId = UNIQUE_ID_PROVIDER.incrementAndGet();
        hostAddress = address;
        lastFailedConnection = -1;
        lastSuccessfulConnection = -1;
    }
    
    public DestAddress getHostAddress()
    {
        return hostAddress;
    }
    
    public void setVendor(String vendor, int vendorVersionMajor, int vendorVersionMinor )
    {
        this.vendor = vendor;
        this.vendorVersionMajor = vendorVersionMajor;
        this.vendorVersionMinor = vendorVersionMinor;
    }
        
    /**
     * @return the vendor
     */
    public String getVendor()
    {
        return vendor;
    }

    /**
     * @return the vendorVersionMajor
     */
    public int getVendorVersionMajor()
    {
        return vendorVersionMajor;
    }

    /**
     * @return the vendorVersionMinor
     */
    public int getVendorVersionMinor()
    {
        return vendorVersionMinor;
    }

    /**
     * @param dailyUptime
     */
    public void setDailyUptime(int dailyUptime)
    {
        avgDailyUptime = dailyUptime;
    }
    
    /**
     * @return the daily average uptime. 
     */
    public int getDailyUptime()
    {
        return avgDailyUptime;
    }
    
    /**
     * @return the isUltrapeer
     */
    public boolean isUltrapeer()
    {
        return isUltrapeer;
    }

    /**
     * @param isUltrapeer the isUltrapeer to set
     */
    public void setUltrapeer(boolean isUltrapeer)
    {
        this.isUltrapeer = isUltrapeer;
    }

    public boolean equals( Object o )
    {
        if ( !(o instanceof CaughtHost ) )
        {
            return false;
        }
        return hostAddress.equals( ((CaughtHost)o).hostAddress );
    }
    
    public int hashCode()
    {
        return hostAddress.hashCode();
    }
    
    /**
     * Returns true if this host has a PHEX vendor code and
     * a daily avg uptime of at least 2 hour and is a ultrapeer.
     * @return true if this is a decent Phex host, false otherwise.
     */ 
    public boolean isDecentPhexHost()
    {
        if ( !StringUtils.equals( vendor, "PHEX" ) )
        {
            return false;
        }
        if ( avgDailyUptime < 3600 )
        {
            return false;
        }
        if ( !isUltrapeer )
        {
            return false;
        }
        return true;
    }
    
    /**
     * Returns 1 if the last connection was successful, -1 if the last
     * connection failed or 0 if not connected.
     * @return 1 if the last connection was successful, -1 if the last
     * connection failed or 0 if not connected.
     */
    public int getConnectionTimeRating()
    {
        if ( lastSuccessfulConnection == -1 && lastFailedConnection == -1 )
        {
            return 0;
        }
        if ( lastFailedConnection > lastSuccessfulConnection )
        {
            return -1;
        }
        else
        {
            return 1;
        }
    }
    
    /**
     * @param l
     */
    public void setLastFailedConnection(long l)
    {
        lastFailedConnection = l;
    }

    /**
     * @param l
     */
    public void setLastSuccessfulConnection(long l)
    {
        lastSuccessfulConnection = l;
    }

    public long getLastFailedConnection()
    {
        return lastFailedConnection;
    }

    public long getLastSuccessfulConnection()
    {
        return lastSuccessfulConnection;
    }
    
    public String toString()
    {
        return "CaughtHost[" + hostAddress.toString() + ",Failed=" + 
            lastFailedConnection + ",Successful=" + lastSuccessfulConnection +
            ",Uptime=" + avgDailyUptime + "]";
    }
    
    /**
     * The id given to each CaughtHost. This is only introduced 
     * to be used in a comparator.
     * @return the unique id
     */
    public int getUniqueId()
    {
        return uniqueId;
    }

}