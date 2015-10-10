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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import phex.common.URN;
import phex.common.address.DefaultDestAddress;
import phex.common.address.DestAddress;
import phex.io.buffer.ByteBuffer;
import phex.prefs.core.LibraryPrefs;
import phex.prefs.core.NetworkPrefs;
import phex.query.QHDFlag;
import phex.security.PhexSecurityManager;
import phex.utils.HexConverter;
import phex.utils.IOUtil;
import phex.xml.XMLUtils;

/**
 * <p>A response to a query message.</p>
 *
 * <p>These should have message IDs matching the query message it is responding
 * to.</p>
 *
 * <p>This class does implement the EQHD extention. It also support the GGEP
 * extension.
 * </p>
 */
public class QueryResponseMsg extends Message
{
    private static final boolean INCLUDE_QHD = true;

    /**
     * push flag mask for beashare metadata.
     */
    private static final byte PUSH_NEEDED_MASK=(byte)0x01;
    /**
     * busy flag mask for beashare metadata.
     */
    private static final byte SERVER_BUSY_MASK=(byte)0x04;
    /**
     * upload flag mask for beashare metadata.
     */
    private static final byte HAS_UPLOADED_MASK=(byte)0x08;
    /**
     * speed flag mask for beashare metadata.
     */
    private static final byte UPLOAD_SPEED_MASK=(byte)0x10;
    /**
     * ggep flag mask for QHD.
     */
    private static final byte GGEP_MASK=(byte)0x20;
    /**
     * chat flag mask for limewire and shareaza metadata.
     */
    private static final byte CHAT_SUPPORTED_MASK = (byte)0x01;
    
    
    /**
     * The security service for parsing. Might be null.
     */
    private final PhexSecurityManager securityService;

    /**
     * <p>The un-parsed body of the query response.</p>
     *
     * <p>For queries that are being forwarded, this body will include all extra
     * data. For queries built using this API, there is no way currently to add
     * extra information to this body.</p>
     */
    private byte[] body;
    
    /**
     * Only count each URN once to reduce counted spams.
     */
    private short uniqueResultCount;
    
    /**
     * Query response records.
     */
    private QueryResponseRecord[] records;

    /**
     * The host address for the query response
     */
    private DestAddress destAddress;

    private GUID remoteClientID;

    /**
     * Defines the four character vendor code of the client that offers the
     * file.
     */
    private String vendorCode;

    /**
     * <p>Defines if a push transfer is needed or not or unknown.</p>
     *
     * <p><em>fixme:</em> Why not use a boolean? Same goes for other flags.</p>
     */
    private QHDFlag pushNeededFlag;

    /**
     * Defines if a server is busy currently or unknown.
     */
    private QHDFlag serverBusyFlag;

    /**
     * Defines if a the server has already uploaded a file.
     */
    private QHDFlag hasUploadedFlag;

    /**
     * Defines if the upload speed of a server.
     */
    private QHDFlag uploadSpeedFlag;

    /**
     * Defines if the server supportes chat.
     */
    private boolean isChatSupported;

    /**
     * Defines if the server supports browse host.
     */
    private boolean isBrowseHostSupported;
    
    /**
     * The push proxy addresses for the GGEP extension of this query 
     * response.
     */
    private DestAddress[] pushProxyAddresses;
    
    private byte[] securityToken;

    /**
     * Defines if the body of the query response is already parsed.
     */
    private boolean isParsed;

