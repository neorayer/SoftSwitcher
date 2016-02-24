package com.skymiracle.softSwitcher.tools.tcpTester;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import com.skymiracle.logger.Logger;
import com.skymiracle.server.ServerInfo;
import com.skymiracle.server.tcpServer.TcpServer;
import com.skymiracle.server.tcpServer.TcpServerConnectionHandler;
import com.skymiracle.util.ByteUtils;

public class TcpTester {

	private String pipeHost;

	private int pipePort;

	private String virtualIp;

	private int virtualPort;

	public TcpTester() {
	}

	public String getPipeHost() {
		return pipeHost;
	}

	public void setPipeHost(String pipeHost) {
		this.pipeHost = pipeHost;
	}

	public int getPipePort() {
		return pipePort;
	}

	public void setPipePort(int pipePort) {
		this.pipePort = pipePort;
	}

	public String getVirtualIp() {
		return virtualIp;
	}

	public void setVirtualIp(String virtualIp) {
		this.virtualIp = virtualIp;
	}

	public int getVirtualPort() {
		return virtualPort;
	}

	public void setVirtualPort(int virtualPort) {
		this.virtualPort = virtualPort;
	}

	public void startCtrPipe() throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(pipeHost, pipePort), 3000);

		/**
		 * 向pipe host 发送初始数据包 1 个字节:角色标志 0x14 (控制监听身份) 4 个字节:自己的虚拟IP
		 */
		byte roleTag = 0x14;
		socket.getOutputStream().write(roleTag);
		socket.getOutputStream().write(
				new InetSocketAddress(virtualIp, virtualPort).getAddress()
						.getAddress());
		byte[] buf = new byte[8];
		while (true) {
			int rlen = socket.getInputStream().read(buf);
			if (rlen <= 0)
				break;
			if (0x00 == buf[0]) {
				Logger.info("noop ctl code: " + buf[0]);
				continue;
			}

			if (0x01 != buf[0]) {
				Logger.info("illigal ctl code: " + buf[0]);
				continue;
			}

			new Thread() {
				public void run() {
					try {
						System.out.println("a new thread");
						startDataPipe();
						System.err.println("Thread Over!");
					}catch(Exception e) {
						
					}
				}
			}.start();
		}

	}

	private int forceRead(InputStream is, byte[] buf) throws IOException {
		int c = 0;
		int pc = 0;
		while (c < buf.length) {
			pc = is.read(buf, c, buf.length - c);
			if (pc == -1)
				break;
			c += pc;
			//System.out.println(c);
		}
		return c;
	}

	private void startDataPipe() throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(pipeHost, pipePort));

		OutputStream os = new BufferedOutputStream(socket.getOutputStream());
		InputStream is = socket.getInputStream();
		os.write((byte) 0x10);
		os.write(new InetSocketAddress(virtualIp, virtualPort).getAddress()
				.getAddress());
		os.flush();

		forceRead(is, new byte[46]);
		byte[] data = new byte[8];
		while (true) {
			int c = forceRead(is, data);
			if (c == 0)
				break;
			long ms = System.currentTimeMillis() -ByteUtils.bs2ulong(data);
			if (ms > 0)
			Logger.debug(""+ms);
		}

	}

	public void connect(String dstVirutalIp, int dstVirtualPort)
			throws IOException, InterruptedException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(pipeHost, pipePort), 3000);
		OutputStream os = socket.getOutputStream();
		byte[] nameData = "zhourui".getBytes();
		// 1 字节：0x12表示自己的身份是client
		os.write((byte) 0x12);

		// 32 字节：用户名
		byte[] data = new byte[32];
		System.arraycopy(nameData, 0, data, 0, nameData.length);
		os.write(data);

		// 4 字节：安全Key
		os.write(new byte[4]);

		{
			// 4 字节：自己的虚拟IP地址
			InetAddress addr = new InetSocketAddress(virtualIp, virtualPort)
					.getAddress();
			os.write(addr.getAddress());
		}

		{
			// 4 字节：目标主机的虚拟IP地址
			InetAddress addr = new InetSocketAddress(dstVirutalIp,
					dstVirtualPort).getAddress();
			os.write(addr.getAddress());

			// 2 字节：目标主机的虚拟端口号
			os.write(ByteUtils.ushort2bs(dstVirtualPort));
		}

		// 之后 ：原本要发送的数据
		for (int i = 0; i < 500000; i++) {
			Thread.sleep(27);
			byte[] bs = ByteUtils.ulong2bs(System.currentTimeMillis());
			os.write(bs);
			os.flush();
		}

	}

	public static void main(String[] args) throws Exception {
		Logger.setLevel(Logger.LEVEL_DEBUG);
		TcpTester fc = new TcpTester();
		fc.setPipeHost("10.1.1.221");
		fc.setPipePort(9333);

		fc.setVirtualIp("10.0.1.1");
		fc.setVirtualPort(6113);

		fc.startCtrPipe();
	}

}
