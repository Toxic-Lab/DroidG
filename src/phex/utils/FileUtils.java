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
package phex.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import phex.common.FileHandlingException;

/**
 * Offers static utility methods for file handling, like creating file path...
 */
public final class FileUtils
{
    private static final int BUFFER_LENGTH = 256 * 1024;

    /**
     * Don't create any instance!
     */
    private FileUtils()
    {
    }
    
    public static String getFileExtension( File file )
    {
        String name = file.getName();
        return getFileExtension(name);
    }
    
    public static String getFileExtension( String fileName )
    {
        int idx = fileName.lastIndexOf( '.' );
        if ( idx == -1 )
        {
            return "";
        }
        else
        {
            return fileName.substring(idx + 1);
        }
    }
    
    public static String replaceFileExtension( String fileName, String newExtension )
    {
        int idx = fileName.lastIndexOf( '.' );
        if ( idx == -1 )
        {
            return fileName + "." + newExtension;
        }
        else
        {
            return fileName.substring(0, idx + 1) + newExtension;
        }
    }

    /**
     * Since we are only supporting J2SE 1.2 and J2SE is not available on
     * MACOS 9 and MACOS X is supporting 255 characters we are shorting only
     * for 255 on MAC.
     *
     * MACOS X filename: 255
     * Windows pathlength: 260
     *         filelength: 255
     */
    public static String convertToLocalSystemFilename(String filename)
    {
        // we generally cut of at 255 it will suit everybody and we have no
        // os comparing stuff to do....
        // TODO we need to improve things here to keep up with the window pathlength
        // handling... but for now we just help the mac guys..
        
        // replace all possible invalid filename characters with _
        filename = StringUtils.replaceChars( filename, "\\/:*?\"<>|", '_' );        
        return filename.substring( 0, Math.min( 255, filename.length() ) );
        /*if ( OS_NAME.indexOf("MAC") != -1)
        {
            return makeShortName(filename, 255);
        }
        else
        {
            return filename;
        }*/
    }

