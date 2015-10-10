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
package phex.common;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

//import phex.common.log.NLogger;
import phex.host.Host;
import phex.msg.InvalidMessageException;
import phex.msg.QRPatchTableMsg;
import phex.msg.QRResetTableMsg;
import phex.msg.QueryMsg;
import phex.msg.RouteTableUpdateMsg;
import phex.query.DynamicQueryConstants;
import phex.servent.Servent;
import phex.share.ShareFile;
import phex.utils.IOUtil;
import phex.utils.StringUtils;

/**
 * Query routing table implementation for the QRP.
 */
public class QueryRoutingTable
{
//    private static final Logger logger = LoggerFactory.getLogger( 
//        QueryRoutingTable.class );
    
    /**
     * The default table TTL representing INFINITY.
     * Its set to 7 since everybody is using it that way...
     * I would prefer 2 we only use QRP when we are a shielded leaf but Limewire
     * is not accepting this value...
     */
    public static final byte DEFAULT_INFINITY_TTL = 0x07;

    /**
     * The default table size of the QR table (64KB).
     */
    public static final int DEFAULT_TABLE_SIZE = 64 * 1024;
    
    /**
     * The minimum table size of the QR table (16KB).
     */
    private static final int MIN_TABLE_SIZE = 16 * 1024;
    
    /**
     * The default table size of the QR table (1024KB).
     */
    private static final int MAX_TABLE_SIZE = 1024 * 1024;
    
    /**
     * Maximum fill ratio of the QRT (5%).
     */
    private static final int MAX_FILL_RATIO = 5;

    /**
     * The query routing table. A BitSet is used since the GDF decided that
     * QRP is only used between Leaf and Ultrapeers. The aggregated QRT
     * exchanged between UPs represents the last hop Leaf and UP as a single unit.
     */
    private BitSet qrTable;

    /**
     * The resized QRT is during aggregation when cases the original QRT table
     * size does not match the size of the QRT to aggregate to.
     * We cache this resized QRT since we might need to use it again for
     * aggregation. The original QRT is not replaced by the resized version since
     * we need the original version for accurate query routing.
     */
    private BitSet resizedQRTable;

    /**
     * The size of the table;
     */
    private int tableSize;

    /**
     * This is the number of bits the hashing result is allowed to have.
     * It is the base 2 log of tableSize.
     */
    private byte tableBits;

    /**
     * The infinity value to use.
     */
    private byte infinity;

    /**
     * The number of marked slots in the qrTable.
     */
    private int entryCount;

    /**
     * The total sequence size of the update messages.
     */
    private byte sequenceSize;

    /**
     * The current sequence number of the update messages.
     */
    private byte sequenceNumber;

    /**
     * The position on which to apply the next patch.
     */
    private int patchPosition;

    /**
     * The inflater to use to uncompress.
     */
    private Inflater inflater;


    public QueryRoutingTable()
    {
        init( DEFAULT_TABLE_SIZE, DEFAULT_INFINITY_TTL );
    }
    
    public QueryRoutingTable( int tableSize )
    {
        init( tableSize, DEFAULT_INFINITY_TTL );
    }

    private void init( int newTableSize, byte infinity )
    {
        if ( qrTable == null || tableSize != newTableSize )
        {
            qrTable = new BitSet( tableSize );
        }
        else
        {
            qrTable.clear();
        }
        this.tableSize = newTableSize;
        this.infinity = infinity;
        this.tableBits = IOUtil.calculateLog2( tableSize );
        resizedQRTable = null;
        entryCount = 0;
        sequenceSize = 0;
        sequenceNumber = 0;
        patchPosition = 0;
        inflater = null;
    }
    
    public double getFillRatio()
    {
        return ( (double)qrTable.cardinality() / (double)tableSize ) * 100.0;
    }
    
    public int getTableSize()
    {
        return tableSize;
    }

