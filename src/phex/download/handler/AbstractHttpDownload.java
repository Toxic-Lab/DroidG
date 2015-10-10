package phex.download.handler;

import phex.download.DownloadEngine;
import phex.host.UnusableHostException;
import phex.http.HTTPHeader;
import phex.http.HTTPHeaderNames;
import phex.http.HTTPResponse;

public abstract class AbstractHttpDownload implements DownloadHandler
{
    protected DownloadEngine downloadEngine;
    
    protected boolean isKeepAliveSupported;
    
    AbstractHttpDownload( DownloadEngine engine )
    {
    	downloadEngine = engine;
    }
    
    protected void updateServerHeader( HTTPResponse response ) throws UnusableHostException
    {
        HTTPHeader[] headers = response.getHeaders( HTTPHeaderNames.SERVER );
        if ( headers == null || headers.length == 0)
        {
            return;
        }
        if ( headers.length > 1 )
        {// faulty candidates seem to sometime send multiple different
         // server headers...
            throw new UnusableHostException( "Suspicious response headers." );
        }
        downloadEngine.getDownloadSet().getCandidate().setVendor( headers[0].getValue() );
    }
    
    protected void updateKeepAliveSupport( HTTPResponse response )
    {
        // check if Keep-Alive connection is accepted
        HTTPHeader header = response.getHeader( HTTPHeaderNames.CONNECTION );
        if ( header != null )
        {
            if ( header.getValue().equalsIgnoreCase( "close" ) )
            {
                isKeepAliveSupported = false;
                return;
            }
            else if ( header.getValue().equalsIgnoreCase( "keep-alive" ) )
            {
                isKeepAliveSupported = true;
                return;
            }
        }
        // missing or unknown connection header do the HTTP method default.
        if ( response.getHTTPVersion().equals("HTTP/1.1") )
        {
            isKeepAliveSupported = true;
        }
        else
        {
            isKeepAliveSupported = false;
        }
    }
}
