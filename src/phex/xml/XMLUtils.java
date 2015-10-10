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
package phex.xml;

import java.io.*;
import java.io.Writer;

/**
 * 
 */
public class XMLUtils
{
    /**
     * Writes a string XML encoded to a writer.
     */
    public static void writeEncoded( Writer out, String str )
        throws IOException
    {
        for (int i = 0; i < str.length(); i++)
        {
            char ch = str.charAt(i);
            switch (ch)
            {
                case '<': out.write("&lt;");
                    break;
                case '>': out.write("&gt;");
                    break;
                case '&': out.write("&amp;");
                    break;
                case '"': out.write("&quot;");
                    break;
                case '\'': out.write("&apos;");
                    break;
                case '\r':
                case '\n':
                    out.write(ch);
                    break;
                default:
                    if (((int) ch < 32) || ((int) ch > 126))
                    {
                        out.write("&#x");
                        out.write(Integer.toString((int) ch, 16));
                        out.write(';');
                    }
                    else
                    {
                        out.write(ch);
                    }
            }
        }
    }
    
    /**
     * Returns true if the argument, a UCS-4 character code, is valid in
     * XML documents.  Unicode characters fit into the low sixteen
     * bits of a UCS-4 character, and pairs of Unicode <em>surrogate
     * characters</em> can be combined to encode UCS-4 characters in
     * documents containing only Unicode.  (The <code>char</code> datatype
     * in the Java Programming Language represents Unicode characters,
     * including unpaired surrogates.)
     *
     * <P> In XML, UCS-4 characters can also be encoded by the use of
     * <em>character references</em> such as <b>&amp;#x12345678;</b>, which
     * happens to refer to a character that is disallowed in XML documents.
     * UCS-4 characters allowed in XML documents can be expressed with
     * one or two Unicode characters.
     *
     * @param ucs4char The 32-bit UCS-4 character being tested.
     */
    static public boolean isXmlChar(int ucs4char)
    {
    // [2] Char ::= #x0009 | #x000A | #x000D
    //          | [#x0020-#xD7FF]
    //  ... surrogates excluded!
    //          | [#xE000-#xFFFD]
    //          | [#x10000-#x10ffff]
    return ((ucs4char >= 0x0020 && ucs4char <= 0xD7FF)
        || ucs4char == 0x000A || ucs4char == 0x0009
        || ucs4char == 0x000D
        || (ucs4char >= 0xE000 && ucs4char <= 0xFFFD)
        || (ucs4char >= 0x10000 && ucs4char <= 0x10ffff));
    }
}
