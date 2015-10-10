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
package phex.utils;

import java.util.Random;

public class RandomUtils
{
    public static final Random RANDOM = new Random();
    
    public static byte[] getBytes( int count )
    {
        byte[] res = new byte[ count ];
        RANDOM.nextBytes( res );
        return res;
    }
    
    /**
     * Returns an int between 0 (inclusive) and maxValue (exclusive).
     * @param maxValue the maximal value to allow
     * @return an int between 0 (inclusive) and maxValue (exclusive).
     */
    public static int getInt( int maxValue )
    {
        return RANDOM.nextInt( maxValue );
    }
    
    public static long getLong()
    {
        return RANDOM.nextLong();
    }
}
