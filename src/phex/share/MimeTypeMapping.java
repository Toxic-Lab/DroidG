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

package phex.share;

import java.util.*;

/**
 * Currently MIME types are only used when a upload from a web browser is done.
 * Therefore initialization of this class only happens in this case.
 */
public final class MimeTypeMapping
{
    private static String MIMETYPE_TEXT_PLAIN = "text/plain";
    private static String MIMETYPE_TEXT_RICHTEXT = "text/richtext";
    private static String MIMETYPE_TEXT_HTML = "text/html";
    private static String MIMETYPE_APPL_ZIP = "application/zip";
    private static String MIMETYPE_APPL_RAR = "application/rar";
    private static String MIMETYPE_APPL_GZIP = "application/gzip";
    private static String MIMETYPE_APPL_TAR = "application/tar";
    private static String MIMETYPE_APPL_SIT = "application/sit";
    private static String MIMETYPE_APPL_TGZ = "application/tgz";
    private static String MIMETYPE_APPL_GZ = "application/gz";
    private static String MIMETYPE_APPL_MSWORD = "application/msword";
    private static String MIMETYPE_APPL_MSEXCEL = "application/msexcel";
    private static String MIMETYPE_APPL_PS = "application/postscript";
    private static String MIMETYPE_APPL_PDF = "application/pdf";
    private static String MIMETYPE_APPL_BINARY = "application/binary";
    private static String MIMETYPE_IMG_GIF = "image/gif";
    private static String MIMETYPE_IMG_JPEG = "image/jpeg";
    private static String MIMETYPE_IMG_TIFF = "image/tiff";
    private static String MIMETYPE_IMG_PNG = "image/png";
    private static String MIMETYPE_AUDIO_AIFF = "audio/aiff";
    private static String MIMETYPE_AUDIO_BASIC = "audio/basic";
    private static String MIMETYPE_AUDIO_WAV = "audio/wav";
    private static String MIMETYPE_AUDIO_MP3 = "audio/mp3";
    private static String MIMETYPE_VIDEO_MSVIDEO = "video/x-msvideo";
    private static String MIMETYPE_VIDEO_QUICKTIME = "video/quicktime";
    private static String MIMETYPE_VIDEO_MPEG = "video/mpeg";

    private static HashMap<String, String> mimeTypeMapping;

    static
    {
        mimeTypeMapping = new HashMap<String, String>( 50 );
        mimeTypeMapping.put("txt", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("ini", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("c", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("h", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("cpp", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("cxx", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("cc", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("chh", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("java", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("csv", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("bat", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("cmd", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("sh", MIMETYPE_TEXT_PLAIN );
        mimeTypeMapping.put("rtf", MIMETYPE_TEXT_RICHTEXT );
        mimeTypeMapping.put("rtx", MIMETYPE_TEXT_RICHTEXT );
        mimeTypeMapping.put("html", MIMETYPE_TEXT_HTML );
        mimeTypeMapping.put("htm", MIMETYPE_TEXT_HTML );
        mimeTypeMapping.put("zip", MIMETYPE_APPL_ZIP );
        mimeTypeMapping.put("rar", MIMETYPE_APPL_RAR );
        mimeTypeMapping.put("gzip", MIMETYPE_APPL_GZIP );
        mimeTypeMapping.put("sit", MIMETYPE_APPL_SIT );
        mimeTypeMapping.put("tgz", MIMETYPE_APPL_TGZ );
        mimeTypeMapping.put("gz", MIMETYPE_APPL_GZ );
        mimeTypeMapping.put("tar", MIMETYPE_APPL_TAR );
        mimeTypeMapping.put("gif", MIMETYPE_IMG_GIF);
        mimeTypeMapping.put("jpeg", MIMETYPE_IMG_JPEG);
        mimeTypeMapping.put("jpg", MIMETYPE_IMG_JPEG);
        mimeTypeMapping.put("tiff", MIMETYPE_IMG_TIFF );
        mimeTypeMapping.put("tif", MIMETYPE_IMG_TIFF );
        mimeTypeMapping.put("png", MIMETYPE_IMG_PNG );
        mimeTypeMapping.put("aiff", MIMETYPE_AUDIO_AIFF );
        mimeTypeMapping.put("aif", MIMETYPE_AUDIO_AIFF );
        mimeTypeMapping.put("au", MIMETYPE_AUDIO_BASIC );
        mimeTypeMapping.put("wav", MIMETYPE_AUDIO_WAV );
        mimeTypeMapping.put("mp3", MIMETYPE_AUDIO_MP3 );
        mimeTypeMapping.put("avi", MIMETYPE_VIDEO_MSVIDEO );
        mimeTypeMapping.put("mov", MIMETYPE_VIDEO_QUICKTIME );
        mimeTypeMapping.put("qt", MIMETYPE_VIDEO_QUICKTIME );
        mimeTypeMapping.put("mpeg", MIMETYPE_VIDEO_MPEG );
        mimeTypeMapping.put("mpg", MIMETYPE_VIDEO_MPEG );
        mimeTypeMapping.put("doc", MIMETYPE_APPL_MSWORD );
        mimeTypeMapping.put("xls", MIMETYPE_APPL_MSEXCEL );
        mimeTypeMapping.put("ps", MIMETYPE_APPL_PS );
        mimeTypeMapping.put("pdf", MIMETYPE_APPL_PDF );
        mimeTypeMapping.put("exe", MIMETYPE_APPL_BINARY);
        mimeTypeMapping.put("dll", MIMETYPE_APPL_BINARY);
        mimeTypeMapping.put("class", MIMETYPE_APPL_BINARY);
        mimeTypeMapping.put("jar", MIMETYPE_APPL_BINARY);
    }

    /**
     * Returns the corresponding MIME type to the given extension.
     * If no MIME type was found it returns applications/binary.
     */
    public static String getMimeTypeForExtension( String ext )
    {
        String mimeType = mimeTypeMapping.get( ext.toLowerCase() );
        if ( mimeType == null )
        {
            mimeType = MIMETYPE_APPL_BINARY;
        }
        return mimeType;
    }

    private MimeTypeMapping()
    {
    }
}