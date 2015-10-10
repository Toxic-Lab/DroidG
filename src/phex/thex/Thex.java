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
 *  Created on 12-Nov-2003
 *  --- CVS Information ---
 *  $Id$
 */
package phex.thex;

import java.util.Vector;

import phex.download.swarming.SWDownloadSegment;

/**
 * @author Laura Requena
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Thex
{

    private String root;

    private String serialization;

    private byte[] serializationByte;

    private int hashSize;

    private Vector leavesHashes;

    private int nodes10serSegment;

    private int firstBlock;

    private int levelsLeft;

    private SWDownloadSegment segment;

    public Thex()
    {
        leavesHashes = new Vector();
    }

    public String getRoot()
    {
        return root;
    }

    public void setSegment(SWDownloadSegment sg)
    {
        segment = sg;
    }

    public SWDownloadSegment getSegment()
    {
        return segment;
    }

    public int getNodes10serSegment()
    {
        return nodes10serSegment;
    }

    public int getFirstBlock()
    {
        return firstBlock;
    }

    public void setLevelsLeft(int levelsL)
    {
        levelsLeft = levelsL;
    }

    public int getLevelsLeft()
    {
        return levelsLeft;
    }

    public void setNodes10serSegment(int nodes10ser)
    {
        nodes10serSegment = nodes10ser;
    }

    public void setFirstBlock(int firstB)
    {
        firstBlock = firstB;
    }

    public Vector getLeavesHashes()
    {
        return leavesHashes;
    }

    public void addLeafHash(byte[] hash)
    {
        leavesHashes.add(hash);
    }

    public String getSerialization()
    {
        return serialization;
    }

    public byte[] getSerializationByte()
    {
        return serializationByte;
    }

    public int getHashSize()
    {
        return hashSize;
    }

    public void setRoot(String root)
    {
        this.root = root;
    }

    public void setSerialization(String serialization)
    {
        this.serialization = serialization;
    }

    public void setSerializationByte(byte[] serializationByte)
    {
        this.serializationByte = serializationByte;
    }

    public void setHashSize(int hashSize)
    {
        this.hashSize = hashSize;
    }

}
