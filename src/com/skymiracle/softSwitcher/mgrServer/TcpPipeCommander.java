package com.skymiracle.softSwitcher.mgrServer;

import java.net.SocketAddress;
import java.util.Map;

import com.skymiracle.server.tcpServer.cmdServer.ConnHandler;
import com.skymiracle.softSwitcher.tcpPipe.TcpPipeServer.SocketInfo;
import com.skymiracle.util.ByteUtils;

public class TcpPipeCommander extends AbsCommander {

	public TcpPipeCommander(ConnHandler connHandler) {
		super(connHandler);
	}

	@Override
	public byte[] doCmd(String head, String tail) throws Exception {
		Map<Integer, SocketInfo> clientSocketMap = tcpPipeServer.clientSocketMap;

		StringBuffer sb = new StringBuffer();
		for (SocketInfo si: clientSocketMap.values()) {
			sb.append("" + si.id);
			sb.append("\t").append(ByteUtils.bs2ip(si.vip, 0));
			sb.append("\t").append(ByteUtils.bs2ip(si.dstVip, 0));
			
			sb.append("\r\n");
		}
		sb.append("-----------------------------------\r\n");
		sb.append("Total: " + clientSocketMap.size() + "\r\n");
		sb.append('.');
		return getBytesCRLF(sb.toString());
		
	}

}
