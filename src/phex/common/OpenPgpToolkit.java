package phex.common;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import phex.http.HTTPHeaderNames;
import phex.http.HttpClientFactory;
import phex.prefs.core.ProxyPrefs;
import phex.utils.HexConverter;
import phex.utils.IOUtil;
import phex.utils.RandomUtils;
import phex.utils.StringUtils;

public class OpenPgpToolkit
{
    //private static final Logger logger = LoggerFactory.getLogger( OpenPgpToolkit.class );
    
    static
    {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * We only use servers supporting port 80, this allows us to use common HTTP proxies.
     */
    // http://www.rossde.com/PGP/pgp_keyserv.html
    private static final List<String> KEYSERVER_LIST = Arrays.asList( new String[] {
        "gpg-keyserver.de", 
        /* "keyserver.pramberger.at",*/
        "pgp.webtru.st",
        "minsky.surfnet.nl", 
        /* "keyserver.linux.it",*/
        "pgpkeys.pca.dfn.de",
        "keyserver.ubuntu.com",
        "wwwkeys.stinkfoot.us.pgp.net"
    } );
    
    public PGPPublicKey lookupKeyById( String keyserver, String keyId ) 
        throws IOException
    {
        String url = "http://"+keyserver+"/pks/lookup?op=get&search="+keyId;
        
        HttpClient client = HttpClientFactory.createHttpClient();
        if ( ProxyPrefs.UseHttp.get().booleanValue()  
            && !StringUtils.isEmpty( ProxyPrefs.HttpHost.get() ) )
        {
            client.getHostConfiguration().setProxy( ProxyPrefs.HttpHost.get(), 
                ProxyPrefs.HttpPort.get().intValue() );
        }
        GetMethod method = new GetMethod( url );
        method.addRequestHeader("Cache-Control", "no-cache");
        method.addRequestHeader( HTTPHeaderNames.CONNECTION,
            "close" );
        
        int responseCode = client.executeMethod(method);
        if ( responseCode < 200 || responseCode > 299 )
        {
            //logger.error( "Failed to connect to keyserver: " + url );
            throw new IOException( "failed rc:" + responseCode );
        }
        
        InputStream bodyStream = method.getResponseBodyAsStream();
        
        ArmoredInputStream as = new ArmoredInputStream( bodyStream );
        PGPPublicKeyRing ring = new PGPPublicKeyRing(as);
        long keyid = IOUtil.deserializeLong( HexConverter.toBytes( 
            keyId.substring( 2 ) ), 0 );
        try
        {
            PGPPublicKey key = ring.getPublicKey( keyid );
            return key;
        }
        catch ( PGPException exp )
        {
            //logger.error( exp.toString(), exp );
            throw new IOException( "PGPException: " + exp.getMessage() );
        }
    }
 
    public String getRandomKeyserver()
    {
        int pos = RandomUtils.getInt( KEYSERVER_LIST.size() );
        return KEYSERVER_LIST.get( pos );
    }
    
    public List<String> getKeyserverList()
    {
        return Collections.unmodifiableList( KEYSERVER_LIST );
    }
}
