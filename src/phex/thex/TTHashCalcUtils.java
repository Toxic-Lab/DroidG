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
 *  Created on 02.06.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.thex;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileInputStream;
import phex.common.format.NumberFormatUtils;
import phex.download.ThexVerificationData.ThexData;
import phex.prefs.core.LibraryPrefs;
import phex.share.ShareFile;
import phex.utils.IOUtil;

import com.bitzi.util.Tiger;
import com.bitzi.util.TigerTree;

/**
 *
 */
public class TTHashCalcUtils
{
    private static transient final byte MERKLE_IH_PREFIX = 0x01;
    private static final int THEX_BLOCK_SIZE = 1024;
    private static final int HASH_SIZE = 24;
    
    /**
     * Calculates the ShareFileThexData of the given ShareFile.
     */
    public static void calculateShareFileThexData( ShareFile shareFile )
        throws IOException 
    {
        if ( shareFile.getThexData( null ) != null )
        {
            return;
        }
        
        long fileSize = shareFile.getFileSize();
        int levels = getTreeLevels( fileSize );
        int nodeSize = getTreeNodeSize( fileSize, levels );
        
        BufferedInputStream inStream = new BufferedInputStream( 
            new FileInputStream( shareFile.getSystemFile() ) );
        List<byte[]> lowestLevelNodes = calculateTigerTreeNodes( nodeSize, fileSize, inStream );
        
        // calculate the full Merkle tree.
        List<List<byte[]>> merkleTreeNodes = calculateMerkleParentNodes( lowestLevelNodes );
        byte[] rootHash = merkleTreeNodes.get(0).get(0);
        int depth = merkleTreeNodes.size() - 1;
        
        ShareFileThexData data = new ShareFileThexData( rootHash, lowestLevelNodes, 
            depth );
        shareFile.setThexData( data );
    }
    
    /**
     * Returns the number of levels to use for the hash tree.
     * We use a static table, this is much faster then the original level loop.
     * @param fileSize
     * @return the number of levels.
     */
    public static int getTreeLevels(long fileSize)
    {
        if (fileSize < 256 * NumberFormatUtils.ONE_KB)
            return 0;
        else if (fileSize < 512 * NumberFormatUtils.ONE_KB)
            return 1;
        else if (fileSize < NumberFormatUtils.ONE_MB)
            return 2;
        else if (fileSize < 2 * NumberFormatUtils.ONE_MB)
            return 3;
        else if (fileSize < 4 * NumberFormatUtils.ONE_MB)
            return 4;
        else if (fileSize < 8 * NumberFormatUtils.ONE_MB)
            return 5;
        else if (fileSize < 16 * NumberFormatUtils.ONE_MB)
            return 6;
        else if (fileSize < 32 * NumberFormatUtils.ONE_MB)
            return 7;
        else if (fileSize < 64 * NumberFormatUtils.ONE_MB)
            return 8;
        else if (fileSize < 256 * NumberFormatUtils.ONE_MB)
            return 9;
        else if (fileSize < 1024 * NumberFormatUtils.ONE_MB)
            return 10;
        else
            return 11;
    }

