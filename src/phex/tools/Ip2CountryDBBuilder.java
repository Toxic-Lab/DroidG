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
package phex.tools;

import java.io.*;
import java.net.*;
import java.util.*;

import phex.common.address.AddressUtils;

/**
 * 
 */
public class Ip2CountryDBBuilder
{
    //ftp://ftp.apnic.net/pub/stats/<registry>/delegated-<registry>-latest
    private static final String APNIC = "ftp://ftp.apnic.net/pub/stats/apnic/delegated-apnic-latest";
    private static final String RIPE = "ftp://ftp.apnic.net/pub/stats/ripe-ncc/delegated-ripencc-latest";
    private static final String ARIN = "ftp://ftp.apnic.net/pub/stats/arin/delegated-arin-latest";
    private static final String LACNIC = "ftp://ftp.apnic.net/pub/stats/lacnic/delegated-lacnic-latest";
    private static final String AFRINIC = "ftp://ftp.apnic.net/pub/stats/afrinic/delegated-afrinic-latest";
    
    
    private static List<IpCountryRange> dataList;
    
    public static void main( String args[] )
        throws Exception
    {   
        //System.setProperty( "ftp.proxyHost", "10.127.100.68" );
        //System.setProperty( "ftp.proxyPort", "8080" );
        
        dataList = new ArrayList<IpCountryRange>();
        
        String[] rirs = {LACNIC, APNIC, RIPE, ARIN, AFRINIC };
        for ( int i = 0; i < rirs.length; i++ )
        {
            System.out.println( "Loading " + rirs[i] );
            URL url = new URL( rirs[i] );
            URLConnection connection = url.openConnection();
            InputStream inputStream = connection.getInputStream();
            readData(inputStream);
            System.out.println( "Total data read: " + dataList.size() );
            inputStream.close();
        }
        
        Collections.sort( dataList );
        
        //writeToOutputFile( "full.csv" );
        
        System.out.println( "before size: " + dataList.size() );
        consolidateList();
        System.out.println( "after size: " + dataList.size() );
        
        writeToOutputFile( "ip2country.csv" );
    }

    private static void writeToOutputFile( String fileName ) throws IOException
    {
        // write to output file
        BufferedWriter writer = new BufferedWriter( new FileWriter( "src/main/resources/phex/resources/" + fileName ) );
        Iterator<IpCountryRange> iterator = dataList.iterator();
        while ( iterator.hasNext() )
        {
            IpCountryRange range = iterator.next();
            //writer.write( "\"" + range.from + "\",\"" + range.to + "\",\""
            //    + range.countryCode + "\"\n" );
            writer.write( "" + range.from + "," + range.to + ","
                + range.countryCode + "\n" );
        }
        writer.close();
    }
    
    private static void consolidateList()
    {
        List<IpCountryRange> consolidatedList = new ArrayList<IpCountryRange>();
        int size = dataList.size();
        for ( int i = 0; i < size; i++ )
        {
            IpCountryRange range = dataList.get( i );
            for ( int j = i+1; j < size; j++ )
            {
                IpCountryRange nextRange = dataList.get( j );
                if ( !range.countryCode.equals( nextRange.countryCode ) )
                {
                    break;
                }
                if ( range.to + 1 != nextRange.from )
                {
                    break;
                }
                // expand range
                range.to = range.to + nextRange.to - nextRange.from + 1;
                // skip item..
                i++;
            }
            consolidatedList.add( range );
        }
        dataList = consolidatedList;
    }

    private static void readData(InputStream inputStream) throws IOException
    {
        BufferedReader reader = new BufferedReader( new InputStreamReader(
            inputStream ) );
        String line;
        while ( (line = reader.readLine()) != null )
        {
            if ( line.startsWith( "#" ) )
            {// comment
                continue;
            }
            if ( line.startsWith( "2") )
            {// version
                continue;
            }
            if ( line.endsWith( "summary") )
            {// summary line
                continue;
            }
            // record!
            StringTokenizer tokenizer = new StringTokenizer( line, "|" );
            // registry
            tokenizer.nextToken();
            // country
            String countryCode = tokenizer.nextToken();
            // type 
            String type = tokenizer.nextToken();
            if ( !type.equals( "ipv4" ) )
            {
                continue;
            }
            // start
            String start = tokenizer.nextToken();
            // value 
            String value = tokenizer.nextToken();
            long rangeValue = Long.parseLong( value );
            
            byte[] ip = AddressUtils.parseIP( start );
            int v1 =  ip[3]        & 0xFF;
            int v2 = (ip[2] <<  8) & 0xFF00;
            int v3 = (ip[1] << 16) & 0xFF0000;
            int v4 = (ip[0] << 24);
            long ipValue = ((long)(v4|v3|v2|v1)) & 0x00000000FFFFFFFFL;
            long toIpValue = ipValue + rangeValue - 1;
            
            IpCountryRange range = new IpCountryRange( ipValue, toIpValue, countryCode );
            dataList.add( range );
        }
    }
    
    private static class IpCountryRange implements Comparable<IpCountryRange>
    {
        long from;
        long to;
        String countryCode;
        
        public IpCountryRange( long from, long to, String cc )
        {
            this.from = from;
            this.to = to;
            countryCode = cc;
        }
        
        @Override
        public String toString()
        {
            return String.valueOf( from ) + " - " + String.valueOf( to ) + " " + countryCode; 
        }
        
        public int compareTo( IpCountryRange range )
        {
            if ( from > range.from )
            {
                return 1;
            }
            else if ( from < range.from )
            {
                return -1;
            }
            return 0;
        }
    }
}