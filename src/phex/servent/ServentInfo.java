package phex.servent;

import phex.common.GnutellaNetwork;
import phex.common.address.DestAddress;
import phex.event.PhexEventService;
import phex.msg.GUID;

public interface ServentInfo
{    
    /**
     * Returns true if this node is currently a ultrapeer, false otherwise.
     * This node is currently a ultrapeer if it is forced to be a ultrapeer or
     * has leaf connections.
     * @return true if the node is currently a ultrapeer, false otherwise.
     */
    public boolean isUltrapeer();

    /**
     * Returns true if the local servent is a shielded leaf node ( has a connection
     * to a ultrapeer as a leaf).
     */
    public boolean isShieldedLeafNode();

    /**
     * Returns true if this node is allowed to become a ultrapeer, false otherwise.
     * @return true if this node is allowed to become a ultrapeer, false otherwise.
     */
    public boolean isAbleToBecomeUltrapeer();

    /**
     * The method checks if we are able to go into leaf state. This is
     * necessary to react accordingly to the "X-UltrapeerNeeded: false" header.
     *
     * @return true if we are able to switch to Leaf state, false otherwise.
     */
    public boolean allowDowngradeToLeaf();

    /**
     * Indicates if this server is currently firewalled or assumed to be firewalled. 
     * To determine this the server is asked if it has received incoming 
     * connections yet (e.g. from TCPConnectBack)
     * @return true if it has connected incoming, false otherwise.
     */
    public boolean isFirewalled();
    
    /**
     * Indicates if this servent has reached its upload limit, all
     * upload slots are full.
     * @return true if the upload limit is reached, false otherwise.
     */
    public boolean isUploadLimitReached();

    /**
     * Returns the current local address. This will be the forced address
     * in case a forced address is set.
     * @return the current determined local address or the user set forced address.
     */
    public DestAddress getLocalAddress();

    /**
     * Returns the GUID of the servent.
     * @return the GUID of the servent.
     */
    public GUID getServentGuid();

    /**
     * Returns the current network.
     * @return the current network.
     */
    public GnutellaNetwork getGnutellaNetwork();

}
