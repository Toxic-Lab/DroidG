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
 *  Created on 19.11.2004
 *  --- CVS Information ---
 *  $Id$
 */
package phex.share.export;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import phex.common.Environment;
import phex.common.address.DestAddress;
import phex.share.ShareFile;
import phex.utils.IOUtil;

/**
 *
 */
public class ExportEngine
{
    public static final String USE_MAGNET_URL_WITH_XS = "UseMagnetURLWithXS";
    public static final String USE_MAGNET_URL_WITH_FREEBASE = "UseMagnetURLWithFreeBase";
    
    private final DestAddress serventAddress;
    private InputStream styleSheetStream;
    private OutputStream destinationStream;
    private List<ShareFile> exportData;
    private Map<String, String> exportOptions;
    
    public ExportEngine( DestAddress serventAddress, OutputStream destinationStream, List<ShareFile> exportData )
    {
        this( serventAddress, null, destinationStream, exportData, null );
    }
    
    public ExportEngine( DestAddress serventAddress, InputStream styleSheetStream, 
        OutputStream destinationStream, List<ShareFile> exportData, 
        Map<String, String> exportOptions )
    {
        if ( destinationStream == null )
        {
            throw new NullPointerException( "No destination to export to" );
        }
        if ( exportData == null )
        {
            throw new NullPointerException( "No data to export given." );
        }
        if ( styleSheetStream == null )
        {
            styleSheetStream = ClassLoader.getSystemResourceAsStream(
                "phex/resources/defaultSharedFilesHTMLExport.xsl" );
                //"phex/resources/magmaSharedFilesYAMLExport.xsl" );
        }
        
        this.serventAddress = serventAddress;
        this.styleSheetStream = styleSheetStream;
        this.destinationStream = destinationStream;
        this.exportData = exportData;
        this.exportOptions = exportOptions;
    }    
    
    /**
     * Known options:
     *  - UseMagnetURLWithXS = true
     *  - UseMagnetURLWithFreeBase = true
     * @param options
     */
    public void setExportOptions( Map<String, String> options )
    {
        exportOptions = options;
    }
    
    public void startExport(  )
    {
        PipedOutputStream pipedOutStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream();
        try
        {
            pipedOutStream.connect( pipedInputStream );
            
            SharedFilesPipeFiller fillerRunnable = new SharedFilesPipeFiller( serventAddress, 
                pipedOutStream, exportData, exportOptions );
            Environment.getInstance().executeOnThreadPool( fillerRunnable, "SharedFilesPipeFiller" );
    
            
            StreamSource styleSheetSource = new StreamSource( styleSheetStream );
            StreamSource dataSource = new StreamSource( pipedInputStream );
            StreamResult result = new StreamResult( destinationStream );
            try
            {
                Transformer transformer = TransformerFactory.newInstance()
                    .newTransformer(styleSheetSource);
                transformer.transform(dataSource, result);
            }
            catch (TransformerException exp)
            {
                //NLogger.error( ExportEngine.class, exp, exp );
            }
        }
        catch ( IOException exp )
        {
            //NLogger.error( ExportEngine.class, exp, exp );
        }
        finally
        {
            IOUtil.closeQuietly( pipedInputStream );
            IOUtil.closeQuietly( pipedOutStream );
        }
    }
}