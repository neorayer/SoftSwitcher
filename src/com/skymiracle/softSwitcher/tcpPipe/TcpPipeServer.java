package com.skymiracle.softSwitcher.tcpPipe;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.skymiracle.logger.Logger;
import com.skymiracle.server.ServerInfo;
import com.skymiracle.server.tcpServer.TcpServer;
import com.skymiracle.util.ByteUtils;

public class TcpPipeServer extends TcpServer {

	public static class SocketInfo {
		public Socket socket = null;
		public int id = 0;
		public byte[] uname = new byte[32];
		public byte[] vip = new byte[4];
		public byte[] skey = new byte[4];
		public int vport = 0;
		public byte[] dstVip = new byte[4];
		public byte[] dstPort = new byte[2];

		public SocketInfo(Socket socket) {
			this.socket = socket;
		}

		public void close() throws IOException {
			if (socket != null)
				socket.close();
		}

	}

	// private Map<Integer, Socket> destSocketMap = new
	// ConcurrentHashMap<Integer, Socket>();

	public Map<Integer, SocketInfo> clientSocketMap = new ConcurrentHashMap<Integer, SocketInfo>();

	private Map<Integer, Socket> controlSocketMap = new ConcurrentHashMap<Integer, Socket>();

	public TcpPipeServer(int port) {
		super(TcpPipeServerHandler.class);
		setName("TcpPipeServer");
		setPort(port);
		setCloseSocketAfterHandle(false);
	}

	@Override
	protected ServerInfo newServerInfoInstance() {
		return null;
	}

	// public Socket getDestServerSocket(byte[] addr) {
	// int i = ByteUtils.bs2int(addr, 0);
	// return destSocketMap.get(i);
	// }

	// public void putDestServerSocket(byte[] addr, Socket socket)
	// throws IOException {
	// int i = ByteUtils.bs2int(addr, 0);
	// Socket oldSocket = destSocketMap.get(i);
	// if (oldSocket != null) {
	// Logger.info("Close old Dest Server Socket.");
	// oldSocket.close();
	// }
	// destSocketMap.put(i, socket);
	// }

	// public void putClientSocket(byte[] addr, Socket socket) throws
	// IOException {
	// int i = ByteUtils.bs2int(addr, 0);
	// Socket oldSocket = clientSocketMap.get(i);
	// if (oldSocket != null) {
	// Logger.info("Close old client Socket.");
	// oldSocket.close();
	// }
	// clientSocketMap.put(i, socket);
	// }

	private int clientIdPointer = 0;

	/**
	 * 
	 */
	public synchronized int putClientSocket(SocketInfo socketInfo)
			throws TcpPipeException {
		final int MAX_CLIENT = 2000;
		int clientId = clientIdPointer;
		int c = 0;
		for (;;) {
			c++;
			if (c > MAX_CLIENT)
				throw new TcpPipeException(
						"client is full, can't alloc clientId");
			clientIdPointer++;
			if (clientIdPointer > 1000)
				clientIdPointer = 0;
			SocketInfo si = clientSocketMap.get(clientIdPointer);
			if (si == null) {
				clientId = clientIdPointer;
				break;
			}
		}
		socketInfo.id = clientId;
		clientSocketMap.put(clientId, socketInfo);
		return clientId;
	}

	public SocketInfo getClientSocket(int clientId) {
		return clientSocketMap.get(clientId);
	}

	public static void main(String[] args) {
		TcpPipeServer server = new TcpPipeServer(6666);
		server.start();
	}

	public synchronized void removeClientSocket(int clientId) {
		SocketInfo oldSi = clientSocketMap.get(clientId);
		if (oldSi != null) {
			try {
				oldSi.close();
			} catch (IOException e) {

			}
		}
		clientSocketMap.remove(clientId);
	}

	// public void removeDestServerSocket(byte[] addr, boolean isClose)
	// throws IOException {
	// int i = ByteUtils.bs2int(addr, 0);
	// if (isClose) {
	// Socket oldSocket = destSocketMap.get(i);
	// if (oldSocket != null) {
	// Logger.info("Close old Dest Server Socket.");
	// oldSocket.close();
	// }
	// }
	// destSocketMap.remove(i);
	// }

	public void putControlSocket(byte[] addr, Socket socket) throws IOException {
		int i = ByteUtils.bs2int(addr, 0);
		Socket oldSocket = controlSocketMap.get(i);
		if (oldSocket != null) {
			Logger.info("Close old Control Socket.");
			oldSocket.close();
		}
		controlSocketMap.put(i, socket);
	}

	public Socket getControlSocket(byte[] addr) {
		int i = ByteUtils.bs2int(addr, 0);
		return controlSocketMap.get(i);
	}

}
