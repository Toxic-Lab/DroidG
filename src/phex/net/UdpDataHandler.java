package phex.net;

import org.xsocket.IDataSource;

import phex.common.address.DestAddress;

public interface UdpDataHandler
{
    public void handleUdpData( IDataSource dataSource, DestAddress orgin );
}
