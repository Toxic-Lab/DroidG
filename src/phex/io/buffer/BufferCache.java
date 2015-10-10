package phex.io.buffer;

import java.nio.ByteBuffer;
import phex.utils.StringUtils;

public class BufferCache
{
    public static final String OK = "OK";
    public static final ByteBuffer OK_BUFFER;
    
    public static final String LFLF = "\n\n";
    public static final ByteBuffer LFLF_BUFFER;
    
    static
    {
        OK_BUFFER   = ByteBuffer.wrap( StringUtils.getBytesInUsAscii( OK ) ).asReadOnlyBuffer();
        LFLF_BUFFER = ByteBuffer.wrap( StringUtils.getBytesInUsAscii( LFLF ) ).asReadOnlyBuffer();
    }
}
