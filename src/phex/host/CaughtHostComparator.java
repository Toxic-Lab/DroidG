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

import java.util.Comparator;

/**
 * This class is responsible for comparing two CaughtHost instances.
 * The Comparator will determine which instance has a higher probability
 * of a successful connection.
 *
 * Test from 2007-12-03 by Arne Babenhauserheide with Phex SVN  after several restarts
 * from the output of 15936 comparisions: 
 *    about 90% had a diffConnRating of 0
 *    phexVal was always 0 - this was different on previous runs
 *    only about 6.8% had an Uptime difference of 0
 *    TODO: Recheck the algorithm with long-running Phex and with restarted Phex after it had run for a long time before that. - Only commenting out teh println for that reason.
 *
 * Real statistics from many/all running Phex' would be nice to have, 
 * but creating them would mean quite a bit of effort, 
 * and might have privacy implications.
 *
 */
public class CaughtHostComparator implements Comparator<CaughtHost>
{
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(CaughtHost host1, CaughtHost host2)
    {
        if ( host1 == host2 || host1.equals( host2 ) )
        {
            return 0;
        }
        
        // first check if last connection was failed or successfull
        int h1Rating = host1.getConnectionTimeRating();
        int h2Rating = host2.getConnectionTimeRating();
        int diff = h1Rating - h2Rating; 
	/* 
	 * diffConnTime seems to have values between -2 and 2 
	 * (each seperate check can return -1, 0 or 1).  
	 */
        // System.out.println("diffConnTime:" + diff); // checking values
        if ( diff != 0 )
        {
            return diff;
        }
        
        /* 
	 * Now give Phex Ultrapeers with 
	 * at least uptime 7200 a slight advantage over others - if both are equal, 
	 * prefer the decent Phex.
	*/
        int phexVal = host1.isDecentPhexHost() ? 1 : 0;
        phexVal -= host2.isDecentPhexHost() ? 1 : 0;
	// System.out.println("phexVal:" + phexVal); // checking values
	if( phexVal != 0 )
        {
            return phexVal;
        }
        
	// compare by daily uptime if known. 
        diff = host1.getDailyUptime() - host2.getDailyUptime();
	// System.out.println("diffUptime:" + diff); // checking values
        if ( diff != 0 )
        {
            return diff;
        }
        // thrid compare which host has the latest successful connection
        diff = (int)(host1.getLastSuccessfulConnection() - host2.getLastSuccessfulConnection());
	// System.out.println("diffLastConn:" + diff); // checking values
        if ( diff != 0)
        {
            return diff;
        }
        // no use the unique identification counter, it is used 
        // to have a constant difference between instances with no
        // other comparable difference.
        return host1.getUniqueId() - host2.getUniqueId();
    }    
}
