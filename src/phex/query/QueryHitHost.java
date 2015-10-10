/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2008 Phex Development Group
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
package phex.query;

import phex.common.address.DestAddress;
import phex.common.format.HostSpeedFormatUtils;
import phex.msg.GUID;
import phex.msg.InvalidMessageException;
import phex.msg.QueryResponseMsg;
import phex.servent.Servent;
import phex.utils.VendorCodes;

/**
 * This class holds the available informations of a QueryHit for a host.
 */
public class QueryHitHost
{
    /**
     * The unique identifier of the host.
     */
    private GUID hostGUID;

    /**
     * The speed of the host in Kbyte/s.
     */
    private int hostSpeed;
    private String cachedHostSpeedFormatted;

    /**
     * Defines if a push transfer is needed or not or unknown.
     */
    private QHDFlag pushNeededFlag;

    /**
     * Defines if a server is busy currently or unknown.
     */
    private QHDFlag serverBusyFlag;

    /**
     * Defines if a the server has already uploaded a file.
     */
    private QHDFlag hasUploadedFlag;

    /**
     * Defines if the upload speed of a server is measured.
     */
    private QHDFlag uploadSpeedFlag;

    /**
     * The vendor name.
     */
    private String vendor;

    /**
     * <p>States wether the servent supportes chat connections.</p>
     */
    private boolean isChatSupported;
    
    /**
     * <p>States wether the servent supportes browse hosts connections.</p>
     */
    private boolean isBrowseHostSupported;

    /**
     * The rating of the host. It is determined from the QHD informations.
     */
    private short hostRating;

    /**
     * The host address of the query hit host.
     */
    private DestAddress hostAddress;
    
    private DestAddress[] pushProxyAddresses;
    
    /**
     * Indicates that the host information is from a udp message.
     */
    private boolean isUdpHost;

    public QueryHitHost( GUID aHostGUID, DestAddress address, int aHostSpeed )
    {
        hostGUID = aHostGUID;
        hostAddress = address;
        hostSpeed =  aHostSpeed;
        cachedHostSpeedFormatted = null;
        setQHDFlags( QHDFlag.QHD_UNKNOWN_FLAG, QHDFlag.QHD_UNKNOWN_FLAG, 
            QHDFlag.QHD_UNKNOWN_FLAG, QHDFlag.QHD_UNKNOWN_FLAG );
        hostRating = -1;
    }

    /**
     * @return the isUdpHost
     */
    public boolean isUdpHost()
    {
        return isUdpHost;
    }

    /**
     * @param isUdpHost the isUdpHost to set
     */
    public void setUdpHost(boolean isUdpHost)
    {
        this.isUdpHost = isUdpHost;
    }

    /**
     * @return Returns the pushProxyAddresses.
     */
    public DestAddress[] getPushProxyAddresses()
    {
        return pushProxyAddresses;
    }
    
    /**
     * @param pushProxyAddresses The pushProxyAddresses to set.
     */
    public void setPushProxyAddresses(DestAddress[] pushProxyAddresses)
    {
        this.pushProxyAddresses = pushProxyAddresses;
    }
    
    /**
     * Sets the vendor code and stores it translated into the vendor name.
     */
    public void setVendorCode( String aVendorCode )
    {
        if ( aVendorCode != null )
        {
            vendor = VendorCodes.getVendorName( aVendorCode );
        }
    }

    /**
     * Returns the vendor name.
     */
    public String getVendor()
    {
        return vendor;
    }

    /**
     * <p>Sets whether the servent supports a chat connections.</p>
     */
    public void setChatSupported( boolean state )
    {
        isChatSupported = state;
    }

    /**
     * <p>States whether the servent supports chat connections.</p>
     *
     * @return true if a servent supports chat connections false otherwise.
     */
    public boolean isChatSupported( )
    {
        return isChatSupported;
    }
    
    /**
     * <p>Sets wether the servent supportes a browse host connections.</p>
     */
    public void setBrowseHostSupported( boolean state )
    {
        isBrowseHostSupported = state;
    }

    /**
     * <p>States wether the servent supportes browse host connections.</p>
     *
     * @return true if a servent supports browse hosts connections false otherwise.
     */
    public boolean isBrowseHostSupported( )
    {
        return isBrowseHostSupported;
    }

    /**
     * Returns the GUID of the host.
     */
    public GUID getHostGUID()
    {
        return hostGUID;
    }

    /**
     * @param guid
     */
    public void setHostGUID(GUID guid)
    {
        hostGUID = guid;
    }

    /**
     * Return the host speed as int value in kbyte/s
     *
     * @return the host speed
     */
    public int getHostSpeed()
    {
        return hostSpeed;
    }
    
    public String getFormattedHostSpeed()
    {
        if ( cachedHostSpeedFormatted == null )
        {
            cachedHostSpeedFormatted = HostSpeedFormatUtils.formatHostSpeed( hostSpeed );
        }
        return cachedHostSpeedFormatted;
    }
    
    /**
     * @param i
     */
    public void setHostSpeed( int speed )
    {
        hostSpeed = speed;
        cachedHostSpeedFormatted = null;
        calculateHostRating();
    }

    /**
     * Returns the host address.
     */
    public DestAddress getHostAddress()
    {
        return hostAddress;
    }

