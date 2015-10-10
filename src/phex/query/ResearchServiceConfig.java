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
 */
package phex.query;



public class ResearchServiceConfig
{
    //private static final int DEFAULT_MIN_REMOTE_CANDIDATES_COUNT = 5;
    private static final long DEFAULT_RESEARCH_TIMEOUT = 2 * 60 * 1000;

    /**
     * The interval is the min interval of a research and can be multiplied x times
     * to create bigger penaltys.
     */
    private static final long DEFAULT_RESEARCH_INTERVAL_MILLIS = 10 * 60 * 1000;

    /**
     * The penalty is a short extra time that can be added x time to the interval.
     */
    private static final long DEFAULT_RESEARCH_TIME_PENALTY = 3 * 60 * 1000;
    private static final long DEFAULT_MAX_RESEARCH_INTERVAL_MILLIS = 2 * 60 * 60 * 1000;
    private static final int DEFAULT_CANDIDATE_INTERVAL_THRESHOLD = 2;

    private long maxResearchIntervalMillis;
    //private int minRemoteCandidatesCount;
    private long researchIntervalMillis;
    private long researchTimeout;
    private int candidatesIntervalThreshold;
    private long researchTimePenalty;
    private boolean isResearchEnabled;

    public ResearchServiceConfig()
    {
        //loadXMLResearchServiceConfig();
        ensureDefaults();

        // intentionally DISABLE automatic research!!
        // This is necessary to maintain a clean network. The code here can be
        // dropped here or must be very very very conservative...
        isResearchEnabled = false;
    }

    /**
     * The minimum candidates. If lower the research starts.
     */
    //public int getMinRemoteCandidatesCount()
    //{
    //    return minRemoteCandidatesCount;
    //}

    /**
     * The interval between two research and the offset after every failed
     * research.
     */
    public long getResearchIntervalMillis()
    {
        return researchIntervalMillis;
    }

    /**
     * The maximum interval between two researchs.
     */
    public long getMaxResearchIntervalMillis()
    {
        return maxResearchIntervalMillis;
    }

    /**
     * Returns the number of candidates after which a the research interval
     * will be multiplied by the number of candidates over this threshold.
     * TODO not integrated in XML yet. But not necessary as long as we dont have
     * it.
     */
    public int getCandidatesIntervalThreshold()
    {
        return candidatesIntervalThreshold;
    }

    /**
     * For shorter extra time to be added on top of the interval.
     * TODO not integrated in XML yet. But not necessary as long as we dont have
     * it.
     */
    public long getResearchTimePenalty()
    {
        return researchTimePenalty;
    }

    /**
     * The time after which a research times out.
     */
    public long getResearchTimeout()
    {
        return researchTimeout;
    }

    public boolean isResearchEnabled()
    {
        return isResearchEnabled;
    }

    /////////////////////////// XML File stuff ///////////////////////////////

    /*private void loadXMLResearchServiceConfig()
    {
        File file = new File( ServiceManager.getXMLResearchServiceFilename() );
        if ( !file.exists() )
        {
            return;
        }
        Document doc = XMLBuilder.loadFromFile( file );
        XMLPhex xphex = XMLPhexXMLCodec.decodeXML( doc.getDocumentElement() );
        XMLResearchServiceConfig config = xphex.getResearchServiceConfig();

        maxResearchIntervalMillis = config.getMaxResearchIntervalMillis();
        //minRemoteCandidatesCount = config.getMinRemoteCandidatesCount();
        researchIntervalMillis = config.getResearchIntervalMillis();
        researchTimeout = config.getResearchTimeout();
        isResearchEnabled = config.getResearchEnabled();
    }*/

    private void ensureDefaults()
    {
        if ( maxResearchIntervalMillis == 0 )
        {
            maxResearchIntervalMillis = DEFAULT_MAX_RESEARCH_INTERVAL_MILLIS;
        }
        //if ( minRemoteCandidatesCount == 0 )
        //{
        //    minRemoteCandidatesCount = DEFAULT_MIN_REMOTE_CANDIDATES_COUNT;
        //}
        if ( researchIntervalMillis == 0 )
        {
            researchIntervalMillis = DEFAULT_RESEARCH_INTERVAL_MILLIS;
        }
        if ( candidatesIntervalThreshold == 0 )
        {
            candidatesIntervalThreshold = DEFAULT_CANDIDATE_INTERVAL_THRESHOLD;
        }
        if ( researchTimeout == 0 )
        {
            researchTimeout = DEFAULT_RESEARCH_TIMEOUT;
        }
        if ( researchTimePenalty == 0 )
        {
            researchTimePenalty = DEFAULT_RESEARCH_TIME_PENALTY;
        }
    }
}