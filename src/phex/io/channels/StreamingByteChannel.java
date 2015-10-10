package phex.io.channels;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import phex.utils.IOUtil;

public class StreamingByteChannel implements ByteChannel
{
    private boolean isOpen;
    private InputStream inStream;
    private OutputStream outStream;
    
    public StreamingByteChannel( Socket socket ) throws IOException
    {
        this( socket.getInputStream(), socket.getOutputStream() );
    }
    
    public StreamingByteChannel( InputStream inStream, OutputStream outStream )
    {
        isOpen = true;
        this.inStream = inStream;
        this.outStream = outStream;
    }
    
    public boolean isOpen()
    {
        return isOpen;
    }
    
    public void close() throws IOException
    {
        isOpen = false;
        IOUtil.closeQuietly( inStream );
        IOUtil.closeQuietly( outStream );
    }

    public int read( ByteBuffer dst ) throws IOException
    {
        int lengthRead;
        int pos = dst.position();
        int limit = dst.limit();
        if ( dst.hasArray() )
        {
            byte[] bufferSrc = dst.array();
            lengthRead = inStream.read( bufferSrc, pos, limit-pos );
            if ( lengthRead > 0 )
            {
                dst.position( dst.position() + lengthRead );
            }
        }
        else
        {
            byte[] buf = new byte[ limit-pos ];
            lengthRead = inStream.read( buf );
            if ( lengthRead > 0 )
            {
                dst.put( buf, 0, lengthRead );
            }
        }
        return lengthRead;
        
    }

    public int write(ByteBuffer src) throws IOException
    {
        int pos = src.position();
        int limit = src.limit();
        if ( src.hasArray() )
        {
            byte[] bufferSrc = src.array();
            outStream.write( bufferSrc, pos, limit-pos );
            src.position( limit );
        }
        else
        {
            byte[] buf = new byte[ limit-pos ];
            src.get( buf );
            outStream.write( buf );
        }
        return limit-pos;
    }

}
