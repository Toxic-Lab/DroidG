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
package phex.prefs.core;

import java.io.File;

import phex.prefs.api.PreferencesFactory;
import phex.prefs.api.Setting;
import phex.utils.SystemProperties;

public class DownloadPrefs extends PhexCorePrefs
{   
    /**
     * The destination directory of finished downloads.
     */
    public static final Setting<String> DestinationDirectory;
    
    /**
     * The directory where incomplete files are stored during download.
     */
    public static final Setting<String> IncompleteDirectory;
    
    /**
     * The max number of parallel workers per download file.
     */
    public static final Setting<Integer> MaxWorkerPerDownload;

    /**
     * The max number of total parallel workers for all download files.
     */
    public static final Setting<Integer> MaxTotalDownloadWorker;
    
    /**
     * The max number of bytes to buffer per download file. When the buffer is 
     * exceeded the complete buffered data is written to disk.
     * 
     * NOTE: Changing this value at runtime can cause problems in buffer handling!
     */
    public static final Setting<Integer> MaxWriteBufferPerDownload;

    /**
     * The max number of bytes to buffer for all download files. When the buffer
     * is exceeded the complete buffered data is written to disk.
     * 
     * NOTE: Changing this value at runtime can cause problems in buffer handling!
     */
    public static final Setting<Integer> MaxTotalDownloadWriteBuffer;

    /**
     * The maximum number of downloads that are allowed per IP.
     * Changing this value is not recommended since most, if not all servents
     * only accept one upload per IP.
     */
    public static final Setting<Integer> MaxDownloadsPerIP;
    
    /**
     * The segment size initially requested on a download connection. It's used 
     * for the first transfer attempt when no transfer speed is yet known.
     * Afterwards the segment size is adjusted for each individual candidate to 
     * approximatly meet the 'SegmentTransferTargetTime'.
     */
    public static final Setting<Integer> SegmentInitialSize;
    
    /**
     * Used to adjust the segment size for an individual candidate to 
     * approximatly meet the SegmentTransferTargetTime depending on the 
     * candidates transfer speed.
     * This is done after the first segment is transfered, sized according to
     * the SegmentInitialSize configuration. 
     */
    public static final Setting<Integer> SegmentTransferTargetTime;
    
    /**
     * Do not allow segments to be larger than this value, regardless of the 
     * previous segment's download rate.
     */
    public static final Setting<Integer> SegmentMaximumSize;
    
    public static final Setting<Integer> SegmentMultiple;
    
    /**
     * If a segment is transferred at less than this speed (b/sec), refuse further downloads
     * from this candidate
     */
    public static final Setting<Integer> CandidateMinAllowedTransferRate;
    
    /**
     * The LogBuffer size used for download candidates logging.
     */
    public static final Setting<Integer> CandidateLogBufferSize;
    
    /**
     * The timeout of a push request in millis.
     */
    public static final Setting<Integer> PushRequestTimeout;
    
    /**
     * Indicates if completed download should be automatically removed.
     */
    public static final Setting<Boolean> AutoRemoveCompleted;
    
    /**
     * Indicates if downloaded magma files should be parsed and its referenced 
     * sources further downloaded.
     */
    public static final Setting<Boolean> AutoReadoutMagmaFiles;
    
    /**
     * Indicates if downloaded metalink files should be parsed and its 
     * reference sources further downloaded.
     */
    public static final Setting<Boolean> AutoReadoutMetalinkFiles;
    
    /**
     * Indicates if downloaded rss files should be parsed and its referenced 
     * sources further downloaded.
     */
    public static final Setting<Boolean> AutoReadoutRSSFiles;
    
    static
    {
        File defaultIncDir = new File( SystemProperties.getPhexDownloadsRoot(), "incomplete" );
        File defaultDownDir = new File( SystemProperties.getPhexDownloadsRoot(), "download" );

        DestinationDirectory = PreferencesFactory.createStringSetting(
            "Download.DestinationDirectory", defaultDownDir.getAbsolutePath(), instance );
        IncompleteDirectory = PreferencesFactory.createStringSetting(
            "Download.IncompleteDirectory", defaultIncDir.getAbsolutePath(), instance );
        MaxWorkerPerDownload = PreferencesFactory.createIntRangeSetting( 
            "Download.MaxWorkerPerDownload", 12, 1, 99, instance );
        MaxTotalDownloadWorker = PreferencesFactory.createIntRangeSetting( 
            "Download.MaxTotalDownloadWorker", 30, 1, 99, instance );
        MaxWriteBufferPerDownload = PreferencesFactory.createIntRangeSetting( 
            "Download.MaxWriteBufferPerDownload", 256*1024, 0, Integer.MAX_VALUE, instance );
        MaxTotalDownloadWriteBuffer = PreferencesFactory.createIntRangeSetting( 
            "Download.MaxTotalDownloadWriteBuffer", 1024*1024, 0, Integer.MAX_VALUE, instance );
        MaxDownloadsPerIP = PreferencesFactory.createIntRangeSetting( 
            "Download.MaxDownloadsPerIP", 1, 1, 99, instance );
        SegmentInitialSize = PreferencesFactory.createIntRangeSetting( 
            "Download.SegmentInitialSize", 16*1024, 1024, 10*1024*1024, instance );
        SegmentTransferTargetTime = PreferencesFactory.createIntRangeSetting( 
            "Download.SegmentTransferTargetTime", 90, 15, 999, instance );
        SegmentMaximumSize = PreferencesFactory.createIntSetting( 
            "Download.SegmentMaximumSize", 10*1024*1024, instance );
        SegmentMultiple = PreferencesFactory.createIntSetting( 
            "Download.SegmentMultiple", 4096, instance );
        CandidateMinAllowedTransferRate = PreferencesFactory.createIntRangeSetting( 
            "Download.CandidateMinAllowedTransferRate", 1, 1, 100*1024, instance );
        CandidateLogBufferSize = PreferencesFactory.createIntSetting( 
            "Download.CandidateLogBufferSize", 0, instance );
        PushRequestTimeout = PreferencesFactory.createIntSetting(
            "Download.PushRequestTimeout", 30 * 1000, instance );
        AutoRemoveCompleted = PreferencesFactory.createBoolSetting(
            "Download.AutoRemoveCompleted", false, instance );
        AutoReadoutMagmaFiles = PreferencesFactory.createBoolSetting(
            "Download.AutoReadoutMagmaFiles", true, instance );
        AutoReadoutMetalinkFiles = PreferencesFactory.createBoolSetting(
            "Download.AutoReadoutMetalinkFiles", true, instance );
        AutoReadoutRSSFiles = PreferencesFactory.createBoolSetting(
            "Download.AutoReadoutRSSFiles", true, instance );
    }
}
