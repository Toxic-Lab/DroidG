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
package phex.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A collection of custom utilities to modify or specially display Strings.
 **/
public final class StringUtils
{
    public static final String ENCODING_ISO_8859_1 = "ISO-8859-1";
    public static final String ENCODING_US_ASCII = "US-ASCII";
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final String FILE_DELIMITERS = " -,._+/*()[]\\";
    
    
    private StringUtils()
    {}
    
    /**
     * Returns the bytes of the string in ENCODING_ISO_8859_1.
     * In case a exception is raised standard s.getBytes() is called.
     * @param s
     * @return a byte[] representation of the string in ISO_8859_1 encoding.
     */
    public static byte[] getBytes(String s)
    {
        try
        {
            return s.getBytes( ENCODING_ISO_8859_1 );
        }
        catch ( UnsupportedEncodingException e )
        {
            return s.getBytes();
        }
    }
    
    /**
     * Returns the bytes of the string in ENCODING_US_ASCII.
     * In case a exception is raised standard s.getBytes() is called.
     * @param s
     * @return a byte[] representation of the string in US-ASCII encoding.
     */
    public static byte[] getBytesInUsAscii(String s)
    {
        try
        {
            return s.getBytes( ENCODING_US_ASCII );
        }
        catch ( UnsupportedEncodingException e )
        {
            return s.getBytes();
        }
    }
    
    /**
     * Checks if the given character <code>c</code> is a file delimiter.
     * @param c the character to check.
     * @return true if <code>c</code> is a file delimiter, false otherwise.
     */
    public static boolean isFileDelimiter( char c )
    {
        switch ( c )
        {
        case ' ':
        case '-':
        case ',':
        case '.':
        case '_':
        case '+':
        case '/':
        case '*':
        case '(':
        case ')':
        case '[':
        case ']':
        case '\\':
            return true;
        default:
            return false;
        }
    }
    
    /**
     * <p>Checks if a String is empty ("") or null.</p>
     *
     *
     * @param val  the String to check, may be null
     * @return <code>true</code> if the String is empty or null
     */
    public static boolean isEmpty( String val )
    {
        return ( val == null || val.length() == 0 );
    }

    /**
     * Convert all file delimiter characters to spaces. 
     * This should be used to create search-terms from filenames.
     **/
    public static String createNaturalSearchTerm( String searchTerm )
    {
        return replaceChars( searchTerm, FILE_DELIMITERS, ' ' );
    }
    
    /**
     * <p>Compares two Strings, returning <code>true</code> if they are equal.</p>
     *
     * <p><code>null</code>s are handled without exceptions. Two <code>null</code>
     * references are considered to be equal. The comparison is case sensitive.</p>
     *
     * <pre>
     * StringUtils.equals(null, null)   = true
     * StringUtils.equals(null, "abc")  = false
     * StringUtils.equals("abc", null)  = false
     * StringUtils.equals("abc", "abc") = true
     * StringUtils.equals("abc", "ABC") = false
     * </pre>
     *
     * @see java.lang.String#equals(Object)
     * @param str1  the first String, may be null
     * @param str2  the second String, may be null
     * @return <code>true</code> if the Strings are equal, case sensitive, or
     *  both <code>null</code>
     *  
     * Taken from org.apache.commons.lang.StringUtils
     */
    public static boolean equals(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equals(str2);
    }

    
    /**
     * <p>Joins the elements of the provided array into a single String
     * containing the provided list of elements.</p>
     *
     * <p>No delimiter is added before or after the list.
     * A <code>null</code> separator is the same as an empty String ("").
     * Null objects or empty strings within the array are represented by
     * empty strings.</p>
     *
     * <pre>
     * StringUtils.join(null, *)                = null
     * StringUtils.join([], *)                  = ""
     * StringUtils.join([null], *)              = ""
     * StringUtils.join(["a", "b", "c"], "--")  = "a--b--c"
     * StringUtils.join(["a", "b", "c"], null)  = "abc"
     * StringUtils.join(["a", "b", "c"], "")    = "abc"
     * StringUtils.join([null, "", "a"], ',')   = ",,a"
     * </pre>
     *
     * @param array  the array of values to join together, may be null
     * @param separator  the separator character to use, null treated as ""
     * @return the joined String, <code>null</code> if null array input
     * 
     * Taken from org.apache.commons.lang.StringUtils
     */
    public static String join(Object[] array, String separator)
    {
        if ( array == null ) { return null; }
        if ( separator == null )
        {
            separator = "";
        }
        int arraySize = array.length;

        // ArraySize ==  0: Len = 0
        // ArraySize > 0:   Len = NofStrings *(len(firstString) + len(separator))
        //           (Assuming that all Strings are roughly equally long)
        int bufSize = ((arraySize == 0) ? 0
            : arraySize
                * ((array[0] == null ? 16 : array[0].toString().length()) + separator.length()));

        StringBuffer buf = new StringBuffer( bufSize );

        for (int i = 0; i < arraySize; i++)
        {
            if ( i > 0 )
            {
                buf.append( separator );
            }
            if ( array[i] != null )
            {
                buf.append( array[i] );
            }
        }
        return buf.toString();
    }
    
