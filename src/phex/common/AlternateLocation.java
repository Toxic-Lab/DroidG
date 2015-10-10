/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2008 Phex Development Group
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
package phex.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.StringTokenizer;

import phex.common.address.AddressUtils;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.MalformedDestAddressException;
//import phex.common.log.NLogger;
import phex.security.AccessType;
import phex.security.PhexSecurityManager;


public class AlternateLocation
{
    /**
     * pushNeeded | serverBusy | I'm firewalled | useHasUploaded | Rating
     *         NO |    UNKNOWN |                |              y |      5
     *
     * hasUploaded == QHD_UNKNOWN_FLAG -> Raiting + 1
     */
    public static final Short DEFAULT_HOST_RATING = Short.valueOf( (short)6 );
    private DestAddress hostAddress;
    private URN urn;

    public AlternateLocation( DestAddress hostAddress, URN urn )
    {
        this.hostAddress = hostAddress;
        this.urn = urn;
    }
    
    /**
     * @return the host address of the AltLoc.
     */
    public DestAddress getHostAddress()
    {
        return hostAddress;
    }
    
    /**
     * Returns the urn of the AlternateLocation.
     * @return the urn of the AlternateLocation.
     */
    public URN getURN()
    {
        return urn;
    }

    /**
     * Returns the alternate location in http format for header values.
     * @return
     */
    public String getHTTPString()
    {
        StringBuffer buffer = new StringBuffer( hostAddress.getHostName() );
        int port = hostAddress.getPort();
        if ( port != 6346 )
        {
            buffer.append( ':' );
            buffer.append( port );
        }
        return buffer.toString();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * Note: The class AlternateLocationCollection relies on a equal implementation
     * that uses hostAddress and urn only!
     *
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        if( !(obj instanceof AlternateLocation) )
        {
            return false;
        }

        if( obj == this )
        {
            return true;
        }
        AlternateLocation altLoc = (AlternateLocation)obj;
        return hostAddress.equals( altLoc.hostAddress ) &&
            urn.equals( altLoc.urn );
    }

    /**
     * Hash code to equal on hash based collections.
     * Note: The class AlternateLocationCollection relies on a hashCode implementation
     * that uses hostAddress and urn only!
     * @return a hash code value for this AlternateLocation.
     */
    @Override
    public int hashCode()
    {
        int h = 7;
        h = ( (31 *h) + ( (hostAddress != null) ? hostAddress.hashCode() : 0 ) );
        h = ( (31 *h) + ( (urn != null) ? urn.hashCode() : 0 ) );
        return h;
    }
    
    /**
     * Parse possible alternate location values. Possible values are:
     * 10.0.0.82
     * 10.0.0.65:8765
     * @param line
     * @return the parsed AlternateLocation or null if parsing failed.
     */
    public static AlternateLocation parseCompactIpAltLoc( String line, URN urn, 
        PhexSecurityManager securityService )
    {
        DestAddress address;
        try
        {
            address = AddressUtils.parseAndValidateAddress( line, 
                false, securityService );
        }
        catch ( MalformedDestAddressException exp )
        {
//            NLogger.debug( AlternateLocation.class,
//                "Malformed alt-location URL: " + exp.getMessage() );
            return null;
        }
                
        AlternateLocation loc = new AlternateLocation( address, urn );
        return loc;
    }


    /**
     * Parse possible alternate location values. Possible values are:
     *
     * http://www.clip2.com/GnutellaProtocol04.pdf (not supported)
     * http://10.0.0.10:6346/get/2468/GnutellaProtocol04.pdf (not supported)
     * http://10.0.0.25:6346/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB
     *      2002-04-30T08:30Z
     * 
     * But we currently only support uri-res URLs and IPs since there are too
     * many fakes on the net.
     * 
     * Parses and validates the URL more strictly. We currently only like HTTP URLs,
     * (this might be a bit too strict but all right currently), with a valid port
     * and only none private addresses.
     *
     * @param line
     * @return the parsed AlternateLocation or null if parsing failes.
     */
    public static AlternateLocation parseUriResAltLoc( String line, PhexSecurityManager securityService )
    {
        StringTokenizer tokenizer = new StringTokenizer( line, " \t\n\r\f\"" );

        String urlStr = null;
        if ( tokenizer.hasMoreTokens() )
        {
            urlStr = tokenizer.nextToken();
        }
        if ( tokenizer.hasMoreTokens() )
        {
            // skip date time string
            tokenizer.nextToken();
            //parseTimestamp( dateTimeStr );
        }
        try
        {
            URL url = new URL( urlStr );
            // verify for HTTP URL
            String protocol = url.getProtocol();
            if ( !"http".equals( protocol ) )
            {
//            	NLogger.debug( AlternateLocation.class, 
//                    "Invalid alt-location URL (Not a http URL): " + urlStr );
                return null;
            }
            
            String host = url.getHost();
            int port = url.getPort();
            if ( port == -1 )
            {
                // Due to the wide confusion on how to handle Alt-Locs without 
                // port Phex will ignore all legacy Alt-Locs without a defined
                // port. Vendors seem not to use same default port (80 or 6346).
//            	NLogger.debug( AlternateLocation.class,
//                    "Invalid legacy alt-loc without specified port." );
                return null;
                
                // create new url with changed http port
                //port = 80;
            }
            
            DestAddress hostAddress = new DefaultDestAddress( host, port );
            if ( !hostAddress.isValidAddress() )
            {
//            	NLogger.debug( AlternateLocation.class,
//                    "Invalid alt-location URL (Invalid address): " + urlStr );
                return null;
            }
            AccessType access = securityService.controlHostAddressAccess(
                hostAddress );
            switch ( access )
            {
                case ACCESS_DENIED:
                case ACCESS_STRONGLY_DENIED:
//                	NLogger.debug( AlternateLocation.class,
//                    "Alt-Location host denied: " + urlStr );
                	return null;
            }
    
            if ( !hostAddress.getIpAddress().isValidIP() )
            {
//            	NLogger.debug( AlternateLocation.class,
//                    "Invalid alt-location URL (Invalid IP used): " + urlStr );
                return null;
            }
            if ( hostAddress.getIpAddress().isSiteLocalIP() )
            {
//            	NLogger.debug( AlternateLocation.class,
//                    "Invalid alt-location URL (Private IP used): " + urlStr );
                return null;
            }
    
            URN urn = URN.parseURNFromUriRes( url.getFile() );
            if ( urn == null )
            {
//            	NLogger.debug( AlternateLocation.class,
//                    "Alt-location path without URN: " + line );
                return null;
            }
            
            AlternateLocation loc = new AlternateLocation( hostAddress, urn );
            return loc;
        }
        catch ( MalformedURLException exp )
        {
//        	NLogger.debug( AlternateLocation.class,
//                "Invalid alt-location URL (Malformed: " + exp.getMessage()
//                + " ): " + urlStr );
            return null;
        }

    }
}