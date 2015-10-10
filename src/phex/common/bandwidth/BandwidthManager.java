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
package phex.common.bandwidth;

import phex.prefs.core.BandwidthPrefs;

/**
 * Manages all bandwidth controllers.
 */
public class BandwidthManager
{
    private BandwidthController serventBandwidthController;
    private BandwidthController networkBandwidthController;
    private BandwidthController downloadBandwidthController;
    private BandwidthController uploadBandwidthController;
    
    public BandwidthManager()
    {
        serventBandwidthController = new BandwidthController( "ServentThrottle",
                BandwidthPrefs.MaxTotalBandwidth.get().intValue() );
        serventBandwidthController.activateShortTransferAvg(1000, 5);
        serventBandwidthController.activateLongTransferAvg(2000, 90);
            
        networkBandwidthController = new BandwidthController( "NetworkThrottle",
                BandwidthPrefs.MaxNetworkBandwidth.get().intValue(),
                serventBandwidthController );
        networkBandwidthController.activateShortTransferAvg(1000, 5);
        networkBandwidthController.activateLongTransferAvg(2000, 90);

        downloadBandwidthController = new BandwidthController( "DownloadThrottle",
                BandwidthPrefs.MaxDownloadBandwidth.get().intValue(),
                serventBandwidthController );
        downloadBandwidthController.activateShortTransferAvg(1000, 5);
        downloadBandwidthController.activateLongTransferAvg(2000, 90);
        
        uploadBandwidthController = new BandwidthController( "UploadThrottle", 
                BandwidthPrefs.MaxUploadBandwidth.get().intValue(),
                serventBandwidthController );
        uploadBandwidthController.activateShortTransferAvg(1000, 5);
        uploadBandwidthController.activateLongTransferAvg(2000, 90);
    }

    public void setDownloadBandwidth( int newDownloadBwInBytes )
    {
        BandwidthPrefs.MaxDownloadBandwidth.set( Integer.valueOf( newDownloadBwInBytes ) );
        downloadBandwidthController.setThrottlingRate(newDownloadBwInBytes);
    }

    public void setNetworkBandwidth(int newNetworkBwInBytes)
    {
        BandwidthPrefs.MaxNetworkBandwidth.set( Integer.valueOf( newNetworkBwInBytes ) );
        networkBandwidthController.setThrottlingRate(newNetworkBwInBytes);
    }
    
    public void setServentBandwidth(int newPhexBwInBytes)
    {
        BandwidthPrefs.MaxTotalBandwidth.set( Integer.valueOf( newPhexBwInBytes ) );
        serventBandwidthController.setThrottlingRate(newPhexBwInBytes);
    }

    public void setUploadBandwidth(int newUploadBwInBytes)
    {
        BandwidthPrefs.MaxUploadBandwidth.set( Integer.valueOf( newUploadBwInBytes ) );
        uploadBandwidthController.setThrottlingRate(newUploadBwInBytes);
    }

    public BandwidthController getServentBandwidthController()
    {
        return serventBandwidthController;
    }

    public BandwidthController getNetworkBandwidthController()
    {
        return networkBandwidthController;
    }

    public BandwidthController getDownloadBandwidthController()
    {
        return downloadBandwidthController;
    }

    public BandwidthController getUploadBandwidthController()
    {
        return uploadBandwidthController;
    }
}
