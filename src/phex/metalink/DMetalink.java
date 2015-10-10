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

// File from:
// Rev.: Revision 63 - Thu Feb 8 13:06:41 2007 UTC 
// http://metalinks.svn.sourceforge.net/viewvc/metalinks/extra-tools/java/saxparser/DMetalink.java?view=log


/*
    This file is part of the saxparser for Java from the Metalinks tools project
    Copyright (C) 2007  A. Bram Neijt <bneijt@gmail.com>

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public License
    as published by the Free Software Foundation; either version 2
    of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/


package phex.metalink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import phex.utils.StringUtils;
import phex.utils.URLCodecUtils;

import com.bitzi.util.Base32;



/** 
 * Metalink data container class
*/

public class DMetalink
{
    /**
     * The workhorse of the system
     */
    public class FileEntry
    {
        public HashMap<String, String> hashes;
        public ArrayList<UrlEntry> urls;
        public String filename;

        public FileEntry(String name)
        {
            filename = name;
            hashes = new HashMap<String, String> ();
            urls = new ArrayList<UrlEntry> ();
        }
        
        public String sha1()
        {
            return hashes.get("sha1");
        }
        
        public String getSha1Urn()
        {
            String sha1Data = hashes.get( "sha1" );
            if ( StringUtils.isEmpty( sha1Data ) )
            {
                return null;
            }
            if ( sha1Data.length() == 40 )
            {
                sha1Data = encodeSha1( sha1Data );
            }
            else if ( sha1Data.length() != 32 )
            {// unknown SHA1 format
                return null;
            }
            return "urn:sha1:" + sha1Data;
        }
        
        /* can this be improved?? */
        private String encodeSha1( String digest )
        {
            byte[] chars = new byte[20];
            for ( int i = 0; i < digest.length(); i+=2 )
            {
                int hn = Byte.parseByte (digest.substring(i, i+1), 16) << 4;
                int ln = Byte.parseByte (digest.substring(i+1, i+2), 16);
                int b = hn + ln;
                chars[i/2] = (byte) b;
            }
            return Base32.encode(chars);
        }
        
        public String magnet( )
        {
            String xtPart = null;
            String sha1Urn = getSha1Urn();
            if ( !StringUtils.isEmpty( sha1Urn ) )
            {
                xtPart = "xt=" + sha1Urn;
            }
            
            String dnPart = null;
            if ( !StringUtils.isEmpty( filename ) )
            {
                dnPart = "dn=" + URLCodecUtils.encodeURL( filename );
            }
            
            StringBuffer altSourceBuf = new StringBuffer();
            for ( DMetalink.UrlEntry urlEntry : urls )
            {
                if ( urlEntry.type.equals( "http" ) )
                {
                    if ( altSourceBuf.length() > 0 )
                    {
                        altSourceBuf.append( "&" );
                    }
                    altSourceBuf.append( "as=" );
                    altSourceBuf.append( URLCodecUtils.encodeURL( urlEntry.url ) );
                } 
            }
            
            StringBuffer magnetBuffer = new StringBuffer();
            if ( !StringUtils.isEmpty( xtPart ) )
            {
                magnetBuffer.append( xtPart );
            }
            if ( !StringUtils.isEmpty( dnPart ) )
            {
                if ( magnetBuffer.length() > 0 )
                {
                    magnetBuffer.append( "&" );
                }
                magnetBuffer.append( xtPart );
            }
            if ( altSourceBuf.length() > 0 )
            {
                if ( magnetBuffer.length() > 0 )
                {
                    magnetBuffer.append( "&" );
                }
                magnetBuffer.append( altSourceBuf );
            }
            
            if ( magnetBuffer.length() == 0 )
            {
                return null;
            }
            
            magnetBuffer.insert( 0, "magnet:?" );
            
            return magnetBuffer.toString();
        }
    }

    public class UrlEntry
    {
        public String type;
        public String url;

        public UrlEntry ( String type, String url )
        {
            this.type = type; this.url = url;
        }
    }

    public ArrayList<FileEntry> files;

    public DMetalink()
    {
        files = new ArrayList<FileEntry> ();
    }

    /** 
     * Start a new file, with a given filename
     */
    public void newFile ( String filename )
    {
        files.add ( new FileEntry (filename) );
    }

    //Add a link to the last started file
    public void addURL ( String proto, String url )
    {
        //TODO allow for type, preference etc.
        FileEntry f = files.get (files.size() - 1);
        f.urls.add (new UrlEntry(proto, url));
    }

    public void addHash (String type, String value)
    {
        FileEntry f = files.get (files.size() - 1);
        f.hashes.put (type, value);
    }

    @Override
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( files.size() + " files known\n" );

        for (FileEntry f : files)
        {
            buffer.append( "filename=" + f.filename + "\n" );
            for( UrlEntry u : f.urls )
            {
                buffer.append( "  url= " + u.url + "\n" );
            }
            
            Set<Entry<String, String>> hashEntrySet = f.hashes.entrySet();
            for ( Entry<String, String> hash : hashEntrySet )
            {
                buffer.append( "  hash(" + hash.getKey() + ")= " + hash.getValue() + "\n" );
            }
        }
        return buffer.toString();
    }
}
