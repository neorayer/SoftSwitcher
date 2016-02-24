package com.skymiracle.softSwitcher;

import com.skymiracle.softSwitcher.mgrServer.MgrServer;
import com.skymiracle.logger.Logger;
import com.skymiracle.softSwitcher.tcpPipe.TcpPipeServer;
import com.skymiracle.softSwitcher.udpPipe.UdpPipeServer;

public class Main {

	public static void main(String[] args) throws Exception {

		int udpPort = 8000;
		int tcpPort = 8000;
		int mgrPort = 9000;
		switch (args.length) {
		case 1:
			udpPort = Integer.parseInt(args[0]);
			tcpPort = udpPort;
			mgrPort = udpPort + 1000;
			break;
		case 2:
			udpPort = Integer.parseInt(args[0]);
			tcpPort = Integer.parseInt(args[1]);
			mgrPort = udpPort + 1000;
			break;
		case 3:
			udpPort = Integer.parseInt(args[0]);
			tcpPort = Integer.parseInt(args[1]);
			mgrPort = Integer.parseInt(args[2]);
		default:
			break;
		}
	
		Logger.setLevel(Logger.LEVEL_DETAIL);
		Logger.setTarget(Logger.TARGET_SIMPLEFILE);
		Logger.setLogFilePath("/gf/log/ss-" + mgrPort + ".log");

		UdpPipeServer ups = new UdpPipeServer(udpPort);
		TcpPipeServer tps = new TcpPipeServer(tcpPort);
		ups.start();
		tps.start();
		
		MgrServer mgrServer = new MgrServer(ups, tps, mgrPort);
		mgrServer.start();
		
	}

}
