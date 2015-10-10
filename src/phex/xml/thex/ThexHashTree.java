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
 *  Created on 02.11.2003
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.thex;

/**
 * 
 */
public class ThexHashTree
{
    private String fileSize;
    private String fileSegmentSize;
    
    private String digestAlgorithm;
    private String digestOutputSize;
    
    private String serializedTreeDepth;
    private String serializedTreeType;
    private String serializedTreeUri;
    
    /**
     * @return
     */
    public String getDigestAlgorithm()
    {
        return digestAlgorithm;
    }

    /**
     * @return
     */
    public String getDigestOutputSize()
    {
        return digestOutputSize;
    }

    /**
     * @return
     */
    public String getFileSegmentSize()
    {
        return fileSegmentSize;
    }

    /**
     * @return
     */
    public String getFileSize()
    {
        return fileSize;
    }

    /**
     * @return
     */
    public String getSerializedTreeDepth()
    {
        return serializedTreeDepth;
    }

    /**
     * @return
     */
    public String getSerializedTreeType()
    {
        return serializedTreeType;
    }

    /**
     * @return
     */
    public String getSerializedTreeUri()
    {
        return serializedTreeUri;
    }

    /**
     * @param string
     */
    public void setDigestAlgorithm(String string)
    {
        digestAlgorithm = string;
    }

    /**
     * @param string
     */
    public void setDigestOutputSize(String string)
    {
        digestOutputSize = string;
    }

    /**
     * @param string
     */
    public void setFileSegmentSize(String string)
    {
        fileSegmentSize = string;
    }

    /**
     * @param string
     */
    public void setFileSize(String string)
    {
        fileSize = string;
    }

    /**
     * @param string
     */
    public void setSerializedTreeDepth(String string)
    {
        serializedTreeDepth = string;
    }

    /**
     * @param string
     */
    public void setSerializedTreeType(String string)
    {
        serializedTreeType = string;
    }

    /**
     * @param string
     */
    public void setSerializedTreeUri(String string)
    {
        serializedTreeUri = string;
    }

}
