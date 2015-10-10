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
 *  Created on 23.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.parser;

public class ParserUtils
{
    public static byte[] fromHexBinary( String s )
    {
        int len = s.length();
        if ( len % 2 != 0 ) return null;
        byte out[] = new byte[len / 2];
        for ( int i = 0; i < len; i += 2 )
        {
            int h = hexToBin( s.charAt( i ) );
            int l = hexToBin( s.charAt( i + 1 ) );
            if ( h == -1 || l == -1 ) return null;
            out[i / 2] = (byte) (h * 16 + l);
        }

        return out;
    }

    public static String collapseWhiteSpace( String text )
    {
        int len = text.length();
        int s;
        for ( s = 0; s < len; s++ )
            if ( isWhiteSpace( text.charAt( s ) ) ) break;

        if ( s == len ) return text;
        StringBuffer result = new StringBuffer( len );
        if ( s != 0 )
        {
            for ( int i = 0; i < s; i++ )
                result.append( text.charAt( i ) );

            result.append( ' ' );
        }
        boolean inStripMode = true;
        for ( int i = s + 1; i < len; i++ )
        {
            char ch = text.charAt( i );
            boolean b = isWhiteSpace( ch );
            if ( !inStripMode || !b )
            {
                inStripMode = b;
                if ( inStripMode )
                    result.append( ' ' );
                else
                    result.append( ch );
            }
        }

        len = result.length();
        if ( len > 0 && result.charAt( len - 1 ) == ' ' )
            result.setLength( len - 1 );
        return result.toString();
    }

    private static final boolean isWhiteSpace( char ch )
    {
        if ( ch > ' ' )
            return false;
        else
            return ch == '\t' || ch == '\n' || ch == '\r' || ch == ' ';
    }

    private static int hexToBin( char ch )
    {
        if ( '0' <= ch && ch <= '9' ) return ch - 48;
        if ( 'A' <= ch && ch <= 'F' ) return (ch - 65) + 10;
        if ( 'a' <= ch && ch <= 'f' )
            return (ch - 97) + 10;
        else
            return -1;
    }
}
