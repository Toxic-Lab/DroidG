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
 *  Created on 17.08.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.prefs.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import phex.utils.StringUtils;


public final class PreferencesFactory
{
    private static final String SET_SER_POSTFIX = "[S:%d]";
    private static final String SET_DESER_POSTFIX = "[S:";
    private static final String LIST_SER_POSTFIX = "[L:%d]";
    private static final String LIST_DESER_POSTFIX = "[L:";
    
    public static Setting<String> createStringSetting( String name, 
        String defaultValue, Preferences preferences )
    {
        String value = preferences.getLoadedProperty( name );
        if ( value == null )
        {
            value = defaultValue;
        }
        Setting<String> setting = new Setting<String>( name, value, defaultValue, preferences );
        preferences.registerSetting( name, setting );
        return setting;
    }
    
    public static Setting<Boolean> createBoolSetting( String name, 
        boolean defaultValue, Preferences preferences )
    {
        Boolean defaultBool = Boolean.valueOf( defaultValue );
        
        String value = preferences.getLoadedProperty( name );
        Boolean boolValue;
        if ( value == null )
        {
            boolValue = defaultBool;
        }
        else if ( value.equals( "true" ) )
        {
            boolValue = Boolean.TRUE;
        }
        else if ( value.equals( "false" ) )
        {
            boolValue = Boolean.FALSE;
        }
        else
        {
            boolValue = defaultBool;
        }
        Setting<Boolean> setting = new Setting<Boolean>( name, boolValue, defaultBool, preferences );
        preferences.registerSetting( name, setting );
        return setting;
    }
    
    public static Setting<Float> createFloatSetting( String name, 
        float defaultValue, Preferences preferences )
    {
        Float defaultFloat = Float.valueOf( defaultValue );
        
        String value = preferences.getLoadedProperty( name );
        Float floatValue;
        // compared to Integer number parsing, Float is not handling null as
        // NumberFormatException.
        if ( StringUtils.isEmpty( value ) )
        {
            floatValue = defaultFloat;
        }
        else
        {
            try
            {
                floatValue = Float.valueOf( value );
            }
            catch ( NumberFormatException exp )
            {
                floatValue = defaultFloat;
            }
        }
        Setting<Float> setting = new Setting<Float>( name, floatValue, defaultFloat, preferences );
        preferences.registerSetting( name, setting );
        return setting;
    }
    
    public static Setting<Long> createLongSetting( String name, 
        long defaultValue, Preferences preferences )
    {
        Long defaultLong = Long.valueOf( defaultValue );
        
        String value = preferences.getLoadedProperty( name );
        Long longValue;
        try
        {
            longValue = Long.valueOf( value );
        }
        catch ( NumberFormatException exp )
        {
            longValue = defaultLong;
        }
        Setting<Long> setting = new Setting<Long>( name, longValue, defaultLong, preferences );
        preferences.registerSetting( name, setting );
        return setting;
    }
    
    public static Setting<Integer> createIntSetting( String name, 
        int defaultValue, Preferences preferences )
    {
        Integer defaultInt = Integer.valueOf( defaultValue );
        
        String value = preferences.getLoadedProperty( name );
        Integer intValue;
        try
        {
            intValue = Integer.valueOf( value );
        }
        catch ( NumberFormatException exp )
        {
            intValue = defaultInt;
        }
        Setting<Integer> setting = new Setting<Integer>( name, intValue, defaultInt, preferences );
        preferences.registerSetting( name, setting );
        return setting;
    }
    
    public static RangeSetting<Integer> createIntRangeSetting( String name, 
        int defaultValue, int minValue, int maxValue, Preferences preferences )
    {
        Integer defaultInt = Integer.valueOf( defaultValue );
        
        String value = preferences.getLoadedProperty( name );
        Integer intValue;
        try
        {
            intValue = Integer.valueOf( value );
        }
        catch ( NumberFormatException exp )
        {
            intValue = defaultInt;
        }
        RangeSetting<Integer> setting = new RangeSetting<Integer>( name, intValue, defaultInt, 
            Integer.valueOf( minValue ), Integer.valueOf( maxValue ), preferences );
        preferences.registerSetting( name, setting );
        return setting;
    }
    
    public static Setting<Short> createShortRangeSetting( String name, 
        short defaultValue, short minValue, short maxValue, Preferences preferences )
    {
        Short defaultShort = Short.valueOf( defaultValue );
        
        String value = preferences.getLoadedProperty( name );
        Short shortValue;
        try
        {
            shortValue = Short.valueOf( value );
        }
        catch ( NumberFormatException exp )
        {
            shortValue = defaultShort;
        }
        RangeSetting<Short> setting = new RangeSetting<Short>( name, shortValue, defaultShort, 
            Short.valueOf( minValue ), Short.valueOf( maxValue ), preferences );
        preferences.registerSetting( name, setting );
        return setting;
    }
    
