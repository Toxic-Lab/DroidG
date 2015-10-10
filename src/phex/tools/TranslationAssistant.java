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
package phex.tools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import phex.utils.IOUtil;
import phex.utils.Localizer;

/**
 *
 */
public class TranslationAssistant
{
    public static void main(String[] args)
    {
        TranslationAssistant assistent = new TranslationAssistant();
        long start = System.currentTimeMillis();
        assistent.findUnusedKeys();
        assistent.findMissingKeys();
        long end = System.currentTimeMillis();
        System.out.println( "---Time: " + (end - start) );
    }
    
    private void findMissingKeys()
    {
        List fileList = getAllPossibleLangFiles();
        InputStream stream = Localizer.class.getResourceAsStream( "/phex/resources/Lang.properties" );
        // make sure it is buffered
        stream = new BufferedInputStream( stream );
        Properties mainProps = new Properties();
        try
        {
            mainProps.load( stream );
        }
        catch ( IOException exp )
        {
            exp.printStackTrace();
        }
        finally
        {
            IOUtil.closeQuietly( stream );
        }
        Set mainKeySet = mainProps.keySet();
        
        Iterator iterator = fileList.iterator();
        while( iterator.hasNext() )
        {
            String filename = (String) iterator.next();
            stream = Localizer.class.getResourceAsStream( filename );
            if ( stream == null ) { continue; }
            // make sure it is buffered
            stream = new BufferedInputStream( stream );
            Properties props = new Properties();
            try
            {
                props.load( stream );
                Set keys = props.keySet();
                Set compareSet = new HashSet( mainKeySet );
                compareSet.removeAll(keys);
                
                Iterator keyIterator = compareSet.iterator();
                if ( keyIterator.hasNext() )
                {
                    System.out.println( "---Missing Keys in " + filename );
                    while( keyIterator.hasNext() )
                    {
                        System.out.println( keyIterator.next() );
                    }
                }
            }
            catch (IOException exp)
            {
                exp.printStackTrace();
            }
            finally
            {
                IOUtil.closeQuietly( stream );
            }
        }

    }
    
    private void findUnusedKeys()
    {
        List fileList = getAllPossibleLangFiles();
        
        Iterator iterator = fileList.iterator();
        while( iterator.hasNext() )
        {
            String filename = (String) iterator.next();
            InputStream stream = Localizer.class.getResourceAsStream( filename );
            if ( stream == null ) { continue; }
            // make sure it is buffered
            stream = new BufferedInputStream( stream );
            Properties props = new Properties();
            try
            {
                props.load( stream );
                Set keys = props.keySet();
                findUnusedKeys( keys, new File("src/main/java/phex") );
                System.out.println( "---Unused Keys in " + filename );
                Iterator keyIterator = keys.iterator();
                while( keyIterator.hasNext() )
                {
                    System.out.println( keyIterator.next() );
                }
            }
            catch (IOException exp)
            {
            }
            finally
            {
                IOUtil.closeQuietly( stream );
            }
        }
    }

    /**
     * @param keys
     * @throws IOException
     */
    private void findUnusedKeysRegEx(Set keys, File source ) throws IOException
    {
        if ( source.isDirectory() )
        {
            File[] childs = source.listFiles(new FileFilter(){
                public boolean accept(File file)
                {
                    return file.isDirectory() || file.getName().endsWith("java");
                }
            });
            for (int i = 0; i < childs.length; i++)
            {
                findUnusedKeysRegEx( keys, childs[i] );
            }
        }
        else
        {
            System.out.println( "searching " + keys.size() +" : " + source );
            FileInputStream inStream = new FileInputStream( source );
            FileChannel channel = inStream.getChannel();
            ByteBuffer byteBuf = ByteBuffer.allocate( (int)source.length() );
            channel.read( byteBuf );
            byteBuf.rewind();
            CharBuffer charBuf = Charset.forName("US-ASCII").decode(byteBuf);
            String[] keyArr = (String[]) keys.toArray( new String[keys.size()] );
            for (int i = 0; i < keyArr.length; i++)
            {
                Pattern pattern = Pattern.compile( ".*"+keyArr[i]+".*", Pattern.DOTALL
                    );
                Matcher matcher = pattern.matcher( charBuf );
                if ( matcher.matches() )
                {
                    keys.remove( keyArr[i] 	);
                    //System.out.println( "found " + keyArr[i] + " in " + source );
                }
            }
        }
    }
    
    /**
     * @param keys
     * @throws IOException
     */
    private void findUnusedKeys(Set keys, File source ) throws IOException
    {
        if ( source.isDirectory() )
        {
            //System.out.println( "looking in (" + keys.size() +"): " + source );
            File[] childs = source.listFiles(new FileFilter(){
                public boolean accept(File file)
                {
                    return file.isDirectory() || file.getName().endsWith("java");
                }
            });
            for (int i = 0; i < childs.length; i++)
            {
                findUnusedKeys( keys, childs[i] );
            }
        }
        else
        {
            FileInputStream inStream = new FileInputStream( source );
            FileChannel channel = inStream.getChannel();
            ByteBuffer byteBuf = ByteBuffer.allocate( (int)source.length() );
            channel.read( byteBuf );
            byteBuf.rewind();
            String fileString = new String( byteBuf.array(), "US-ASCII" );
            String[] keyArr = (String[]) keys.toArray( new String[keys.size()] );
            for (int i = 0; i < keyArr.length; i++)
            {
                if ( fileString.indexOf( keyArr[i]) != -1 )
                {
                    keys.remove( keyArr[i] 	);
                }
            }
        }
    }

    /**
     * @return
     */
    private List getAllPossibleLangFiles()
    {
        List<Locale> availableLocales = Localizer.getAvailableLocales();
        List<String> fileList = new ArrayList<String>();
        fileList.add( "/phex/resources/Lang.properties" );
        fileList.add( "/Lang.properties" );
        for ( Locale locale : availableLocales )
        {
            StringBuffer buffer = new StringBuffer( "Lang" );
            String language = locale.getLanguage();
            if ( language.length() > 0 )
            {
                buffer.append( '_' );
                buffer.append( language );
                fileList.add( "/phex/resources/" + buffer.toString() + ".properties" );
                fileList.add( "/" + buffer.toString() + ".properties" );
                String country = locale.getCountry();
                if ( country.length() > 0 )
                {
                    buffer.append( '_' );
                    buffer.append( country );
                    fileList.add( "/phex/resources/" + buffer.toString() + ".properties" );
                    fileList.add( "/" + buffer.toString() + ".properties" );
                }
            }
        }
        return fileList;
    }
}
