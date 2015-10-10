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
package phex.http;

import java.util.StringTokenizer;


public class XQueueParameters
{
    private static final int DEFAULT_POLL_MIN = 60 * 1000;
    private static final int DEFAULT_POLL_MAX = 120 * 1000;

    private int position;
    private int length;
    private int limit;
    private int pollMin;
    private int pollMax;

    /**
     *
     * @param position the position in the waiting queue.
     * @param length the length of the waiting queue.
     * @param limit number of concurrent uploads allowed.
     * @param pollMin min poll time in millis.
     * @param pollMax max poll time in millis.
     */
    public XQueueParameters( int position, int length, int limit, int pollMin,
        int pollMax )
    {
        this.position = position;
        this.length = length;
        this.limit = limit;
        this.pollMin = pollMin;
        this.pollMax = pollMax;
    }

    public int getPosition()
    {
        return position;
    }

    /**
     * Returns the time in mills the Worker should sleep till next connection try.
     * @return the time in mills the Worker should sleep till next connection try.
     */
    public int getRequestSleepTime()
    {
        // this is ( pollMax / 2 ) / 5
        // and gives a gradient for 5 positions that is below pollMax / 2
        int m = pollMax / 10;
        int func = m * (position - 1) + pollMin + 1000;
        return Math.min( (pollMax + pollMin) / 2, func );
    }

    public void update( XQueueParameters updateParameters )
    {// updates all parameters that are set ( not -1 )
        if ( updateParameters.position != -1 )
        {
            position = updateParameters.position;
        }

        if ( updateParameters.length != -1 )
        {
            length = updateParameters.length;
        }

        if ( updateParameters.limit != -1 )
        {
            limit = updateParameters.limit;
        }

        if ( updateParameters.pollMin != -1 )
        {
            pollMin = updateParameters.pollMin;
        }

        if ( updateParameters.pollMax != -1 )
        {
            pollMax = updateParameters.pollMax;
        }
    }

    public String buildHTTPString()
    {
         return "position=" + position
            + ",length=" + length
            + ",limit=" + limit
            + ",pollMin=" + pollMin
            + ",pollMax=" + pollMax;
    }

    /**
     * Trys to parse the http X-Queue value.
     * Returns null if there is a parsing error.
     * @param httpXQueueValue the http header value for X-Queue
     * @return a XQueueParamerters object or null if there is a parsing error.
     */
    public static XQueueParameters parseXQueueParameters( String httpXQueueValue )
    {
        // Parse a X-Queue value in the following format:
        // 'position=2,length=5,limit=4,pollMin=45,pollMax=120'
        // The parameters can be ordered differently.

        StringTokenizer tokenizer = new StringTokenizer( httpXQueueValue, "," );

        int queuePosition = -1;
        int queueLength = -1;
        int queueLimit = -1;
        int queuePollMin = -1;
        int queuePollMax = -1;
        String lowerCaseToken;
        try
        {
            while( tokenizer.hasMoreTokens() )
            {
                lowerCaseToken = tokenizer.nextToken().trim().toLowerCase();
                if ( lowerCaseToken.startsWith( "position" ) )
                {
                    queuePosition = parseIntValue( lowerCaseToken );
                }
                else if ( lowerCaseToken.startsWith( "length" ) )
                {
                    queueLength = parseIntValue( lowerCaseToken );
                }
                else if ( lowerCaseToken.startsWith( "limit" ) )
                {
                    queueLimit = parseIntValue( lowerCaseToken );
                }
                else if ( lowerCaseToken.startsWith( "pollmin" ) )
                {
                    queuePollMin = parseIntValue( lowerCaseToken );
                }
                else if ( lowerCaseToken.startsWith( "pollmax" ) )
                {
                    queuePollMax = parseIntValue( lowerCaseToken );
                }
            }
        }
        catch ( NumberFormatException exp )
        {
//        	NLogger.debug( XQueueParameters.class,
//                "Invalid X-Queue value: " + httpXQueueValue );
            return null;
        }
        catch ( IndexOutOfBoundsException exp )
        {
//        	NLogger.debug( XQueueParameters.class,
//                "Invalid X-Queue value: " + httpXQueueValue );
            return null;
        }

        if ( queuePollMin == -1 && queuePollMax == -1)
        {
            queuePollMin = DEFAULT_POLL_MIN;
            queuePollMax = DEFAULT_POLL_MAX;
        }
        else if ( queuePollMin == -1 )
        {
            // convert to millis
            queuePollMax *= 1000;
            queuePollMin = Math.min( DEFAULT_POLL_MIN, queuePollMax / 2 );
        }
        else if ( queuePollMax == -1 )
        {
            // convert to millis
            queuePollMin *= 1000;
            queuePollMin = Math.max( DEFAULT_POLL_MAX, queuePollMin );
        }
        else
        {
            // convert to millis
            queuePollMin *= 1000;
            queuePollMax *= 1000;
        }

        XQueueParameters queueParameters = new XQueueParameters( queuePosition,
            queueLength, queueLimit, queuePollMin, queuePollMax );
        return queueParameters;
    }

    private static int parseIntValue( String line )
    {
        int idx = line.indexOf( '=' );
        String value = line.substring( idx + 1 ).trim();
        return Integer.parseInt( value );
    }
}