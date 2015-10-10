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
package phex.share.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import phex.common.URN;
import phex.common.address.DestAddress;
import phex.share.ShareFile;
import phex.utils.IOUtil;
import phex.utils.URLCodecUtils;
import phex.utils.URLUtil;
import phex.xml.XMLUtils;

/**
 * <shared-file-export>
 *   <export-options>
 *     <option name='UseMagnetURLWithXS'>true</option>
 *   </export-options>
 *   <shared-file-list>
 *     <shared-file> 
 *       <index>1</index>
 *       <name>phex_2.0.0.76.exe</name>
 *       <search-count>100</search-count>
 *       <sha1>T2SXTXXCTJKIDMDVONPRHPXH4NOZRBT4</sha1>
 *       <upload-count>10</upload-count>
 *       <file-size>2252314</file-size>
 *       <urn>urn:sha1:T2SXTXXCTJKIDMDVONPRHPXH4NOZRBT4</urn>
 *     
 *       ... for more check the source...  
 * 
 *     </shared-file>
 *   </shared-file-list>
 * </shared-file-export>
 */
public class SharedFilesPipeFiller implements Runnable
{
    private final DestAddress serventAddress;
    private Writer utf8Writer;
    private List<ShareFile> exportData;
    private Map<String, String> exportOptions;
    
    public SharedFilesPipeFiller( DestAddress serventAddress, 
        OutputStream outStream, List<ShareFile> exportData, 
        Map<String, String> options )
    {
        this.serventAddress = serventAddress;
        
        if ( exportData == null )
        {
            throw new NullPointerException( "No exportData given." );
        }
        this.exportData = exportData;
        
        try
        {
            utf8Writer = new OutputStreamWriter( outStream, "UTF-8" );
        }
        catch (UnsupportedEncodingException e)
        {
            assert( false );
        }
        exportOptions = options;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        try
        {
            utf8Writer.write("<shared-file-export>" );
            if ( exportOptions != null && exportOptions.size() > 0 )
            {
                utf8Writer.write("<export-options>");
                Iterator<String> optionIterator = exportOptions.keySet().iterator();
                while( optionIterator.hasNext() )
                {
                    String optionName = optionIterator.next();
                    utf8Writer.write("<option name='" + optionName +  "'>");
                    utf8Writer.write( exportOptions.get(optionName) );
                    utf8Writer.write("</option>");
                }
                utf8Writer.write("</export-options>");
            }
            utf8Writer.write("<shared-file-list>");

            for ( ShareFile file : exportData )
            {                
                URN urn = file.getURN();
                if ( urn == null )
                {// we skip reporting of null urn files... they don't make sense...
                    continue;
                }
                
                utf8Writer.write("<shared-file>");
                utf8Writer.write("<index>"); 
                XMLUtils.writeEncoded( utf8Writer, String.valueOf( file.getFileIndex() ) ); 
                utf8Writer.write("</index>");
                
                utf8Writer.write("<name>");
                XMLUtils.writeEncoded( utf8Writer, file.getFileName() ); 
                utf8Writer.write("</name>");
                
                utf8Writer.write("<name-urlenc>");
                XMLUtils.writeEncoded( utf8Writer, 
                    URLCodecUtils.encodeURL( file.getFileName() ) ); 
                utf8Writer.write("</name-urlenc>");
                
                utf8Writer.write("<search-count>");
                XMLUtils.writeEncoded( utf8Writer, String.valueOf( file.getSearchCount() ) );
                utf8Writer.write("</search-count>");
                
                utf8Writer.write("<sha1>");
                XMLUtils.writeEncoded( utf8Writer, file.getSha1() );
                utf8Writer.write("</sha1>");
                
                utf8Writer.write("<upload-count>");
                XMLUtils.writeEncoded( utf8Writer, String.valueOf( file.getUploadCount() ) ); 
                utf8Writer.write("</upload-count>");
                
                utf8Writer.write("<file-size>");
                XMLUtils.writeEncoded( utf8Writer, String.valueOf( file.getFileSize() ) ); 
                utf8Writer.write("</file-size>");
                
                utf8Writer.write("<urn>"); 
                XMLUtils.writeEncoded( utf8Writer, urn.getAsString() ); 
                utf8Writer.write("</urn>");
                
                if( file.getAltLocCount() > 0 )
                {
                    Set<DestAddress> locs = file.getAltLocContainer().getAltLocsForExport( 
                        serventAddress );
                    for ( DestAddress add : locs )
                    {
                        utf8Writer.write( "<alt-loc>" );
                        XMLUtils.writeEncoded( utf8Writer, add.getFullHostName() );
                        utf8Writer.write( "</alt-loc>" );
                    }
                }
                
                utf8Writer.write("<magnet-url>");
                if ( exportOptions != null && 
                     "true".equals( exportOptions.get( ExportEngine.USE_MAGNET_URL_WITH_XS ) ) )
                {
                    XMLUtils.writeEncoded( utf8Writer, URLUtil.buildMagnetURLWithXS( 
                        file.getSha1(), file.getFileName(), serventAddress ) );
                }
                else
                {
                    XMLUtils.writeEncoded( utf8Writer, URLUtil.buildMagnetURL( 
                        file.getSha1(), file.getFileName() ) );
                }
                utf8Writer.write("</magnet-url>");
                
                utf8Writer.write("<name2res-url>"); 
                XMLUtils.writeEncoded( utf8Writer, URLUtil.buildFullName2ResourceURL( 
                    serventAddress, urn ) ); 
                utf8Writer.write("</name2res-url>");
                
                utf8Writer.write("</shared-file>");
            }
            utf8Writer.write("</shared-file-list>");
            utf8Writer.write("</shared-file-export>" );
        }
        catch ( IOException exp )
        {
            //NLogger.error( SharedFilesPipeFiller.class, exp, exp );
        }
        finally
        {
            IOUtil.closeQuietly(utf8Writer);
        }
    }
    
}
