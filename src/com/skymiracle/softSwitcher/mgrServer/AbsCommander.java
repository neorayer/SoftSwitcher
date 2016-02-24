package com.skymiracle.softSwitcher.mgrServer;

import com.skymiracle.server.tcpServer.cmdServer.Commander;
import com.skymiracle.server.tcpServer.cmdServer.ConnHandler;
import com.skymiracle.softSwitcher.tcpPipe.TcpPipeServer;
import com.skymiracle.softSwitcher.udpPipe.UdpPipeServer;

public abstract class AbsCommander extends Commander{

	protected MgrServer mgrServer;
	
	protected UdpPipeServer udpPipeServer;
	
	protected TcpPipeServer tcpPipeServer;
	
	public AbsCommander(ConnHandler connHandler) {
		super(connHandler);
		
		this.mgrServer = (MgrServer)getCmdServer();
		this.udpPipeServer = this.mgrServer.udpPipeServer;
		this.tcpPipeServer = this.mgrServer.tcpPipeServer;

	}

}
