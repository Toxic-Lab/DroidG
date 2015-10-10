/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Phex Development Group
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
 *  --- CVS Information ---
 *  $Id$
 */
package phex.common;

import java.util.*;

/**
 * A expiry date is used to hold a timestamp of an expiry or the indication that
 * it never expires or expires at the end of the session.
 */
public class ExpiryDate extends Date
{
    public static final long EXPIRES_NEVER = Long.MAX_VALUE;
    public static final long EXPIRES_END_OF_SESSION = -1;

    public static final ExpiryDate NEVER_EXPIRY_DATE = new ExpiryDate( EXPIRES_NEVER );
    public static final ExpiryDate SESSION_EXPIRY_DATE = new ExpiryDate( EXPIRES_END_OF_SESSION );
    
    /**
     * Returns a expiry date object for the given expiry date. When Long.MAX_VALUE
     * is used as input then NEVER_EXPIRY_DATE is returned, if -1 is used as
     * input SESSION_EXPIRY_DATE is returned.
     * Any other values return a new ExpiryDate representing the given input date.
     * @param expiryDate
     * @return
     */
    public static ExpiryDate getExpiryDate( long expiryDate )
    {
        if ( expiryDate == EXPIRES_NEVER )
        {
            return NEVER_EXPIRY_DATE;
        }
        else if ( expiryDate == EXPIRES_END_OF_SESSION )
        {
            return SESSION_EXPIRY_DATE;
        }
        else
        {
            return new ExpiryDate( expiryDate );
        }
    }

    /**
     * @param expiryDate The date in millis after which this rule expires, use EXPIRES_NEVER
     * (Long.MAX_VALUE) for indefinite (never), or EXPIRES_END_OF_SESSION (-1)
     * for end of session.
     */
    private ExpiryDate( long expiryDate )
    {
        super( expiryDate );
    }

    public boolean isExpiringEndOfSession()
    {
        return getTime() == EXPIRES_END_OF_SESSION;
    }

    public boolean isExpiringNever()
    {
        return getTime() == EXPIRES_NEVER;
    }
    
    public boolean isExpired()
    {
        if ( isExpiringEndOfSession() || isExpiringNever() )
        {
            return false;
        }
        
        return getTime() < System.currentTimeMillis();
    }
}