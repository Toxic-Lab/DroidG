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
package phex.query;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang.SystemUtils;

import phex.host.Host;
import phex.msg.QueryMsg;
import phex.msghandling.MessageSubscriber;

/**
 * Query history monitor class that writes the QueryMsg search string into a
 * file. The search string is in UTF-8.<br>
 * The class will not close the Writer, if the Writer throws an IOException
 * it will be logged and monitoring will be interrupted.
 * <br>
 * Usage Example:
 * <code>
 *     File file = new File( StatisticPrefs.QueryHistoryLogFile.get() );
 *     File parent = file.getParentFile();
 *     if ( parent != null )
 *     {
 *         parent.mkdirs();
 *     }
 *     file.createNewFile();
 *     fileWriter = new FileWriter( file.getAbsolutePath(), true );
 *     new QueryMsgToWriterMonitor( fileWriter );
 * </code>
 */
public class QueryMsgToWriterMonitor implements MessageSubscriber<QueryMsg>
{
    private Writer outputWriter;

    public QueryMsgToWriterMonitor(Writer outputWriter )
    {
        if ( outputWriter == null )
        {
            throw new NullPointerException( "Output writer is null." );
        }
        this.outputWriter = outputWriter;
    }
    
    public void onMessage(QueryMsg query, Host sourceHost)
    {
        String searchString = query.getSearchString();
        if ( searchString.length() > 0 && !searchString.equals( "\\" ) 
             && !searchString.startsWith( "urn:sha1:" ) )
        {
            synchronized ( this )
            {
                try
                {
                    outputWriter.write( query.getSearchString() );
                    /*outputWriter.write( query.getSearchString() + "\t" +
                        query.getHeader().getHopsTaken() + "\t" +
                        query.getHeader().getTTL() + "\t");
                    URN[] urns = query.getQueryURNs();
                    for ( int i = 0; i < urns.length; i++ )
                    {
                        if ( urns[i].isSha1Nid() )
                        {
                            outputWriter.write( urns[i].getAsString() );
                            break;
                        }
                    }*/
                    outputWriter.write( SystemUtils.LINE_SEPARATOR );
                }
                catch ( IOException exp )
                {
//                    NLogger.error( QueryMsgToWriterMonitor.class, 
//                        exp.getMessage(), exp );
                    outputWriter = null;
                }
            }
        }
    }
}