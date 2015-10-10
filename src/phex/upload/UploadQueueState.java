package phex.upload;

import phex.share.ShareFile;

public class UploadQueueState
{
    private final long firstQueueTime;
    private final UploadState uploadState;
    private ShareFile lastRequestedFile;

    public UploadQueueState( UploadState uploadState, ShareFile lastRequestedFile)
    {
        this.firstQueueTime = System.currentTimeMillis();
        this.uploadState = uploadState;
        this.lastRequestedFile = lastRequestedFile;
    }
    
    public void setLastRequestedFile( ShareFile shareFile )
    {
        this.lastRequestedFile = shareFile;
    }
    
    public ShareFile getLastRequestedFile()
    {
        return lastRequestedFile;
    }
    
    /**
     * @return the firstQueueTime
     */
    public long getFirstQueueTime()
    {
        return firstQueueTime;
    }

    /**
     * @return the uploadState
     */
    public UploadState getUploadState()
    {
        return uploadState;
    }

}