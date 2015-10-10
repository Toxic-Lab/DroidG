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

import java.util.Comparator;

public class BootstrapHostComparator implements Comparator<BootstrapHost>
{
    public static final BootstrapHostComparator INSTANCE = new BootstrapHostComparator();
    
    private BootstrapHostComparator()
    {
    }
    
    public int compare( BootstrapHost h1, BootstrapHost h2 )
    {
        if ( h1.equals(h2) )
        {
            return 0;
        }
        long diff = h1.getEarliestReConnectTime() - h2.getEarliestReConnectTime();
        if ( diff == 0)
        {
            return h1.hashCode() - h2.hashCode();
        }
        else if ( diff > 0 )
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }

}
