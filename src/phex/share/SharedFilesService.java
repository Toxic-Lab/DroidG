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
package phex.share;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Map.Entry;

import phex.common.AbstractLifeCycle;
import phex.common.Environment;
import phex.common.EnvironmentConstants;
import phex.common.Phex;
import phex.common.PhexVersion;
import phex.common.QueryRoutingTable;
import phex.common.RunnerQueueWorker;
import phex.common.ThreadTracking;
import phex.common.URN;
import phex.common.collections.IntSet;
import phex.common.collections.StringTrie;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.event.PhexEventService;
import phex.event.PhexEventTopics;
import phex.event.UserMessageListener;
import phex.msg.QueryMsg;
import phex.prefs.core.LibraryPrefs;
import phex.servent.Servent;
import phex.thex.FileHashCalculationHandler;
import phex.thex.ThexCalculationWorker;
import phex.utils.FileUtils;
import phex.utils.ReadWriteLock;
import phex.utils.StringUtils;
import phex.xml.sax.DPhex;
import phex.xml.sax.XMLBuilder;
import phex.xml.sax.share.DSharedFile;
import phex.xml.sax.share.DSharedLibrary;

/**
 *
 */
public class SharedFilesService extends AbstractLifeCycle
    implements FileHashCalculationHandler
{
    //private static final Logger logger = LoggerFactory.getLogger( SharedFilesService.class );
    
    private final PhexEventService eventService;
    
    private ReadWriteLock rwLock;
    
    /**
     * The search engine used to query for shared files.
     */
    private QueryResultSearchEngine searchEngine;
    
    /**
     * This HashMap maps native File objects to its shared counter part in the 
     * phex system.
     */
    private HashMap<File, SharedDirectory> directoryShareMap;
    
    /**
     * A list of shared directories.
     */
    private ArrayList<SharedDirectory> sharedDirectories;
    
    /**
     * A maps that maps URNs to the file they belong to. This is for performant
     * searching by urn.
     * When accessing this object locking via the rwLock object is required.
     */
    private HashMap<URN, ShareFile> urnToFileMap;
    
    /**
     * This map contains all absolute file paths as keys for the ShareFile
     * behind it.
     * When accessing this object locking via the rwLock object is required.
     */
    private HashMap<String, ShareFile> nameToFileMap;
    
    /**
     * A map that contains the network creation time and a Set of ShareFile
     * the time belongs to. 
     * When accessing this object locking via the rwLock object is required.
     */
    private Map<Long, Set<ShareFile>> timeToFileMap;
    
    /**
     * This lists holds all shared files at there current index position.
     * When files are un-shared during runtime a null is placed at the index
     * position of the removed file. Access via the file index is done using
     * the method getFileByIndex( fileIndex ).
     * When accessing this object locking via the rwLock object is required.
     */
    private ArrayList<ShareFile> indexedSharedFiles;
    
    /**
     * This list contains the shared files without gaps. It is used for direct
     * and straight access via the getFileAt( position ). Also it is used for
     * getFileCount().
     * When accessing this object locking via the rwLock object is required.
     */
    private ArrayList<ShareFile> sharedFiles;

    /**
     * The total size of the shared files.
     */
    private int totalFileSizeKb;
    
    /**
     * The string trie containing the key words and an IntSet with matching 
     * file indices. 
     */
    private StringTrie<IntSet> keywordTrie;
    
    /**
     * Local query routing table. Contains all shared files.
     */
    private QueryRoutingTable localRoutingTable;
    
    /**
     * Indicates if the local qrt needs to be rebuild i.e. after shared files 
     * have changed.
     */
    private boolean localQRTNeedsUpdate;
    
    
    
    /**
     * A instance of a background runner queue to calculate
     * urns.
     */
    private RunnerQueueWorker urnThexCalculationRunner;
    
    /**
     * Lock object to lock saving of shared file lists.
     */
    private static Object saveSharedFilesLock = new Object();
    
    /**
     * Object that holds the save job instance while a save job is running. The
     * reference is null if the job is not running.
     */
    private SaveSharedFilesJob saveSharedFilesJob;
    
    public SharedFilesService( Servent servent )
    {
        eventService = Phex.getEventService();
        
        rwLock = new ReadWriteLock();
        urnThexCalculationRunner = new RunnerQueueWorker( Thread.NORM_PRIORITY-1 );
        
        Environment.getInstance().scheduleTimerTask( 
            new FileRescanTimer(), FileRescanTimer.TIMER_PERIOD,
            FileRescanTimer.TIMER_PERIOD );
        
        searchEngine = new QueryResultSearchEngine( servent, this );
        
        directoryShareMap = new HashMap<File, SharedDirectory>();
        sharedDirectories = new ArrayList<SharedDirectory>();
        urnToFileMap = new HashMap<URN, ShareFile>();
        nameToFileMap = new HashMap<String, ShareFile>();
        timeToFileMap = new TreeMap<Long, Set<ShareFile>>( Collections.reverseOrder() );
        indexedSharedFiles = new ArrayList<ShareFile>();
        sharedFiles = new ArrayList<ShareFile>();
        keywordTrie = new StringTrie<IntSet>( true );
        totalFileSizeKb = 0;
        localQRTNeedsUpdate = true;
    }
    
    @Override
    protected void doStart() throws Exception
    {
        // TODO can we ensure that this is called as the last start or after
        // all other life cycle have been started?
        FileRescanRunner.rescan( this, true, false );
    }
    
    @Override
    protected void doStop() throws Exception
    {
        triggerSaveSharedFiles();
    }
    
    public List<ShareFile> handleQuery( QueryMsg queryMsg )
    {
        return searchEngine.handleQuery( queryMsg );
    }
    
    public QueryRoutingTable getLocalRoutingTable()
    {
        if ( localQRTNeedsUpdate )
        {
            localRoutingTable = QueryRoutingTable.createLocalQueryRoutingTable( 
                getSharedFiles() );
        }
        return localRoutingTable;
    }
    
    /**
     * Returns the part of the file path that is shared.
     * @param file the file to determine the shared part of the path for.
     * @return the part of the file path that is shared.
     */
    private String getSharedFilePath( File file )
    {
        rwLock.readLock();
        try
        {            
            File highestDir = file.getParentFile();
            
            // copy shared directories set to prevent concurrent modification.
            // TODO this could still cause ConcurrentModificationException during the toArray() 
            // operation, but it occurs very rarely.
            ArrayList<String> sharedDirectoriesCopy = new ArrayList<String>( 
                LibraryPrefs.SharedDirectoriesSet.get() );
            for ( String dirStr : sharedDirectoriesCopy )
            {
                // TODO this call will create many File instances, can't they be cached?
                File dir = new File( dirStr );
                if ( FileUtils.isChildOfDir( file, dir )
                  && FileUtils.isChildOfDir( highestDir, dir ) )
                {
                    highestDir = dir;
                }
            }
            // also share the shared dir itself.
            File highestParent = highestDir.getParentFile();
            if ( highestParent != null )
            {
                highestDir = highestParent;
            }
            String pathStr = highestDir.getAbsolutePath();
            int length = pathStr.length();
            if ( !pathStr.endsWith( File.separator ) )
            {
                length++;
            }
            return file.getAbsolutePath().substring( length );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); 
            }
        }
    }
    
    /**
     * Returns the a shared file by name. If the given name is null or a
     * file with this name is not found then null is returned.
     */
    public ShareFile getFileByName( String name )
    {
        rwLock.readLock();
        try
        {
            return nameToFileMap.get( name );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); 
            }
        }
    }
    
    /**
     * Returns the a shared file by file. If the given file is null or a
     * file is not found then null is returned.
     */
    public ShareFile getShareFileByFile( File file )
    {
        return getFileByName( file.getAbsolutePath() );
    }
    
    /**
     * Gets the file at the given index in the shared file list.
     * To access via the file index use the method getFileByIndex( fileIndex )
     */
    public ShareFile getFileAt( int index )
    {
        rwLock.readLock();
        try
        {
            if ( index >= sharedFiles.size() )
            {
                return null;
            }
            return sharedFiles.get( index );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); 
            }
        }
    }
    
    public Iterator<IntSet> getIndexIteratorForPrefixTerm( String searchTerm,
        int startOffset, int stopOffset )
    {
        return keywordTrie.getPrefixedBy( searchTerm, startOffset, stopOffset );
    }
        
    
    /**
     * Creates a list containing all SharedFiles.
     * @return a list with all shared files.
     */
    public List<ShareFile> getSharedFiles()
    {
        rwLock.readLock();
        try
        {
            return new ArrayList<ShareFile>( sharedFiles );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); 
            }
        }
    }
    
    /**
     * Returns the shared files count.
     */
    public int getFileCount()
    {
        rwLock.readLock();
        try
        {
            return sharedFiles.size();
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); 
            }
        }
    }
    
    /**
     * Returns the total size of all shared files in KB.
     */
    public int getTotalFileSizeInKb()
    {
        rwLock.readLock();
        try
        {
            return totalFileSizeKb;
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
    }
    
    /**
     * Returns a shared file by its index number.
     */
    public ShareFile getFileByIndex( int fileIndex )
        throws IndexOutOfBoundsException
    {
        rwLock.readLock();
        try
        {
            if ( fileIndex >= indexedSharedFiles.size() )
            {
                return null;
            }
            return indexedSharedFiles.get( fileIndex );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
    }
    
    /**
     * Returns a shared file by its urn. If the given urn is null or a
     * file with this URN is not found then null is returned.
     */
    public ShareFile getFileByURN( URN fileURN )
        throws IndexOutOfBoundsException
    {
        rwLock.readLock();
        try
        {
            if ( fileURN == null )
            {
                return null;
            }
            return urnToFileMap.get( fileURN );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
    }
    
    //clear counters
    public void clearLibrarySearchCounters()
    {   
        int index=0;
        rwLock.readLock();
        try
        {
            while(index<sharedFiles.size())
            {
                sharedFiles.get(index).clearSearchCounters();
                index++;
            }
        }
        finally
        {    
            try
            {
                rwLock.readUnlock();
            }            
            catch(IllegalAccessException exp)
            {
                //logger.error(exp.toString(), exp);
            }        
        }    
    }

    public void clearLibraryUploadCounters()
    {    
        int index=0;
        rwLock.readLock();
        try
        {    
            while(index<sharedFiles.size())
            {
                sharedFiles.get(index).clearUploadCounters();
                index++;
            }        
        }        
        finally
        {
            try
            {
                rwLock.readUnlock();
            }
            catch(IllegalAccessException exp)
            {
                //logger.error(exp.toString(), exp);
            }
        }    
    }
    
    /**
     * Returns whether a file with the given URN is shared or not. 
     * @return true when a file with the given URN is shared, false otherwise.
     */
    public boolean isURNShared( URN fileURN )
        throws IndexOutOfBoundsException
    {
        rwLock.readLock();
        try
        {
            if ( fileURN == null )
            {
                return false;
            }
            return urnToFileMap.containsKey( fileURN );
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
    }
    
    public List<ShareFile> getFilesByURNs( URN[] urns )
    {
        rwLock.readLock();
        try
        {
            List<ShareFile> results = new ArrayList<ShareFile>( urns.length );
            for( int i = 0; i < urns.length; i++ )
            {
                ShareFile file = urnToFileMap.get( urns[i] );
                if ( file != null )
                {
                    results.add( file );
                }
            }
            return results;
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            {
            	//logger.error( exp.toString(), exp ); }
            }
        }
    }
    
    public List<ShareFile> getNewestFiles( int count )
    {
        List<ShareFile> fileList = new ArrayList<ShareFile>();
        Iterator<Entry<Long, Set<ShareFile>>> iterator = timeToFileMap.entrySet().iterator();
        while ( iterator.hasNext() && fileList.size() < count )
        {
            Set<ShareFile> fileSet = iterator.next().getValue();
            Iterator<ShareFile> setIterator = fileSet.iterator();
            while( setIterator.hasNext() && fileList.size() < count )
            {
                ShareFile shareFile = setIterator.next();
                fileList.add(shareFile);
            }
        }
        return fileList;
    }
    
    /**
     * Adds a shared file if its not already shared.
     * Its important that the file owns a valid urn when being added.
     */
    public void addSharedFile( ShareFile shareFile )
    {
        File file = shareFile.getSystemFile();
        // check if file is already there
        if ( getFileByName( file.getAbsolutePath() ) != null )
        {
            return;
        }
        
        rwLock.writeLock();
        int position;
        try
        {
            position = indexedSharedFiles.size();
            shareFile.setFileIndex( position );
            indexedSharedFiles.add( shareFile );
            sharedFiles.add( shareFile );

            // don't add to urn map yet since urns get calculated in background.
            nameToFileMap.put( file.getAbsolutePath(), shareFile );
            
            // fill search trie...
            String keywordsString = getSharedFilePath( shareFile.getSystemFile() ).toLowerCase();
            String[] keywords = StringUtils.split( keywordsString, StringUtils.FILE_DELIMITERS );
            for (int i = 0; i < keywords.length; i++)
            {
                IntSet indices = keywordTrie.get( keywords[i] );
                if ( indices == null )
                {// lazy initialize
                    indices = new IntSet();
                    keywordTrie.add( keywords[i], indices );
                }
                indices.add( position );
            }
            
            totalFileSizeKb += file.length() / 1024;
            localQRTNeedsUpdate = true;
        }
        finally
        {
            try{ rwLock.writeUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
        //fireSharedFileAdded( position );
    }
    
    private void addTimeToFile( ShareFile shareFile )
    {
        try{ rwLock.assertWriteLock(); }
        catch (IllegalAccessException exp )
        { 
        	//logger.error( exp.toString(), exp ); }
        }

        Long time = shareFile.getNetworkCreateTime();
        Set<ShareFile> shareFileSet = timeToFileMap.get( time );
        if ( shareFileSet == null )
        {
            shareFileSet = new HashSet<ShareFile>();
            timeToFileMap.put(time, shareFileSet);
        }
        shareFileSet.add( shareFile );
    }
    
    /**
     * Removed a shared file if its shared.
     */
    public void removeSharedFile( ShareFile shareFile )
    {
        rwLock.writeLock();
        int position;
        try
        {
            // clear index position...
            int fileIndex = shareFile.getFileIndex();
            indexedSharedFiles.set( fileIndex, null );
            
            // clear from trie...
            String keywordsString = getSharedFilePath( shareFile.getSystemFile() ).toLowerCase();
            String[] keywords = StringUtils.split( keywordsString, StringUtils.FILE_DELIMITERS );
            for ( int i = 0; i < keywords.length; i++ )
            {
                IntSet indices = keywordTrie.get( keywords[i] );
                if (indices != null)
                {
                    indices.remove( fileIndex );
                    if (indices.size() == 0)
                    {
                        keywordTrie.remove( keywords[i] );
                    }
                }
            }

            // remove name to file map
            File file = shareFile.getSystemFile();
            urnToFileMap.remove( shareFile.getURN() );
            nameToFileMap.remove( file.getAbsolutePath() );
            removeTimeToFile( shareFile );

            // try to find shareFile in access list
            position = sharedFiles.indexOf( shareFile );
            if ( position != -1 )
            {// if removed update data
                sharedFiles.remove( position );
                totalFileSizeKb -= shareFile.getFileSize() / 1024;
                localQRTNeedsUpdate = true;
            }
        }
        finally
        {
            try{ rwLock.writeUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
        //if ( position != -1 )
        //{// if removed fire events
        //    fireSharedFileRemoved( position );
        //}
    }
    
    /**
     * When calling lock must be owned!
     * @param shareFile
     */
    private void removeTimeToFile( ShareFile shareFile )
    {
        try{ rwLock.assertWriteLock(); }
        catch (IllegalAccessException exp )
        { 
        	//logger.error( exp.toString(), exp ); }
        }
        
        Long time = shareFile.getNetworkCreateTime();
        Set<ShareFile> shareFileSet = timeToFileMap.get(time);
        if ( shareFileSet == null )
        {
            return;
        }
        shareFileSet.remove(shareFile);
        if ( shareFileSet.size() == 0 )
        {
            timeToFileMap.remove(time);
        }
    }
    
    /**
     * Adds a shared file if its not already shared.
     */
    public void updateSharedDirecotries( 
        HashMap<File,SharedDirectory> sharedDirectoryMap,
        HashSet<SharedDirectory> sharedDirectoryList )
    {
        rwLock.writeLock();
        try
        {
            directoryShareMap.clear();
            directoryShareMap.putAll(sharedDirectoryMap);
            sharedDirectories.clear();
            sharedDirectories.addAll(sharedDirectoryList);
            sharedDirectoriesChanged();
        }
        finally
        {
            try{ rwLock.writeUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
    }
    
    /**
     * Do not modify!
     */
    public SharedDirectory[] getSharedDirectories()
    {
        rwLock.readLock();
        try
        {
            SharedDirectory[] array = new SharedDirectory[ sharedDirectories.size() ];
            array = sharedDirectories.toArray( array );
            return array;
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
    }
    
    /**
     * 
     */
    public SharedDirectory getSharedDirectory( File file )
    {
        if ( !file.isDirectory() )
        {
            return null;
        }
        rwLock.readLock();
        try
        {
            SharedResource resource = directoryShareMap.get( file );
            if ( resource instanceof SharedDirectory )
            {
                return (SharedDirectory)resource;
            }
            return null;
        }
        finally
        {
            try{ rwLock.readUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
    }
    
    /**
     * Adds a urn to file mapping for this ShareFile. When calling make sure
     * the urn is already set.
     * @param shareFile
     */
    public void addUrn2FileMapping( ShareFile shareFile )
    {
        rwLock.writeLock();
        try
        {
            assert( shareFile.getURN() != null );
            urnToFileMap.put( shareFile.getURN(), shareFile );
            // only add time once we have a valid URN... 
            // it makes no sense to return whats new files without urn.
            addTimeToFile( shareFile );        
        }
        finally
        {
            try{ rwLock.writeUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
    }
    
    /**
     * Queues a ShareFile for calculating THEX.
     * 
     * @param shareFile the share file to calculate the
     *        thex hash for.
     */
    public void queueUrnCalculation( ShareFile shareFile )
    {
        UrnCalculationWorker worker = new UrnCalculationWorker(
            shareFile, this );
        urnThexCalculationRunner.add( worker );
    }
    
    /**
     * Queues a ShareFile for calculating its URN.
     * 
     * @param shareFile the share file to calculate the
     *        urn hash for.
     */
    public void queueThexCalculation( ShareFile shareFile )
    {
        ThexCalculationWorker worker = new ThexCalculationWorker(
            shareFile);
        urnThexCalculationRunner.add( worker );
    }
    
    public void setCalculationRunnerPause( boolean state )
    {
        urnThexCalculationRunner.setPause( state );
    }
    
    public int getCalculationRunnerQueueSize()
    {
        return urnThexCalculationRunner.getQueueSize();
    }
        
    /**
     * Clears the complete shared file list. Without making information
     * persistent.
     */
    public void clearSharedFiles()
    {
        rwLock.writeLock();
        try
        {
            urnThexCalculationRunner.stopAndClear();
            sharedFiles.clear();
            indexedSharedFiles.clear();
            urnToFileMap.clear();
            nameToFileMap.clear();
            timeToFileMap.clear();
            totalFileSizeKb = 0;
            localQRTNeedsUpdate = true;
        }
        finally
        {
            try{ rwLock.writeUnlock(); }
            catch (IllegalAccessException exp )
            { 
            	//logger.error( exp.toString(), exp ); }
            }
        }
        //fireAllSharedFilesChanged();
    }
    
    /**
     * Triggers a save of the download list. The call is not blocking and returns
     * directly, the save process is running in parallel.
     */
    public void triggerSaveSharedFiles( )
    {
        //logger.debug( "Trigger save shared files..." );
        synchronized( saveSharedFilesLock )
        {
            if ( saveSharedFilesJob != null )
            {
                // save shared files is already in progress. we rerequest a save.
                saveSharedFilesJob.triggerFollowUpSave();
            }
            else
            {
                saveSharedFilesJob = new SaveSharedFilesJob();
                saveSharedFilesJob.start();
            }
        }
    }
    
    public DSharedLibrary loadSharedLibrary()
    {
        //logger.debug( "Load shared library configuration file." );
        
        File file = Environment.getInstance().getPhexConfigFile(
            EnvironmentConstants.XML_SHARED_LIBRARY_FILE_NAME );

        DPhex dPhex;
        try
        {
            ManagedFile managedFile = Phex.getFileManager().getReadWriteManagedFile( file );
            dPhex = XMLBuilder.loadDPhexFromFile( managedFile );
            if ( dPhex == null )
            {
                //logger.debug( "No shared library configuration file found." );
                return null;
            }
        }
        catch ( InterruptedIOException exp )
        {
            // no error... just plain interruption.
            return null;
        }
        catch ( IOException exp )
        {
            //logger.error( exp.toString(), exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SharedFilesLoadFailed, 
                new String[]{ exp.toString() } );
            // likely loading of shared files failed... again... lets try to find
            // the cause and create a spare copy of the failed file
            try
            {
                FileUtils.copyFile(file, new File( file.getAbsolutePath() + ".failed" ) );
            }
            catch (IOException e)
            {
                //logger.error( "Failed to store failed file copy: {}", e );
            }
            return null;
        }
        catch ( ManagedFileException exp )
        {
            //logger.error( exp.toString(), exp );
            Environment.getInstance().fireDisplayUserMessage( 
                UserMessageListener.SharedFilesLoadFailed, 
                new String[]{ exp.toString() } );
            return null;
        }

        // update old download list
        DSharedLibrary sharedLibrary = dPhex.getSharedLibrary();
        return sharedLibrary;
    }
        
    ///////////////////// START event handling methods ////////////////////////
    private void sharedDirectoriesChanged()
    {
        eventService.publish( PhexEventTopics.Share_Update, "" );
    }
    ///////////////////// END event handling methods /////////////////////////
    
    ///////////////////// START inner classes        /////////////////////////
    
    private class FileRescanTimer extends TimerTask
    {
        // once per minute
        public static final long TIMER_PERIOD = 1000 * 60;

        @Override
        public void run()
        {
            try
            {
                FileRescanRunner.rescan( SharedFilesService.this, false, false );
            }
            catch ( Throwable th )
            {
                //logger.error( th.toString(), th );
            }
        }
    }
    
    private class SaveSharedFilesJob extends Thread
    {
        private volatile boolean isFollowUpSaveTriggered;

        public SaveSharedFilesJob()
        {
            super( ThreadTracking.rootThreadGroup, "SaveSharedFilesJob" );
            setPriority( Thread.MIN_PRIORITY );
        }

        public void triggerFollowUpSave()
        {
            isFollowUpSaveTriggered = true;
        }

        /**
         * Saving of the shared file list is done asynchronously to make sure that there
         * will be no deadlocks happening
         */
        @Override
        public void run()
        {
            FileManager fileMgr = Phex.getFileManager();
            File libraryFile = Environment.getInstance().getPhexConfigFile(
                EnvironmentConstants.XML_SHARED_LIBRARY_FILE_NAME );
            File tmpFile = Environment.getInstance().getPhexConfigFile(
                EnvironmentConstants.XML_SHARED_LIBRARY_FILE_NAME
                + ".tmp" );
            do
            {
                //logger.debug( "Saving shared library." );
                isFollowUpSaveTriggered = false;
                rwLock.readLock();
                try
                {
                    DPhex dPhex = new DPhex();
                    dPhex.setPhexVersion( PhexVersion.getFullVersion() );
                    
                    DSharedLibrary dLibrary = createDSharedLibrary();
                    dPhex.setSharedLibrary( dLibrary );

                    // first save into temporary file...
                    ManagedFile tmpMgFile = fileMgr.getReadWriteManagedFile( tmpFile );
                    XMLBuilder.saveToFile( tmpMgFile, dPhex );

                    // after saving copy temporary file to real file.
                    ManagedFile libraryMgFile = fileMgr.getReadWriteManagedFile(libraryFile);
                    // lock library file.
                    try
                    {
                        libraryMgFile.acquireFileLock();                    
                        FileUtils.copyFile( tmpFile, libraryFile );
                    }
                    finally
                    {
                        libraryMgFile.releaseFileLock();
                    }

                    //File zipFile = Environment.getInstance().getPhexConfigFile(
                    //    EnvironmentConstants.XML_SHARED_LIBRARY_FILE_NAME + ".def" );
                    //OutputStream out = new DeflaterOutputStream( new FileOutputStream( zipFile ) );
                    //FileInputStream inStream = new FileInputStream( libraryFile );
                    //int c;
                    //byte[] buffer = new byte[16*1024];
                    //while ( (c = inStream.read( buffer )) != -1 )
                    //{
                    //    out.write(buffer, 0, c);
                    //}
                }
                catch (ManagedFileException exp)
                {
                    if ( exp.getCause() instanceof InterruptedException )
                    { // the thread was interrupted and requested to stop, most likely
                        // by user request.
                        //logger.debug( exp.toString() );
                    }
                    else
                    {
                        // TODO during close this message is never displayed since application
                        // will exit too fast. A solution to delay exit process in case 
                        // SlideInWindows are open needs to be found.
                        //logger.error( exp.toString(), exp );
                        Environment.getInstance().fireDisplayUserMessage(
                            UserMessageListener.SharedFilesSaveFailed,
                            new String[]{ exp.toString() } );
                    }
                }
                catch (IOException exp)
                {
                    //logger.error( exp.toString(), exp );
                    Environment.getInstance().fireDisplayUserMessage(
                        UserMessageListener.SharedFilesSaveFailed, new String[]
                        { exp.toString() } );
                }
                finally
                {
                    try
                    {
                        rwLock.readUnlock();
                    }
                    catch (IllegalAccessException exp)
                    {
                        //logger.error( exp.toString(), exp );
                    }
                }
            }
            while ( isFollowUpSaveTriggered );
            //logger.debug( "Finished saving download list..." );

            synchronized ( saveSharedFilesLock )
            {
                // give created instance free once we are finished..
                saveSharedFilesJob = null;
            }
        }

        private DSharedLibrary createDSharedLibrary()
        {
            DSharedLibrary library = new DSharedLibrary();
            rwLock.readLock();
            try
            {
                List<DSharedFile> sharedFileList = library.getSubElementList();
                for( ShareFile file : sharedFiles )
                {
                    try
                    {
                        if ( file.getURN() == null )
                        {
                            continue;
                        }
                        DSharedFile dFile = file.createDSharedFile();
                        sharedFileList.add( dFile );
                    }
                    catch (Exception exp)
                    {
                        //logger.error( "SharedFile skipped due to error.", exp );
                    }
                }
            }
            finally
            {
                try
                {
                    rwLock.readUnlock();
                }
                catch (IllegalAccessException exp)
                {
                    //logger.error( exp.toString(), exp );
                }
            }
            return library;
        }
    }

    ///////////////////// END inner classes   ////////////////////////
}