    /**
     * Build a new MsgQueryResponse with a header, a client GUID, host address,
     * port and speed, and an array of MsgResRecord instances representing hits.
     *
     * @param header  the MsgHeader to attach, which will have its function
     *                property set to MsgHeader.sQueryResponse
     * @param clientID     the GUID of the client that requested the query
     * @param aHostAddress  the DestAddress of the responding servent
     * @param speed        the speed of the responding servent
     * @param records      an array of MsgResRecord objects representing hits to
     *                     the query which must be shorter than 256
     * @throws IllegalArgumentException if there are more than 255 records
     */
    public QueryResponseMsg( MsgHeader header, GUID clientID,
        DestAddress aHostAddress, int speed, QueryResponseRecord records[],
        DestAddress[] pushProxyAddresses, boolean hasConnectedIncoming,
        boolean isHostBusyUploading )
    {
        super( header );
        if( records.length > 255 )
        {
            throw new IllegalArgumentException(
                "A maximum of 255 records can be associated with a single " +
                "response: " + records.length );
        }
        
        getHeader().setPayloadType(MsgHeader.QUERY_HIT_PAYLOAD);

        remoteClientID = clientID;
        destAddress = aHostAddress;
        this.records = records;
        
        this.pushProxyAddresses = pushProxyAddresses;

        // when we are behind firewall or have not accepted an incoming connection
        // like defined in the protocol
        boolean isPushNeeded;
        if ( hasConnectedIncoming )
        {
            isPushNeeded = false;
            pushNeededFlag = QHDFlag.QHD_FALSE_FLAG;
        }
        else
        {
            isPushNeeded = true;
            pushNeededFlag = QHDFlag.QHD_TRUE_FLAG;
        }
        uploadSpeedFlag = QHDFlag.QHD_FALSE_FLAG;

        boolean isServerBusy = isHostBusyUploading;

        try
        {
            buildBody( speed, isPushNeeded, isServerBusy );
        }
        catch (IOException e)
        {// should never happen
//            NLogger.error( QueryResponseMsg.class, e, e);
        }
        getHeader().setDataLength( body.length );
        isParsed = true;
        securityService = null;
    }

    /**
     * <p>Create a query response with its header and body.</p>
     *
     * <p>The header becomes owned by this message. Its function property will
     * be set to MsgHeader.sQueryResponse.</p>
     *
     * <p>The body is not parsed directly
     * cause some queries are just forwarded without the need of beeing completely
     * parsed. This allows the extention data (such as GGEP blocks) to be
     * forwarded despite there being no API to modify these.</p>
     *
     * @param header  the MsgHeader to use as header
     * @throws InvalidMessageException
     */
    public QueryResponseMsg( MsgHeader header, byte[] aBody, PhexSecurityManager securityService )
    	throws InvalidMessageException
    {
        super( header );
        if ( header.getPayload() != MsgHeader.QUERY_HIT_PAYLOAD )
        {
            throw new IllegalArgumentException( "Invalid message type: " + header.getPayload() );
        }
        this.securityService = securityService;

        body = aBody;
        header.setDataLength( body.length );
        
        // validate port
        int port = IOUtil.unsignedShort2Int( IOUtil.deserializeShortLE( body, 1 ) );
        destAddress = new DefaultDestAddress( getHostIP(), port );
        if ( !destAddress.isValidAddress() )
        {
            throw new InvalidMessageException( "Invalid address: " + destAddress );
        }
        
        isParsed = false;
    }

    private void buildBody( int speed, boolean isPushNeeded, boolean isServerBusy )
        throws IOException
    {
        ByteArrayOutputStream bodyStream = new ByteArrayOutputStream( );
        int recordCount = records.length;
        bodyStream.write( (byte) recordCount );
        
        IOUtil.serializeShortLE( (short)destAddress.getPort(), bodyStream );
        byte[] ipAddress = destAddress.getIpAddress().getHostIP();
        bodyStream.write( ipAddress );
        IOUtil.serializeIntLE( speed, bodyStream );

        for (int i = 0; i < recordCount; i++)
        {
            records[ i ].write( bodyStream );
        }

        if ( INCLUDE_QHD )
        {
            // add vendor code 'PHEX'
            bodyStream.write( (byte) 0x50 );
            bodyStream.write( (byte) 0x48 );
            bodyStream.write( (byte) 0x45 );
            bodyStream.write( (byte) 0x58 );
            // open data length
            bodyStream.write( (byte) 2 );
            // open data flags
            byte isPushNeededByte = (byte) 0;
            if ( isPushNeeded )
            {
                isPushNeededByte = PUSH_NEEDED_MASK;
            }
            byte isServerBusyByte = (byte) 0;
            if ( isServerBusy )
            {
                isServerBusyByte = SERVER_BUSY_MASK;
            }
            byte isGGEPUsedByte = (byte)0;
            if ( LibraryPrefs.AllowBrowsing.get().booleanValue() 
              || ( pushProxyAddresses != null && pushProxyAddresses.length > 0 ) )
            {
                isGGEPUsedByte = GGEP_MASK;
            }

            bodyStream.write( (byte) (
                  isPushNeededByte
                | SERVER_BUSY_MASK
                | 0 //HAS_UPLOADED_MASK we dont know that yet
                // we know we never measured that speed
                | UPLOAD_SPEED_MASK
                | GGEP_MASK ) );
            bodyStream.write( (byte) (
                  PUSH_NEEDED_MASK
                | isServerBusyByte
                | 0 //(hasUploadedSuccessfully ? HAS_UPLOADED_MASK : 0)
                // we know we never measured that speed
                | 0 //(isSpeedMeasured ? UPLOAD_SPEED_MASK : 0));
                | isGGEPUsedByte ) );

            // private QHD area
            // mark for chat able.
            if ( NetworkPrefs.AllowChatConnection.get().booleanValue() )
            {
                bodyStream.write( (byte) 0x01 );
            }
            else
            {
                bodyStream.write( (byte) 0x00 );
            }

            //GGEP block
            byte[] ggepBytes = GGEPBlock.getQueryReplyGGEPBlock( 
                LibraryPrefs.AllowBrowsing.get().booleanValue(), pushProxyAddresses );
            if ( ggepBytes.length > 0 )
            {
                bodyStream.write( ggepBytes );
            }
        }
        remoteClientID.write( bodyStream );
        
        body = bodyStream.toByteArray();
    }

