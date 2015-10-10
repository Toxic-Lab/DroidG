package phex.xml.sax.favorites;

import org.xml.sax.SAXException;

import phex.xml.sax.DElement;
import phex.xml.sax.PhexXmlSaxWriter;


public class DFavoriteHost implements DElement
{
    private byte[] ip;

    private String hostName;

    private boolean hasPort;

    private int port;

    public byte[] getIp()
    {
        return ip;
    }

    public void setIp( byte[] value )
    {
        ip = value;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName( String value )
    {
        hostName = value;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort( int value )
    {
        port = value;
        hasPort = true;
    }
    
    public boolean hasPort( )
    {
        return hasPort;
    }

    public void serialize( PhexXmlSaxWriter writer ) throws SAXException
    {
        writer.startElm( "favorite-host", null );
        
        if( hostName != null )
        {
            writer.startElm( "host-name", null );
            writer.elmText( hostName );
            writer.endElm( "host-name" );
        }
        
        if( ip != null )
        {
            writer.startElm( "ip", null );
            writer.elmHexBinary( ip );
            writer.endElm( "ip" );
        }
        
        if( hasPort )
        {
            writer.startElm( "port", null );
            writer.elmInt(port);
            writer.endElm( "port" );
        }
        writer.endElm( "favorite-host" );
    }
}
