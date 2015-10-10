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
 */
package phex.common;

import java.util.*;

public class MediaType
{
    public static final String MEDIA_TYPE_ANY = "AnyMediaType";
    public static final String MEDIA_TYPE_AUDIO = "AudioMediaType";
    public static final String MEDIA_TYPE_VIDEO = "VideoMediaType";
    public static final String MEDIA_TYPE_PROGRAM = "ProgramMediaType";
    public static final String MEDIA_TYPE_IMAGES = "ImagesMediaType";
    public static final String MEDIA_TYPE_DOCUMENTS = "DocumentsMediaType";
    public static final String MEDIA_TYPE_ROMS = "RomsMediaType";
    public static final String MEDIA_TYPE_OPEN_FORMATS = "OpenFormatsType";
    public static final String MEDIA_TYPE_META = "MetaMediaType";
    
    public static final String[] AUDIO_FILE_EXT = new String[] { 
        "aif", "aifc", "aiff", "ape", "apl", "au", "iso", "lqt",
        "mac", "med", "mid", "midi", "mod", "mp3", "mpa", "mpga", "mp1",
        "ogg", "ra", "ram", "rm", "rmi", "rmj", "snd", "vqf", 
        "wav", "wma" };
    public static final String[] VIDEO_FILE_EXT = new String[] { 
        "asf", "avi", "dcr", "div", "divx", "dv", "dvd", "ogm",
        "dvx", "flc", "fli", "flv", "flx", "jve", "m2p", "m2v", "m1v", "mkv", "mng",
        "mov", "mp2", "mp2v", "mp4", "mpe", "mpeg", "mpg", "mpv", "mpv2",
        "nsv", "ogg", "ram", "rm", "rv", "smi", "smil", "swf", "qt", "vcd",
        "vob", "vrml", "wml", "wmv" };
    public static final String[] PROGRAM_FILE_EXT = new String[] { 
        "7z", "ace", "arj", "awk", "bin", "bz2", "cab", "csh",
        "deb", "dmg", "img", "exe", "gz", "gzip", "hqx", "iso", "jar", "lzh", 
        "lha", "mdb", "msi", "msp", "pl", "rar", "rpm", "sh", "shar", "sit",
        "tar", "tgz", "taz", "z", "zip", "zoo", "pl", "ebuild" };
    public static final String[] IMAGE_FILE_EXT = new String[] { 
        "ani", "bmp", "cpt", "cur", "dcx", "dib", "drw",
        "emf", "fax", "gif", "icl", "ico", "iff", "ilbm", "img", "jif",
        "jiff", "jpe", "jpeg", "jpg", "lbm", "mac", "mic", "pbm", "pcd",
        "pct", "pcx", "pic", "png", "pnm", "ppm", "psd", "ras", "rgb", "rle",
        "sgi", "sxd", "svg", "tga", "tif", "tiff", "wmf", "wpg", "xbm", "xcf", 
        "xpm", "xwd" };
    public static final String[] DOCUMENTS_FILE_EXT = new String[] {
        "ans", "asc", "chm", "csv", "dif", "diz", "doc", "eml",
        "eps", "epsf", "hlp", "html", "htm", "info", "latex", "man", "mcw",
        "mht", "mhtml", "odt", "pdf", "ppt", "ps", "rtd", "rtf", "rtt", "sxw", "sxc", 
        "tex", "texi", "txt", "wk1", "wps", "wri", "xhtml", "xls", "xml", "sla", "kwd"
    };
    public static final String[] ROMS_FILE_EXT = new String[] {
        "bin", "smd", "smc", "fig", "srm", "zip", "sav", "rar", "frz", "fra", 
        "zs1", "pcx"
    };
    public static final String[] OPEN_FORMATS_FILE_EXT = new String[] {
        "ogg", "ogm", "tgz", "gz", "tbz", "bz2", "bz", "png", 
        "flac", "tar", "gzip", "txt", "mkv", "odt", "sla", "kwd", "tex"
    };
    public static final String[] META_FILE_EXT = new String[] {
        "magma", "xml", "collection", "torrent", "col"
    };

    private static MediaType[] allMediaTypes;
    private static MediaType streamableMediaTypes;

    static
    {
        allMediaTypes = new MediaType[ 9 ];
        // the any media type must be on index 0 for method getMediaTypeAny()
        allMediaTypes[ 0 ] = new MediaType( MEDIA_TYPE_ANY, (Set<String>)null );
        allMediaTypes[ 1 ] = new MediaType( MEDIA_TYPE_AUDIO, AUDIO_FILE_EXT );
        allMediaTypes[ 2 ] = new MediaType( MEDIA_TYPE_VIDEO, VIDEO_FILE_EXT );
        allMediaTypes[ 3 ] = new MediaType( MEDIA_TYPE_PROGRAM, PROGRAM_FILE_EXT );
        allMediaTypes[ 4 ] = new MediaType( MEDIA_TYPE_IMAGES, IMAGE_FILE_EXT );
        allMediaTypes[ 5 ] = new MediaType( MEDIA_TYPE_DOCUMENTS, DOCUMENTS_FILE_EXT );
        allMediaTypes[ 6 ] = new MediaType( MEDIA_TYPE_ROMS, ROMS_FILE_EXT );
        allMediaTypes[ 7 ] = new MediaType( MEDIA_TYPE_OPEN_FORMATS, OPEN_FORMATS_FILE_EXT );
        allMediaTypes[ 8 ] = new MediaType( MEDIA_TYPE_META, META_FILE_EXT  );
        
        // media types not in allMediaTypes array to hide it from UI
        
        // we assume all audio and video media types are streamable...
        Set<String> concatSet = new TreeSet<String>();
        concatSet.addAll( Arrays.asList( AUDIO_FILE_EXT ) );
        concatSet.addAll( Arrays.asList( VIDEO_FILE_EXT ) );
        streamableMediaTypes = new MediaType( "", concatSet );
    }

