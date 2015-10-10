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
package phex.http;

public class HTTPHeader
{
    /**
     * The http header name.
     */
    private String name;

    /**
     * The http header value.
     */
    private String value;

    /**
     * Creates a new http header with name and value
     * @param name the http header name.
     * @param value the http header value.
     */
    public HTTPHeader( String name, String value )
    {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns the http header name.
     * @return the http header name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the http header value.
     * @return the http header value.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Returns true if the value equals the string "true", otherwise false is
     * returned.
     * @return true if the value equals the string "true", otherwise false is
     * returned.
     */
    public boolean booleanValue()
    {
        return Boolean.valueOf( value ).booleanValue();
    }
    
    /**
     * Returns the float value of the string.
     * @return the float value of the string.
     * @throws NumberFormatException if the string can not be
     * parsed into a float.
     */
    public float floatValue()
        throws NumberFormatException
    {
        return Float.parseFloat( value );
    }
    
    /**
     * Returns the long value of the string.
     * @return the long value of the string.
     * @throws NumberFormatException if the string can not be
     * parsed into a long.
     */
    public long longValue()
        throws NumberFormatException
    {
        return Long.parseLong( value );
    }
    
    /**
     * Returns the byte value of the string.
     * @return the byte value of the string.
     * @throws NumberFormatException if the string can not be
     * parsed into a byte.
     */
    public byte byteValue()
        throws NumberFormatException
    {
        return Byte.parseByte( value );
    }
    
    /**
     * Returns the int value of the string.
     * @return the int value of the string.
     * @throws NumberFormatException if the string can not be
     * parsed into a int.
     */
    public int intValue()
        throws NumberFormatException
    {
        return Integer.parseInt( value );
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( !(obj instanceof HTTPHeader) )
        {
            return false;
        }
        return ((HTTPHeader)obj).name.equals( name );
    }

    /**
     * Hash code to equal on hash based collections.
     * @return a hash code value for this HTTPHeader.
     */
    @Override
    public int hashCode()
    {
        int h = 0;
        h = ((127 *h) + (( name != null ) ? name.hashCode() : 0 ));
        h = ((127 *h) + (( value != null ) ? value.hashCode() : 0 ));
        return h;
    }
}