    /**
     * <p>Get the number of response records attached to this response.</p>
     */
    public short getRecordCount()
    {
        // instead of parsing the complete body we will only read out the
        // record length. This is much more performant.
        return (short)IOUtil.unsignedByte2int( body[0] );
    }
    
    /**
     * The number of unique URN reuslts.
     * @return the number of unique results.
     * @throws InvalidMessageException 
     */
    public short getUniqueResultCount() throws InvalidMessageException
    {
        parseBody();
        return uniqueResultCount;
    }

    /**
     * Returns the IP address of the host as a byte array.
     * The body is not parsed to get this data.
     */
    private byte[] getHostIP()
    {
        byte[] ip = new byte[4];
        // the ip starts at byte 3
        ip[0] = body[3];
        ip[1] = body[4];
        ip[2] = body[5];
        ip[3] = body[6];
        return ip;
    }

    public DestAddress getDestAddress()
    {
        return destAddress;
    }
    
    /**
     * Overrides the DestAddress from OOB response information
     * in case the delivered DestAddress is not valid.
     */
    public void setDestAddressFromOOB( DestAddress address )
    {
        destAddress = address;
    }

    /**
     * Get the speed of the remote servent in kbyte/sec.
     * @return the remote host speed 
     */
    public int getRemoteHostSpeed()
    {
        // instead of parsing the complete body we will only read out the
        // record length.
        long speed = IOUtil.unsignedInt2Long( 
            IOUtil.deserializeIntLE( body, 7 ) );
        return IOUtil.castLong2Int( speed );
    }

    /**
     * <p>If present, return the vendor code of the servent, otherwise null.</p>
     *
     * <p>For example, responses arrising from phex will have a vendor code of
     * "PHEX".</p>
     *
     * @return the vendor code
     * @throws InvalidMessageException 
     */
    public String getVendorCode() throws InvalidMessageException
    {
        parseBody();
        return vendorCode;
    }

    /**
     * <p>States wether the servent will require push to fetch data.</p>
     *
     * <p>A servent will require push if it can not accept TCP connections to
     * fetch the matching resource because, for example, it is the other side of
     * a fire wall.</p>
     * Since this field can hold three states ( QHD_TRUE_FLAG, QHD_FALSE_FLAG,
     * QHD_UNKNOWN_FLAG ) and all three states are evaluated a boolean can't be used.
     *
     * @return a short that will be one of the QueryConstants QHD_UNKNOWN_FLAG,
     *         QHD_TRUE_FLAG or QHD_FALSE_FLAG
     * @throws InvalidMessageException 
     */
    public QHDFlag getPushNeededFlag() throws InvalidMessageException
    {
        parseBody();
        return pushNeededFlag;
    }

    /**
     * <p>States wether the servent is currently buisy.</p>
     *
     * <p>A servent is buisy if all available upload slots are filled.</p>
     *
     * Since this field can hold three states ( QHD_TRUE_FLAG, QHD_FALSE_FLAG,
     * QHD_UNKNOWN_FLAG ) and all three states are evaluated a boolean can't be used.
     *
     * @return a short that will be one of the QueryConstants QHD_UNKNOWN_FLAG,
     *         QHD_TRUE_FLAG or QHD_FALSE_FLAG
     * @throws InvalidMessageException 
     */
    public QHDFlag getServerBusyFlag() throws InvalidMessageException
    {
        parseBody();
        return serverBusyFlag;
    }

