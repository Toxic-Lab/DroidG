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
 *  --- SVN Information ---
 *  $Id$
 */
package phex.msg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import phex.common.URN;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.common.address.IpAddress;
import phex.io.buffer.ByteBuffer;
import phex.prefs.core.MessagePrefs;
import phex.servent.Servent;
import phex.utils.IOUtil;
import phex.utils.StringUtils;

/**
 * <p>Encapsulation of a Gnutella query message.</p>
 *
 * <p>Queries are encouraged to be smaller than 256 bytes. Larger messages may
 * be dropped. You can expect messages larger than 4k to be dropped.</P>
 *
 * <p>Currently, there is no support in this class for extentions such as HUGE,
 * XML or GGEP.
 *
 *
 * <p>As of Feb, 2003, this class supports and encapsulates the new Minimum Speed
 * definition, as described at
 *      http://groups.yahoo.com/group/the_gdf/files/Proposals/MinSpeed.html
 * This definition change is encapsulated within this class.</p>
 *
 */
public class QueryMsg extends Message
{
    //private static final Logger logger = LoggerFactory.getLogger( QueryMsg.class );
    
    
    /** 
     * MINSPEED_*_BIT - these are the bit numbers for the new
     * meanings of MinSpeed bits.
     */
    private static final int MINSPEED_BITBASED_BIT = 15;
    private static final int MINSPEED_FIREWALL_BIT = 14;
    private static final int MINSPEED_XML_BIT = 13;
    
    public static final int NO_FEATURE_QUERY_SELECTOR = 0;
    public static final int WHAT_IS_NEW_FEATURE_QUERY_SELECTOR = 1;
    

    /**
     * This indicates if the min speed field is using the new bit based
     * representation.
     */
    private boolean minSpeedIsBitBased;
    
    /**
     * Indicates if the query requester is firewalled. 
     * Variables represent a bit carried inside the MinSpeed field.
     */
    private boolean requesterIsFirewalled;
    
    /**
     * Indicates if the query requester is capable of receiving XML results. 
     * Variables represent a bit carried inside the MinSpeed field.
     */
    private boolean requesterIsXmlResultsCapable;
    
    /**
     * Indicates if secure OOB replies should be used.
     */
    private boolean requestSecureOOBReplies;

    /**
     * <p>The search string for this query.</p>
     *
     * <p>Servents should treat this search string as a list of keywords. They
     * should respond with files that match all keywords. They may chose to only
     * respond with files with all keywords in the query order. Case will be
     * ignored. Wildcards and regular expressions are not supported.</p>
     *
     * <p>If the query is four spaces "    " and TTL=1 and hops=0, it should be
     * interpreted as a request for a complete directory listing.</p>
     */
    private String searchString;
    
    private int featureQuerySelector;

    /**
     * <p>The un-parsed body of the query.</p>
     *
     * <p>For queries that are being forwarded, this body will include all extra
     * data. For queries built using this API, there is no way currently to add
     * extra information to this body.</p>
     */
    private byte[] body;

    /**
     * Defines if the body of the message is already parsed.
     */
    private boolean isParsed;

    /**
     * Contains all urns to search for. This attribute is null if urns are not
     * in the query.
     */
    private Set<URN> queryURNSet;
    
    /**
     * Defines the origin IpAddress of the query request.
     */
    private byte[] originIpAddress;

    /**
     * Defines the origin port number of the query request.
     */
    private int originPort;

    /**
     * Create a query with a new header, a given ttl and search string.
     * <p>The header will be modified so that its function property becomes
     * MsgHeader.sQuery. The header argument is owned by this object.</p>
     */
    public QueryMsg( GUID guid, byte ttl, String aSearchString, URN queryURN,
        boolean isRequesterCapableOfXmlResults, boolean isRequesterBehindFirewall,
        boolean isOOBReplyAllowed, int featureQuerySelector )
    {// TODO3 extend query to dispatch a couple of URNs, this can be used
     // for researching multiple candidates for multiple files. But verify
     // that this works with common clients.
        super( new MsgHeader( guid, MsgHeader.QUERY_PAYLOAD, ttl, 0 ) );
        searchString = aSearchString;
        this.featureQuerySelector = featureQuerySelector;
        if ( StringUtils.isEmpty( searchString ) )
        {
            searchString = "\\";
        }
        if ( queryURN != null )
        {
            queryURNSet = new HashSet<URN>( 1 );
            queryURNSet.add( queryURN );
        }
        else
        {
            queryURNSet = Collections.emptySet();
        }
        
        minSpeedIsBitBased = true;
        requesterIsFirewalled = isRequesterBehindFirewall;
        requesterIsXmlResultsCapable = isRequesterCapableOfXmlResults;
        this.requestSecureOOBReplies = isOOBReplyAllowed;
        try
        {
            buildBody();
        }
        catch (IOException e)
        {// should never happen
            //logger.error( e.toString(), e );
        }
        getHeader().setDataLength( body.length );
    }

