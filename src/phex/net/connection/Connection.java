/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2007 Phex Development Group
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
 *  --- SVN Information ---
 *  $Id$
 */
package phex.net.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;

import phex.common.bandwidth.BandwidthController;
import phex.connection.ConnectionClosedException;
import phex.io.buffer.ByteBuffer;
import phex.io.channels.BandwidthByteChannel;
import phex.net.repres.SocketFacade;
import phex.utils.GnutellaInputStream;
import phex.utils.GnutellaOutputStream;
import phex.utils.IOUtil;

/**
 * 
 */
public class Connection
{   
    protected SocketFacade socket;
    
    private BandwidthController bandwidthController;
    
    private BandwidthByteChannel bandwidthByteChannel;
    
    protected GnutellaInputStream inputStream;
    private GnutellaOutputStream outputStream;
    
    
    /**
     * Creates a new Connection object for the given socket.
     * 
     * The standard BandwidthController used is the NetworkBandwidthController.
     * @param socket
     */
    public Connection( SocketFacade socket, BandwidthController bandwidthController )
    {
        if ( socket == null )
        {
            throw new IllegalArgumentException( "SocketFacade required.");
        }
        if ( bandwidthController == null )
        {
            throw new IllegalArgumentException( "Bandwidth controller required.");
        }
        this.socket = socket;
        this.bandwidthController = bandwidthController;
    }
    
    protected Connection()
    {
    }

    public void setBandwidthController( BandwidthController bandwidthController )
    {
        this.bandwidthController = bandwidthController;
        if ( bandwidthByteChannel != null )
        {
            bandwidthByteChannel.setBandwidthController( bandwidthController );
        }
    }
    
    private synchronized void initBandwidthByteChannel() throws IOException
    {
        if ( bandwidthByteChannel == null )
        {
            bandwidthByteChannel = new BandwidthByteChannel( socket.getChannel(), 
                bandwidthController );
        }
    }
    
    public SocketFacade getSocket()
    {
        return socket;
    }
    
    /**
     * @deprecated use read( ByteBuffer ) / write( ByteBuffer );
     */
    @Deprecated
    public GnutellaInputStream getInputStream()
        throws IOException
    {
        if ( inputStream == null )
        {
            if ( socket == null )
            {
                throw new ConnectionClosedException( "Connection already closed" );
            }
            initBandwidthByteChannel();
            InputStream inStream = Channels.newInputStream( bandwidthByteChannel );
            inputStream = new GnutellaInputStream( inStream );
        }
        return inputStream;
    }
    
    /**
     * @deprecated use read( ByteBuffer ) / write( ByteBuffer );
     */
    @Deprecated
    public int readPeek() throws IOException
    {
        return getInputStream().peek();
    }
    
    /**
     * @deprecated use read( ByteBuffer ) / write( ByteBuffer );
     */
    @Deprecated
    public String readLine() throws IOException
    {
        String line = getInputStream().readLine();
        return line;
    }

    /**
     * @deprecated use read( ByteBuffer ) / write( ByteBuffer );
     */
    @Deprecated
    public GnutellaOutputStream getOutputStream()
        throws IOException
    {
        if ( outputStream == null )
        {
            if ( socket == null )
            {
                throw new ConnectionClosedException( "Connection already closed" );
            }
            initBandwidthByteChannel();
            OutputStream outStream = Channels.newOutputStream( bandwidthByteChannel );
            outputStream = new GnutellaOutputStream( outStream );
        }
        return outputStream;
    }
    
    public void write( ByteBuffer buffer ) throws IOException
    {
        int pos = buffer.position();
        int limit = buffer.limit();
        if ( buffer.hasArray() )
        {
            byte[] bufferSrc = buffer.array();
            getOutputStream().write( bufferSrc, pos, limit-pos );
            buffer.position( limit );
        }
        else
        {
            byte[] buf = new byte[ limit-pos ];
            buffer.get( buf );
            getOutputStream().write( buf );
        }
    }
    
    public void read( ByteBuffer buffer ) throws IOException
    {
        int pos = buffer.position();
        int limit = buffer.limit();
        if ( buffer.hasArray() )
        {
            byte[] bufferSrc = buffer.array();
            int length = getInputStream().read( bufferSrc, pos, limit-pos );
            buffer.skip( length );
        }
        else
        {
            byte[] buf = new byte[ limit-pos ];
            int length = getInputStream().read( buf );
            buffer.put( buf, 0, length );
        }
    }
    
    public void flush() throws IOException
    {
        getOutputStream().flush();
    }
    
    public void disconnect()
    {
        //NLogger.debug( Connection.class, "Disconnecting socket " + socket );
        IOUtil.closeQuietly( inputStream );
        IOUtil.closeQuietly( outputStream );
        IOUtil.closeQuietly( socket );
        inputStream = null;
        outputStream = null;
        socket = null;        
    }
}
