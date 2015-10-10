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
package phex.query;

/**
 * 
 */
public interface DynamicQueryConstants
{
    /**
     * The minimum length a search term must have.
     */
    public static int MIN_SEARCH_TERM_LENGTH = 2;
    
    /**
     * The max. estimated query horizon that is tried to be reached.
     */
    public static final int MAX_ESTIMATED_QUERY_HORIZON = 200000;
    
    /**
     * The time to wait in millis on queried per hop.
     */
    public static final int DEFAULT_TIME_TO_WAIT_PER_HOP = 2400;
    
    public static final int DEFAULT_TIME_TO_DECREASE_PER_HOP = 10;
    
    
    /**
     * The number of millis after which the time to wait per hop is adjusted.
     */
    public static final int TIMETOWAIT_ADJUSTMENT_DELAY = 6000;
    
    /**
     * The number of millis to adjust the time to wait per hop. This 
     * will be multiplied by a factor calculated from the received results
     * ratio.
     */
    public static final int TIMETOWAIT_ADJUSTMENT = 200;
    
    /**
     * The default max ttl of hosts not providing a max ttl value.
     */
    public static final byte DEFAULT_MAX_TTL = 4;
    
    /**
     * The default degree value of not dynamic query supporting hosts.
     */
    public static final int NON_DYNAMIC_QUERY_DEGREE = 6;
}