package com.skymiracle.softSwitcher.udpPipe;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.skymiracle.logger.Logger;
import com.skymiracle.server.udpServer.UdpServer;
import com.skymiracle.util.ByteUtils;

public class UdpPipeServer extends UdpServer {
	class VirtualAddr {
		public int ipi;
		public int port;

		public VirtualAddr(int ipi, int port) {
			this.ipi = ipi;
			this.port = port;
		}
	}

	public PacLogger pacLogger = new PacLogger();

	private Map<Integer, Map<Integer, SocketAddress>> ipPortTunnelMap = new ConcurrentHashMap<Integer, Map<Integer, SocketAddress>>();

	/* <SockKey, ipi> */
//	private Map<String, VirtualAddr> tunnelVirtualAddrMap = new ConcurrentHashMap<String, VirtualAddr>();

	// private Map<String, Integer> userVipMap = new HashMap<String, Integer>();

	public UdpPipeServer(int port) throws Exception {
		super("UdpPipeServer", port, UdpPipeServerHandler.class);

	}

	public void switchDacket(byte[] srcIpbs, int srcPort, byte[] destIpbs,
			int destPort, byte[] bs, int len) throws IOException {
		if (ByteUtils.isBroadCastIp(destIpbs)) {
			broadcastSend(srcIpbs, srcPort, destPort, bs, len);
			return;
		}

		SocketAddress sockAddr = getTunnel(destIpbs, destPort);
		if (sockAddr == null) {
			pacLogger.pacSwitch("No Dest Socket: ", srcIpbs, srcPort, destIpbs,
					destPort, bs, len);
			return;
		}

		DatagramPacket newDp = new DatagramPacket(bs, 0, len, sockAddr);
		pacLogger.pacSwitch("Switch: ", srcIpbs, srcPort, destIpbs, destPort,
				bs, len);

		dSocket.send(newDp);
	}

	private void broadcastSend(byte[] srcIpbs, int srcPort, int destPort,
			byte[] bs, int len) throws IOException {
		Integer srcIpi = ByteUtils.ipbs2Integer(srcIpbs);

		pacLogger.broadcast(srcIpbs, srcPort, destPort, bs, len);
		DataInputStream dis ;
		for (Map.Entry<Integer, Map<Integer, SocketAddress>> entry : this.ipPortTunnelMap
				.entrySet()) {

			// ignore self
			Integer ipi = entry.getKey();
			if (ipi.equals(srcIpi)) {
				// Logger.debug("Ignore self.");
				continue;
			}

			// ingore not my C-Level subnet
			// TODO:: no test
			if ((srcIpi & 0xffffff00) != (ipi & 0xffffff00)) {
				continue;
			}

			Map<Integer, SocketAddress> portTunnelMap = entry.getValue();

			SocketAddress sockAddr = portTunnelMap.get(new Integer(destPort));

			if (sockAddr == null) {
				continue;
			}

			pacLogger.broadcastSend(ipi, destPort, bs, len, sockAddr);
			DatagramPacket newDp = new DatagramPacket(bs, 0, len, sockAddr);
			dSocket.send(newDp);
		}
	}

	private SocketAddress getTunnel(Integer ipi, int port) {
		Map<Integer, SocketAddress> portTunnelMap = this.ipPortTunnelMap
				.get(ipi);
		if (portTunnelMap == null)
			return null;

		SocketAddress sockAddr = portTunnelMap.get(new Integer(port));
		if (sockAddr != null)
			return sockAddr;
		return null;
	}

	private SocketAddress getTunnel(byte[] ipbs, int port) {
		Integer ipi = ByteUtils.ipbs2Integer(ipbs);
		SocketAddress sockAddr = getTunnel(ipi, port);
		return sockAddr;
	}

	public void regTunnel(byte[] ipbs, int port, SocketAddress sockAddr) {
		Integer ipi = ByteUtils.ipbs2Integer(ipbs);

		Map<Integer, SocketAddress> portTunnelMap = this.ipPortTunnelMap
				.get(ipi);
		if (portTunnelMap == null) {
			portTunnelMap = new ConcurrentHashMap<Integer, SocketAddress>();
			this.ipPortTunnelMap.put(ipi, portTunnelMap);
		}

		SocketAddress oldSockAddr = portTunnelMap.get(port);
		if (oldSockAddr != null && sockAddr.equals(oldSockAddr))
			return;

		portTunnelMap.put(port, sockAddr);

		InetSocketAddress iSockAddr = (InetSocketAddress) sockAddr;
		String sockStr = iSockAddr.toString();
		pacLogger.reg(ipbs, port, sockStr);

//		VirtualAddr vAddr = tunnelVirtualAddrMap.get(sockStr);
//		if (vAddr != null && vAddr.ipi != ipi) {
//			unRegTunnel(vAddr.ipi);
//		}
//		vAddr = new VirtualAddr(ipi, port);
//		tunnelVirtualAddrMap.put(sockStr, vAddr);

	}

	// public void unRegTunnel(byte[] ipbs) {
	// Integer ipi = ByteUtils.ipbs2Integer(ipbs);
	// this.ipPortTunnelMap.remove(ipi);
	// // TODO maybe memory hole
	// }

	public Map<Integer, Map<Integer, SocketAddress>> getIpPortTunnelMap() {
		return ipPortTunnelMap;
	}

	public void unRegTunnel(byte[] ipbs, int port, SocketAddress socketAddress) {
		Integer ipi = ByteUtils.ipbs2Integer(ipbs);
		this.ipPortTunnelMap.remove(ipi);
		pacLogger.unReg(ipi);
	}
	
	private static byte[] natKeepingData = new byte[] { (byte) 255 };

	//TODO:当并发用户很多时可能引起严重的性能问题
	private void sendNatKeepingData() throws IOException, InterruptedException {
		for (Map.Entry<Integer, Map<Integer, SocketAddress>> entry : this.ipPortTunnelMap
				.entrySet()) {
			int ipi = entry.getKey();
			Map<Integer, SocketAddress> portMap = entry.getValue();
			for (Map.Entry<Integer, SocketAddress> portEntry: portMap.entrySet()) {
				int port = portEntry.getKey();
				SocketAddress sockAddr = portEntry.getValue();
				if (port == 27015 || port == 6112 || port == 6111) {
					DatagramPacket dPacket = new DatagramPacket(natKeepingData, 1, sockAddr);
					dSocket.send(dPacket);
//					Logger.debug("sendNatKeepingData " + sockAddr.toString());
					Thread.sleep(5);
				}
			}
		}
	}

	@Override
	public void start() {
		super.start();
		// NAT Keeping Thread
		new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						sendNatKeepingData();
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	public static void main(String[] args) throws Exception {
		Logger.setLevel(Logger.LEVEL_DETAIL);
		UdpPipeServer server = new UdpPipeServer(6666);
		server.start();
	}

	

}
