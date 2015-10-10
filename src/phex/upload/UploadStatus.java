package phex.upload;

public enum UploadStatus
{
    /**
     * The status of a just incomming upload request before it is 
     * processed further.
     */
    ACCEPTING_REQUEST,
    
    /**
     * The status of a request in the handshake phase.
     */
    HANDSHAKE,
    
    /**
     * The status of a upload indicating that a upload is queued.
     */
    QUEUED,
    
    /**
     * The status of a upload indicating that a thex upload is in progress.
     */
    UPLOADING_THEX,
    
    /**
     * The status of a upload indicating that a thex upload is in progress.
     */
    UPLOADING_DATA,
    
    /**
     * The status of a upload indicating that a upload is completed.
     */
    COMPLETED,
    
    /**
     * The status of a upload indicating that a upload is aborted.
     */
    ABORTED
}
