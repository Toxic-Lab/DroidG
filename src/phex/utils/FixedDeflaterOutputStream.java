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

import net.sf.jazzlib.DeflaterOutputStream;


/**
 * Fixed DeflaterOutputStream to solve the problem:
 * http://developer.java.sun.com/developer/bugParade/bugs/4255743.html
 */
public class FixedDeflaterOutputStream extends DeflaterOutputStream
{    
    public FixedDeflaterOutputStream(OutputStream outStream )
    {
        super( outStream );
    }
    
    public void flush()
        throws IOException
    {
//        if( def.finished() )
//        {
//            return;
//        } 
//        
//        // This code is from the javasoft bug database to solve the flushing
//        // problem..
//        // http://developer.java.sun.com/developer/bugParade/bugs/4255743.html
//        // By switching compression level we force Deflater to flush its data.
//        //def.setLevel(Deflater.NO_COMPRESSION);
//        
//        def.setInput( IOUtil.EMPTY_BYTE_ARRAY, 0, 0);
//
//        def.setLevel( Deflater.NO_COMPRESSION );
//        deflate();
//
//        def.setLevel( Deflater.DEFAULT_COMPRESSION );
//        deflate();

        super.flush();
    }
    
    public int getTotalIn()
    {
        return def.getTotalIn();
    }
    
    public int getTotalOut()
    {
        return def.getTotalOut();
    }
}
