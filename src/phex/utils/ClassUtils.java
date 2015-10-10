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
 *  Created on 10.03.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.utils;



/**
 *
 */
public class ClassUtils
{
    /**
     * Null safe getClass().toString() call on an object
     * @param obj the object to get the string representation from.
     * @return the class string.
     */
    public static String getClassString( Object obj )
    {
        if ( obj == null )
        {
            return "null";
        }
        else
        {
            return obj.getClass().toString();
        }
    }
    
    /**
     * Tries to load a class by name without throwing exceptions.
     * If class loading failed null is returned.
     * @param className
     * @return the class or null.
     */
    public static Class classForNameQuitly(String className)
    {
        if (className == null)
        {
            return null;
        }
        try
        {
            Class clazz = Class.forName(className);
            return clazz;
        }
        catch (ClassNotFoundException exp)
        {
            //NLogger.debug( ClassUtils.class, exp, exp);
        }
        return null;
    }

    public static Object newInstanceQuitly(Class clazz)
    {
        if (clazz == null)
        {
            return null;
        }

        try
        {
            Object instance = clazz.newInstance();
            return instance;
        }
        catch (IllegalAccessException exp)
        {
            //NLogger.debug( ClassUtils.class, exp, exp);
            return null;
        }
        catch (InstantiationException exp)
        {
            //NLogger.debug( ClassUtils.class, exp, exp);
            return null;
        }
    }
}