    /**
     * <p>States wether the servent has ever successfuly uploaded any file.</p>
     *
     * Since this field can hold three states ( QHD_TRUE_FLAG, QHD_FALSE_FLAG,
     * QHD_UNKNOWN_FLAG ) and all three states are evaluated a boolean can't be used.
     *
     * @return a short that will be one of the QueryConstants QHD_UNKNOWN_FLAG,
     *         QHD_TRUE_FLAG or QHD_FALSE_FLAG
     * @throws InvalidMessageException 
     */
    public QHDFlag getHasUploadedFlag() throws InvalidMessageException
    {
        parseBody();
        return hasUploadedFlag;
    }

    /**
     * <p>States wether the servent calculates its upload speed as the highest
     * average upload speed of the last 10 uploads, or as a user-defined value.
     * </p>
     * Since this field can hold three states ( QHD_TRUE_FLAG, QHD_FALSE_FLAG,
     * QHD_UNKNOWN_FLAG ) and all three states are evaluated a boolean can't be used.
     *
     * @return a short that will be one of the QueryConstants QHD_UNKNOWN_FLAG,
     *         QHD_TRUE_FLAG or QHD_FALSE_FLAG
     * @throws InvalidMessageException 
     */
    public QHDFlag getUploadSpeedFlag() throws InvalidMessageException
    {
        parseBody();
        return uploadSpeedFlag;
    }

    /**
     * <p>States wether the servent supportes chat connections.</p>
     *
     * @return true if a servent supports chat connections false otherwise.
     * @throws InvalidMessageException 
     */
    public boolean isChatSupported() throws InvalidMessageException
    {
        parseBody();
        return isChatSupported;
    }

    /**
     * <p>States wether the servent supportes browse host.</p>
     *
     * @return true if a servent supports browse host connections false otherwise.
     * @throws InvalidMessageException 
     */
    public boolean isBrowseHostSupported() throws InvalidMessageException
    {
        parseBody();
        return isBrowseHostSupported;
    }
    
    /**
     * Returns the collected PushProxy addresses from the GGEP
     * extension or null if no addresses are found.
     * @return push proxy addresses or null.
     * @throws InvalidMessageException 
     */
    public DestAddress[] getPushProxyAddresses() throws InvalidMessageException
    {
        parseBody();
        return pushProxyAddresses;
    }
    
    /**
     * Returns the security token used for OOB replies.
     * @return the OOB security token.
     * @throws InvalidMessageException
     */
    public byte[] getSecurityToken() throws InvalidMessageException
    {
        parseBody();
        return securityToken;
    }

    /**
     * Get the GUID of the remote servent.
     *
     * @return the GUID of the remote client
     */
    public GUID getRemoteServentID()
    {
        if ( remoteClientID == null )
        {
            parseRemoteClientID();
        }
        return remoteClientID;
    }

    /**
     * Get the i'th MsgResRecord that encapsulates the i'th hit to the query in
     * this response.
     *
     * @throws InvalidMessageException 
     * @throws IndexOutOfBoundsException  if i is negative or not less than
     *         getRecordCount()
     */
    public QueryResponseRecord[] getMsgRecords( )
        throws InvalidMessageException
    {
        parseBody();
        return records;
    }

