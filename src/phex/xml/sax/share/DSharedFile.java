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
 *  Created on 10.03.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.share;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xml.sax.SAXException;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public class DSharedFile implements DElement
{
    public static final String ELEMENT_NAME = "SF";
    
    private String fileName;
    
    private String sha1;
    
    private String thexRootHash;
    
    private boolean hasThexTreeDepth;
    private int thexTreeDepth;
    
    private String thexLowestLevelNodes;

    private boolean hasCreationTime;
    private long creationTime;
    
    private boolean hasLastModified;
    private long lastModified;

    private boolean hasHitCount;
    private int hitCount;

    private boolean hasLastSeen;
    private long lastSeen;

    private boolean hasUploadCount;
    private int uploadCount;
    
    private List<DAlternateLocation> altLocList;
    
    public DSharedFile()
    {
        altLocList = new ArrayList<DAlternateLocation>();
    }
    
    public List<DAlternateLocation> getAltLocList()
    {
        return altLocList;
    }

    public long getCreationTime()
    {
        return creationTime;
    }

    public void setCreationTime( long creationTime )
    {
        hasCreationTime = true;
        this.creationTime = creationTime;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName( String fileName )
    {
        this.fileName = fileName;
    }

    public int getHitCount()
    {
        return hitCount;
    }

    public void setHitCount( int hitCount )
    {
        hasHitCount = true;
        this.hitCount = hitCount;
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public void setLastModified( long lastModified )
    {
        hasLastModified = true;
        this.lastModified = lastModified;
    }

    public long getLastSeen()
    {
        return lastSeen;
    }

    public void setLastSeen( long lastSeen )
    {
        hasLastSeen = true;
        this.lastSeen = lastSeen;
    }

    public String getSha1()
    {
        return sha1;
    }

    public void setSha1( String sha1 )
    {
        this.sha1 = sha1;
    }

    public String getThexLowestLevelNodes()
    {
        return thexLowestLevelNodes;
    }

    public void setThexLowestLevelNodes( String thexLowestLevelNodes )
    {
        this.thexLowestLevelNodes = thexLowestLevelNodes;
    }

    public String getThexRootHash()
    {
        return thexRootHash;
    }

    public void setThexRootHash( String thexRootHash )
    {
        this.thexRootHash = thexRootHash;
    }

    public int getThexTreeDepth()
    {
        return thexTreeDepth;
    }

    public void setThexTreeDepth( int thexTreeDepth )
    {
        hasThexTreeDepth = true;
        this.thexTreeDepth = thexTreeDepth;
    }

    public int getUploadCount()
    {
        return uploadCount;
    }

    public void setUploadCount( int uploadCount )
    {
        hasUploadCount = true;
        this.uploadCount = uploadCount;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );
        
        if( fileName != null )
        {
            writer.startElm( "FID", null );
            writer.elmText( fileName );
            writer.endElm( "FID" );
        }
        
        if( sha1 != null )
        {
            writer.startElm( "SHA1", null );
            writer.elmText( sha1 );
            writer.endElm( "SHA1" );
        }
        
        if( thexRootHash != null )
        {
            writer.startElm( "TxRH", null );
            writer.elmText( thexRootHash );
            writer.endElm( "TxRH" );
        }
        
        if( hasThexTreeDepth )
        {
            writer.startElm( "TxD", null );
            writer.elmInt( thexTreeDepth );
            writer.endElm( "TxD" );
        }
        
        if( thexLowestLevelNodes != null )
        {
            writer.startElm( "TxLLN", null );
            writer.elmText( thexLowestLevelNodes );
            writer.endElm( "TxLLN" );
        }
        
        if( hasCreationTime )
        {
            writer.startElm( "CT", null );
            writer.elmLong( creationTime );
            writer.endElm( "CT" );
        }
        
        if( hasLastModified )
        {
            writer.startElm( "LM", null );
            writer.elmLong( lastModified );
            writer.endElm( "LM" );
        }
        
        if( hasLastSeen )
        {
            writer.startElm( "LS", null );
            writer.elmLong( lastSeen );
            writer.endElm( "LS" );
        }
        
        if( hasHitCount )
        {
            writer.startElm( "HC", null );
            writer.elmLong( hitCount );
            writer.endElm( "HC" );
        }
        
        if( hasUploadCount )
        {
            writer.startElm( "UC", null );
            writer.elmLong( uploadCount );
            writer.endElm( "UC" );
        }
        
        Iterator iterator = altLocList.iterator();
        while( iterator.hasNext() )
        {
            DElement element = (DElement) iterator.next();
            element.serialize(writer);
        }
        
        writer.endElm( ELEMENT_NAME );
    }
}
