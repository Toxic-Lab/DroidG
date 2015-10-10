package phex.metalink;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.URI;
import org.xml.sax.SAXException;

import phex.utils.StringUtils;

public class MetalinkParser
{
    
    public static List<URI> parseMagnetUriFromFile( File metalinkFile )
    {
        try
        {
            BufferedInputStream inStream = new BufferedInputStream( new FileInputStream( 
                metalinkFile ) );
            
            MetalinkParser parser = new MetalinkParser( inStream );
            parser.start();
            return parser.getMagnets();
        }
        catch ( IOException exp )
        {
            //NLogger.warn( MetalinkParser.class, exp.getMessage(), exp);
            return Collections.emptyList();
        }
    }
    
    private List<URI> magnets;
    private InputStream inStream;
    
    public MetalinkParser( InputStream inStream )
    {
        magnets = new ArrayList<URI>();
        this.inStream = inStream;
    }
        
    
    public void start( ) 
        throws IOException
    {
        // Use an instance of DMetalink as data holder for the SAX event handler
        DMetalink metalink = new DMetalink();
        MetalinkSAXHandler handler = new MetalinkSAXHandler( metalink );
        // Use the default (non-validating) parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        
        try
        {
            SAXParser saxParser = factory.newSAXParser();
            
            // Parse the input
            saxParser.parse( inStream, handler);
            
            for( DMetalink.FileEntry fileEntry : metalink.files )
            {
                String magnetStr = fileEntry.magnet();
                if ( StringUtils.isEmpty( magnetStr ) )
                {
                    continue;
                }
                magnets.add( new URI( magnetStr, true ) );
            }
        }
        catch ( ParserConfigurationException exp )
        {
            //NLogger.error( MetalinkParser.class, exp, exp );
            throw new IOException( "Parsing Metalink XML failed." );
        }
        catch ( SAXException exp )
        {
            //NLogger.error( MetalinkParser.class, exp, exp );
            throw new IOException( "Parsing Metalink XML failed." );
        }
    }
    
    public List<URI> getMagnets ()
    {
        return magnets;
    }
}