    /**
     * Appends the fileToAppend on the destination file. The file that is appended
     * will be removed afterwards.
     * @throws IOException in case an IO operation fails
     */
    public static void appendFile( File destination, File fileToAppend )
        throws IOException
    {
        long destFileLength = destination.length();
        long appendFileLength = fileToAppend.length();
        // open files
        FileInputStream inStream = new FileInputStream( fileToAppend );
        try
        {
            RandomAccessFile destFile = new RandomAccessFile( destination, "rwd" );
            try
            {
                // extend file length... this causes dramatical performance boost since
                // contents is streamed into already freed space.
                destFile.setLength( destFileLength + appendFileLength );
                destFile.seek( destFileLength );
                byte[] buffer = new byte[ (int)Math.min( BUFFER_LENGTH, appendFileLength ) ];
                int length;
                while ( -1 != (length = inStream.read(buffer)) )
                {
                    long start2 = System.currentTimeMillis();
                    destFile.write( buffer, 0, length );
                    long end2 = System.currentTimeMillis();
                    try
                    {
                        Thread.sleep( (end2 - start2) * 2 );
                    }
                    catch ( InterruptedException exp )
                    {
                        // reset interrupted flag
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            finally
            {
                destFile.close();
                IOUtil.closeQuietly( destFile );
            }
        }
        finally
        {
            IOUtil.closeQuietly( inStream );
        }
        
        FileUtils.deleteFileMultiFallback( fileToAppend );
    }
    
    /**
     * Copys the source file to the destination file. Old contents of the
     * destination file will be overwritten.
     * This is a optimized version of the org.apache.commons.io.FileUtils.copy
     * source, with larger file buffer for a faster copy process.
     * @throws IOException in case an IO operation fails
     */
    public static void copyFile( File source, File destination )
        throws IOException
    {
        copyFile( source, destination, source.length() );
        if (source.length() != destination.length())
        {
            String message = "Failed to copy full contents from " + source
                + " to " + destination + " - " + source.length() + "/" + destination.length();
            throw new IOException(message);
        }
    }

    /**
     * Copys the source file to the destination file. Old contents of the
     * destination file will be overwritten.
     * This is a optimized version of the org.apache.commons.io.FileUtils.copy
     * source, with larger file buffer for a faster copy process.
     * @throws IOException in case an IO operation fails
     */
    public static void copyFile( File source, File destination, long copyLength )
        throws IOException
    {   
        // check source exists
        if (!source.exists())
        {
            String message = "File " + source + " does not exist";
            throw new FileNotFoundException(message);
        }

        //does destinations directory exist ?
        if (destination.getParentFile() != null
            && !destination.getParentFile().exists())
        {
            forceMkdir( destination.getParentFile() );
        }

        //make sure we can write to destination
        if (destination.exists() && !destination.canWrite())
        {
            String message = "Unable to open file " + destination
                + " for writing.";
            throw new IOException(message);
        }

        //makes sure it is not the same file
        if (source.getCanonicalPath().equals(destination.getCanonicalPath()))
        {
            String message = "Unable to write file " + source + " on itself.";
            throw new IOException(message);
        }
        
        if ( copyLength == 0 )
        {
            truncateFile( destination, 0 );
        }

        FileInputStream input = null;
        FileOutputStream output = null;
        try
        {
            input = new FileInputStream(source);
            output = new FileOutputStream(destination);
            long lengthLeft = copyLength;
            byte[] buffer = new byte[(int) Math.min(BUFFER_LENGTH,
                lengthLeft + 1)];
            int read;
            while ( lengthLeft > 0 )
            {
                read = input.read(buffer);
                if ( read == -1 )
                {
                    break;
                }
                lengthLeft -= read;
                output.write(buffer, 0, read);
            }
            output.flush();
            output.getFD().sync();
        }
        finally
        {
            IOUtil.closeQuietly(input);
            IOUtil.closeQuietly(output);
        }

        //file copy should preserve file date
        destination.setLastModified(source.lastModified());
    }
    
    /**
     * Splits the source file at the splitpoint into the destination file.
     * The result will be the source file containing data to the split point and
     * the destination file containing the data from the split point to the end
     * of the file.
     * @param source the source file to split.
     * @param destination the destination file to split into.
     * @param splitPoint the split point byte position inside the file.
     * When the file size is 10 and the splitPoint is 3 the source file will contain
     * the first 3 bytes while the destination file will contain the last 7 bytes.
     * @throws IOException in case of a IOException during split operation.
     */
    public static void splitFile( File source, File destination, long splitPoint )
        throws IOException
    {
        // open files
        RandomAccessFile sourceFile = new RandomAccessFile( source, "rws" );
        try
        {
            FileOutputStream outStream = new FileOutputStream( destination );
            try
            {
                sourceFile.seek( splitPoint );
                byte[] buffer = new byte[ (int)Math.min( BUFFER_LENGTH, source.length() + 1) ];
                int length;
                while ( -1 != (length = sourceFile.read(buffer)) )
                {
                    outStream.write( buffer, 0, length );
                }
                sourceFile.setLength( splitPoint );
            }
            finally
            {
                IOUtil.closeQuietly(outStream);
            }
        }
        finally
        {
            IOUtil.closeQuietly(sourceFile);
        }
    }
    
    /**
     * This method performce a multi fallback file rename operation to try to 
     * work around the Java problems with rename operations.
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6213298
     * @throws FileHandlingException 
     */ 
    public static void renameFileMultiFallback( File sourceFile, File destFile )
        throws FileHandlingException
    {
        if ( destFile.exists() )
        {
            // cant rename to file that already exists
            throw new FileHandlingException(
                FileHandlingException.FILE_ALREADY_EXISTS );
        }
        if ( !sourceFile.exists() )
        {
            return;
        }
        
        boolean succ = sourceFile.renameTo( destFile );
        if ( succ )
        {
            //NLogger.warn( FileUtils.class, "First renameTo operation worked!" );
            return;
        }
        //NLogger.warn( FileUtils.class, "First renameTo operation failed." );
        // Try to run garbage collector to make the file rename operation work
        System.gc();
        // Yield thread and to hope GC kicks in...
        Thread.yield();
        // Try rename again...
        succ = sourceFile.renameTo( destFile );
        if ( succ )
        {
            return;
        }
        //NLogger.warn( FileUtils.class, "Second renameTo operation failed." );
        // Rename failed again... just perform a slow copy/delete operation
        FileInputStream input = null;
        FileOutputStream output = null;
        try
        {
            input = new FileInputStream(sourceFile);
            output = new FileOutputStream(destFile);
            long lengthLeft = sourceFile.length();
            byte[] buffer = new byte[(int) Math.min(BUFFER_LENGTH,
                lengthLeft + 1)];
            int read;
            while ( lengthLeft > 0 )
            {
                read = input.read(buffer);
                if ( read == -1 )
                {
                    break;
                }
                lengthLeft -= read;
                output.write(buffer, 0, read);
            }            
        }
        catch ( IOException exp )
        {
            //NLogger.warn( FileUtils.class, "Third renameTo operation failed." );
            throw new FileHandlingException(
                FileHandlingException.RENAME_FAILED, exp );
        }
        finally
        {
            IOUtil.closeQuietly(input);
            IOUtil.closeQuietly(output);
        }
        //file copy should preserve file date
        destFile.setLastModified(sourceFile.lastModified());

        // try to delete file
        FileUtils.deleteFileMultiFallback( sourceFile );
    }
    
    /**
     * This method performs a multi fallback file delete operation to try to 
     * work around the Java problems with delete operations.
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6213298
     * @param file the file to delete. 
     */ 
    public static void deleteFileMultiFallback( File file )
    {
        boolean succ = file.delete();
        if ( succ )
        {
            return;
        }
        //NLogger.warn( FileUtils.class, "First delete operation failed." );
        // Try to run garbage collector to make the file delete operation work
        System.gc();
        // Yield thread and to hope GC kicks in...
        Thread.yield();
        // Try again...
        succ = file.delete( );
        if ( succ )
        {
            return;
        }
        //NLogger.warn( FileUtils.class, "Second delete operation failed." );
        // Last chance... try to delete on exit...
        file.deleteOnExit();
        // and truncate file to at least free up the space...
        try
        {
            FileUtils.truncateFile( file, 0 );
        }
        catch ( IOException exp )
        {
            //NLogger.warn( FileUtils.class, "Delete/truncate operation failed." );
        }
    }

    /**
     * Truncates an input file to the requested size. If the file doesnt exists
     * or is of equal or smaller size noting is done.
     * 
     * @param file The file to truncate.
     * @param size The size to truncate to.
     * @throws IOException in case an IO error occures during truncating.
     */
    public static void truncateFile( File file, long size )
        throws IOException
    {
        if ( size < 0 )
        {
            throw new IllegalArgumentException( "File size < 0: " + size );
        }
        if ( file.exists()
          && file.length() > size )
        {
            RandomAccessFile raf = null;
            try
            {
                raf = new RandomAccessFile( file, "rws" );
                raf.setLength( size );
            }
            finally
            {
                IOUtil.closeQuietly(raf);
            }
        }
    }

    /**
     * Checks if subDir is a sub directory of maybeParentDir.
     */
    public static boolean isChildOfDir( File maybeChild, File maybeParentDir )
    {
        // it can't be a sub dir if they don't start the same way...
        if ( !maybeChild.getAbsolutePath().startsWith( maybeParentDir.getAbsolutePath() ) )
        {
            return false;
        }
        return isChildOfDirInternal( maybeChild, maybeParentDir );
    }

    /**
     * Checks if subDir is a sub directory of maybeParentDir. Used for internal
     * processing.
     */
    private static boolean isChildOfDirInternal( File maybeChild, File maybeParentDir )
    {
        File parent = maybeChild.getParentFile();
        // no parent dir... cant be sub
        if ( parent == null )
        {
            return false;
        }
        // parent equals we have a sub dir
        if ( parent.equals( maybeParentDir ) )
        {
            return true;
        }
        // go up one level and check again
        return isChildOfDirInternal( parent, maybeParentDir );
    }
    
    /**
     * From Apache Jakarta Commons IO.
     * 
     * Copies a whole directory to a new location.
     * <p>
     * This method copies the contents of the specified source directory
     * to within the specified destination directory.
     * <p>
     * The destination directory is created if it does not exist.
     * If the destination directory did exist, then this method merges
     * the source with the destination, with the source taking precedence.
     *
     * @param srcDir  an existing directory to copy, must not be null
     * @param destDir  the new directory, must not be null
     * @param preserveFileDate  true if the file date of the copy
     *  should be the same as the original
     *
     * @throws NullPointerException if source or destination is null
     * @throws IOException if source or destination is invalid
     * @throws IOException if an IO error occurs during copying
     * @since Commons IO 1.1
     */
    public static void copyDirectory(File srcDir, File destDir,
            boolean preserveFileDate) throws IOException {
        if (srcDir == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destDir == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if ( !srcDir.exists() ) {
            throw new FileNotFoundException("Source '" + srcDir + "' does not exist");
        }
        if ( !srcDir.isDirectory() ) {
            throw new IOException("Source '" + srcDir + "' exists but is not a directory");
        }
        if (srcDir.getCanonicalPath().equals(destDir.getCanonicalPath())) {
            throw new IOException("Source '" + srcDir + "' and destination '" + destDir + "' are the same");
        }
        doCopyDirectory(srcDir, destDir, preserveFileDate);
    }

    /**
     * From Apache Jakarta Commons IO.
     * 
     * Internal copy directory method.
     * 
     * @param srcDir  the validated source directory, not null
     * @param destDir  the validated destination directory, not null
     * @param preserveFileDate  whether to preserve the file date
     * @throws IOException if an error occurs
     * @since Commons IO 1.1
     */
    private static void doCopyDirectory(File srcDir, File destDir, boolean preserveFileDate)
        throws IOException 
    {
        if (destDir.exists()) 
        {
            if ( !destDir.isDirectory() ) 
            {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } 
        else 
        {
            forceMkdir( destDir );
            if (preserveFileDate) 
            {
                destDir.setLastModified(srcDir.lastModified());
            }
        }
        if ( !destDir.canWrite() ) 
        {
            throw new IOException("Destination '" + destDir + "' cannot be written to");
        }
        // recurse
        File[] files = srcDir.listFiles();
        if (files == null) 
        {  // null if security restricted
            throw new IOException("Failed to list contents of " + srcDir);
        }
        for (int i = 0; i < files.length; i++) 
        {
            File copiedFile = new File(destDir, files[i].getName());
            if (files[i].isDirectory()) 
            {
                doCopyDirectory(files[i], copiedFile, preserveFileDate);
            } 
            else 
            {
                doCopyFile(files[i], copiedFile, preserveFileDate);
            }
        }
    }
    
    /**
     * From Apache Jakarta Commons IO.
     * 
     * Internal copy file method.
     * 
     * @param srcFile  the validated source file, not null
     * @param destFile  the validated destination file, not null
     * @param preserveFileDate  whether to preserve the file date
     * @throws IOException if an error occurs
     */
    private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) 
        throws IOException
    {        
        if ( destFile.exists() && destFile.isDirectory() )
        {
            throw new IOException( "Destination '" + destFile
                + "' exists but is a directory" );
        }

        FileChannel input = new FileInputStream( srcFile ).getChannel();
        try
        {
            FileChannel output = new FileOutputStream( destFile ).getChannel();
            try
            {
                output.transferFrom( input, 0, input.size() );
            } finally
            {
                IOUtil.closeQuietly( output );
            }
        } finally
        {
            IOUtil.closeQuietly( input );
        }

        if ( srcFile.length() != destFile.length() )
        {
            throw new IOException( "Failed to copy full contents from '"
                + srcFile + "' to '" + destFile + "'" );
        }
        if ( preserveFileDate )
        {
            destFile.setLastModified( srcFile.lastModified() );
        }
    }
    
    /**
     * Recursively delete a directory.
     *
     * @param directory  directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory)
        throws IOException {
        if (!directory.exists()) {
            return;
        }

        cleanDirectory(directory);
        if (!directory.delete()) {
            String message =
                "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Clean a directory without deleting it.
     *
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }
    
    /**
     * Delete a file. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     *      (java.io.File methods returns a boolean)</li>
     * </ul>
     *
     * @param file  file or directory to delete, not null
     * @throws NullPointerException if the directory is null
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            if (!file.exists()) {
                throw new FileNotFoundException("File does not exist: " + file);
            }
            if (!file.delete()) {
                String message =
                    "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }
    
    /**
     * Makes a directory, including any necessary but nonexistent parent
     * directories. If there already exists a file with specified name or
     * the directory cannot be created then an exception is thrown.
     *
     * @param directory  directory to create, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws IOException if the directory cannot be created
     */
    public static void forceMkdir(File directory) throws IOException {
        if (directory.exists()) {
            if (directory.isFile()) {
                String message =
                    "File "
                        + directory
                        + " exists and is "
                        + "not a directory. Unable to create directory.";
                throw new IOException(message);
            }
        } else {
            if (!directory.mkdirs()) {
                String message =
                    "Unable to create directory " + directory;
                throw new IOException(message);
            }
        }
    }
}