    /**
     * <p>Replaces a String with another String inside a larger String,
     * for the first <code>max</code> values of the search String.</p>
     *
     * <p>A <code>null</code> reference passed to this method is a no-op.</p>
     *
     * <pre>
     * StringUtils.replace(null, *, *, *)         = null
     * StringUtils.replace("", *, *, *)           = ""
     * StringUtils.replace("any", null, *, *)     = "any"
     * StringUtils.replace("any", *, null, *)     = "any"
     * StringUtils.replace("any", "", *, *)       = "any"
     * StringUtils.replace("any", *, *, 0)        = "any"
     * StringUtils.replace("abaa", "a", null, -1) = "abaa"
     * StringUtils.replace("abaa", "a", "", -1)   = "b"
     * StringUtils.replace("abaa", "a", "z", 0)   = "abaa"
     * StringUtils.replace("abaa", "a", "z", 1)   = "zbaa"
     * StringUtils.replace("abaa", "a", "z", 2)   = "zbza"
     * StringUtils.replace("abaa", "a", "z", -1)  = "zbzz"
     * </pre>
     *
     * @param text  text to search and replace in, may be null
     * @param repl  the String to search for, may be null
     * @param with  the String to replace with, may be null
     * @param max  maximum number of values to replace, or <code>-1</code> if no maximum
     * @return the text with any replacements processed,
     *  <code>null</code> if null String input
     *  
     * Taken from org.apache.commons.lang.StringUtils
     */
    public static String replace(String text, String repl, String with, int max) 
    {
        if ( isEmpty( text ) || isEmpty( repl ) || with == null || max == 0)
        {
            return text;
        }
        int start = 0;
        int end = text.indexOf(repl, start);
        if ( end == -1 )
        {
            return text;
        }
        
        int increase = with.length() - repl.length();
        increase =  (increase < 0 ? 0 : increase );
        increase *= (max < 0 ? 16 : (max > 64 ? 64 : max ) );
        
        StringBuffer buf = new StringBuffer( text.length() + increase );
        while ( end != -1)
        {
            buf.append(text.substring(start, end)).append(with);
            start = end + repl.length();
            if (--max == 0)
            {
                break;
            }
            end = text.indexOf( repl, start );
        }
        buf.append(text.substring(start));
        return buf.toString();
    }
    
    /**
     * <p>Replaces multiple characters in a String in one go.
     * This method can also be used to delete characters.</p>
     *
     * <p>For example:<br />
     * <code>replaceChars(&quot;hello&quot;, &quot;ho&quot;, 'j') = jellj</code>.</p>
     *
     * <p>A <code>null</code> string input returns <code>null</code>.
     * An empty ("") string input returns an empty string.</p>
     *
     * <pre>
     * StringUtils.replaceChars(null, *, *)         = null
     * StringUtils.replaceChars("", *, *)           = ""
     * StringUtils.replaceChars("abc", null, *)     = "abc"
     * StringUtils.replaceChars("abc", "", *)       = "abc"
     * StringUtils.replaceChars("abcba", "bc", 'y') = "ayyya"
     * StringUtils.replaceChars("abcba", "b", 'y')  = "aycya"
     * </pre>
     *
     * @param str  String to replace characters in, may be null
     * @param searchChars  a set of characters to search for, may be null
     * @param replaceChar  the character to replace
     * @return modified String, <code>null</code> if null string input
     * 
     * Taken from org.apache.commons.lang.StringUtils (heavily modified)
     */
    public static String replaceChars(String str, String searchChars, char replaceChar ) 
    {
        if (isEmpty(str) || isEmpty(searchChars)) {
            return str;
        }
        boolean modified = false;
        int strLength = str.length();
        StringBuffer buf = new StringBuffer(strLength);
        for (int i = 0; i < strLength; i++) {
            char ch = str.charAt(i);
            int index = searchChars.indexOf(ch);
            if (index >= 0) 
            {
                modified = true;
                buf.append( replaceChar );
            }
            else
            {
                buf.append(ch);
            }
        }
        if (modified) {
            return buf.toString();
        } else {
            return str;
        }
    }
    
