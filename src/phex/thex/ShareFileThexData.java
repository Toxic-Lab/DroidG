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
 *  Created on 17.06.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.thex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.bitzi.util.Base32;

/**
 *
 */
public class ShareFileThexData
{
    private String rootHash;
    private List<byte[]> lowestLevelNodes;
    private int treeDepth;
    

    /**
     * 
     */
    public ShareFileThexData( byte[] rootHash, 
        List<byte[]> lowestLevelNodes, int depth )
    {
        this.rootHash = Base32.encode( rootHash );
        this.lowestLevelNodes = lowestLevelNodes;
        this.treeDepth = depth;
    }
    
    public ShareFileThexData( String rootHash, 
        String xjbLowestLevelNodes, int depth )
    {
        this.rootHash = rootHash;
        this.lowestLevelNodes = parseXJBLowestLevelNodes( xjbLowestLevelNodes );
        this.treeDepth = depth;
    }
    
    public String getRootHash()
    {
        return rootHash;
    }
    
    public int getTreeDepth()
    {
        return treeDepth;
    }
    
    public byte[] getSerializedTreeNodes()
    {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        List<List<byte[]>> allNodes = TTHashCalcUtils.calculateMerkleParentNodes( 
            lowestLevelNodes );
        Iterator<List<byte[]>> iterator = allNodes.iterator();
        try
        {
            while ( iterator.hasNext() )
            {
                Iterator<byte[]> subIterator = (iterator.next()).iterator();
                while ( subIterator.hasNext() )
                {
                    outStream.write( subIterator.next() );
                }
            }
        }
        catch (IOException exp)
        {// this should never happen!
            //NLogger.error( ShareFileThexData.class, exp, exp );
            throw new RuntimeException(exp);
        }
        return outStream.toByteArray();
    }
    
    public String getXJBLowestLevelNodes()
    {
        Iterator<byte[]> iterator = lowestLevelNodes.iterator();
        StringBuffer xjbString = new StringBuffer();
        while( iterator.hasNext() )
        {
            byte[] nodeData = iterator.next();
            String node = new String( org.bouncycastle.util.encoders.Base64.encode( nodeData ) );
            xjbString.append( node );
            xjbString.append( "-" );
            
        }
        return xjbString.toString();
    }
    
    /**
     * @param rootHash2
     * @param xjbLowestLevelNodes
     * @param depth
     */
    public void updateFromCache( String rootHash, String xjbLowestLevelNodes, int depth )
    {
        this.rootHash = rootHash;
        this.lowestLevelNodes = parseXJBLowestLevelNodes( xjbLowestLevelNodes );
        this.treeDepth = depth;
    }
    
    private static List<byte[]> parseXJBLowestLevelNodes( String xjbString )
    {
        StringTokenizer tokenizer = new StringTokenizer( xjbString, "-");
        List<byte[]> list = new ArrayList<byte[]>();
        while ( tokenizer.hasMoreTokens() )
        {
            String node = tokenizer.nextToken();
            byte[] nodeData = org.bouncycastle.util.encoders.Base64.decode( node.getBytes() );
            list.add( nodeData );
        }
        return list;
    }
    
    
    
//    public int calculateTotalNodeCount()
//    {
//        int prev = lowestLevelNodes.size();
//        int count = prev;
//        for ( int i = treeDepth - 1; i >= 0; i++ )
//        {
//            prev = (int)Math.ceil( prev / 2.0 );
//            count += prev;
//        }
//        return count;
//    }
}
