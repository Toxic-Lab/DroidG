/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2009 Phex Development Group
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
package phex.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.params.HttpParams;

import phex.common.Phex;

public class HttpClientFactory
{
    private static HttpConnectionManager connectionManager;
    
    static
    {
        HttpParams globalParams = DefaultHttpParams.getDefaultParams();
        globalParams.setParameter( HttpMethodParams.USER_AGENT,
            Phex.getFullPhexVendor() );
        globalParams.setIntParameter( HttpClientParams.MAX_REDIRECTS, 10 );
        
        connectionManager = new MultiThreadedHttpConnectionManager();
    }
    
    /**
     * Basically we could have one http client for the whole application, 
     * but since we have very different configuration, with proxy and without,
     * for GWebCaches and for PushProxies, we decided to create several instances
     * for different cases. 
     * @return a new http client.
     */
    public static HttpClient createHttpClient()
    {
        HttpClient client = new HttpClient( connectionManager );
        return client;
    }
}
