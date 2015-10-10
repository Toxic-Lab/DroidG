package phex.connection;

import phex.common.address.DestAddress;

public class ConnectionStatusEvent
{
    public enum Status
    {
        /**
         * The connection to the host failed.
         */
        CONNECTION_FAILED,
        
        /**
         * The handshake with the host failed to be 
         * processed.
         */
        HANDSHAKE_FAILED,
        
        /**
         * The handshake was successful but our or the
         * remote servent rejected the connection.
         */
        HANDSHAKE_REJECTED,
        
        /**
         * The handshake was successful.
         */
        SUCCESSFUL
    }
    
    private final DestAddress hostAddres;
    private final Status status;
    
    public ConnectionStatusEvent(final DestAddress hostAddres, final Status status)
    {
        super();
        this.hostAddres = hostAddres;
        this.status = status;
    }

    /**
     * @return the hostAddres
     */
    public DestAddress getHostAddres()
    {
        return hostAddres;
    }

    /**
     * @return the status
     */
    public Status getStatus()
    {
        return status;
    }
}
