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
 *  Created on 23.11.2005
 *  --- CVS Information ---
 *  $Id$
 */
package phex.xml.sax;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import phex.common.Phex;
import phex.common.file.FileManager;
import phex.common.file.ManagedFile;
import phex.common.file.ManagedFileException;
import phex.common.file.ManagedFileInputStream;
import phex.common.file.ManagedFileOutputStream;
import phex.utils.IOUtil;
import phex.xml.sax.parser.UnicodeInputStream;

/**
 * Helper class to create, generate and parse a xml Document from various
 * resources.
 */
public class XMLBuilder
{
    public static DPhex loadDPhexFromFile( File file )
        throws IOException
    {
        FileManager fileMgr = Phex.getFileManager();
        ManagedFile managedFile;
        try
        {
            managedFile = fileMgr.getReadWriteManagedFile( file );
        }
        catch ( ManagedFileException exp )
        {
            // TODO refactor for Java 6
            IOException ioexp = new IOException( );
            ioexp.initCause( exp );
            throw ioexp;
        }
        return loadDPhexFromFile( managedFile );
    }

    /**
     * Tries to load the DPhex marshaled object from the given file.
     * If the file doesn't exist, null is returned.
     * @param managedFile the file to load the DPhex object from.
     * @return the DPhex object or null if file doesn't exist.
     * @throws IOException 
     */
    public static DPhex loadDPhexFromFile( ManagedFile managedFile )
        throws IOException
    {
        if ( !managedFile.exists() )
        {
            return null;
        }
        //NLogger.debug( XMLBuilder.class, "Loading DPhex from: " + managedFile );
        InputStream inStream = null;
        try
        {
            managedFile.acquireFileLock();
            inStream = new ManagedFileInputStream( managedFile, 0 );
            return readDPhexFromStream(inStream);
        }        
        finally
        {
            IOUtil.closeQuietly( inStream );
            IOUtil.closeQuietly( managedFile );
            managedFile.releaseFileLock();
        }
    }
        
    /**
     * Trys to read the DPhex marshaled object from the given stream.
     * @param inStream the stream to read the DPhex object from.
     * @return the DPhex object.
     * @throws IOException 
     */
    public static DPhex readDPhexFromStream( InputStream inStream ) 
        throws IOException
    {
        // TODO workaround for Java 1.4.2 bug, when files contain optional BOM 
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4508058
        // should only occur with crimson parser in java 1.4 but seems like
        // xerces in java 1.5 is able to handle these. Though we always use it
        // to be save.
        UnicodeInputStream ucInStream = new UnicodeInputStream( inStream, "UTF-8" );
        // make sure we are not interrupted before continuing.
        if ( Thread.currentThread().isInterrupted() )
        {
            throw new InterruptedIOException("Thread interrupted.");
        }
        try
        {
            ucInStream.getEncoding();
        }
        catch ( IllegalStateException exp) 
        {
            if ( Thread.currentThread().isInterrupted() )
            {
                throw new InterruptedIOException("Thread interrupted.");
            }
            else
            {
                //NLogger.error(XMLBuilder.class, exp, exp);
            }
        }
        DPhex phex = PhexXmlSaxParser.parsePhexXml( ucInStream );
        return phex;
    }
    
    public static byte[] serializeToBytes( DPhex dPhex ) 
        throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream( );
        PhexXmlSaxWriter.serializePhexXml(bos, dPhex);
        return bos.toByteArray();
    }
    
    public static void saveToFile( File file, DPhex dPhex )
        throws IOException
    {
        try
        {
            ManagedFile managedFile = Phex.getFileManager()
                .getReadWriteManagedFile( file );
            saveToFile( managedFile, dPhex );
        }
        catch ( ManagedFileException exp )
        {
            // TODO refactor for Java 6
            IOException ioexp = new IOException( );
            ioexp.initCause( exp );
            throw ioexp;
        }
    }
    
    public static void saveToFile( ManagedFile managedFile, DPhex dPhex )
        throws IOException, ManagedFileException
    {
        ManagedFileOutputStream outStream = null;
        try
        {
            managedFile.acquireFileLock();
            managedFile.setLength( 0 );
            outStream = new ManagedFileOutputStream( managedFile, 0 );
            PhexXmlSaxWriter.serializePhexXml(outStream, dPhex);
        }
        finally
        {
            IOUtil.closeQuietly( outStream );
            IOUtil.closeQuietly( managedFile );
            managedFile.releaseFileLock();
        }
    }    
}