    /**
     * <p>Splits the provided text into an array, separators specified.
     * This is an alternative to using StringTokenizer.</p>
     *
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as one separator.
     * For more control over the split use the StrTokenizer class.</p>
     *
     * <p>A <code>null</code> input String returns <code>null</code>.
     * A <code>null</code> separatorChars splits on whitespace.</p>
     *
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("abc def", null) = ["abc", "def"]
     * StringUtils.split("abc def", " ")  = ["abc", "def"]
     * StringUtils.split("abc  def", " ") = ["abc", "def"]
     * StringUtils.split("ab:cd:ef", ":") = ["ab", "cd", "ef"]
     * </pre>
     *
     * @param str  the String to parse, may be null
     * @param separatorChars  the characters used as the delimiters,
     *  <code>null</code> splits on whitespace
     * @return an array of parsed Strings, <code>null</code> if null String input
     */
    public static String[] split(String str, String separatorChars) 
    {
        return splitWorker(str, separatorChars, -1, false);
    }
    
    /**
     * Performs the logic for the <code>split</code> and
     * <code>splitPreserveAllTokens</code> methods that return a maximum array
     * length.
     * 
     * @param str
     *            the String to parse, may be <code>null</code>
     * @param separatorChars
     *            the separate character
     * @param max
     *            the maximum number of elements to include in the array. A zero
     *            or negative value implies no limit.
     * @param preserveAllTokens
     *            if <code>true</code>, adjacent separators are treated as
     *            empty token separators; if <code>false</code>, adjacent
     *            separators are treated as one separator.
     * @return an array of parsed Strings, <code>null</code> if null String
     *         input
     */
    private static String[] splitWorker(String str, String separatorChars,
        int max, boolean preserveAllTokens)
    {
        // Performance tuned for 2.0 (JDK1.4)
        // Direct code is quicker than StringTokenizer.
        // Also, StringTokenizer uses isSpace() not isWhitespace()

        if (str == null)
        {
            return null;
        }
        int len = str.length();
        if (len == 0)
        {
            return EMPTY_STRING_ARRAY;
        }
        List<String> list = new ArrayList<String>();
        int sizePlus1 = 1;
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        if (separatorChars == null)
        {
            // Null separator means use whitespace
            while (i < len)
            {
                if (Character.isWhitespace(str.charAt(i)))
                {
                    if (match || preserveAllTokens)
                    {
                        lastMatch = true;
                        if (sizePlus1++ == max)
                        {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                else
                {
                    lastMatch = false;
                }
                match = true;
                i++;
            }
        }
        else if (separatorChars.length() == 1)
        {
            // Optimise 1 character case
            char sep = separatorChars.charAt(0);
            while (i < len)
            {
                if (str.charAt(i) == sep)
                {
                    if (match || preserveAllTokens)
                    {
                        lastMatch = true;
                        if (sizePlus1++ == max)
                        {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                else
                {
                    lastMatch = false;
                }
                match = true;
                i++;
            }
        }
        else
        {
            // standard case
            while (i < len)
            {
                if (separatorChars.indexOf(str.charAt(i)) >= 0)
                {
                    if (match || preserveAllTokens)
                    {
                        lastMatch = true;
                        if (sizePlus1++ == max)
                        {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                else
                {
                    lastMatch = false;
                }
                match = true;
                i++;
            }
        }
        if (match || (preserveAllTokens && lastMatch))
        {
            list.add(str.substring(start, i));
        }
        return list.toArray(new String[list.size()]);
    }
    
    
    
    private static Random randomizer = new Random();
    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6',
        '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    
    
    public static String generateRandomUUIDString()
    {
        byte[] uuid = new byte[16];
        randomizer.nextBytes( uuid );
        uuid[6] &= (byte) 0x0F; //0000 1111 
        uuid[6] |= (byte) 0x40; //0100 0000 set version 4 (random)
        
        uuid[8] &= (byte) 0x3F; //0011 1111
        uuid[8] |= (byte) 0x80; //1000 0000
        
        // generate string rep...
        StringBuffer buf = new StringBuffer( 36 );
        for(int i = 0; i < 16; i++)
        {
            int val = uuid[i] & 0xFF;
            buf.append( HEX_CHARS[ val >> 4 ] );
            buf.append( HEX_CHARS[ val & 0x0F ] );
        }
        buf.insert(8, '-');
        buf.insert(13, '-');
        buf.insert(18, '-');
        buf.insert(23, '-');
        // -> 00000000-0000-0000-0000-000000000000
        
        return buf.toString();        
    }
}