    private String name;
    private Set<String> fileExtSet;
    private String fileTypesView;

    private MediaType( String aName, String[] fileExtArray )
    {
        name = aName;
        fileExtSet = new TreeSet<String>( Arrays.asList( fileExtArray ) );
    }
    
    private MediaType( String aName, Set<String> fileExtSet )
    {
        name = aName;
        this.fileExtSet = fileExtSet;
    }

    public String getName()
    {
        return name;
    }

    public String getFileTypesUIText()
    {
        if ( fileTypesView == null )
        {
            if ( fileExtSet == null || fileExtSet.size() == 0)
            {
                return "";
            }
            StringBuffer buffer = new StringBuffer( fileExtSet.size() * 5 );
            buffer.append( "<html>" );
            int charCount = 0;
            Iterator iterator = fileExtSet.iterator();
            while( iterator.hasNext() )
            {
                String val = (String) iterator.next();
                buffer.append( val );
                charCount += val.length();
                if ( iterator.hasNext() )
                {
                    buffer.append( ", " );
                    charCount += 2;
                }
                if ( charCount > 50 )
                {
                    charCount = 0;
                    buffer.append ( "<br>" );
                }
            }
            fileTypesView = buffer.toString();
        }
        return fileTypesView;
    }

    /**
     * Verifys that the extension is a extension of this media type.
     * Returnes true if the given extension is a extension of this media type.
     */
    public boolean isExtensionOf( String extension )
    {
        // this is for any media type
        if ( fileExtSet == null )
        {
            return true;
        }
        extension = extension.toLowerCase();
        return fileExtSet.contains(extension);
    }

    /**
     * Verifys that the extension of the given filename is a extension of this
     * media type.
     * Returnes true if the given filename extension is a extension of this media type.
     */
    public boolean isFilenameOf( String filename )
    {
        // this is for any media type
        if ( fileExtSet == null )
        {
            return true;
        }

        int index = filename.lastIndexOf(".");
        // if no '.' or index is last char of the file name return false
        if (index == -1 || index == filename.length() )
        {
            return false;
        }
        String extension = filename.substring( index + 1, filename.length() );
        return isExtensionOf( extension );
    }
    
    
    
    public static MediaType getStreamableMediaType()
    {
        return streamableMediaTypes;
    }

    public static MediaType[] getAllMediaTypes()
    {
        return allMediaTypes;
    }

    public static MediaType getMediaTypeAny()
    {
        // first element in all array...
        return allMediaTypes[0];
    }
    
    public static MediaType getAudioMediaType()
    {
        return allMediaTypes[1];
    }
    
    public static MediaType getVideoMediaType()
    {
        return allMediaTypes[2];
    }
    
    public static MediaType getProgramMediaType()
    {
        return allMediaTypes[3];
    }
    
    public static MediaType getImageMediaType()
    {
        return allMediaTypes[4];
    }
    
    public static MediaType getDocumentsMediaType()
    {
        return allMediaTypes[5];
    }
    
    public static MediaType getRomsMediaType()
    {
        return allMediaTypes[6];
    }
    
    public static MediaType getOpenFormatsMediaType()
    {
        return allMediaTypes[7];
    }
    
    public static MediaType getMetaMediaType()
    {
        return allMediaTypes[8];
    }
    
    public static MediaType getNoWmvMovAsfMediaType()
    {
        return allMediaTypes[9];
    }
    
    public static MediaType getTypeForName( String name )
    {
        if ( name.equals( MEDIA_TYPE_ANY ) ) return getMediaTypeAny();
        else if ( name.equals( MEDIA_TYPE_AUDIO ) ) return getAudioMediaType();
        else if ( name.equals( MEDIA_TYPE_VIDEO ) ) return getVideoMediaType();
        else if ( name.equals( MEDIA_TYPE_PROGRAM ) ) return getProgramMediaType();
        else if ( name.equals( MEDIA_TYPE_IMAGES ) ) return getImageMediaType();
        else if ( name.equals( MEDIA_TYPE_DOCUMENTS ) ) return getDocumentsMediaType();
        else if ( name.equals( MEDIA_TYPE_ROMS ) ) return getRomsMediaType();
        else if ( name.equals( MEDIA_TYPE_OPEN_FORMATS ) ) return getOpenFormatsMediaType();
        else if ( name.equals( MEDIA_TYPE_META ) ) return getMetaMediaType();
        else return null;
    }
}
