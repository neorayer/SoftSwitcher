package com.skymiracle.softSwitcher.mgrServer;

import java.net.SocketAddress;
import java.util.Map;

import com.skymiracle.server.tcpServer.cmdServer.ConnHandler;
import com.skymiracle.util.ByteUtils;

public class RoomsCommander extends AbsCommander {

	public RoomsCommander(ConnHandler connHandler) {
		super(connHandler);
	}

	@Override
	public byte[] doCmd(String head, String tail) throws Exception {
		Map<Integer, Map<Integer, SocketAddress>> map = udpPipeServer
				.getIpPortTunnelMap();
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<Integer, Map<Integer, SocketAddress>> entry : map
				.entrySet()) {
			int ipi = entry.getKey();
			sb.append(ByteUtils.uint2ip(ipi)).append("\r\n");
			
			Map<Integer, SocketAddress> saMap = entry.getValue();
			for (Map.Entry<Integer, SocketAddress> saEntry : saMap.entrySet()) {
				int port = saEntry.getKey();
				SocketAddress saAddr = saEntry.getValue();
				
				sb.append("\t:").append(port);
				sb.append(" - ").append(saAddr.toString());
				sb.append("\r\n");
			}
		}
		sb.append('.');
		return getBytesCRLF(sb.toString());
		
	}

}
