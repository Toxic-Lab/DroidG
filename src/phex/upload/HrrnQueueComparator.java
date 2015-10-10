package phex.upload;

import java.util.Comparator;

import phex.prefs.core.BandwidthPrefs;

/**
 * Implements a HRRN Queue Comparator
 * http://en.wikipedia.org/wiki/Highest_response_ratio_next
 */
public class HrrnQueueComparator implements Comparator<UploadQueueState>
{
    private long baseTime;
    
    public HrrnQueueComparator( long baseTime )
    {
        this.baseTime = baseTime;
    }

    public int compare(UploadQueueState q1, UploadQueueState q2)
    {
        if ( q1 == q2 )
        {
            return 0;
        }
        
        double ratio1 = calcRatio( q1, baseTime );
        double ratio2 = calcRatio( q2, baseTime );
        
        if ( ratio1 > ratio2 )
        {
            return -1;
        }
        else if ( ratio1 < ratio2 )
        {
            return 1;
        }
        else
        {
            int timeDiff = (int)(q1.getFirstQueueTime() - q2.getFirstQueueTime());
            if ( timeDiff != 0 )
            {
                return timeDiff;
            }
            int indexDiff = q1.getUploadState().hashCode() - q2.getUploadState().hashCode();
            assert indexDiff != 0 : "UploadState hashCode matches.";
            return indexDiff;
        }
    }
    
    public static double calcRatio( UploadQueueState state, long baseTime )
    {
        long fileSize = state.getLastRequestedFile().getFileSize();
        float uploadSpeed = BandwidthPrefs.MaxUploadBandwidth.get().floatValue();
        double estimatedTimeSec = fileSize / uploadSpeed;
        double waitTimeSec = Math.max( (baseTime - state.getFirstQueueTime()) / 1000.0, 1);
        double ratio = 1.0 + Math.max( 1, waitTimeSec ) / estimatedTimeSec;
        return ratio;
    }
}
