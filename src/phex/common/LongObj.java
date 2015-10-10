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

/**
 * Class that is similar to <code>Long</code> but is mutable.
 */
public class LongObj extends Number
{
    public long value;

    public LongObj()
    {
    }

    public LongObj( long v )
    {
        value = v;
    }

    public void setValue( long v )
    {
        this.value = v;
    }

    public long getValue()
    {
        return value;
    }

    @Override
	public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        else if ( ! ( o instanceof LongObj ) )
        {
            return false;
        }
        return value == ((LongObj)o).value;
    }

    @Override
	public int hashCode()
    {
        return (int)value;
    }

    /**
     * Increases the integer by inc and returns the new value.
     */
    public long inc( long inc )
    {
        value += inc;
        return value;
    }

    /**
     * Increases the integer by one and returns the new value.
     */
    public long inc()
    {
        value ++;
        return value;
    }

    public String toString()
    {
        return String.valueOf( value );
    }

    /**
     * Returns the value of the specified number as an <code>int</code>.
     * This may involve rounding.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type <code>int</code>.
     */
    public int intValue()
    {
        return (int)getValue();
    }

    /**
     * Returns the value of the specified number as a <code>long</code>.
     * This may involve rounding.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type <code>long</code>.
     */
    public long longValue()
    {
        return getValue();
    }

    /**
     * Returns the value of the specified number as a <code>float</code>.
     * This may involve rounding.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type <code>float</code>.
     */
    public float floatValue()
    {
        return getValue();
    }

    /**
     * Returns the value of the specified number as a <code>double</code>.
     * This may involve rounding.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type <code>double</code>.
     */
    public double doubleValue()
    {
        return getValue();
    }

}