    public void aggregateToRouteTable( QueryRoutingTable queryRoutingTable )
    {
        BitSet bitSetToAggregate;
        if ( tableSize != queryRoutingTable.tableSize )
        {
            bitSetToAggregate = queryRoutingTable.resizeRouteTable( tableSize );
        }
        else
        {
            bitSetToAggregate = queryRoutingTable.qrTable;
        }
        if ( bitSetToAggregate == null )
        {// try to find out what went wrong here...
            //logger.error( "Aggregate source is null: {}/{}",
                //tableSize, queryRoutingTable.tableSize );
        }
        qrTable.or( bitSetToAggregate );
    }

    /**
     * Adds a character sequence to the qrt. This sequence is split into words
     * each word is added separately into the qrt.
     * @param absoluteFilePath the character sequence to add.
     */
    private void add( String absoluteFilePath )
    {
        String[] words = splitFilePath( absoluteFilePath );
        for ( int i = 0; i < words.length; i++ )
        {
            int hashVal = qrpHash( words[i], 0, words[i].length(), tableBits );
            if ( !qrTable.get( hashVal ) )
            {
                entryCount++;
                // instead of TTL just mark slot
                qrTable.set( hashVal );
                resizedQRTable = null;
            }
        }
    }

    /**
     * Adds a single word into the qrt the word is hashed without modification.
     * @param singleWord the word to add to the qrt without modification.
     */
    private void addWord( String singleWord )
    {
        int hashVal = qrpHash( singleWord, 0, singleWord.length(), tableBits );
        if ( !qrTable.get( hashVal ) )
        {
            entryCount++;
            // instead of TTL just mark slot
            qrTable.set( hashVal );
            resizedQRTable = null;
        }
    }

    /**
     * Checks if the QRT has the hash value of the given search string flagged.
     * @param searchString the search string to check.
     * @return true if the hash of the search string is flagged.
     */
    public boolean containsQuery( QueryMsg query )
    {
        String searchString = query.getSearchString();
        boolean isInvalidSearchString = ( searchString == null || 
            searchString.length() < DynamicQueryConstants.MIN_SEARCH_TERM_LENGTH );
        
        if ( query.hasQueryURNs() && isInvalidSearchString )
        {
            URN[] urns = query.getQueryURNs();
            for( int i = 0; i < urns.length; i++ )
            {
                String urnString = urns[i].getAsString();
                int hashVal = qrpHash( urnString, 0, urnString.length(), tableBits );
                if( qrTable.get( hashVal ) )
                {// if we have a single match we have a match here.
                    return true;
                }
            }
            // no urn matches...
            return false;
        }
        
        if ( isInvalidSearchString )
        {
            return false;
        }

        String[] words = splitQueryString( searchString );
        for ( int i = 0; i < words.length; i++ )
        {
            int hashVal = qrpHash( words[i], 0, words[i].length(), tableBits );
            if ( !qrTable.get( hashVal ) )
            {
                return false;
            }
        }
        return true;
    }

