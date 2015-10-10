package phex.io.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import phex.common.bandwidth.BandwidthController;

public class BandwidthByteChannel implements ByteChannel
{
    private ByteChannel delegate;
    
    private BandwidthController bandwidthController;
    
    public BandwidthByteChannel( ByteChannel delegate, 
        BandwidthController bandwidthController )
    {
        if ( delegate == null )
        {
            throw new NullPointerException( "no delegate" );
        }
        this.delegate = delegate;
        this.bandwidthController = bandwidthController;
    }
    
    /**
     * @param bandwidthController The bandwidthController to set.
     */
    public void setBandwidthController(BandwidthController bandwidthController)
    {
        this.bandwidthController = bandwidthController;
    }
    
    public boolean isOpen()
    {
        return delegate.isOpen();
    }
    
    public void close() throws IOException
    {
        delegate.close();
    }

    public int read( ByteBuffer dst ) throws IOException
    {
        int pos = dst.position();
        int len = dst.limit() - pos;
        int available = bandwidthController.getAvailableByteCount( len, true, false );
        dst.limit( pos + available );
        int readLen = delegate.read( dst );
        if ( readLen >= 0 )
        {
            bandwidthController.markBytesUsed( readLen );
        }
        return readLen;
    }

    public int write( ByteBuffer src ) throws IOException
    {
        int pos = src.position();
        int len = src.limit() - pos;
        int totalWritten = 0;
        while ( len > 0 )
        {
            int available = bandwidthController.getAvailableByteCount( len, true, true );
            src.limit( pos + available );
            int written = delegate.write( src );
            len -= written;
            totalWritten += written;
            pos = src.position();
        }
        return totalWritten;
    }

}
