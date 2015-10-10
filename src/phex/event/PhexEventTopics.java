/*
 *  PHEX - The pure-java Gnutella-servent.
 *  Copyright (C) 2001 - 2009 Phex Development Group
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
package phex.event;

/**
 * Holds constants for Phex event topics.
 * All members are immutable.
 */
public final class PhexEventTopics
{
    public static final String Net_Favorites = "phex:net/favorites";
    public static final String Net_Hosts = "phex:net/hosts";
    public static final String Net_ConnectionStatus = "phex:net/connectionStatus";
    
    public static final String Host_Disconnect = "phex:host/disconnect";
    
    public static final String Download_File = "phex:download/file";
    public static final String Download_File_Completed = "phex:download/file/completed";
    public static final String Download_Candidate = "phex:download/candidate";
    public static final String Download_Candidate_Status = "phex:download/candidate/status";
    
    public static final String Upload_State = "phex:upload/state";
    public static final String Share_Update = "phex:share/update";
     
    public static final String Incoming_Uri = "phex:incoming/uri";
    public static final String Incoming_Magma = "phex:incoming/magma";
    public static final String Incoming_Rss = "phex:incoming/rss";
    
    public static final String Search_Update = "phex:search/update";
    public static final String Search_Data = "phex:search/data";
    public static final String Search_Monitor_Results = "phex:search/monitor/results";
    
    public static final String Query_Monitor = "phex:query/monitor";
    
    public static final String Chat_Update = "phex:chat/update";
    
    public static final String Security_Rule = "phex:security/rule";
    
    public static final String Servent_GnutellaNetwork = "phex:servent/gnutellaNetwork";
    public static final String Servent_OnlineStatus = "phex:servent/onlineStatus";
    public static final String Servent_LocalAddress = "phex:servent/localAddress";
    
    private PhexEventTopics()
    {
        throw new AssertionError();
    }
}
