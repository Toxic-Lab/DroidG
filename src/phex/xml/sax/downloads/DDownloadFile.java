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
 *  Created on 01.11.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax.downloads;

import org.xml.sax.SAXException;

import phex.xml.sax.DElement;
import phex.xml.sax.DSubElementList;
import phex.xml.sax.PhexXmlSaxWriter;

public class DDownloadFile implements DElement
{
    public static final String ELEMENT_NAME = "swDownloadFile";

    private String localFileName;
    private String destinationDirectory;
    private String incompleteFileName;

    private String searchTerm;

    private String fileURN;

    private String scopeSelectionStrategy;

    private boolean hasStatus;

    private int status;

    private boolean hasCreationTime;
    private long creationTime;

    private boolean hasModificationTime;
    private long modificationTime;

    private boolean hasFileSize;

    private long fileSize;

    private DSubElementList<DDownloadCandidate> candidateList;
    
    private DSubElementList<DDownloadScope> unverifiedScopesList;

    private DSubElementList<DDownloadScope> finishedScopesList;
    
    public DDownloadFile()
    {
        candidateList = new DSubElementList<DDownloadCandidate>(  );
        unverifiedScopesList = new DSubElementList<DDownloadScope>(  );
        finishedScopesList = new DSubElementList<DDownloadScope>(  );
    }

    /**
     * @return the destinationDirectory
     */
    public String getDestinationDirectory()
    {
        return destinationDirectory;
    }

    /**
     * @param destinationDirectory the destinationDirectory to set
     */
    public void setDestinationDirectory( String destinationDirectory )
    {
        this.destinationDirectory = destinationDirectory;
    }

    public long getCreationTime()
    {
        return creationTime;
    }

    public void setCreationTime( long creationTime )
    {
        this.creationTime = creationTime;
        hasCreationTime = true;
    }

    public long getFileSize()
    {
        return fileSize;
    }

    public void setFileSize( long fileSize )
    {
        this.fileSize = fileSize;
        hasFileSize = true;
    }

    public String getFileURN()
    {
        return fileURN;
    }

    public void setFileURN( String fileURN )
    {
        this.fileURN = fileURN;
    }

    public String getIncompleteFileName()
    {
        return incompleteFileName;
    }

    public void setIncompleteFileName( String incompleteFileName )
    {
        this.incompleteFileName = incompleteFileName;
    }

    public String getLocalFileName()
    {
        return localFileName;
    }

    public void setLocalFileName( String localFileName )
    {
        this.localFileName = localFileName;
    }

    public long getModificationTime()
    {
        return modificationTime;
    }

    public void setModificationTime( long modificationTime )
    {
        this.modificationTime = modificationTime;
        hasModificationTime = true;
    }

    public String getScopeSelectionStrategy()
    {
        return scopeSelectionStrategy;
    }

    public void setScopeSelectionStrategy( String scopeSelectionStrategy )
    {
        this.scopeSelectionStrategy = scopeSelectionStrategy;
    }

    public String getSearchTerm()
    {
        return searchTerm;
    }

    public void setSearchTerm( String searchTerm )
    {
        this.searchTerm = searchTerm;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus( int status )
    {
        this.status = status;
        hasStatus = true;
    }

    public DSubElementList<DDownloadCandidate> getCandidateList()
    {
        return candidateList;
    }

    public DSubElementList<DDownloadScope> getUnverifiedScopesList()
    {
        return unverifiedScopesList;
    }
    
    public DSubElementList<DDownloadScope> getFinishedScopesList()
    {
        return finishedScopesList;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( ELEMENT_NAME, null );

        if ( localFileName != null )
        {
            writer.startElm( "localfilename", null );
            writer.elmText( localFileName );
            writer.endElm( "localfilename" );
        }
        
        if ( destinationDirectory != null )
        {
            writer.startElm( "dest-dir", null );
            writer.elmText( destinationDirectory );
            writer.endElm( "dest-dir" );
        }

        if ( incompleteFileName != null )
        {
            writer.startElm( "incomplete-file-name", null );
            writer.elmText( incompleteFileName );
            writer.endElm( "incomplete-file-name" );
        }

        if ( searchTerm != null )
        {
            writer.startElm( "searchterm", null );
            writer.elmText( searchTerm );
            writer.endElm( "searchterm" );
        }

        if ( fileURN != null )
        {
            writer.startElm( "file-urn", null );
            writer.elmText( fileURN );
            writer.endElm( "file-urn" );
        }

        if ( scopeSelectionStrategy != null )
        {
            writer.startElm( "scope-strategy", null );
            writer.elmText( scopeSelectionStrategy );
            writer.endElm( "scope-strategy" );
        }

        if ( hasStatus )
        {
            writer.startElm( "status", null );
            writer.elmInt( status );
            writer.endElm( "status" );
        }

        if ( hasCreationTime )
        {
            writer.startElm( "created-time", null );
            writer.elmLong( creationTime );
            writer.endElm( "created-time" );
        }

        if ( hasModificationTime )
        {
            writer.startElm( "modified-time", null );
            writer.elmLong( modificationTime );
            writer.endElm( "modified-time" );
        }

        if ( hasFileSize )
        {
            writer.startElm( "filesize", null );
            writer.elmLong( fileSize );
            writer.endElm( "filesize" );
        }

        if ( candidateList != null && !candidateList.getSubElementList().isEmpty() )
        {
            candidateList.serialize( writer );
        }
        
        if ( unverifiedScopesList != null && !unverifiedScopesList.getSubElementList().isEmpty() )
        {
            unverifiedScopesList.serialize( writer );
        }
        
        if ( finishedScopesList != null && !finishedScopesList.getSubElementList().isEmpty() )
        {
            finishedScopesList.serialize( writer );
        }

        writer.endElm( ELEMENT_NAME );
    }
}
