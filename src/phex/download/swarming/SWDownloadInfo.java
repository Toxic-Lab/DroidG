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
package phex.download.swarming;

import phex.common.format.TimeFormatUtils;
import phex.download.swarming.SWDownloadCandidate.CandidateStatus;
import phex.utils.Localizer;

public final class SWDownloadInfo implements SWDownloadConstants
{
    // dont allow to create instances
    private SWDownloadInfo()
    {
    }

    /**
     * Returns a localized string for the given status of a download file.
     */
    public static String getDownloadFileStatusString( int status )
    {
        switch( status )
        {
            case STATUS_FILE_WAITING:
                return Localizer.getString( STATUS_FILE_WAITING_KEY );
            case STATUS_FILE_DOWNLOADING:
                return Localizer.getString( STATUS_FILE_DOWNLOADING_KEY );
            case STATUS_FILE_COMPLETED:
            case STATUS_FILE_COMPLETED_MOVED:
                return Localizer.getString( STATUS_FILE_COMPLETED_KEY );
            case STATUS_FILE_STOPPED:
                return Localizer.getString( STATUS_FILE_STOPPED_KEY );
            case STATUS_FILE_QUEUED:
                return Localizer.getString( STATUS_FILE_QUEUED_KEY );
            default:
                Object[] arguments = new Object[1];
                arguments[0] = Integer.valueOf( status );
                return Localizer.getFormatedString( STATUS_UNRECOGNIZED_KEY,
                    arguments );
        }
    }
    
    public static String getDownloadFileStatusString2( int status )
    {
        switch( status )
        {
            case STATUS_FILE_WAITING:
                return "Waiting";
            case STATUS_FILE_DOWNLOADING:
                return "Downloading";
            case STATUS_FILE_COMPLETED:
            case STATUS_FILE_COMPLETED_MOVED:
                return "Completed";
            case STATUS_FILE_STOPPED:
                return "Stopped";
            case STATUS_FILE_QUEUED:
                return "Queuing";
            default:
                Object[] arguments = new Object[1];
                arguments[0] = Integer.valueOf( status );
                return Localizer.getFormatedString( "Unrecognized %d",
                    arguments );
        }
    }
    

    /**
     * Returns a localized string for the given status of a download candidate.
     */
    public static String getDownloadCandidateStatusString(
        SWDownloadCandidate candidate )
    {
        Object[] arguments;
        CandidateStatus status = candidate.getStatus();
        switch( status )
        {
            case IGNORED:
                return Localizer.getString( STATUS_CANDIDATE_IGNORED_KEY )
                    + " (" + Localizer.getString( candidate.getStatusReason() ) + ").";
            case BAD:
                arguments = new Object[2];
                arguments[0] = Long.valueOf( candidate.getStatusTimeLeft() / 1000 );
                arguments[1] = TimeFormatUtils.convertSecondsToTime(
                    (int)(candidate.getStatusTimeLeft() / 1000 ) );
                return Localizer.getFormatedString( STATUS_CANDIDATE_BAD_KEY,
                    arguments );
            case WAITING:
                return Localizer.getString( STATUS_CANDIDATE_WAITING_KEY );
            case BUSY:
                arguments = new Object[2];
                arguments[0] = Long.valueOf( candidate.getStatusTimeLeft() / 1000 );
                arguments[1] = TimeFormatUtils.convertSecondsToTime(
                    (int)(candidate.getStatusTimeLeft() / 1000 ) );
                return Localizer.getFormatedString( STATUS_CANDIDATE_BUSY_KEY,
                    arguments );
            case CONNECTING:
                arguments = new Object[1];
                arguments[0] = Long.valueOf( candidate.getStatusTimeLeft() / 1000 );
                return Localizer.getFormatedString( STATUS_CANDIDATE_CONNECTING_KEY,
                    arguments );
            case ALLOCATING_SEGMENT:
                return Localizer.getString( STATUS_CANDIDATE_ALLOCATING_SEGMENT_KEY );
            case RANGE_UNAVAILABLE:
                arguments = new Object[1];
                arguments[0] = TimeFormatUtils.convertSecondsToTime(
                    (int)(candidate.getStatusTimeLeft() / 1000) );
                return Localizer.getFormatedString( STATUS_CANDIDATE_RANGE_UNAVAILABLE_KEY,
                    arguments );
            case REMOTLY_QUEUED:
                arguments = new Object[3];
                arguments[0] = candidate.getXQueueParameters().getPosition();
                arguments[1] = TimeFormatUtils.convertSecondsToTime(
                    (int)(candidate.getStatusTimeLeft() / 1000 ) );
                return Localizer.getFormatedString( STATUS_CANDIDATE_REMOTLY_QUEUED_KEY,
                    arguments );
            case CONNECTION_FAILED:
                arguments = new Object[3];
                arguments[0] = Integer.valueOf( candidate.getFailedConnectionTries() );
                arguments[1] = Long.valueOf( candidate.getStatusTimeLeft() / 1000 );
                arguments[2] = TimeFormatUtils.convertSecondsToTime(
                    (int)(candidate.getStatusTimeLeft() / 1000 ) );
                return Localizer.getFormatedString( STATUS_CANDIDATE_CONNECTION_FAILED_KEY,
                    arguments );
            case PUSH_REQUEST:
                arguments = new Object[1];
                arguments[0] = Long.valueOf( candidate.getStatusTimeLeft() / 1000 );
                return Localizer.getFormatedString( STATUS_CANDIDATE_PUSH_REQUEST_KEY,
                    arguments );
            case REQUESTING:
                return Localizer.getFormatedString( STATUS_CANDIDATE_REQUESTING_KEY,
                    (Object[])null );
            case DOWNLOADING:
                return Localizer.getFormatedString( STATUS_CANDIDATE_DOWNLOADING_KEY,
                    (Object[])null );
            default:
                arguments = new Object[1];
                arguments[0] = status.name();
                return Localizer.getFormatedString( STATUS_UNRECOGNIZED_KEY,
                    arguments );
        }
    }
    
    public static String getDownloadCandidateStatusString2(
            SWDownloadCandidate candidate )
        {
            CandidateStatus status = candidate.getStatus();
            switch( status )
            {
                case IGNORED:
                    return "Ignored candidate";
                case BAD:
                    return "Candidate not available";
                case WAITING:
                    return "Waiting";
                case BUSY:
                    return "Host busy";
                case CONNECTING:
                	return "Connecting";
                case ALLOCATING_SEGMENT:
                    return "Waiting for segments";
                case RANGE_UNAVAILABLE:
                    return "Range unavailable";
                case REMOTLY_QUEUED:
                    return "Queued on position";
                case CONNECTION_FAILED:
                    return "Connection failed";
                case PUSH_REQUEST:
                    return "Push request";
                case REQUESTING:
                    return "Requesting";
                case DOWNLOADING:
                    return "Downloading";
                default:
                    return "Unrecognized Status";
            }
        }
    
}