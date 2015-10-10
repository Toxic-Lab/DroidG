/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2005 Phex Development Group
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
 *  Created on 01.11.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.downloads;

import org.xml.sax.SAXException;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;

public class DDownloadCandidate implements DElement
{
    public static final String ELEMENT_NAME = "candidate";
   
    private String guid;
    
    private boolean hasFileIndex;
    private long fileIndex;
    
    private String fileName;
    private String downloadUri;
    private String resourceUrn;
    private String remoteHost;
    private String vendor;
    
    private boolean hasConnectionFailedRepetition;
    private int connectionFailedRepetition;
    
    private boolean hasPushNeeded;
    private boolean isPushNeeded;
    
    private boolean hasThexSupported;
    private boolean isThexSupported;
    
    private boolean hasChatSupported;
    private boolean isChatSupported;
    
    private boolean hasLastConnectionTime;
    private long lastConnectionTime;
    
    public int getConnectionFailedRepetition()
    {
        return connectionFailedRepetition;
    }

    public void setConnectionFailedRepetition( int connectionFailedRepetition )
    {
        this.connectionFailedRepetition = connectionFailedRepetition;
        hasConnectionFailedRepetition = true;
    }

    public String getDownloadUri()
    {
        return downloadUri;
    }

    public void setDownloadUri( String downloadUri )
    {
        this.downloadUri = downloadUri;
    }

    public String getResourceUrn()
    {
        return resourceUrn;
    }

    public void setResourceUrn(String resourceUrn)
    {
        this.resourceUrn = resourceUrn;
    }

    public long getFileIndex()
    {
        return fileIndex;
    }

    public void setFileIndex( long fileIndex )
    {
        this.fileIndex = fileIndex;
        hasFileIndex = true;
    }

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName( String fileName )
    {
        this.fileName = fileName;
    }

    public String getGuid()
    {
        return guid;
    }

    public void setGuid( String guid )
    {
        this.guid = guid;
    }

    public boolean isChatSupported()
    {
        return isChatSupported;
    }

    public void setChatSupported( boolean isChatSupported )
    {
        this.isChatSupported = isChatSupported;
        hasChatSupported = true;
    }

    public boolean isPushNeeded()
    {
        return isPushNeeded;
    }

    public void setPushNeeded( boolean isPushNeeded )
    {
        this.isPushNeeded = isPushNeeded;
        hasPushNeeded = true;
    }

    public boolean isThexSupported()
    {
        return isThexSupported;
    }

    public void setThexSupported( boolean isThexSupported )
    {
        this.isThexSupported = isThexSupported;
        hasThexSupported = true;
    }

    public long getLastConnectionTime()
    {
        return lastConnectionTime;
    }

    public void setLastConnectionTime( long lastConnectTime )
    {
        this.lastConnectionTime = lastConnectTime;
        hasLastConnectionTime = true;
    }
    
    public boolean isSetLastConnectionTime()
    {
        return hasLastConnectionTime;
    }

    public String getRemoteHost()
    {
        return remoteHost;
    }

    public void setRemoteHost( String remoteHost )
    {
        this.remoteHost = remoteHost;
    }

    public String getVendor()
    {
        return vendor;
    }

    public void setVendor( String vendor )
    {
        this.vendor = vendor;
    }




    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );
        
        if( guid != null )
        {
            writer.startElm( "guid", null );
            writer.elmText( guid );
            writer.endElm( "guid" );
        }
        
        if( hasFileIndex )
        {
            writer.startElm( "fileindex", null );
            writer.elmLong( fileIndex );
            writer.endElm( "fileindex" );
        }
        
        if( fileName != null )
        {
            writer.startElm( "filename", null );
            writer.elmText( fileName );
            writer.endElm( "filename" );
        }
        
        if( downloadUri != null )
        {
            writer.startElm( "download-uri", null );
            writer.elmText( downloadUri );
            writer.endElm( "download-uri" );
        }
        
        if ( resourceUrn != null )
        {
            writer.startElm( "resource-urn", null );
            writer.elmText( resourceUrn );
            writer.endElm( "resource-urn" );
        }
        
        if( remoteHost != null )
        {
            writer.startElm( "remotehost", null );
            writer.elmText( remoteHost );
            writer.endElm( "remotehost" );
        }
        
        if( vendor != null )
        {
            writer.startElm( "vendor", null );
            writer.elmText( vendor );
            writer.endElm( "vendor" );
        }
        
        if( hasConnectionFailedRepetition )
        {
            writer.startElm( "connectionFailedRepetition", null );
            writer.elmInt( connectionFailedRepetition );
            writer.endElm( "connectionFailedRepetition" );
        }
        
        if( hasPushNeeded )
        {
            writer.startElm( "isPushNeeded", null );
            writer.elmBol( isPushNeeded );
            writer.endElm( "isPushNeeded" );
        }
        
        if( hasThexSupported )
        {
            writer.startElm( "isThexSupported", null );
            writer.elmBol( isThexSupported );
            writer.endElm( "isThexSupported" );
        }
        
        if( hasChatSupported )
        {
            writer.startElm( "isChatSupported", null );
            writer.elmBol( isChatSupported );
            writer.endElm( "isChatSupported" );
        }
        
        if( hasLastConnectionTime )
        {
            writer.startElm( "last-connect", null );
            writer.elmLong( lastConnectionTime );
            writer.endElm( "last-connect" );
        }
        
        writer.endElm( ELEMENT_NAME );
    }      
}
