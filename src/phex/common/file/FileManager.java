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
 *  Created on 08.09.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.common.file;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.commons.collections.map.ReferenceMap;

import phex.prefs.core.FilePrefs;

/**
 * FileManager should be acquired through Phex.getFileManager()
 */
public class FileManager
{    
    /**
     * Maps File objects to ManagedFiles
     * Both references are weak to let them be cleaned by garbage collector when
     * unused anywhere.
     */
    private ReferenceMap fileManagedFileMap;
    
    /**
     * A set to maintain and count all open files.
     */
    private LinkedHashMap<ManagedFile, ManagedFile> openFileMap;
    
    public FileManager()
    {
        fileManagedFileMap = new ReferenceMap( ReferenceMap.WEAK, ReferenceMap.WEAK );
    }

    /**
     * This method is called in order to cleanly shutdown the manager. It
     * should contain all cleanup operations to ensure a nice shutdown of Phex.
     */
    public void shutdown()
    {
        // close all open files
        if ( openFileMap == null )
        {
            return;
        }

// this code can cause a deadlock situation since we close files while holding
// openFileMap lock
//        synchronized( openFileMap )
//        {
//            Iterator iterator = openFileMap.keySet().iterator();
//            while( iterator.hasNext() )
//            {
//                ManagedFile file = (ManagedFile) iterator.next();
//                try
//                {
//                    file.closeFile();
//                }
//                catch ( ManagedFileException exp )
//                {
//                    NLogger.error( NLoggerNames.ManagedFile, exp, exp );
//                }
//            }
//        }
    }
    
    private ManagedFile getManagedFile( File file )
    {
        synchronized( fileManagedFileMap )
        {
            // find if we already have a ManagedFile in cache
            ManagedFile managedFile = (ManagedFile) fileManagedFileMap.get(file);
            if ( managedFile == null )
            {
                managedFile = new ManagedFile( file );
                fileManagedFileMap.put(file, managedFile);
            }
            return managedFile;
        }
    }
    
    public ManagedFile getReadWriteManagedFile( File file )
        throws ManagedFileException
    {
        ManagedFile managedFile = getManagedFile( file );
        managedFile.setAccessMode( ManagedFile.AccessMode.READ_WRITE_ACCESS );
        return managedFile;
    }
    
    public ReadOnlyManagedFile getReadOnlyManagedFile( File file )
        throws ManagedFileException
    {
        ManagedFile managedFile = getManagedFile( file );
        managedFile.setAccessMode( ManagedFile.AccessMode.READ_ONLY_ACCESS );
        return managedFile;
    }
    
    private void initOpenFileTracking()
    {
        // no open file limit set.
        if ( FilePrefs.OpenFilesLimit.get().intValue() == 0 )
        {
            openFileMap = null;
            return;
        }
        
        synchronized( this )
        {
            if ( openFileMap == null )
            {
                openFileMap = new LinkedHashMap<ManagedFile, ManagedFile>( 
                    FilePrefs.OpenFilesLimit.get().intValue(), 0.75f, true );
            }
        }
    }
    
    protected void trackFileOpen( ManagedFile managedFile ) 
        throws ManagedFileException
    {
        initOpenFileTracking();
        if ( openFileMap == null )
        {
            return;
        }
        ManagedFile oldestEntry = null;
        synchronized( openFileMap )
        {
            if ( openFileMap.size() >= FilePrefs.OpenFilesLimit.get().intValue() )
            {
                Iterator<ManagedFile> iterator = openFileMap.keySet().iterator();
                oldestEntry = iterator.next();
                // remove file from map but wait with closing the file until
                // we are outside of synchronized block, else we cause a deadlock.
                iterator.remove();
            }
            openFileMap.put( managedFile, managedFile );
        }
        if ( oldestEntry != null )
        {
            oldestEntry.closeFile();
        }
    }
    
    protected void trackFileInUse( ManagedFile managedFile )
    {
        initOpenFileTracking();
        if ( openFileMap == null )
        {
            return;
        }
        synchronized( openFileMap )
        {
            if ( openFileMap.containsKey( managedFile ) )
            {
                openFileMap.get( managedFile );
            }
        }
    }
    
    protected void trackFileClose( ManagedFile managedFile )
    {
        initOpenFileTracking();
        if ( openFileMap == null )
        {
            return;
        }
        synchronized( openFileMap )
        {
            openFileMap.remove( managedFile );
        } 
    }
}