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
 */package phex.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;

/**
 * 
 */
public class GnutellaOutputStream extends OutputStream
{
    private boolean isOutputDeflated;
    private OutputStream outStream;
        
    public GnutellaOutputStream( OutputStream outStream )
    {
        this.outStream = outStream;
        isOutputDeflated = false;
    }
    
    public void activateOutputDeflation()
    {
        FixedDeflaterOutputStream deflaterOutStream =
            new FixedDeflaterOutputStream( outStream );

        outStream = deflaterOutStream;
        isOutputDeflated = true;
    }

    /**
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException
    {
//        int totalOut = 0;
//        if ( isOutputDeflated )
//        {
//            totalOut = ((DeflaterOutputStream)outStream).getTotalOut();
//        }
        outStream.write( b );
    }
    
    public void write( byte[] b ) throws IOException
    {
        this.write( b, 0, b.length );        
    }
    
    public void write( byte[] b, int off, int len ) throws IOException
    {
//        int totalOut = 0;
//        if ( isOutputDeflated )
//        {
//            totalOut = ((DeflaterOutputStream)outStream).getTotalOut();
//        }
        
        outStream.write( b, off, len );        
    }
    
    public void flush()
        throws IOException
    {
        outStream.flush();
    }
}
