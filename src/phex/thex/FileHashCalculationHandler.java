package phex.thex;

import phex.share.ShareFile;

public interface FileHashCalculationHandler
{
    /**
     * Queues a ShareFile for calculating THEX.
     * 
     * @param shareFile the share file to calculate the
     *        thex hash for.
     */
    public void queueThexCalculation( ShareFile shareFile );
    
    /**
     * Queues a ShareFile for calculating its URN.
     * 
     * @param shareFile the share file to calculate the
     *        urn hash for.
     */
    public void queueUrnCalculation( ShareFile shareFile );
}
