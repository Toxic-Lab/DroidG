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
package phex.utils;

import java.util.Arrays;

/**
 * This is a high performance search engine. It uses the Boyer-Moore serarch
 * algorithem to find patterns in text. This algorithem outperforms the Java
 * standard brute-force search algorithem by a factor of 3:1 and more.
 * Integrated to the standard Boyer-Moore algorithm was the ability to handle
 * Java Unicode. The Unicode is hashed into a 128 byte array which could cause
 * a few extra compares because of overlapping if search patterns contain multi
 * alphabet character codes. But this will almost never happen.
 * The alogithem is NOT handling local specific unicode character coallation
 * because it slows down the performance dramatically. Also in the gnutella case
 * we are not able to determine the local of the search pattern.
 *
 * @version 1.0
 */
public class SearchEngine
{
    private final static int SKIP_TABLE_SIZE = 128;
    private char[] text;
    private char[] pattern;
    private int skipTable[];
    private int patternLength;
    private int textLength;

    public SearchEngine()
    {
        skipTable = new int[ SKIP_TABLE_SIZE ];
    }

    /**
     * Sets the text that should be search in.
     * The isLowerCase option is for optimization to skip the convert to
     * lower case. Lower case conversion must be done by yourself if set to true.
     */
    public void setText( String aText, boolean isLowerCase )
    {
        // for slight performance reasons
        textLength = aText.length();

        if ( !isLowerCase )
        {
            // conversion to lower case to save performance
            text = new char[ textLength ];
            for ( int i = 0; i < textLength; i++ )
            {
                text[i] = Character.toLowerCase( aText.charAt( i ) );
            }
        }
        else
        {
            text = aText.toCharArray();
        }
    }

    /**
     * Works with a char array instead of a string. This way the string
     * doesn't need to be copied into a char array when isLowerCase is true.
     * If isLowerCase is false this method gives no performence gain compared
     * to the setText( String, boolean ) method.
     * The call is not safe to outside changes of the textArr!
     */
    public void setText( char[] textArr, boolean isLowerCase )
    {
        // for slight performance reasons
        textLength = textArr.length;

        if ( !isLowerCase )
        {
            // conversion to lower case to save performance
            text = new char[ textLength ];
            for ( int i = 0; i < textLength; i++ )
            {
                text[i] = Character.toLowerCase( textArr[ i ] );
            }
        }
        else
        {
            text = textArr;
        }
    }

    /**
     * Sets the pattern that will be searched for in the text.
     * The isLowerCase option is for optimization to skip the convert to
     * lower case. Lower case conversion must be done by yourself if set to true.
     */
    public void setPattern( String aPattern, boolean isLowerCase )
    {
        // for slight performance reasons
        patternLength = aPattern.length();

        if ( !isLowerCase )
        {
            // conversion to lower case to save performance
            pattern = new char[ patternLength ];
            for ( int i = 0; i < patternLength; i++ )
            {
                pattern[i] = Character.toLowerCase( aPattern.charAt( i ) );
            }
        }
        else
        {
            pattern = aPattern.toCharArray();
        }
        initSkipTable();
    }

    /**
     * Initializes the skip table for Boyer-Moore
     */
    private void initSkipTable()
    {
        // init fill table
        Arrays.fill( skipTable, patternLength );

        // map available chars
        int idx;
        int val;
        char patternChar;
        for (int i = 0; i < patternLength - 1; i++)
        {
            patternChar = pattern[ i ];
            val = patternLength - i - 1;
            //idx = ((int)Character.toUpperCase( patternChar ) ) % SKIP_TABLE_SIZE;
            //skipTable[ idx ] = val;
            //idx = ((int)Character.toLowerCase( patternChar ) ) % SKIP_TABLE_SIZE;
            idx = ((int) patternChar ) % SKIP_TABLE_SIZE;
            skipTable[ idx ] = val;
        }
    }

    /**
     * Perform matching of the pattern in the text using Boyer-Moore
     * algorithm. Returns true if a match was found. False otherwise.
     */
    public boolean match()
    {
        char patternChar;
        char textChar;
        int searchIndex;
        int compareIndex;
        int textIndex;
        int skipTblIndex;
        boolean found = false;

        textIndex = searchIndex = patternLength - 1;
        while ( searchIndex >= 0 && textIndex < textLength )
        {
            searchIndex = patternLength - 1;
            compareIndex = textIndex;
            while ( true )
            {
                if ( searchIndex < 0 )
                {
                    // search string found
                    found = true;
                    break;
                }

                patternChar = pattern[ searchIndex ];
                textChar = text[ compareIndex ];

               // Also the algorithm was (not) extended to ignore case differences. This is done by
               // doing upper and lower case checks because, conversion in a single direction does
               // not work properly for some alphabets, which have strange rules about case
               // conversion.
                /*if ( /*Character.toUpperCase( patternChar ) != Character.toUpperCase( textChar ) &&
                     Character.toLowerCase( patternChar ) != Character.toLowerCase( textChar ) )*/
                if ( patternChar != textChar )
                {
                    skipTblIndex = ((int)text[ textIndex ]) % SKIP_TABLE_SIZE;
                    textIndex = textIndex + skipTable[ skipTblIndex ];
                    break;
                }

                // stepback
                searchIndex--;
                compareIndex--;
            }
        }
        return found;
    }
}