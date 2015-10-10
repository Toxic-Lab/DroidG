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
 *  --- CVS Information ---
 *  $Id$
 */
package phex.utils;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;

/**
 * 
 */
public class URLCodecUtils
{
    private static URLCodec urlCodec = new URLCodec();
    
    /**
     * Decodes a URL safe string into its original form. Escaped 
     * characters are converted back to their original representation.
     *
     * @param pString URL safe string to convert into its original form
     * @return original string 
     * @throws RuntimeException Thrown if URL decoding is unsuccessful.
     * 		This only happens in the case of a UnsupportedEncodingException
     * 		which should never occur in reality.
     */
    public static String decodeURL( String url )
    {
        try
        {
            return urlCodec.decode( url );
        }
        catch ( DecoderException exp )
        {
//            NLogger.error( URLCodecUtils.class, exp, exp );
            throw new RuntimeException( exp.toString() + ": " + exp.getMessage() );
        }
    }
    
    /**
     * @throws RuntimeException Thrown if URL encoding is unsuccessful.
     * 		This only happens in the case of a UnsupportedEncodingException
     * 		which should never occur in reality.
     */
    public static String encodeURL( String url )
    {
        try
        {
            return urlCodec.encode( url );
        }
        catch ( EncoderException exp )
        {
//            NLogger.error( URLCodecUtils.class, exp, exp );
            throw new RuntimeException( exp.toString() + ": " + exp.getMessage() );
        }
    }
}