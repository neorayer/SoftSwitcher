package com.skymiracle.softSwitcher.tcpPipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import com.skymiracle.logger.Logger;
import com.skymiracle.server.tcpServer.TcpServerConnectionHandler;
import com.skymiracle.softSwitcher.tcpPipe.TcpPipeServer.SocketInfo;
import com.skymiracle.util.ByteUtils;

public class TcpPipeServerHandler extends TcpServerConnectionHandler {

	private final static byte[] CTL_TAG_BS_NOOP = new byte[] { 0x00 };

	private final static byte[] CTL_TAG_BS_CREQ = new byte[] { 0x01 };

	private byte roleTag;

	private InputStream in;

	TcpPipeServer pipeServer;

	@Override
	public void handleConnection() throws Exception {
		try {
			//socket.setSoTimeout(40000);
			pipeServer = (TcpPipeServer) tcpServer;
			in = socket.getInputStream();

			// read role tag
			byte[] bs = new byte[1];
			in.read(bs);
			roleTag = bs[0];
			

			switch (roleTag) {
			case TcpPipe.ROLE_TAG_SERVER:
				Logger.info("role=server connect in");
				dealWithServerHandler();
				break;
			case TcpPipe.ROLE_TAG_CLIENT:
				Logger.info("role=client connect in");
				dealWithCleintHandler();
				break;
			case TcpPipe.ROLE_TAG_CONTROL:
				Logger.info("role=control connect in");
				dealWithControlerHandler();
				break;
			default:
				throw new TcpPipeException("Illegal role tag.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			socket.close();
		}
	}

	private void dealWithControlerHandler() throws IOException {
		byte[] myAddr = new byte[4];
		in.read(myAddr);
		Logger.info("myAddr=" + ByteUtils.bs2ip(myAddr, 0));
		pipeServer.putControlSocket(myAddr, socket);
		socket.setSoTimeout(0);
		socket.setKeepAlive(true);
		try {
			while (true) {
				synchronized (socket) {
					try {
						socket.getOutputStream().write(CTL_TAG_BS_NOOP);
					}catch(SocketException e) {
						break;
					}
				}
				Thread.sleep(25000);
			}
			Logger.info("Control socket break!");
		} catch (Exception e) {
			Logger.warn("", e);
		}
	}

	private void dealWithServerHandler() throws IOException,
			InterruptedException {
		SocketInfo serverSi = new SocketInfo(socket);
		// 读取 server 的虚拟IP, 4 byte
		in.read(serverSi.vip);
		String vip = ByteUtils.bs2ip(serverSi.vip, 0);
		Logger.info("myAddr=" + vip);
		
		// 读取 请求此次server 连接的 clientId, 4 byte
		in.read(serverSi.dstVip);
		int clientId = ByteUtils.bs2int(serverSi.dstVip, 0);
		SocketInfo clientSi = pipeServer.getClientSocket(clientId);
		
		if (clientSi == null) {
			Logger.error("Can't find clientSocket id=" + clientId);
			socket.close();
			return;
		}
		
		// send to server
		OutputStream  dOs = socket.getOutputStream();
		dOs.write(clientSi.uname); // 32 字节：用户名
		dOs.write(clientSi.skey); //4 字节：安全Key
		dOs.write(clientSi.dstPort);
		dOs.flush();

		TcpPipe tcpPipe = new TcpPipe(serverSi, clientSi);
		DestServerTcpPipeListener listener = new DestServerTcpPipeListener(
				clientSi, serverSi);
		tcpPipe.setPipeListener(listener);
		try {
			tcpPipe.connect();
		}catch(Exception e) {
			e.printStackTrace();
		}

			

//		pipeServer.putDestServerSocket(myAddr, socket);
//		final Socket controlSocket = pipeServer.getControlSocket(myAddr);
//		if (controlSocket == null) {
//			Logger.error("controlSocket is null. myAddr=" + vip);
//			socket.close();
//			return;
//		}
//
//		synchronized (controlSocket) {
//			controlSocket.notify();
//		}
	}

	private void dealWithCleintHandler() throws IOException, TcpPipeException,
			InterruptedException {
		SocketInfo si = new SocketInfo(socket);
		si.socket = socket;
		
		// 32 字节：用户名
		in.read(si.uname);
		
		 //4 字节：安全Key
		in.read(si.skey);

		// 4 byte: 自己的虚拟IP地址
		in.read(si.vip);
		String myAddrStr = ByteUtils.bs2ip(si.vip, 0);
		Logger.info("myAddr=" + myAddrStr);

		// 4 byte: 目标服务器虚拟IP
		in.read(si.dstVip);
		String destAddrStr = ByteUtils.bs2ip(si.dstVip, 0);
		
		//2 byte: 目标服务器端口
		in.read(si.dstPort);
		Logger.info("destAddr=" + destAddrStr + ":"
				+ ByteUtils.bs2ushort(si.dstPort, 0));

		//存入Client SocketInfo
		int clientId = pipeServer.putClientSocket(si);
		
		// 获取目标server 虚拟IP对应的 control socket
		final Socket controlSocket = pipeServer.getControlSocket(si.dstVip);
		if (controlSocket == null )  {
			Logger.error("controlSocket is null");
			return;
		}
		if (controlSocket.getOutputStream() == null) {
			Logger.error("controlSocket.outputStream is null");
			return;
		}
		
		// 向目标发起请求服务端管道连接的请求
		synchronized(controlSocket){
			OutputStream os = controlSocket.getOutputStream();
			//发请求码, 1 byte
			os.write(CTL_TAG_BS_CREQ);
			
			//发clientId, 4 byte
			byte[] bs = ByteUtils.uint2bs(clientId);
			
			os.write(bs);
		}

		//TODO:这里缺乏检查client socket失效的机制，要尽快找出solution
	}

	public class DestServerTcpPipeListener implements TcpPipeListener {
		private SocketInfo clientSi;
		private SocketInfo serverSi;

		public DestServerTcpPipeListener(SocketInfo clientSi,
				SocketInfo serverSi) {
			super();
			this.clientSi = clientSi;
			this.serverSi = serverSi;
		}

		public void OnPipeBroken() {
			pipeServer.removeClientSocket(clientSi.id);
		}
	}
}