    /**
     * Returns the tree node size.
     * The result should be the next larger power of 2 from the file size.  
     */
    public static int getTreeNodeSize(long fileSize, int depth)
    {
        // max number of nodes
        // Java 6 - Math.scalb() might perform faster.
        int nodes = (int)Math.pow(2, depth);
        
        // "calculated" file part node size
        int fileNodeSize = (int)Math.ceil( (double)fileSize / (double)nodes );
        
        // Node size must be the next larger power of 2 from the file size.
        int pow = IOUtil.calculateCeilLog2( fileNodeSize );
        // Java 6 - Math.scalb() might perform faster.
        int nodeSize = (int)Math.pow(2, pow);
        return nodeSize;
    }
    
    
    /**
     * Calcualtes the TigerTree nodes with the given nodeSize. 
     * 
     * @param nodeSize Must be 2^n (n>=10) for Merkle HashTree
     */
    private static List<byte[]> calculateTigerTreeNodes( int nodeSize, long fileSize,
        InputStream inStream ) throws IOException
    {
        int thexCalculationMode = LibraryPrefs.ThexCalculationMode.get().intValue();
        int nodeCount = (int)Math.ceil((double)fileSize/(double)nodeSize);
        List<byte[]> nodeList = new ArrayList<byte[]>( nodeCount );
        
        MessageDigest tigerTreeDigest = new TigerTree();
        long totalRead = 0;
        int readCount = 0;
        byte[] buffer = new byte[THEX_BLOCK_SIZE * 128];
        while ( totalRead < fileSize && readCount != -1)
        {
            tigerTreeDigest.reset();
            int nodePos = 0;
            // read out of an single node... and add digest to nodeList
            long start = System.currentTimeMillis();
            while ( nodePos < nodeSize 
                    && (readCount = inStream.read( buffer ) ) != -1 )
            {
                tigerTreeDigest.update( buffer, 0, readCount );
                nodePos += readCount;
                totalRead += readCount;
                try
                {
                    long end = System.currentTimeMillis();
                    Thread.sleep( (end - start) * thexCalculationMode );
                }
                catch (InterruptedException ie)
                {
                    // reset interrupted flag
                    Thread.currentThread().interrupt();
                    throw new IOException( "Hashing file interrupted." );
                }
                start = System.currentTimeMillis();
            }
            nodeList.add(tigerTreeDigest.digest());
            
            if ( readCount == -1 && totalRead != fileSize )
            {
                //NLogger.error( ThexCalculationWorker.class, "Hashing file failed.");
                throw new IOException( "Hashing file failed." );
            }
        }
        return nodeList;
    }
    
    /*
     * Iterative method to generate the parent nodes of an arbitrary
     * depth.
     *
     * The 0th element of the returned List will always be a List of size
     * 1, containing a byte[] of the root hash.
     */
    public static List<List<byte[]>> calculateMerkleParentNodes( List<byte[]> childNodes )
    {
        List<List<byte[]>> merkleTreeNodes = new ArrayList<List<byte[]>>();
        merkleTreeNodes.add( Collections.unmodifiableList( childNodes ) );
        
        List tmpNodes = childNodes;
        while ( tmpNodes.size() > 1 )
        {
            // calculate Merkle hash tree parent for node
            MessageDigest md = new Tiger();
            int size = (int) Math.ceil(tmpNodes.size() / 2.0);
            List<byte[]> parentNodes = new ArrayList<byte[]>(size);
            Iterator iterator = tmpNodes.iterator();
            while ( iterator.hasNext() )
            {
                byte[] left = (byte[]) iterator.next();
                if ( iterator.hasNext() )
                {
                    byte[] right = (byte[]) iterator.next();
                    md.reset();
                    md.update(MERKLE_IH_PREFIX);
                    md.update(left, 0, left.length);
                    md.update(right, 0, right.length);
                    byte[] result = md.digest();
                    parentNodes.add(result);
                }
                else
                {
                    parentNodes.add(left);
                }
            }
            merkleTreeNodes.add(0, parentNodes);
            tmpNodes = parentNodes;
        }
        return merkleTreeNodes;
    }
    
    
    
    public static List<List<byte[]>> resolveMerkleNodes( byte[] data, long fileSize ) throws IOException 
    {
        int levels = getTreeLevels( fileSize );
        List<byte[]> hashList = new ArrayList<byte[]>();

        if (data.length % HASH_SIZE != 0) 
        {
            throw new IOException( "invalid hash tree size." );
        }

        // split the hashes arrays
        for (int i = 0; i + HASH_SIZE <= data.length; i += HASH_SIZE)
        {
            byte[] hash = new byte[HASH_SIZE];
            System.arraycopy(data, i, hash, 0, HASH_SIZE);
            hashList.add(hash);
        }
        
        List<List<byte[]>> merkleTreeNodes = new ArrayList<List<byte[]>>( levels + 1 );
        List<byte[]> parentRow = null;
        List<byte[]> currentRow = null;
        
        Iterator<byte[]> hashListIterator = hashList.iterator();
        if ( !hashListIterator.hasNext() )
        {
            throw new IOException( "missing root hash." );
        }
        
        byte[] root = hashListIterator.next();
        parentRow = new ArrayList<byte[]>( 1 );
        parentRow.add( root );
        merkleTreeNodes.add( Collections.unmodifiableList( parentRow ) );
        
        currentRow = new ArrayList<byte[]>( 2 );
        int rowIndex = 1;
        boolean verified = true;
        
        while (rowIndex <= levels && hashListIterator.hasNext()) 
        {
            verified = false;
            byte[] hash = hashListIterator.next();
            currentRow.add( hash );
            if ( currentRow.size() > parentRow.size() * 2 ) 
            {
                throw new IOException("hash tree is corrupt.");
            } 
            
            if (  currentRow.size() == parentRow.size() * 2 - 1 
               || currentRow.size() == parentRow.size() * 2 )
            {
                if( verifyMerkleChildToParent( currentRow, parentRow ) ) 
                {
                    // current row is valid..
                    parentRow = currentRow;
                    merkleTreeNodes.add( Collections.unmodifiableList( currentRow ) );
                    rowIndex++;

                    if( rowIndex <= levels && hashListIterator.hasNext() )
                    {
                        currentRow = new ArrayList<byte[]>( parentRow.size() * 2 );
                    }
                    verified = true;
                }
            }
        }
        if(!verified)
        {
            throw new IOException( "hash tree is corrupt." );
        }
        return merkleTreeNodes;
    }
    
