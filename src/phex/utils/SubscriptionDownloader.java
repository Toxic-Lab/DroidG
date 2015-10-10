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
 *  --- CVS Information ---
 *  $Id$
 */
package phex.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;

import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;

import phex.common.Environment;
import phex.common.Phex;
import phex.event.PhexEventService;
import phex.event.PhexEventTopics;
import phex.prefs.core.SubscriptionPrefs;
import phex.servent.Servent;
import phex.share.FileRescanRunner;

public class SubscriptionDownloader extends TimerTask
{
    public SubscriptionDownloader()
    {
        Environment.getInstance().scheduleTimerTask( this, 0, 14 * 24  * 3600 * 1000 );
    }

    public void run()
    {
        List subscriptionMagnets = loadSubscriptionList();
        String uriStr;
        Iterator iterator = subscriptionMagnets.iterator();
        
        // Sync subscription operation with a possible rescan process, this 
        // prevents downloads of files already existing but not yet scanned.
        FileRescanRunner.sync();
        
        while ( iterator.hasNext() )
        {
            uriStr = (String) iterator.next();
            if ( SubscriptionPrefs.DownloadSilently.get().booleanValue() )
            {
                try
                {
                    createDownload( uriStr );
                } //This donwloads the magma via magnet silently in the background. 
                catch (URIException exp)
                {
                    //NLogger.error( SubscriptionDownloader.class, exp.getMessage(), exp );
                }
            }
            else
            {
                PhexEventService eventService = Phex.getEventService();
                eventService.publish( PhexEventTopics.Incoming_Uri, uriStr );
            }
        }
    }

    private List loadSubscriptionList()
    {
        String name = "/subscription.list";
        InputStream stream = SubscriptionDownloader.class
            .getResourceAsStream( name );
        
        // TODO verify the code inside this if{}... is this really correct what
        // happens here?? looks strange...
        if ( stream == null )
        {
            List<String> subscriptionMagnets = SubscriptionPrefs.SubscriptionMagnets.get();
            List<String> list = SubscriptionPrefs.SubscriptionMagnets.get();
            if ( subscriptionMagnets != null
                && SubscriptionPrefs.default_subscriptionMagnets != null )
            {
                subscriptionMagnets.add( SubscriptionPrefs.default_subscriptionMagnets );
            }
            return list;
        }
        try
        {
            // make sure it is buffered
            InputStreamReader input = new InputStreamReader( stream );
            BufferedReader reader = new BufferedReader( input );
            List<String> list = new ArrayList<String>();
            String line = reader.readLine();
            while ( line != null )
            {
                list.add( line );
                line = reader.readLine();
            }
            List<String> oldSubscriptionMagnets = SubscriptionPrefs.SubscriptionMagnets.get();
            list.addAll( oldSubscriptionMagnets );
            return list;
        }
        catch (IOException exp)
        {
            //NLogger.warn( SubscriptionDownloader.class, exp );
        }
        finally
        {
            IOUtil.closeQuietly( stream );
        }
        return Collections.EMPTY_LIST;
    }

    public void createDownload( String uriStr ) throws URIException
    {
        if (uriStr.length() == 0)
        {
            return;
        }
        URI uri = new URI( uriStr, true );
        Servent.getInstance().getDownloadService().addFileToDownload( uri, true );
    }
}
