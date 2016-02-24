package com.skymiracle.softSwitcher.mgrServer;

import com.skymiracle.server.tcpServer.cmdServer.CmdServer;
import com.skymiracle.softSwitcher.tcpPipe.TcpPipeServer;
import com.skymiracle.softSwitcher.udpPipe.UdpPipeServer;

public class MgrServer extends CmdServer {

	protected UdpPipeServer udpPipeServer;

	protected TcpPipeServer tcpPipeServer;

	public MgrServer(UdpPipeServer udpPipeServer, TcpPipeServer tcpPipeServer, int mgrPort)
			throws Exception {
		super("PipeMgrServer", mgrPort);

		this.udpPipeServer = udpPipeServer;
		this.tcpPipeServer = tcpPipeServer;

		setShortConn(false);

		addQuitCmd("quit");
		addQuitCmd("exit");
		addCommander(RoomsCommander.class);
		addCommander(LogCommander.class);
		addCommander(GcCommander.class);
		addCommander(TcpPipeCommander.class);

	}

}