    public static Setting<Set<String>> createSetSetting( String name, 
        Preferences preferences )
    {
        Set<String> values = deserializeSet( name, preferences );        
        Setting<Set<String>> setting = new Setting<Set<String>>( name, values, 
            null, preferences );
        preferences.registerSetting( name, setting );
        return setting;
    }
    
    public static Setting<List<String>> createListSetting( String name, 
        Preferences preferences )
    {
        List<String> values = deserializeList( name, preferences );        
        Setting<List<String>> setting = new Setting<List<String>>( name, values, 
            null, preferences );
        preferences.registerSetting( name, setting );
        return setting;
    }
    
    ///// special purpose factory methods
    public static Setting<Integer> createListeningPortSetting( String name,
        Preferences preferences )
    {
        String value = preferences.getLoadedProperty( name );
        int port;
        try
        {
            port = Integer.parseInt( value );
        }
        catch ( NumberFormatException exp )
        {
            port = -1;
        }
        
        // no valid listening port is set yet. Choose a random port from 4000-49150
        if ( port < 1 || port > 49150 )
        {
            Random random = new Random(System.currentTimeMillis());
            port = random.nextInt( 45150 );
            port += 4000;
        }
        Setting<Integer> setting = new Setting<Integer>( name, Integer.valueOf( port ), 
            null, preferences );
        preferences.registerSetting( name, setting );
        return setting;
    }
    ///// END special purpose factory methods
    
    
    private static List<String> deserializeList( String name, Preferences preferences )
    {
        List<String> list = new ArrayList<String>();
        List<String> names = preferences.getPrefixedPropertyNames( name + LIST_DESER_POSTFIX );
        Collections.sort( names, new ListPostfixKeyComparator() );
        for ( String key : names )
        {
            String value = preferences.getLoadedProperty( key );
            if ( !StringUtils.isEmpty( value ) )
            {
                list.add( value );
            }
        }
        return list;
    }
    
    private static Set<String> deserializeSet( String name, Preferences preferences )
    {
        Set<String> set = new HashSet<String>();
        String prefix = name + SET_DESER_POSTFIX;
        
        List<String> names = preferences.getPrefixedPropertyNames( prefix );
        for ( String key : names )
        {
            String value = preferences.getLoadedProperty( key );
            if ( !StringUtils.isEmpty( value ) )
            {
                set.add( value );
            }
        }
        return set;
    }

    /**
     * This method takes a Setting and serializes it into the given properties.
     * It takes care of using multiple elements for List and other special types. 
     * @param setting
     * @param properties
     */
    public static void serializeSetting( Setting<?> setting, Properties properties )
    {
        if ( setting == null )
        {
            throw new NullPointerException( "setting should not be null" );
        }
        String name = setting.getName();
        Object value = setting.get();
        if ( value instanceof String )
        {
            properties.setProperty( name, (String)value );
        }
        else if ( value instanceof Number )
        {
            properties.setProperty( name, ((Number)value).toString() );
        }
        else if ( value instanceof Boolean )
        {
            properties.setProperty( name, ((Boolean)value).toString() );
        }
        else if ( value instanceof Set )
        {
            Set<String> setValue = (Set<String>)value;
            int pos = 1;
            for( String elem : setValue )
            {
                properties.setProperty( name + String.format( SET_SER_POSTFIX, Integer.valueOf( pos++ ) ), elem );
            }
        }
        else if ( value instanceof List )
        {
            List<String> listValue = (List<String>)value;
            int listSize = listValue.size();
            for ( int i=0; i < listSize; i++ )
            {
                properties.setProperty( name + String.format( LIST_SER_POSTFIX, Integer.valueOf( i ) ), 
                    listValue.get( i ) );
            }
        }
        else
        {
//            NLogger.error( PreferencesFactory.class, 
//                "Unknwon settings value type: " + name + " / " + ClassUtils.getClassString( value ) );
        }
    }
    
    private static final class ListPostfixKeyComparator 
        implements Comparator<String>, Serializable
    {
        public int compare( String key1, String key2 )
        {
            if ( key1.equals( key2 ) )
            {
                return 0;
            }
            int idx1 = key1.lastIndexOf( LIST_DESER_POSTFIX ) + LIST_DESER_POSTFIX.length();
            int idx1E = key1.indexOf( ']', idx1 );
            String val1Str = key1.substring( idx1, idx1E );
            int val1;
            try
            {
                val1 = Integer.parseInt( val1Str );
            }
            catch ( NumberFormatException exp )
            {
                val1 = Integer.MAX_VALUE;
            }
            
            int idx2 = key2.lastIndexOf( LIST_DESER_POSTFIX ) + LIST_DESER_POSTFIX.length();
            int idx2E = key2.indexOf( ']', idx2 );
            String val2Str = key1.substring( idx2, idx2E );
            int val2;
            try
            {
                val2 = Integer.parseInt( val2Str );
            }
            catch ( NumberFormatException exp )
            {
                val2 = Integer.MAX_VALUE;
            }
            if ( val1 == val2 )
            {
                return key1.hashCode() - key2.hashCode();
            }
            return val1 - val2;
        }
        
    }
}
