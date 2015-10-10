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
 *  Created on 04.01.2006
 *  --- CVS Information ---
 *  $Id$
 */
package phex.msg.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import phex.msg.InvalidMessageException;
import phex.msg.MsgHeader;
import phex.utils.IOUtil;

/**
 * A message to tell which capabilities are supported.
 */
public class CapabilitiesVMsg extends VendorMsg
{
    private static final int VERSION = 1;
    
    /**
     * Feature search bytes. Represent 'WHAT'
     */
    private static final byte[] FEATURE_SEARCH_BYTES = {(byte)87, (byte)72,
                                                        (byte)65, (byte)84};
    private static final int FEATURE_SEARCH_VERSION = 1;
    
    private static CapabilitiesVMsg myCapavilitiesVMsg;
    
    /**
     * Supported capabilities.
     */
    private final Set<SupportedCapability> capabilitiesSet;
    
    /**
     * @param header
     * @param version
     * @param data
     */
    public CapabilitiesVMsg( MsgHeader header, byte[] vendorId,
        int subSelector, int version, byte[] data)
        throws InvalidMessageException
    {
        super(header, vendorId, subSelector, version, data);
        if (version > VERSION)
        {
            throw new InvalidMessageException(
                "Vendor Message 'MessagesSupported' with invalid version: "
                    + version);
        }
        capabilitiesSet = new HashSet<SupportedCapability>();
        try
        {
            ByteArrayInputStream dataStream = new ByteArrayInputStream( data );
            // first read 2 bytes caps...
            int itemCount = IOUtil.unsignedShort2Int( IOUtil.deserializeShortLE( dataStream ) );
            byte[] itemBytes = new byte[6];
            for (int i = 0; i < itemCount; i++)
            {
                dataStream.read(itemBytes);
                SupportedCapability supportedCapability = new SupportedCapability(itemBytes);
                capabilitiesSet.add( supportedCapability );
            }
            
            // we currently don't support 4 byte caps of version 1
            //if ( dataStream.available() > 0 )
            //{
                // next read 4 bytes caps...
                //itemCount = IOUtil.unsignedShort2Int( IOUtil.deserializeShortLE( dataStream ) );
                //itemBytes = new byte[8];
                //for (int i = 0; i < itemCount; i++)
                //{
                //    dataStream.read(itemBytes);
                //    SupportedCapability supportedCapability = new SupportedCapability(itemBytes);
                //    capabilitiesSet.add( supportedCapability );
                //}
            //}
        } 
        catch ( IOException exp )
        {
            //NLogger.error( CapabilitiesVMsg.class, exp, exp );
            throw new InvalidMessageException(exp.getMessage());
        }
    }
    
    /**
     * Create my MessagesSupportedVMsg used to tell others.
     */
    private CapabilitiesVMsg()
    {
        super(VENDORID_NULL, SUBSELECTOR_CAPABILITIES, VERSION, 
            IOUtil.EMPTY_BYTE_ARRAY );
        capabilitiesSet = new HashSet<SupportedCapability>();
        createCapabilitiesMsgData( );
    }
    
    public boolean isCapabilitySupported( byte[] capabilityName )
    {
        for( SupportedCapability cap : capabilitiesSet )
        {
            if (Arrays.equals(capabilityName, cap.getName() ) )
            {
                return true;
            }            
        }
        return false;
    }
    
    public boolean isFeatureSearchSupported() 
    {
        return isCapabilitySupported(FEATURE_SEARCH_BYTES);
    }
    
    public static CapabilitiesVMsg getMyCapabilitiesVMsg()
    {
        if ( myCapavilitiesVMsg == null )
        {
            myCapavilitiesVMsg = new CapabilitiesVMsg();
        }
        return myCapavilitiesVMsg;
    }
    
    private void createCapabilitiesMsgData( )
    {
        SupportedCapability featureCap = null;
        featureCap = new SupportedCapability(FEATURE_SEARCH_BYTES, 
            FEATURE_SEARCH_VERSION);
        capabilitiesSet.add( featureCap );
        
        try 
        {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            IOUtil.serializeShortLE( (short)capabilitiesSet.size(), outStream );
            
            for( SupportedCapability cap : capabilitiesSet )
            {
                // we currently don't support sending 4 byte caps of version 1
                cap.serialize( outStream );
            }
            // we currently don't support sending 4 byte caps of version 1            
            byte[] data = outStream.toByteArray();
            setVenderMsgData( data );
        }
        catch (IOException exp)
        {
            // should never happen
            //NLogger.error( CapabilitiesVMsg.class, exp, exp );
        }
    }
    
    /** 
     * Holds single capability.
     */  
    private static class SupportedCapability 
    {
        private byte[] name;
        private int version;
        private int hashCode = -1;
        
        public SupportedCapability(byte[] name, int version) 
        {
            this.name = name;
            this.version = version;
        }

        /**
         * Constructs a new SupportedMessageBlock with data from the 
         * InputStream.  If not enough data is available,
         * throws BadPacketException.
         */
        public SupportedCapability( byte[] itemBytes )
            throws InvalidMessageException
        {
            if ( itemBytes.length < 6 || itemBytes.length > 8 )
            {
                throw new InvalidMessageException( "Invalid capability data: " + itemBytes.length );
            }
            
            // first 4 bytes are capability name
            name = new byte[4];
            System.arraycopy(itemBytes, 0, name, 0, 4);
            version = IOUtil.unsignedShort2Int( IOUtil.deserializeShortLE(
                itemBytes, 4 ) );
        }
        
        public void serialize( OutputStream outStream ) 
            throws IOException
        {
            outStream.write( name );
            IOUtil.serializeShortLE((short)version, outStream);
        }
        
        public byte[] getName()
        {
            return name;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof SupportedCapability) 
            {
                SupportedCapability cap = (SupportedCapability) obj;
                return version == cap.version && Arrays.equals( name, cap.name );
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            if ( hashCode == -1 )
            {
                int code = 37*version;
                for (int i = 0; i < name.length; i++)
                {
                    code += 37*name[i];
                }
                hashCode = code;
            }
            return hashCode;
        }
        
        @Override
        public String toString() 
        {
            return new String(name) + "/" + version;
        }
    }
}