    public void updateRouteTable( RouteTableUpdateMsg message, Host sourceHost )
        throws InvalidMessageException
    {
        if ( message.getVariant() == RouteTableUpdateMsg.RESET_TABLE_VARIANT )
        {
            int msgTableSize = ((QRResetTableMsg)message).getTableSize();
            //logger.debug( "Reseting QRT from: {} - Size: {}",
             //   sourceHost, msgTableSize );
			// ignoring INFINITY :-( since all seem to be using 7
            init( msgTableSize, infinity );
        }
        else if ( message.getVariant() == RouteTableUpdateMsg.PATCH_TABLE_VARIANT )
        {
            QRPatchTableMsg patchMessage = (QRPatchTableMsg)message;

            // Validate message info...
            byte msgSequenceSize = patchMessage.getSequenceSize();
            byte msgSequenceNumber = patchMessage.getSequenceNumber();
//            NLogger.debug( QueryRoutingTable.class,
//				"Patching QRT from: " + sourceHost + " " + 
//				msgSequenceNumber + "/" + msgSequenceSize );

            if ( sequenceSize == 0 && sequenceNumber == 0 )
            {// initializing case...
                sequenceSize = msgSequenceSize;
                sequenceNumber = 1;
            }

            if ( sequenceSize != msgSequenceSize || msgSequenceNumber != sequenceNumber
                || msgSequenceSize == 0 )
            {
                throw new InvalidMessageException(
                    "QRTPatchMsg sequence size or number not valid.\n" +
                    "Size: (" + msgSequenceSize + "/" + sequenceSize + ").\n" +
                    "Number: (" + msgSequenceNumber + "/" + sequenceNumber + ").");
            }
            sequenceNumber = msgSequenceNumber;

            byte[] patchData = patchMessage.getPatchData();

            byte compressor = patchMessage.getCompressor();
            if ( compressor == QRPatchTableMsg.COMPRESSOR_ZLIB )
            {
                if ( sequenceNumber == 1 )
                {
                    inflater = new Inflater();
                }
                try
                {
                    patchData = IOUtil.inflate( inflater, patchData );
                }
                catch ( DataFormatException exp )
                {
                    patchData = null;
                    // in case the inflate data format does not work.
//                    if ( NLogger.isWarnEnabled(QueryRoutingTable.class) )
//                    {
//                        NLogger.warn(QueryRoutingTable.class,
//                            "Invalid QRT data format to inflate.", exp );
//                    }
                }
                if ( patchData == null )
                {
                    throw new InvalidMessageException(
                        "Can't inflate patch data" );
                }
            }
            else if ( compressor != QRPatchTableMsg.COMPRESSOR_NONE )
            {// validation fails..
                throw new InvalidMessageException(
                    "QRTPatchMsg Unknown compression: " + compressor );
            }

            // expand patch data.. this consumes extra memory... can't we do
            // it on the fly?
            byte entryBits = patchMessage.getEntryBits();
            if ( entryBits == 4 )
            {
                byte[] buf = new byte[ patchData.length * 2 ];
                byte tmpVal;
                for ( int i = 0; i < patchData.length; i++ )
                {
                    buf[ i * 2 ] = (byte)(patchData[i]>>4);
                    tmpVal = (byte)(patchData[i] & 0x0F);
                    if ( (tmpVal & 0x08) != 0 )
                    {
                        tmpVal = (byte)(0xF0 | tmpVal);
                    }
                    buf[ i * 2 + 1 ] = tmpVal;
                }
                patchData = buf;
            }
            else if ( entryBits != 8 )
            {// validation fails..
                throw new InvalidMessageException(
                    "QRTPatchMsg Unknown ENTRY_BITS value: " + entryBits );
            }

            try
            {
                // used to determine if a new entry was set
                boolean prevBitSet, currBitSet;
                int loggedInvalidPatchFieldValue = 0;
                for ( int i = 0; i < patchData.length; i++ )
                {
                    prevBitSet = qrTable.get( patchPosition );
                    
                    //if ( patchData[i] == 1 - infinity )
                    if ( patchData[i] < 0 )// use this to also accept invalid clients
                    {
                        qrTable.set( patchPosition );
                        resizedQRTable = null;
                    }
                    //else if ( patchData[i] == infinity - 1 )
                    else if ( patchData[i] > 0 )// use this to also accept invalid clients
                    {
                        qrTable.clear( patchPosition );
                        resizedQRTable = null;
                    }
                    else if ( patchData[i] != 0 )
                    {// we received a QRT with a patch data value that is not
                     // really in range...
                     // we like to log each value only once therefore we
                     // flag the logged value
                        if ( loggedInvalidPatchFieldValue == 0 ||
                             loggedInvalidPatchFieldValue != patchData[i] )
                        {
//                            NLogger.warn(QueryRoutingTable.class,
//                                "Received invalid PatchData field value: " 
//                                + patchData[i] + " - " + sourceHost );
                            loggedInvalidPatchFieldValue = patchData[i];
                        }
                    }
                    currBitSet = qrTable.get( patchPosition );
                    if ( prevBitSet && !currBitSet )
                    {
                        entryCount --;
                    }
                    else if ( !prevBitSet && currBitSet )
                    {
                        entryCount ++;
                    }
                    patchPosition ++;
                }
            }
            catch ( IndexOutOfBoundsException exp )
            {
                throw new InvalidMessageException(
                    "QRTPatchMsg Wrong patch message data size." );
            }

            if ( sequenceNumber == sequenceSize )
            {
                sequenceSize = 0;
                sequenceNumber = 0;
                patchPosition = 0;
                inflater = null;
//                NLogger.debug( QueryRoutingTable.class,
//                    "Updated QRT: " + entryCount + " / " + tableSize );
            }
            else
            {
                sequenceNumber ++;
            }
        }
    }

