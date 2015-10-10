/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2009 Phex Development Group
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
package phex.common;

import java.util.Locale;

import phex.utils.URLCodecUtils;

/**
 * Represents a URN according to RFC 2141
 * http://www.ietf.org/rfc/rfc2141.txt
 * All URNs have the following syntax (phrases enclosed in quotes are
 * REQUIRED):<br>
 * <URN> ::= "urn:" <NID> ":" <NSS>
 */
public class URN
{
    public static final String SHA1 = "sha1";
    public static final String BITPRINT = "bitprint";
    public static final String URN_PREFIX = "urn:";

    /**
     * Lazy initialized value for the hash code of a URN. If the URN is modified
     * this value must be reset for recalculation.
     */
    private int hashCode = -1;

    private String urnString;
    private String urnNID;
    private String urnNSS;

    /**
     * Parses a string into a URN. If the URN string is not valid a
     * IllegalArgumentException is thrown.
     */
    public URN( String aURNString )
    {
        // parse and validate urn...
        // this code is copyed and littly modified from isValidURN for
        // better peformance.

        // urn must start with 'urn:'
        if ( aURNString.length() < 4 )
        {
            throw new IllegalArgumentException( "URN not valid: " + aURNString );
        }
        String prefix = aURNString.substring( 0, 4 ).toLowerCase( Locale.US );
        if ( !prefix.equals( URN_PREFIX ) )
        {
            throw new IllegalArgumentException( "URN not valid: " + aURNString );
        }
        int colonIdx = aURNString.indexOf( ':', 4 );
        if ( colonIdx == -1 )
        {
            throw new IllegalArgumentException( "URN not valid: " + aURNString );
        }
        urnNID = aURNString.substring( 4, colonIdx );
        if ( !isValidNamespaceIdentifier( urnNID ) )
        {
            throw new IllegalArgumentException( "URN not valid (NID): " + aURNString );
        }
        urnNSS = aURNString.substring( colonIdx + 1, aURNString.length() );
        if ( !isValidNamespaceSpecificString( urnNSS ) )
        {
            throw new IllegalArgumentException( "URN not valid (NSS): " + aURNString );
        }
        urnString = aURNString;
    }

    public boolean isSha1Nid()
    {
        return SHA1.equalsIgnoreCase( urnNID );
    }
    
    public boolean isBitprintNid()
    {
        return BITPRINT.equalsIgnoreCase( urnNID );
    }

    public String getNamespaceSpecificString()
    {
        return urnNSS;
    }
    
    public String getSHA1Nss()
    {
        if ( SHA1.equalsIgnoreCase( urnNID ) )
        {
            //urn:sha1:[32-character-SHA1]
            return urnNSS;
        }
        else if ( BITPRINT.equalsIgnoreCase( urnNID ) )
        {
            //urn:bitprint:[32-character-SHA1].[39-character-TigerTree]
            return urnNSS.substring( 0, 32 );
        }
        return null;
    }
    
    public String getTigerTreeRootNss()
    {
        if ( BITPRINT.equalsIgnoreCase( urnNID ) )
        {
            //urn:bitprint:[32-character-SHA1].[39-character-TigerTree]
            return urnNSS.substring( 33, 72 );
        }
        return null;
    }

    /**
     * Returns the urn string in the form "urn:" <NID> ":" <NSS>.
     * @return the urn string in the form "urn:" <NID> ":" <NSS>.
     */
    public String getAsString()
    {
        return urnString;
    }

    /**
     * Override equals() to ensure that two different instances with equal URN
     * are "equal".
     */
    public boolean equals( Object urn )
    {
        if ( urn instanceof URN )
        {
            return equals( (URN) urn );
        }
        else
        {
            return false;
        }
    }

    /**
     * Override equals() to ensure that two different instances with equal URN
     * are "equal".
     */
    public boolean equals( URN urn )
    {
        // when both are same nid directly compare them...
        if ( urnNID.equalsIgnoreCase( urn.urnNID ) )
        {
            return urn.urnString.equalsIgnoreCase( urnString );
        }
        
        // otherwise both nss need to be convertable to sha1
        String thisSHA1Nss = getSHA1Nss();
        if ( thisSHA1Nss == null )
        {// this should not happen sha1 and bitprint are convertable...
            throw new RuntimeException( "Cant compare URNs" );
        }
        return thisSHA1Nss.equals( urn.getSHA1Nss() );
    }

    /**
     * Override hashCode() to ensure that two different instances with equal URN
     * generate the same hashCode. This is necessary to find URNs in Maps.
     */
    public int hashCode()
    {
        // lazy initialize hash code.
        if ( hashCode == -1 )
        {
            hashCode = 3 * ( urnString.hashCode() + getClass().hashCode() );
        }
        return hashCode;
    }

    /**
     * Retruns true if the given string is a valid URN.
     * According to RFC 2141:
     * All URNs have the following syntax (phrases enclosed in quotes are
     * REQUIRED):<br>
     * <URN> ::= "urn:" <NID> ":" <NSS>
     */
    public static boolean isValidURN( String urn )
    {
        // urn must start with 'urn:'
        if ( urn.length() < 4 )
        {
            return false;
        }
        String prefix = urn.substring( 0, 4 ).toLowerCase( Locale.US );
        if ( !prefix.equals( URN_PREFIX ) )
        {
            return false;
        }
        int colonIdx = urn.indexOf( ':', 4 );
        if ( colonIdx == -1 )
        {
            return false;
        }
        String nid = urn.substring( 4, colonIdx );
        if ( !isValidNamespaceIdentifier( nid ) )
        {
            return false;
        }
        String nss = urn.substring( colonIdx + 1, urn.length() );
        if ( !isValidNamespaceSpecificString( nss ) )
        {
            return false;
        }
        return true;
    }

    /**
     * Checks if this namespace specific string is valid. Valid is the nss part
     * of
     * urn:bitprint:[32-character-SHA1].[39-character-TigerTree]
     * and
     * urn:sha1:[32-character-SHA1]
     */
    public static boolean isValidNamespaceSpecificString( String nss )
    {
        int length = nss.length();
             // [32-character-SHA1]
        if ( length == 32 ||
             // [32-character-SHA1].[39-character-TigerTree]
             length == 72
             )
        {
            return true;
        }
        return false;
    }

    /**
     * Checks if this namespace identifier is supported. Only sha1 is supported
     * currently.
     */
    public static boolean isValidNamespaceIdentifier( String nid )
    {
        String lcNID = nid.toLowerCase( Locale.US );
        // currently only sh1 is supported
        if ( lcNID.equals( SHA1 ) )
        {
            return true;
        }
        else if ( lcNID.equals( BITPRINT ) )
        {
            return true;
        }
        return false;
    }

    public static URN parseURNFromUriRes( String uriResLine )
    {
        String lowerCaseLine = uriResLine.toLowerCase( Locale.US );
        if ( lowerCaseLine.startsWith( "/uri-res/n2r?urn:" ) )
        {
            String urnStr = URLCodecUtils.decodeURL( uriResLine.substring( 13 ) );
            if ( isValidURN( urnStr ) )
            {
                URN urn = new URN( urnStr );
                return urn;
            }
        }
        return null;
    }
}