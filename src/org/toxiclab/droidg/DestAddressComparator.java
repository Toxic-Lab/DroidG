package org.toxiclab.droidg;

import java.util.Comparator;

import phex.common.address.DestAddress;
import phex.common.address.IpAddress;

/**
 * 
 */
public class DestAddressComparator implements Comparator<DestAddress>
{
    public int compare(DestAddress ha1, DestAddress ha2)
    {
        IpAddress ip1 = ha1.getIpAddress();
        IpAddress ip2 = ha2.getIpAddress();
        if ( ip1 != null && ip2 != null )
        {
            long ip1l = ip1.getLongHostIP();
            long ip2l = ip2.getLongHostIP();
    
            if ( ip1l < ip2l
               || ( ip1l == ip2l && ha1.getPort() < ha2.getPort() ) )
            {
                return -1;
            }
            else
            {
                return 1;
            }
        }
        else
        {
            return ha1.getHostName().compareTo(ha2.getHostName());
        }
    }

}