    /**
     * Returns a resized version of this QueryRoutingTable that matches the new
     * given size. The returned QRT is either newly generated or the a catched
     * version is used.
     * @param newSize the new size of the QRT
     * @return
     */
    private BitSet resizeRouteTable( int newSize )
    {
        if ( tableSize == newSize )
        {
            // no need to resize...
            return qrTable;
        }

        if ( resizedQRTable != null && resizedQRTable.size() == newSize )
        {
            return resizedQRTable;
        }

        resizedQRTable = new BitSet( newSize );

        // create the resized version by using a simple algorithm.
        double factor = ( (double)newSize ) / ( (double)tableSize );

        for ( int i = 0; i < tableSize; i++ )
        {
            if ( !qrTable.get( i ) )
            {// bit not set... don't need to handle...
                continue;
            }

            int from = (int)Math.floor( i * factor );
            int to = (int)Math.ceil( (i+1) * factor );

            for (int j = from; j < to; j++)
            {
                resizedQRTable.set( j );
            }
        }
        return resizedQRTable;
    }


    /**
     * Splits a file path into pieces and takes creates a array of the pieces
     * and there prefixes.
     */
    private static String[] splitQueryString( String queryString )
    {
        StringTokenizer tokenizer = new StringTokenizer( queryString,
            StringUtils.FILE_DELIMITERS );
        ArrayList<String> list = new ArrayList<String>( 10 );
        while( tokenizer.hasMoreTokens() )
        {
            String word = tokenizer.nextToken();
            list.add( word );
        }
        String[] strArr = new String[ list.size() ];
        list.toArray( strArr );
        return strArr;
    }

    /**
     * Splits a file path into pieces and takes creates a array of the pieces
     * and there prefixes.
     */
    private static String[] splitFilePath( String filePath )
    {
        StringTokenizer tokenizer = new StringTokenizer( filePath,
            StringUtils.FILE_DELIMITERS );
        ArrayList<String> list = new ArrayList<String>( 20 );
        while( tokenizer.hasMoreTokens() )
        {
            String word = tokenizer.nextToken();
            list.add( word );

            // generate prefix strings
            int length = word.length();
            for ( int i = 1; i < 5 && (length - i) > 5; i++ )
            {
                list.add( word.substring( 0, length - i ) );
            }
        }
        String[] strArr = new String[ list.size() ];
        list.toArray( strArr );
        return strArr;
    }

    /**
     * Creates the current local QueryRoutingTable that is used to update
     * remote clients.
     */
    public static QueryRoutingTable createLocalQueryRoutingTable( List<ShareFile> sharedFiles )
    {
        long start = System.currentTimeMillis();
        HashSet<String> wordSet = new HashSet<String>();
        for ( ShareFile file : sharedFiles )
        {
            // add urn:sha1:xxx
            //URN urn = sharedFiles[i].getURN();
            //if ( urn != null )
            //{
            //    wordSet.add( urn.getAsString() );
            //}

            // add splitted words.
            String[] words = splitFilePath( file.getSystemFile().getAbsolutePath() );
            for ( int j = 0; j < words.length; j++ )
            {
                wordSet.add( words[ j ] );
            }
        }
        
        QueryRoutingTable qrTable = new QueryRoutingTable( MIN_TABLE_SIZE );
        while( true )
        {
            fillLocalQRTWithShare( qrTable, wordSet );
            if ( qrTable.tableSize < MAX_TABLE_SIZE && qrTable.getFillRatio() > MAX_FILL_RATIO )
            {
                qrTable.init( qrTable.tableSize * 2, DEFAULT_INFINITY_TTL );
                continue;
            }
            break;
        }

        long end = System.currentTimeMillis();
//        NLogger.debug( QueryRoutingTable.class,
//            "Created QRT: " + qrTable.entryCount + " / " + qrTable.tableSize
//            + " time: " + ( end - start) );
        return qrTable;
    }