    /**
     * <p>Create a new MsgQuery with its header and body.</p>
     *
     * <p>The header will be modified so that its function property becomes
     * MsgHeader.sQuery. The header argument is owned by this object.</p>
     *
     * <p>The body is not parsed directly
     * cause some queries are just forwarded without the need of being completely
     * parsed. This allows the extention data (such as GGEP blocks) to be
     * forwarded despite there being no API to modify these.</p>
     *
     * @param header the MsgHeader to associate with the new message
     * @param aBody the message body
     */
    public QueryMsg( MsgHeader header, byte[] aBody )
    {
        super( header );
        getHeader().setPayloadType(MsgHeader.QUERY_PAYLOAD);
        body = aBody;
        requestSecureOOBReplies = false;
        // parse the body
        parseBody();
    }
    
    public QueryMsg( QueryMsg query, byte ttl )
    {
        super( new MsgHeader( MsgHeader.QUERY_PAYLOAD, 0 ) );
        getHeader().copy( query.getHeader() );
        getHeader().setTTL( ttl );
        this.body = query.body;
        this.searchString = query.searchString;
        this.featureQuerySelector = query.featureQuerySelector;
        this.minSpeedIsBitBased = query.minSpeedIsBitBased;
        this.requesterIsFirewalled = query.requesterIsFirewalled;
        this.requesterIsXmlResultsCapable = query.requesterIsXmlResultsCapable;
        this.requestSecureOOBReplies = query.requestSecureOOBReplies;
        this.isParsed = query.isParsed;
        this.queryURNSet = new HashSet<URN>( query.queryURNSet );
    }

    /**
     * Determine whether the query uses the MinSpeed
     * field as a 'reclaimed' field, where the bits have individual meanings.
     * @return boolean
     */
    private boolean isMinSpeedBitBased()
    {
        return minSpeedIsBitBased;
    }

    /**
     * Determine whether the query source is a firewalled servent.
     * This can only be true when the query is using the new MinSpeed meaning.
     * @return boolean
     */
    public boolean isRequesterFirewalled()
    {
        return minSpeedIsBitBased && requesterIsFirewalled;
    }

    /**
     * Determine whether the query source is capable of handling XML results.
     * This can only be true when the query is using the new MinSpeed meaning.
     * @return boolean
     */
    public boolean isRequesterXmlResultsCapable()
    {
        return minSpeedIsBitBased && requesterIsXmlResultsCapable;
    }

    /**
     * Set whether the query uses the MinSpeed
     * field as a bit-based field, where the bits have individual meanings.
     * @return boolean
     */
    private void setMinSpeedIsBitBased(boolean newValue)
    {
        minSpeedIsBitBased = newValue;
    }

    /**
     * Set whether the query source is a firewalled servent.
     * This field will only be used when MinSpeed is bit-based.
     * @return boolean
     */
    private void setRequesterIsFirewalled(boolean newValue)
    {
        requesterIsFirewalled = newValue;
    }

    /**
     * Set whether the query source is capable of handling XML results.
     * This field will only be used when MinSpeed is bit-based.
     * @return boolean
     */
    private void setRequesterIsXmlResultsCapable(boolean newValue)
    {
        requesterIsXmlResultsCapable = newValue;
    }

    /**
     * Utility method -- Determine whether a particular bit in a short is set.
     *
     * @param shortIn   The value whose bit will be checked
     * @param bitNumber The bit number to check (0 is least significant, 15 is most significant)
     * @return boolean
     */
    private static boolean isBitSet(short shortIn, int bitPos)
    {
        int bitValue = 1 << bitPos;
        return (shortIn & bitValue) != 0;
    }

    /**
     * Utility method -- Set a bit in a short.
     *
     * @param shortIn   The value whose bit may be changed.
     * @param bitNumber The bit number to set/clear (0 is least significant, 15 is most significant)
     * @return boolean
     */
    private static short setBit(short shortIn, int bitPos)
    {
        short mask = (short) (1 << bitPos);
        return (short) (shortIn | mask);
    }

    /**
     * Version of setBit() which accepts Objects (for testing)
     * @param shortIn
     * @param bitPos
     * @return short
     */
    private static short setBit(Short shortIn, Integer bitPos)
    {
        return setBit(shortIn.shortValue(), bitPos.intValue());
    }

