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
package phex.statistic;

public interface StatisticProviderConstants
{
    public static final String TOTAL_BANDWIDTH_PROVIDER = "TotalBandwidthProvider";
    public static final String NETWORK_BANDWIDTH_PROVIDER = "NetworkBandwidthProvider";
    public static final String DOWNLOAD_BANDWIDTH_PROVIDER = "DownloadBandwidthProvider";
    public static final String UPLOAD_BANDWIDTH_PROVIDER = "UploadBandwidthProvider";

    public static final String TOTALMSG_IN_PROVIDER = "TotalMsgInProvider";
    public static final String PINGMSG_IN_PROVIDER = "PingMsgInProvider";
    public static final String PONGMSG_IN_PROVIDER = "PongMsgInProvider";
    public static final String PUSHMSG_IN_PROVIDER = "PushMsgInProvider";
    public static final String QUERYMSG_IN_PROVIDER = "QueryMsgInProvider";
    public static final String QUERYHITMSG_IN_PROVIDER = "QueryHitMsgInProvider";

    public static final String TOTALMSG_OUT_PROVIDER = "TotalMsgOutProvider";
    public static final String PINGMSG_OUT_PROVIDER = "PingMsgOutProvider";
    public static final String PONGMSG_OUT_PROVIDER = "PongMsgOutProvider";
    public static final String PUSHMSG_OUT_PROVIDER = "PushMsgOutProvider";
    public static final String QUERYMSG_OUT_PROVIDER = "QueryMsgOutProvider";
    public static final String QUERYHITMSG_OUT_PROVIDER = "QueryHitMsgOutProvider";

    public static final String DROPEDMSG_TOTAL_PROVIDER = "DropedMsgTotalProvider";
    public static final String DROPEDMSG_IN_PROVIDER = "DropedMsgInProvider";
    public static final String DROPEDMSG_OUT_PROVIDER = "DropedMsgOutProvider";

    public static final String UPTIME_PROVIDER = "UptimeProvider";
    public static final String DAILY_UPTIME_PROVIDER = "DailyUptimeProvider";

    public static final String SESSION_UPLOAD_COUNT_PROVIDER = "SessionUploadCountProvider";
    public static final String TOTAL_UPLOAD_COUNT_PROVIDER = "TotalUploadCountProvider";
    public static final String SESSION_DOWNLOAD_COUNT_PROVIDER = "SessionDownloadCountProvider";
    public static final String TOTAL_DOWNLOAD_COUNT_PROVIDER = "TotalDownloadCountProvider";
    
    public static final String PUSH_DOWNLOAD_ATTEMPTS_PROVIDER = "PushDownloadAttemptsProvider";
    public static final String PUSH_DOWNLOAD_SUCESS_PROVIDER = "PushDownloadSucessProvider";
    public static final String PUSH_DOWNLOAD_FAILURE_PROVIDER = "PushDownloadFailureProvider";
    
    public static final String PUSH_DLDPUSHPROXY_ATTEMPTS_PROVIDER = "PushDldPushProxyAttemptsProvider";
    public static final String PUSH_DLDPUSHPROXY_SUCESS_PROVIDER = "PushDldPushProxySuccessProvider";
    
    public static final String PUSH_UPLOAD_ATTEMPTS_PROVIDER = "PushUploadAttemptsProvider";
    public static final String PUSH_UPLOAD_SUCESS_PROVIDER = "PushUploadSucessProvider";
    public static final String PUSH_UPLOAD_FAILURE_PROVIDER = "PushUploadFailureProvider";
    
    public static final String HORIZON_HOST_COUNT_PROVIDER = "HorizonHostCountProvider";
    public static final String HORIZON_FILE_COUNT_PROVIDER = "HorizonFileCountProvider";
    public static final String HORIZON_FILE_SIZE_PROVIDER = "HorizonFileSizeProvider";
    

}