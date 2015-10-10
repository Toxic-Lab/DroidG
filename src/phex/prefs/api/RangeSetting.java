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
 *  Created on 15.08.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.prefs.api;

public class RangeSetting<T extends Comparable<T>> extends Setting<T>
{
    protected final T minValue;
    protected final T maxValue;
    
    /**
     * @param value
     * @param defaultValue
     * @param minValue
     * @param maxValue
     */
    public RangeSetting( String key, T value, T defaultValue, 
        T minValue, T maxValue, Preferences preferences )
    {
        super( key, value, defaultValue, preferences );
        if ( maxValue == null || minValue == null )
        {
            throw new NullPointerException( "Min or max value is null" );
        }
        if ( maxValue.compareTo( minValue ) < 0 )
        {
            throw new IllegalArgumentException( "Max less then min." );
        }
        this.minValue = minValue;
        this.maxValue = maxValue;
    }
    
    public T max()
    {
        return maxValue;
    }
    
    @Override
    public void set( T newValue )
    {
        if ( newValue.compareTo( maxValue ) > 0 ) 
        {
            super.set( maxValue );
        }
        else if ( newValue.compareTo( minValue ) < 0)
        {
            super.set( minValue );
        }
        else
        {
            super.set( newValue );
        }
    }
}