    private static void fillLocalQRTWithShare( QueryRoutingTable qrTable, HashSet<String> wordSet )
    {
        int counter = 0;
        for ( String word : wordSet )
        {
            qrTable.addWord( word );
            counter ++;
            // check if the table is already full
            if ( counter%1000 == 0 && qrTable.tableSize < MAX_TABLE_SIZE && qrTable.getFillRatio() > MAX_FILL_RATIO )
            {
                return;
            }
        }
    }
    
    /**
     * Fills the given QueryRoutingTable with the last received 
     * QueryRoutingTables of connected leafs, when the servent is an ultrapeer.
     * @param qrTable the QueryRoutingTable to fill.
     * @param servent the Servent to act for.
     */
    public static void fillQRTWithLeaves( QueryRoutingTable qrTable, Servent servent )
    {
        if ( !servent.isUltrapeer() )
        {
            return;
        }
        // add QRT of leafs...
        Host[] leaves = servent.getHostService().getNetworkHostsContainer().getLeafConnections();
        QueryRoutingTable hostQRT;
        for ( int i = 0; i < leaves.length; i++ )
        {
            // http://groups.yahoo.com/group/the_gdf/message/23092
            if ( leaves[i].getHopsFlowLimit() < 3 )
            {// don't aggregate QRT of a leaf which doesn't want any queries...
                continue;
            }
            hostQRT = leaves[i].getLastReceivedRoutingTable();
            if ( hostQRT != null )
            {
                qrTable.aggregateToRouteTable( hostQRT );
            }
        }
    }