    /**
     * Stores the QHD flags
     */
    public void setQHDFlags( QHDFlag aPushNeededFlag, QHDFlag aServerBusyFlag,
        QHDFlag aHasUploadedFlag, QHDFlag aUploadSpeedFlag )
    {
        pushNeededFlag = aPushNeededFlag;
        serverBusyFlag = aServerBusyFlag;
        hasUploadedFlag = aHasUploadedFlag;
        uploadSpeedFlag = aUploadSpeedFlag;
    }

    /**
     * Returns if a push is needed for the host. If the flag is set to unknown
     * then false is returned. The downloader should try a push after failed
     * normal connection.
     */
    public boolean isPushNeeded()
    {
        return pushNeededFlag == QHDFlag.QHD_TRUE_FLAG;
    }

    /**
     * @return the pushNeededFlag
     */
    public QHDFlag getPushNeededFlag()
    {
        return pushNeededFlag;
    }

    /**
     * @return the serverBusyFlag
     */
    public QHDFlag getServerBusyFlag()
    {
        return serverBusyFlag;
    }
    
    /**
     * @return the hasUploadedFlag
     */
    public QHDFlag getHasUploadedFlag()
    {
        return hasUploadedFlag;
    }

    /**
     * @return the uploadSpeedFlag
     */
    public QHDFlag getUploadSpeedFlag()
    {
        return uploadSpeedFlag;
    }
    
    

    /**
     * Returns the from the QHD calculated rating.
     */
    public short getHostRating()
    {
        if ( hostRating == -1 )
        {
            calculateHostRating();
        }
        return hostRating;
    }

    /**
     * pushNeeded | serverBusy | I'm firewalled | useHasUploaded | Rating
     *         -1 |         -1 |                |              y |      6
     *         -1 |          0 |                |              y |      5
     *          0 |       0/-1 |             -1 |              y |      4
     *          1 |         -1 |             -1 |              y |      3
     *          0 |       0/-1 |              1 |              y |      3
     *          1 |          0 |             -1 |              y |      2
     *            |          1 |                |              n |      1
     *          1 |            |              1 |              n |      0
     *
     * If useHasUploaded is set to y in this chart then depending on the flag
     * the rating is raised.
     * hasUploaded == QHD_TRUE_FLAG -> Raiting + 2
     * hasUploaded == QHD_UNKNOWN_FLAG -> Raiting + 1
     * hasUploaded == QHD_FALSE_FLAG -> Raiting + 0
     */
    private void calculateHostRating()
    {
        // TODO use isPushRecommended information to rate.
        // A push is recommended if the pushNeededFlag is set or the host has a
        // private host address and we are in a lan.


        // remote host and local host is firewalled... download not possible
        Servent servent = Servent.getInstance();
        if (pushNeededFlag == QHDFlag.QHD_TRUE_FLAG
            && servent.isFirewalled() )
        {
            hostRating = 0;
            return;
        }
        // servent is busy
        if ( serverBusyFlag == QHDFlag.QHD_TRUE_FLAG )
        {
            hostRating = 1;
            return;
        }

        // complex ratings influenced by hasUploaded
        short tmpHostRating;
        // remote is firewalled i'm not firewalled
        if ( pushNeededFlag == QHDFlag.QHD_TRUE_FLAG )
        {
            if ( serverBusyFlag == QHDFlag.QHD_FALSE_FLAG )
            {
                tmpHostRating = 3;
            }
            else // serverBusyFlag == QHDFlag.QHD_UNKNOWN_FLAG
            {
                tmpHostRating = 2;
            }
        }
        else if ( pushNeededFlag == QHDFlag.QHD_FALSE_FLAG )
        {
            if ( serverBusyFlag == QHDFlag.QHD_FALSE_FLAG )
            {
                tmpHostRating = 6;
            }
            else // serverBusyFlag == QHDFlag.QHD_UNKNOWN_FLAG
            {
                tmpHostRating = 5;
            }
        }
        else // pushNeededFlag == QHDFlag.QHD_UNKNOWN_FLAG
        {// serverBusyFlag != QHDFlag.QHD_TRUE_FLAG
            if ( servent.isFirewalled() )
            {// I'm firewalled
                tmpHostRating = 3;
            }
            else
            {// I'm not firewalled
                tmpHostRating = 4;
            }
        }

        if ( hasUploadedFlag == QHDFlag.QHD_TRUE_FLAG )
        {
            tmpHostRating += 2;
        }
        else if ( hasUploadedFlag == QHDFlag.QHD_UNKNOWN_FLAG )
        {
            tmpHostRating += 1;
        }
        hostRating = tmpHostRating;
    }
    
    public static QueryHitHost createFrom( QueryResponseMsg sourceMsg ) 
        throws InvalidMessageException
    {
        QueryHitHost qhh = new QueryHitHost( sourceMsg.getRemoteServentID(), 
            sourceMsg.getDestAddress(), sourceMsg.getRemoteHostSpeed() );
        qhh.setQHDFlags( sourceMsg.getPushNeededFlag(), sourceMsg.getServerBusyFlag(),
            sourceMsg.getHasUploadedFlag(), sourceMsg.getUploadSpeedFlag() );
        qhh.setVendorCode( sourceMsg.getVendorCode() );
        qhh.setChatSupported( sourceMsg.isChatSupported() );
        qhh.setBrowseHostSupported( sourceMsg.isBrowseHostSupported() );
        qhh.setPushProxyAddresses( sourceMsg.getPushProxyAddresses() );
        qhh.setUdpHost( sourceMsg.isUdpMsg() );
        return qhh;
    }
}