    /**
     * Make an independent copy of a MsgQueryResponse into this message.
     *
     * @param b  the MsgQueryResponse to copy all data from
     */
    public void copy(QueryResponseMsg b)
    {
        getHeader().copy(b.getHeader());
        destAddress = b.destAddress;
        remoteClientID = b.remoteClientID;
        int recordCount = b.records.length;
        records = new QueryResponseRecord[ recordCount ];
        for (int i = 0; i < recordCount; i++)
        {
            QueryResponseRecord rec = new QueryResponseRecord();
            rec.copy( b.records[ i ] );
            records[ i ] = rec;
        }
        body = b.body;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ByteBuffer createMessageBuffer()
    {
        return ByteBuffer.wrap( body );
    }

    private void parseBody( )
        throws InvalidMessageException
    {
        if ( isParsed )
        {
            return;
        }
        assert securityService != null : "PhexSecurityService missing";
        //System.err.println("Parsing QH ar!");
        short recordCount = getRecordCount();

        // skip record count -> 1
        // skip port         -> 2
        // skip ip           -> 4
        // skip speed        -> 4
        int offset = 11;

        uniqueResultCount = 0;
        Set<URN> uniqueURNs = new HashSet<URN>();
        records = new QueryResponseRecord[ recordCount ];
        
        int highestAltLocCount = 0;
        for (int i = 0; i < recordCount; i++)
        {
            QueryResponseRecord rec = new QueryResponseRecord();
            offset = rec.deserialize(body, offset, securityService);
            
            if ( rec.getAlternateLocations() != null )
            {
                highestAltLocCount = Math.max(highestAltLocCount,
                        rec.getAlternateLocations().length);
            }
            
            records[ i ] = rec;
            URN urn = rec.getURN();
            if ( urn != null )
            {
                uniqueURNs.add( urn );
            }
            else
            {
                uniqueResultCount ++;
            }
        }
        uniqueResultCount += uniqueURNs.size();

        // Handle Bearshare meta informations. The format is documented in
        // the GnutellaProtocol04.pdf document
        pushNeededFlag = QHDFlag.QHD_UNKNOWN_FLAG;
        serverBusyFlag = QHDFlag.QHD_UNKNOWN_FLAG;
        hasUploadedFlag = QHDFlag.QHD_UNKNOWN_FLAG;
        uploadSpeedFlag = QHDFlag.QHD_UNKNOWN_FLAG;
        // GGEP extensions
        isBrowseHostSupported = false;

        if ( offset <= (getHeader().getDataLength() - 16 - 4 - 2) )
        {
            // parse meta data
            // Use ISO encoding for two bytes characters on some platforms.
            try
            {
                vendorCode = new String( body, offset, 4, "ISO-8859-1");
            }
            catch ( UnsupportedEncodingException exp )
            {// Should never happen...
//                NLogger.error( QueryResponseMsg.class, exp );
                throw new RuntimeException( exp );
            }
            if ( isVendorCodeValid( vendorCode ) )
            {
                vendorCode = vendorCode.intern();
            }
            else
            {
                String hexVendorCode = HexConverter.toHexString( body, offset, 4 );
//                NLogger.warn(QueryResponseMsg.class,
//                    /*getHeader().getFromHost() +*/ 
//                    ": Illegal QHD vendor code found: " + vendorCode + " ("
//                    + hexVendorCode + "). Body: " +
//                    HexConverter.toHexString( body ) );
                vendorCode = hexVendorCode;
            }
            offset += 4;

            int openDataLength = IOUtil.unsignedByte2int( body[ offset ] );
            offset += 1;

            // parse upload speed, have uploaded, busy and push
            if ( openDataLength > 1)
            {   // if we have a flag byte
                byte flag1 = body[ offset ];
                byte flag2 = body[ offset + 1];

                // check if push flag is meaningful do it reversed from other checks
                if ( ( flag2 & PUSH_NEEDED_MASK ) != 0 )
                {
                    if ( ( flag1 & PUSH_NEEDED_MASK ) != 0 )
                    {
                        pushNeededFlag = QHDFlag.QHD_TRUE_FLAG;
                    }
                    else
                    {
                        pushNeededFlag = QHDFlag.QHD_FALSE_FLAG;
                    }
                }

                // check if server busy flag meaningful
                if ((flag1 & SERVER_BUSY_MASK) != 0)
                {
                    if ( (flag2 & SERVER_BUSY_MASK) != 0 )
                    {
                        serverBusyFlag = QHDFlag.QHD_TRUE_FLAG;
                    }
                    else
                    {
                        serverBusyFlag = QHDFlag.QHD_FALSE_FLAG;
                    }
                }

                // check if the uploaded flag is meaningful
                if ((flag1 & HAS_UPLOADED_MASK) != 0)
                {
                    if ( (flag2 & HAS_UPLOADED_MASK) != 0 )
                    {
                        hasUploadedFlag = QHDFlag.QHD_TRUE_FLAG;
                    }
                    else
                    {
                        hasUploadedFlag = QHDFlag.QHD_FALSE_FLAG;
                    }
                }
                if ((flag1 & UPLOAD_SPEED_MASK) != 0 )
                {
                    if ( (flag2 & UPLOAD_SPEED_MASK) != 0 )
                    {
                        uploadSpeedFlag = QHDFlag.QHD_TRUE_FLAG;
                    }
                    else
                    {
                        uploadSpeedFlag = QHDFlag.QHD_FALSE_FLAG;
                    }
                }
                if ((flag1 & GGEP_MASK) != 0 && (flag2 & GGEP_MASK) !=0 )
                {// parse GGEP area should follow after open data area but
                 // we can't be sure...
                    int ggepMagicIndex = offset + 2;
                    // search for real magic index
                    while ( ggepMagicIndex < body.length )
                    {
                        if ( body[ ggepMagicIndex ] == GGEPBlock.MAGIC_NUMBER )
                        {
                            // found index!
                            break;
                        }
                        ggepMagicIndex ++;
                    }
                    // if there are GGEPs, see if Browse Host supported...
                    GGEPBlock[] ggepBlocks = GGEPBlock.parseGGEPBlocks( body, ggepMagicIndex );
                    isBrowseHostSupported = GGEPBlock.isExtensionHeaderInBlocks(
                        ggepBlocks, GGEPBlock.BROWSE_HOST_HEADER_ID );
                    pushProxyAddresses = GGEPExtension.parsePushProxyExtensionData( ggepBlocks, securityService );
                    securityToken = GGEPBlock.getExtensionDataInBlocks( ggepBlocks, GGEPBlock.SECURE_OOB_ID );
                }
            }
            // skip unknown open data length
            offset += openDataLength;
            
            // alt loc validation...
            // Lime: 10, GTKG: 15, 
            int maxAcceptedAltLocs = 16;
            if ( vendorCode.equals("LIME") )
            {
                maxAcceptedAltLocs = 10;
            }

            if ( highestAltLocCount > maxAcceptedAltLocs )
            {
//                if ( NLogger.isWarnEnabled( QueryResponseMsg.class ) )
//                {
//                    NLogger.warn( QueryResponseMsg.class,
//                        "QueryRespRecord with " + highestAltLocCount + " alt locs"
//                        + " - vendor: " + vendorCode + " host: " + getDestAddress() + " access: " 
//                        + securityService.controlHostAddressAccess( getDestAddress() ) );
//                    for ( int i=0; i < records.length; i++ )
//                    {
//                        DestAddress[] altLocs = records[i].getAlternateLocations();
//                        if ( altLocs != null && altLocs.length > maxAcceptedAltLocs )
//                        {
//                            NLogger.warn( QueryResponseMsg.class,
//                                "QueryRespRecord with " + altLocs.length + " alt locs"
//                                + " - file: " + records[i].getFilename() );
//                        }
//                    }
//                    if ( getHeader().getHopsTaken() <= 1 )
//                    {
//                        NLogger.warn( QueryResponseMsg.class,
//                            "Number of query response record alt-locs exceed the acceptable maximum for LIME: "
//                            + highestAltLocCount + "/10 " +
//                            "---- QRR Hops: " + getHeader().getHopsTaken() + " From: " + getDestAddress() );
//                    }
//                }
                throw new InvalidMessageException( 
                   "Number of query response record alt-locs exceed the acceptable maximum for LIME: "
                   + highestAltLocCount + "/" + 10 );
            }

            //Parse private area of Limewire and Shareaza to read out chat
            //flag. If chatflag is 0x1 chat is supported, if 0x0 its not.
            int privateDataLength = body.length - offset - 16;
            if ( privateDataLength > 0 &&
               ( vendorCode.equals("LIME") || vendorCode.equals("RAZA")
              || vendorCode.equals("PHEX") ) )
            {
                byte flag = body[ offset ];
                isChatSupported = ( flag & CHAT_SUPPORTED_MASK ) != 0;
            }

            //System.out.println( (mHeader.getDataLen() -16 - 4 -2) + "  " + offset + "  " +
            //    /*new String( body, offset, mHeader.getDataLen() - offset) + "   " +*/
            //    openDataLength + "  " + pushNeededFlag + "  " + serverBusyFlag + "  " +
            //    uploadSpeedFlag + "  " + hasUploadedFlag + "  " + vendorCode );
        }
        parseRemoteClientID();
        
        isParsed = true;
    }

    private void parseRemoteClientID()
    {
        if ( remoteClientID == null )
        {
            remoteClientID = new GUID();
        }
        remoteClientID.deserialize(body, getHeader().getDataLength() - GUID.DATA_LENGTH);
    }

    private static boolean isVendorCodeValid( String vendorCode )
    {
        // verify length
        if ( vendorCode.length() != 4 )
        {
            return false;
        }
        // verify characters
        for ( int i = 0; i < 4; i++ )
        {
            if ( !XMLUtils.isXmlChar( vendorCode.charAt( i ) ) )
            {
                return false;
            }
        }
        return true;
    }
}
