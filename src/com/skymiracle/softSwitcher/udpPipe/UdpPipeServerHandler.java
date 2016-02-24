package com.skymiracle.softSwitcher.udpPipe;

import java.io.IOException;
import java.net.DatagramPacket;

import com.skymiracle.server.udpServer.UdpRecvHandler;
import com.skymiracle.util.ByteUtils;

public class UdpPipeServerHandler extends UdpRecvHandler {

	private UdpPipeServer udpPipeServer;

	// private static byte[] CMD_SPEED_BS = {'s', 'p', 'e', 'd'};

	@Override
	public void handleDataRecv() throws IOException {
		udpPipeServer = (UdpPipeServer) udpServer;

		int len = dPacket.getLength();
		byte[] bs = dPacket.getData();

		if (len == 15) {
			if (bs[0] == 's' && bs[1] == 'p' && bs[2] == 'e' && bs[3] == 'd') {
				DatagramPacket respDp = new DatagramPacket(bs, 0, len, dPacket
						.getSocketAddress());
				dSocket.send(respDp);
				return;
			}
		}

		/*
		 * UDP打洞数据包，格式如下： 3 个字节：{'d', 'i', 'g'} 4 个字节：自身的虚拟ip地址，网络格式 2
		 * 个字节：自身监听udp端口号，网络格式
		 */
		if (len == 9) {
			if (bs[0] == 'd' && bs[1] == 'i' && bs[2] == 'g') {
				byte[] ipbs = new byte[4];
				ByteUtils.memcpy(ipbs, 0, bs, 3, 4);
				int port = ByteUtils.bs2ushort(bs, 7);

				udpPipeServer.regTunnel(ipbs, port, dPacket.getSocketAddress());
				return;
			}else if (bs[0] == 'u' && bs[1] == 'd' && bs[2] == 'g') {
				byte[] ipbs = new byte[4];
				ByteUtils.memcpy(ipbs, 0, bs, 3, 4);
				int port = ByteUtils.bs2ushort(bs, 7);

				udpPipeServer.unRegTunnel(ipbs, port, dPacket.getSocketAddress());
				return;
			}
		}

		byte[] destIpbs = new byte[4];
		System.arraycopy(bs, 4, destIpbs, 0, 4);
		int srcPort = ByteUtils.bs2ushort(bs, 8);
		int destPort = ByteUtils.bs2ushort(bs, 10);

		udpPipeServer.regTunnel(bs, srcPort, dPacket.getSocketAddress());

		udpPipeServer.switchDacket(bs, srcPort, destIpbs, destPort, bs, len);
	}
}
