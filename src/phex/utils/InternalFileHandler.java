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
package phex.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.URI;

import phex.metalink.MetalinkParser;
import phex.servent.Servent;
import phex.share.FileRescanRunner;

/**
 * Offers internal handling for files like magma-lists, rss-feeds, podcasts and similar. 
 */
public final class InternalFileHandler
{
    public static void magmaReadout( File file )
    {
    	try
        {
            BufferedInputStream inStream = new BufferedInputStream( new FileInputStream( file ) );
            MagmaParser parser = new MagmaParser( inStream );
            parser.start();

            List magnetList = parser.getMagnets();
            String relativeDownloadDir = parser.getMagmaName();
            Iterator iter = magnetList.iterator();
        
            // Sync subscription operation with a possible rescan process, this 
            // prevents downloads of files already existing but not yet scanned.
            FileRescanRunner.sync();

            while (iter.hasNext())
            {
                String magnet = (String) iter.next();
                URI uri = new URI( magnet, true );

                // dont add already downloading or shared urns.
                // If we didn't get a relativeDownloadDir, 
                // Download to the default folder. 
                if ( relativeDownloadDir.length() == 0 )
                {
                    downloadUri( uri );
                }
                // Else download to the relativeDOwnloadDir inside the default folder. 
                else
                {
                    downloadUri( uri, relativeDownloadDir );                    
                }
                
            }
/*            String uuri = parser.getUpdateURI();
            if ( uuri != null)
            {
               URI uri = new URI( uuri, true );
               sheduledReadout(uri, 60000);
            }

            */
            
        }
        catch (IOException exp)
        {
            //NLogger.warn( InternalFileHandler.class, exp.getMessage(), exp);
        }
    }
    public static void rssReadout( File file )
    {
            if (!file.exists())
        {
            return;
        }
        try
        {
            Reader reader = new BufferedReader(new FileReader(file));
            RSSParser parser = new RSSParser(reader);
            parser.start();

            List magnetList = parser.getMagnets();
            Iterator iter = magnetList.iterator();
        
            // Sync subscription operation with a possible rescan process, this 
            // prevents downloads of files already existing but not yet scanned.
            FileRescanRunner.sync();

            while (iter.hasNext())
            {
                String magnet = (String) iter.next();
                URI uri = new URI( magnet, true );
                downloadUri( uri );
            }

        }
        catch (IOException exp)
        {
            //NLogger.error( InternalFileHandler.class, exp.getMessage(), exp);
        }
    }
    
    public static void metalinkReadout( File metalinkFile )
    {
        if ( !metalinkFile.exists() )
        {
            return;
        }
        List<URI> magnetList = MetalinkParser.parseMagnetUriFromFile( metalinkFile );
        if ( magnetList == null || magnetList.isEmpty() )
        {
            return;
        }

        // Sync subscription operation with a possible rescan process, this 
        // prevents downloads of files already existing but not yet scanned.
        FileRescanRunner.sync();

        for ( URI magnet : magnetList )
        {
            downloadUri( magnet );
        }
    }

    public static void scheduledReadout( URI uri, long time )
    {
        // this should download again after a certain time. 
    	downloadUri( uri );
    }

    private static void downloadUri( URI uri )
    {
        try
        {
            Servent.getInstance().getDownloadService().addFileToDownload( uri, true );
        }
        catch (IOException exp)
        {
            //NLogger.warn( InternalFileHandler.class, exp.getMessage(), exp);
        }
    }

    /**
     * Download the uri to the specified relative download dir
     *  
     * @param uri: A magnet uri
     * @param relativeDownloadDir: The name of the target download dir inside the default download dir. 
     */
    private static void downloadUri( URI uri, String relativeDownloadDir )
    {
        try
        {
            Servent.getInstance().getDownloadService().addFileToDownload( uri, relativeDownloadDir, true );
        }
        catch (IOException exp)
        {
            //NLogger.warn( InternalFileHandler.class, exp.getMessage(), exp);
        }
    }


}