    /**
     * Version of isBitSet() which accepts Objects (for testing)
     * @param shortIn
     * @param bitPos
     * @return boolean
     */
    private static boolean isBitSet(Short shortIn, Integer bitPos)
    {
        return isBitSet(shortIn.shortValue(), bitPos.intValue());
    }

    /**
     * Returns the feature query selector of the query. Possible values are
     * NO_FEATURE_QUERY_SELECTOR, WHAT_IS_NEW_FEATURE_QUERY_SELECTOR
     * @return
     */
    public int getFeatureQuerySelector()
    {        
        return featureQuerySelector;
    }
    
    public boolean isWhatsNewQuery()
    {
        return featureQuerySelector == WHAT_IS_NEW_FEATURE_QUERY_SELECTOR;
    }

    /**
     * Returns a Iterator of queried URNs to look for.
     */
    public URN[] getQueryURNs()
    {
        URN[] urns = new URN[ queryURNSet.size() ];
        return queryURNSet.toArray( urns );
    }

    /**
     * Indicates if the query carrys query urns.
     * @return true if the query carrys query urns.
     */
    public boolean hasQueryURNs()
    {
        return !queryURNSet.isEmpty();
    }

    /**
     * <p>Get the search string for this query.</p>
     *
     * <p>Servents should treat this search string as a list of keywords. They
     * should respond with files that match all keywords. They may chose to only
     * respond with files with all keywords in the query order. Case will be
     * ignored. Wildcards and regular expressions are not supported.</p>
     *
     * <p>If the query is four spaces "    " and TTL=1 and hops=0, it should be
     * interpreted as a request for a complete directory listing.</p>
     *
     * @return  the String representation of the query
     */
    public String getSearchString()
    {
        return searchString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer createMessageBuffer()
    {
        return ByteBuffer.wrap( body );
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer(100);

        buf.append("[")
            .append(getHeader())
            .append(", MinSpeedIsBitBased=")
            .append(minSpeedIsBitBased);
        if (minSpeedIsBitBased)
        {
            buf.append(", RequesterIsFirewalled=")
            .append(requesterIsFirewalled)
            .append(", RequesterIsXmlResultsCapable=")
            .append(requesterIsXmlResultsCapable);
        }

        buf.append(", SearchString=")
            .append(searchString)
            .append("]");

        return buf.toString();
    }

    private void addPhexExtendedOriginGGEP( GGEPBlock ggepBlock )
    {
        DestAddress[] addresses = new DestAddress[1];        
        DestAddress localAddress = Servent.getInstance().getLocalAddress();
        addresses[0] = new DefaultDestAddress( localAddress.getIpAddress(), 
            localAddress.getPort() );
        ggepBlock.addExtension( GGEPBlock.PHEX_EXTENDED_ORIGIN, addresses, 1 );
    }

    public int getOriginPort()
    {
        return this.originPort;
    }

    public byte[] getOriginIpAddress()
    {
        return this.originIpAddress;
    }
    
    private void buildBody()
        throws IOException
    {
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream( );
        short complexMinSpeed = buildComplexMinSpeed();
        // we send min speed in big endian byte order since we only care for
        // bit based min speed and go according to bit based min speed specs.
        IOUtil.serializeShort( complexMinSpeed, bodyStream );
        
        bodyStream.write( searchString.toLowerCase().getBytes("UTF-8") );
        bodyStream.write( 0 );
        
        boolean writeGemExtension = false;
        
        if ( queryURNSet.size() == 0 )
        {
            // request sha1 URLs only
            bodyStream.write( (URN.URN_PREFIX + URN.SHA1).getBytes() );
            writeGemExtension = true;
        }
        else
        {// we query for urns... add list content to body...
            for ( URN urn : queryURNSet )
            {
                bodyStream.write( urn.getAsString().getBytes() );
            }
            writeGemExtension = true;
        }
        
        // add a possible GGEP extension...
        GGEPBlock ggepBlock = new GGEPBlock( true );

        // add the feature query header
        if ( featureQuerySelector > 0 )
        {
            ggepBlock.addExtension(GGEPBlock.FEATURE_QUERY_HEADER_ID, featureQuerySelector);
        }
        
        if ( requestSecureOOBReplies )
        {
            ggepBlock.addExtension( GGEPBlock.SECURE_OOB_ID );
        }

        if (MessagePrefs.UseExtendedOriginIpAddress.get().booleanValue())
        {
            addPhexExtendedOriginGGEP(ggepBlock);
        }

        byte[] ggepData = ggepBlock.getBytes();
        if ( ggepData.length > 0 )
        {
            // only write ggep when we have a extension.
            if ( writeGemExtension )
            {
                bodyStream.write( 0x1c );
            }
            bodyStream.write(ggepData);
            writeGemExtension = true;
        }
        
        bodyStream.write( 0 );
        
        body = bodyStream.toByteArray();
    }

    private void parseBody()
    {
        try
        {
            ByteArrayInputStream inStream = new ByteArrayInputStream(body);
            // Get & parse the MinSpeed field
            // we read min speed in big endian byte order since we only care for
            // bit based min speed and go according to bit based min speed specs.
            short minSpeedField = IOUtil.deserializeShort(inStream);
            parseMinSpeed(minSpeedField);

            byte[] queryBytes = IOUtil.readBytesToNull(inStream);
            searchString = new String(queryBytes, "UTF-8");

            // read extension area... each extension is seperated by a 0x1c FS
            // "file separator" character

            byte[] extensionBytes = IOUtil.readBytesToNull(inStream);
            
            HUGEBlock hugeBlock = new HUGEBlock( extensionBytes );
            
            queryURNSet = hugeBlock.getURNS();
            // no URLs in query.. use empty list.
            if (queryURNSet == null)
            {
                queryURNSet = Collections.emptySet();
            }
            
            GGEPBlock[] ggepBlocks = hugeBlock.getGGEPBlocks();
            if ( ggepBlocks != null )
            {
                if ( GGEPBlock.isExtensionHeaderInBlocks(ggepBlocks, 
                    GGEPBlock.FEATURE_QUERY_HEADER_ID ) )
                {
                    featureQuerySelector = GGEPExtension.parseIntExtensionData( 
                        ggepBlocks, GGEPBlock.FEATURE_QUERY_HEADER_ID, 0 ); 
                }
                if ( GGEPBlock.isExtensionHeaderInBlocks(ggepBlocks, 
                    GGEPBlock.SECURE_OOB_ID ) )
                {
                    requestSecureOOBReplies = true;
                }
                if ( MessagePrefs.UseExtendedOriginIpAddress.get().booleanValue() )
                {
                    try {
                        byte[] value = GGEPBlock.getExtensionDataInBlocks(ggepBlocks, GGEPBlock.PHEX_EXTENDED_ORIGIN);
                        originIpAddress = new IpAddress(value).getHostIP();
                        originPort = IOUtil.deserializeShortLE(value, 4);
                    }
                    catch (Exception ex) {
                      //  logger.error( ex.toString(), ex );
                    }
                }
            }
        }
        catch (IOException e)
        {
            //logger.error( e.toString(), e );
        }
    }

    private void parseMinSpeed(short minSpeedIn)
    {
        minSpeedIsBitBased = isBitSet(minSpeedIn, MINSPEED_BITBASED_BIT);

        // REWORK drop queries without this bit.
        if (isMinSpeedBitBased())
        // Incoming MinSpeed IS bit-based
        {
            requesterIsFirewalled = isBitSet(minSpeedIn, MINSPEED_FIREWALL_BIT);
            requesterIsXmlResultsCapable = isBitSet(minSpeedIn, MINSPEED_XML_BIT);
        }
/*
        if (Logger.isLevelLogged(Logger.FINEST))
        {
            String bin = Integer.toBinaryString(minSpeedIn);
            if (bin.length() > 16) bin = bin.substring(16);
            StringBuffer buf = new StringBuffer(100);
            buf.append("MsgQuery.parseMinSpeed: MinSpeed=")
                .append(minSpeedIn)
                .append(" (")
                .append(bin)
                .append(")  --  ")
                .append("     bit-based: ")
                .append(minSpeedIsBitBased)
                .append("     firewalled: ")
                .append(requesterIsFirewalled)
                .append("     xml: ")
                .append(requesterIsXmlResultsCapable);

            Logger.logMessage(Logger.FINEST, Logger.NETWORK, buf.toString());
        }
*/
    }

    /**
     * This method takes all of the discreet local variables and compiles them into
     * a short, in the bit-based format for MinSpeed.  It is called when the query is
     * being serialized for output to another servent.
     * @return short containing all of the appropriate bits set
     */
    private short buildComplexMinSpeed()
    {
        // Start with a real minimum speed numeric value
        short complexMinSpeed = (short) 0;

        // Set any appropriate bits
        if (minSpeedIsBitBased)
        {
            // First, indicate that the field is bit-ased
           complexMinSpeed = setBit(complexMinSpeed, MINSPEED_BITBASED_BIT);

            // Firewall bit
            if (requesterIsFirewalled)
            {
                complexMinSpeed = setBit(complexMinSpeed, MINSPEED_FIREWALL_BIT);
            }

            // XML bit
            if ( !requestSecureOOBReplies && requesterIsXmlResultsCapable)
            {
                complexMinSpeed = setBit(complexMinSpeed, MINSPEED_XML_BIT);
            }
        }

        return complexMinSpeed;
    }

}