    private static boolean verifyMerkleChildToParent( List<byte[]> childNodes, List<byte[]> expectedParentNodes )
    {
        List<byte[]> parentNodeListOfChilds = calculateMerkleParentRow( childNodes );
        if ( parentNodeListOfChilds.size() != expectedParentNodes.size() )
        {
            return false;
        }
        
        int size = expectedParentNodes.size();
        for ( int i = 0; i < size; i++ )
        {
            byte[] nodes = parentNodeListOfChilds.get(i);
            byte[] expectedNodes = expectedParentNodes.get(i);
            if( !Arrays.equals( nodes, expectedNodes ) )
            {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Calculates the parent row of the Merkle HashTree for a given child
     * row.
     */
    private static List<byte[]> calculateMerkleParentRow( List<byte[]> childNodes )
    {
        MessageDigest md = new Tiger();
        int size = childNodes.size();        
        List<byte[]> parentRow = new ArrayList<byte[]>( (int)Math.ceil( size/2.0 ) );
        Iterator<byte[]> childNodesIterator = childNodes.iterator();
        while ( childNodesIterator.hasNext() ) 
        {
            byte[] leftNode = childNodesIterator.next();
            if ( !childNodesIterator.hasNext() )
            {
                parentRow.add(leftNode);
                continue;
            }            
            byte[] rightNode = childNodesIterator.next();
            md.reset();
            md.update( MERKLE_IH_PREFIX );
            md.update( leftNode, 0, leftNode.length );
            md.update( rightNode, 0, rightNode.length );
            byte[] result = md.digest();
            parentRow.add(result);
        }
        return parentRow;
    }
    
    /**
     * 
     */
    public static boolean verifyTigerTreeHash( ThexData thexData, ManagedFile managedFile,
        long offset, long length )
    {
        ManagedFileInputStream inStream = new ManagedFileInputStream( managedFile, offset );
        
        int thexCalculationMode = LibraryPrefs.ThexCalculationMode.get().intValue();
        MessageDigest tigerTreeDigest = new TigerTree();
        long totalRead = 0;
        int readCount = 0;
        byte[] buffer = new byte[THEX_BLOCK_SIZE * 128];
        try
        {
            while ( totalRead < length && (readCount = inStream.read( buffer )) != -1)
            {
                long start = System.currentTimeMillis();
                tigerTreeDigest.update( buffer, 0, readCount );
                totalRead += readCount;
                try
                {
                    long end = System.currentTimeMillis();
                    Thread.sleep( (end - start) * thexCalculationMode );
                }
                catch (InterruptedException ie)
                {
                    // reset interrupted flag
                    Thread.currentThread().interrupt();
                    throw new IOException( "Hashing file interrupted." );
                }            
            }
        }
        catch ( IOException exp )
        {
            return false;
        }
        byte[] hash = tigerTreeDigest.digest();
        byte[] expected = thexData.getNodeHash( (int)(offset / thexData.getNodeSize()) );
        
        boolean verifyed = Arrays.equals( hash, expected );
        return verifyed;
    }
}
