/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2006 Phex Development Group
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

import java.io.IOException;
import java.io.OutputStream;

import phex.common.bandwidth.BandwidthController;

/**
 *
 *
 */
public class BandwidthOutputStream extends OutputStream
{
    private OutputStream outStream;

    private BandwidthController bandwidthController;

    public BandwidthOutputStream(OutputStream outStream,
        BandwidthController aBandwidthController)
    {
        this.outStream = outStream;
        bandwidthController = aBandwidthController;
    }

    /**
     * @param bandwidthController The bandwidthController to set.
     */
    public void setBandwidthController(BandwidthController bandwidthController)
    {
        this.bandwidthController = bandwidthController;
    }
    
    public void write(int b) throws IOException
    {
        // this call will always return at least 1 directly or after blocking.
        bandwidthController.getAvailableByteCount( 1, true, true );
        outStream.write(b);
    }
    
    public void write( byte[] b ) throws IOException
    {
        this.write( b, 0, b.length );        
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
        while (len > 0)
        {
            int available = bandwidthController.getAvailableByteCount( len, true, true );
            outStream.write(b, off, available);
            len -= available;
            off += available;
        }
    }

    public void flush() throws IOException
    {
        outStream.flush();
    }

    public void close() throws IOException
    {
        outStream.flush();
    }
}