package com.skymiracle.softSwitcher.tcpPipe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import com.skymiracle.logger.Logger;
import com.skymiracle.softSwitcher.tcpPipe.TcpPipeServer.SocketInfo;

public class TcpPipe {

	public final static byte ROLE_TAG_SERVER = 0x10;

	public final static byte ROLE_TAG_CLIENT = 0x12;

	public final static byte ROLE_TAG_CONTROL = 0x14;

	private SocketInfo si_1;

	private SocketInfo si_2;

	private int bufLen = 4096;

	private boolean isShutdown = false;

	private TcpPipeListener pipeListener = null;
	
	public TcpPipe() {

	}

	public TcpPipe(SocketInfo si_1, SocketInfo si_2) {
		super();
		this.si_1 = si_1;
		this.si_2 = si_2;
		try {
			this.si_1.socket.setTcpNoDelay(true);
			this.si_2.socket.setTcpNoDelay(true);
		} catch (SocketException e) {
			Logger.warn("", e);
		}
	}


	public int getBufLen() {
		return bufLen;
	}

	public void setBufLen(int bufLen) {
		this.bufLen = bufLen;
	}

	public void connect() throws IOException {
		connectInputOutput(si_1.socket, si_2.socket);
		connectInputOutput(si_2.socket, si_1.socket);

//		Thread t = new Thread() {
//			@Override
//			public void run() {
//				while (true) {
//					try {
//						if (isShutdown) {
//							try {
//								si_1.socket.close();
//							} catch (IOException e1) {
//								// e1.printStackTrace();
//							}
//							try {
//								si_2.socket.close();
//							} catch (IOException e) {
//								// e.printStackTrace();
//							}
//							break;
//						}
//						Thread.sleep(200);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//				pipeListener.OnPipeBroken();
//			}
//		};
//		t.setName("PipeMgrThread");
//		t.start();
	}

	private String socketStr(Socket so) {
		String s = "[" + so.getLocalAddress() + ":" + so.getLocalPort();
		s += "<=>";
		s += so.getRemoteSocketAddress() + "]";
		return s;
	}

	private Thread connectInputOutput(final Socket so1, final Socket so2)
			throws IOException {
		final InputStream in = so1.getInputStream();
		final OutputStream out = so2.getOutputStream();
		Thread t = new Thread() {
			@Override
			public void run() {
				isShutdown = false;
				Logger.debug("tcpPipe Conn :" + socketStr(so1) + " ==> "
						+ socketStr(so2));
				byte[] buf = new byte[bufLen];
				int rLen = -1;
				try {
					while (true) {
//						long begin = System.currentTimeMillis();
						rLen = in.read(buf);
						if (rLen <= 0)
							break;
//						System.out.println("------------------" + (System.currentTimeMillis() - begin));
						out.write(buf, 0, rLen);
					}
				} catch (IOException e) {
//					e.printStackTrace();
				}
				isShutdown = true;
				pipeListener.OnPipeBroken();
				Logger.debug("tcpPipe Break:" + socketStr(so1) + " ==> "
						+ socketStr(so2));
			}
		};
		t.setName("PipeIO-Thread");
		t.start();
		return t;

	}

	public TcpPipeListener getPipeListener() {
		return pipeListener;
	}

	public void setPipeListener(TcpPipeListener pipeListener) {
		this.pipeListener = pipeListener;
	}
	


}
