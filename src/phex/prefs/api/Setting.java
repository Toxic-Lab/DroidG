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
package phex.prefs.api;


public class Setting<T>
{
    protected final Preferences preferences;
    
    /**
     * The name of this setting.
     */
    protected final String name;
    
    /**
     * The value of this setting.
     */
    protected T value;
    
    /**
     * The default value of this setting.
     */
    protected final T defaultValue;
    
    protected boolean isAlwaysSaved;
    
    /**
     * @param value
     * @param defaultValue
     */
    protected Setting( String name, T value, T defaultValue,
        Preferences preferences )
    {
        this.preferences = preferences;
        this.name = name;
        this.value = value;
        this.defaultValue = defaultValue;
        this.isAlwaysSaved = false;
    }
    
    /**
     * @return the isAlwaysSaved
     */
    public boolean isAlwaysSaved()
    {
        return isAlwaysSaved;
    }

    /**
     * @param isAlwaysSaved the isAlwaysSaved to set
     */
    public void setAlwaysSaved( boolean isAlwaysSaved )
    {
        this.isAlwaysSaved = isAlwaysSaved;
    }

    public void set( T newValue )
    {
        if ( !(value == null ? newValue == null : value.equals( newValue ) ) )
        {
            this.value = newValue;
            preferences.saveRequiredNotify();
        }
    }
    
    /**
     * Method to notify a setting that it has changed. Can be used in case
     * a setting is changed without calling its set() method i.e. Collections.
     */
    public void changed()
    {
        preferences.saveRequiredNotify();
    }
    
    public T get()
    {
        return value;
    }
    
    public String getName()
    {
        return name;
    }
    
    public boolean isDefault()
    {
        // handles the case of null == null
        if ( value == defaultValue ) 
        {
            return true;
        }
        if ( value == null || defaultValue == null ) 
        {
            return false;
        }
        return value.equals( defaultValue );
    }
}