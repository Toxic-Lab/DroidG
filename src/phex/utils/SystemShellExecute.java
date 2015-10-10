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
 *  Created on 27.10.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.utils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.SystemUtils;

import phex.common.MediaType;
import phex.prefs.core.SystemToolsPrefs;

public class SystemShellExecute
{
    public static void exploreFolder( File dir ) throws IOException
    {
        String path;
        try 
        {
            path = dir.getCanonicalPath();
        } 
        catch(IOException ioe) 
        {
            path = dir.getPath();
        }

        String command = null;
        if ( SystemUtils.IS_OS_WINDOWS )
        {
            command = "explorer";
        }
        else if ( SystemUtils.IS_OS_MAC_OSX )
        {
            command = "open";
        }
        if ( command == null )
        {
            return;
        }
        String[] cmdArr = new String[]{ command, path };
        Runtime.getRuntime().exec( cmdArr );
    }
    
    public static void launchFile( File file ) throws IOException
    {
        String path;
        try 
        {
            path = file.getCanonicalPath();
        } 
        catch(IOException ioe) 
        {
            path = file.getPath();
        }

        if ( SystemUtils.IS_OS_WINDOWS )
        {
            WindowsShellExecute.executeViaShell( path );
            return;
        }
        else if ( SystemUtils.IS_OS_MAC_OSX )
        {
            String command = "open";
            String[] cmdArr = new String[]{ command, path };
            Runtime.getRuntime().exec( cmdArr );
            return;
        }
        else
        {
            launchOtherOsFile( path );
        }
    }
    
    private static void launchOtherOsFile( String filepath )
        throws IOException
    {
        String commandStr;
        if ( MediaType.getVideoMediaType().isFilenameOf(filepath) )
        {
            commandStr = SystemToolsPrefs.OpenVideoCmdOtherOS.get();
        }
        else if ( MediaType.getImageMediaType().isFilenameOf(filepath) )
        {
            commandStr = SystemToolsPrefs.OpenImageCmdOtherOS.get();
        }
        else if ( MediaType.getAudioMediaType().isFilenameOf(filepath) )
        {
            commandStr = SystemToolsPrefs.OpenAudioCmdOtherOS.get();
        }
        else
        {
            commandStr = SystemToolsPrefs.OpenBrowserCmdOtherOS.get();
        }
        
        // Define a pattern which will split on spaces but respect quotes
        Pattern p = Pattern.compile("(\".*?\"|\\S+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher( commandStr );
        List tokens = new LinkedList();
        while ( m.find() )
        {
            tokens.add( m.group(1) );
        }
        String[] result = new String[ tokens.size() ];
        tokens.toArray(result);
        
        StringBuffer replacedCmd = new StringBuffer();
        for( int i = 0; i < result.length; i++ )
        {
            result[i] = StringUtils.replace(result[i], "%filepath%", filepath, -1);
            
            replacedCmd.append( result[i] );
            replacedCmd.append( " " );
        }
//        NLogger.debug(SystemShellExecute.class,
//            "Executing " + replacedCmd );
        Runtime.getRuntime().exec( result );
    }
}