    public static Iterator<RouteTableUpdateMsg> buildRouteTableUpdateMsgIterator( QueryRoutingTable currentTable,
        QueryRoutingTable oldTable )
    {
        ArrayList<RouteTableUpdateMsg> msgList = new ArrayList<RouteTableUpdateMsg>();

        if ( oldTable == null )
        {
            // never sent a table before... send reset msg first
            msgList.add( new QRResetTableMsg( currentTable.tableSize,
                currentTable.infinity ) );
        }

        boolean isPatchNeeded = false;
        // we always send 4 signed bits tables, therefore we only need to patch half
        // table size.
        byte[] patchData = new byte[ currentTable.tableSize / 2 ];

        for ( int i = 0; i < patchData.length; i++ )
        {
            byte b1;
            byte b2;
            if ( oldTable == null )
            {
                if ( currentTable.qrTable.get( i * 2 ) )
                {
                    b1 = (byte)(1 - currentTable.infinity);
                }
                else
                {
                    b1 = (byte)0;
                }
                if ( currentTable.qrTable.get( i * 2 + 1) )
                {
                    b2 = (byte)(1 - currentTable.infinity);
                }
                else
                {
                    b2 = (byte)0;
                }
            }
            else
            {
                boolean currentVal = currentTable.qrTable.get( i * 2 );
                if ( currentVal == oldTable.qrTable.get( i * 2 ) )
                {
                    b1 = (byte)0;
                }
                else if ( currentVal )
                {
                    b1 = (byte)(1 - currentTable.infinity);
                }
                else
                {
                    b1 = (byte)(currentTable.infinity - 1);
                }
                currentVal = currentTable.qrTable.get( i * 2 + 1);
                if ( currentVal == oldTable.qrTable.get( i * 2 + 1) )
                {
                    b2 = (byte)0;
                }
                else if ( currentVal )
                {
                    b2 = (byte)(1 - currentTable.infinity);
                }
                else
                {
                    b2 = (byte)(currentTable.infinity - 1);
                }
            }
            patchData[i] = (byte)( ( b1 << 4 ) | ( b2 & 0x0F ) );

            // check if we need a patch
            if ( patchData[i] != 0 )
            {
                isPatchNeeded = true;
            }
        }

        if ( !isPatchNeeded )
        {// no patch message needed
            return msgList.iterator();
        }

        // try to compress data
        byte compressor = QRPatchTableMsg.COMPRESSOR_NONE;
        byte[] compressedPatchData = IOUtil.deflate( patchData );
        // verify if compressing made sense...
        if ( compressedPatchData.length < patchData.length )
        {
            patchData = compressedPatchData;
            compressor = QRPatchTableMsg.COMPRESSOR_ZLIB;
        }

        // build patch messages
        // 1KB max message size was proposed...

        byte sequenceSize = (byte)Math.ceil( (double)patchData.length /
            (double)QRPatchTableMsg.MAX_MESSAGE_DATA_SIZE );
        byte sequenceNo = 1;
        int offset = 0;
        do
        {
            int length = Math.min( QRPatchTableMsg.MAX_MESSAGE_DATA_SIZE,
                patchData.length - offset );

            QRPatchTableMsg msg = new QRPatchTableMsg( sequenceNo, sequenceSize,
                // we patch into 0x04 entry bits since all bits are between -1 and 1
                compressor, (byte)4, patchData, offset, length );
            msgList.add( msg );
            offset += length;
            sequenceNo ++;
        }
        while ( offset < patchData.length );
        return msgList.iterator();
    }


    ////////////////////////////////////////////////////////////////////////////
    // Optimized Code from Limewire for QRP String hashing                    //
    ////////////////////////////////////////////////////////////////////////////

    /*
     * The official platform-independent hashing function for query-routing.  The
     * key property is that it allows interpolation of hash tables of different
     * sizes.  More formally k*hash(x,n)<=hash(x, kn)<=k*hash(x,n)+1.<p>
     *
     * This experimental version does not necessarily work cross-platform,
     * however, nor is it secure in any sense.   See Chapter 12.3.2. of
     * for details of multiplication-based algorithms.
     */

    /**
     */
    private static final int A_INT = 0x4F1BBCDC;

    /**
     * Returns the same value as hash(x.substring(start, end), bits),
     * but tries to avoid allocations.  Note that x is lower-cased
     * when hashing.
     *
     * @param x the string to hash
     * @param bits the number of bits to use in the resulting answer
     * @param start the start offset of the substring to hash
     * @param end just PAST the end of the substring to hash
     * @return the hash value
     */
    private static int qrpHash( String x, int start, int end, byte bits )
    {
        //1. First turn x[start...end-1] into a number by treating all 4-byte
        //chunks as a little-endian quadword, and XOR'ing the result together.
        //We pad x with zeroes as needed.
        //    To avoid having do deal with special cases, we do this by XOR'ing
        //a rolling value one byte at a time, taking advantage of the fact that
        //x XOR 0==x.
        int xor=0;  //the running total
        int j=0;    //the byte position in xor.  INVARIANT: j==(i-start)%4
        for (int i=start; i<end; i++)
        {
            //TODO: internationalization be damned?
            int b = Character.toLowerCase(x.charAt(i)) & 0xFF;
            b = b<<(j*8);
            xor = xor^b;
            j = (j+1)%4;
        }
        //2. Now map number to range 0 - (2^bits-1).
        //Multiplication-based hash function.  See Chapter 12.3.2. of CLR.
        long prod= (long)xor * (long)A_INT;
        long ret= prod << 32;
        ret = ret >>> (32 + (32 - bits));
        return (int)ret;
    }
}