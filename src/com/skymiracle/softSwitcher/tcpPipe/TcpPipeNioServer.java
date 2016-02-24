package com.skymiracle.softSwitcher.tcpPipe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.skymiracle.logger.Logger;
import com.skymiracle.server.ServerImpl;
import com.skymiracle.server.ServerInfo;
import com.skymiracle.util.ByteUtils;

public class TcpPipeNioServer extends ServerImpl {

	public Map<Integer, SocketChannel> destSoChannelMap = new HashMap<Integer, SocketChannel>();

	public Map<Integer, SocketChannel> clientSoChannelMap = new HashMap<Integer, SocketChannel>();

	public Map<SocketChannel, SocketChannel> pipeMap = new HashMap<SocketChannel, SocketChannel>();

	private boolean isFinished = true;

	private int port = 1234;

	public TcpPipeNioServer() {
		setName("TcpPipeServer");
		setPort(1010);
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	protected ServerInfo newServerInfoInstance() {
		return null;
	}

	public SocketChannel getDestServerSoChannel(byte[] addr) {
		int i = ByteUtils.bs2int(addr, 0);
		return destSoChannelMap.get(i);
	}

	public void setDestServerSoChannel(byte[] addr, SocketChannel soChannel)
			throws IOException {
		int i = ByteUtils.bs2int(addr, 0);
		SocketChannel oldSoChannel = destSoChannelMap.get(i);
		if (oldSoChannel != null) {
			Logger.info("Close old Dest Server Socket.");
			oldSoChannel.close();
		}
		destSoChannelMap.put(i, soChannel);
	}

	public void setClientSoChannel(byte[] addr, SocketChannel soChannel)
			throws IOException {
		int i = ByteUtils.bs2int(addr, 0);
		SocketChannel oldSoChannel = clientSoChannelMap.get(i);
		if (oldSoChannel != null) {
			Logger.info("Close old client Socket.");
			oldSoChannel.close();
		}
		clientSoChannelMap.put(i, soChannel);
	}

	public static void main(String[] args) {
		TcpPipeNioServer server = new TcpPipeNioServer();
		server.start();
	}

	public void start() {
		new Thread(this).start();
	}

	public void stop() {
		isFinished = true;

	}

	private Map<SocketChannel, Object> justAccSocketMap = new HashMap<SocketChannel, Object>();

	public void run() {
		try {
			final ServerSocketChannel channel = ServerSocketChannel.open();
			ServerSocket socket = channel.socket();
			socket.setReceiveBufferSize(1024 * 1024);
			channel.configureBlocking(false);
			final Selector selector = Selector.open();
			socket.bind(new InetSocketAddress(port));
			channel.register(selector, SelectionKey.OP_ACCEPT);
			final ByteBuffer bb = ByteBuffer.allocate(2024);
			int count = 0;
			long begin = System.currentTimeMillis();
			isFinished = false;
			while (!isFinished) {
				try {
					int n = selector.select();
					if (n == 0)
						continue;
					Iterator it = selector.selectedKeys().iterator();
					while (it.hasNext()) {
						SelectionKey key = (SelectionKey) it.next();
						if (key.isAcceptable()) {
							SocketChannel soChannel = channel.accept();
							soChannel.configureBlocking(false);
							SelectionKey soKey = soChannel.register(selector,
									SelectionKey.OP_READ);
							justAccSocketMap.put(soChannel, new Object());
							soKey.attach(new TcpPipeHandler());
						}
						if (key.isReadable()) {
							SocketChannel soChannel = (SocketChannel) key
									.channel();
							soChannel.read(bb);

							TcpPipeHandler handler = (TcpPipeHandler) key
									.attachment();
							handler.dealWith(soChannel, bb);
							bb.position(0);

							count++;
						}
						it.remove();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			long spend = System.currentTimeMillis() - begin;
		} catch (IOException e) {
			e.printStackTrace();
		}
		isFinished = true;
	}

	public class TcpPipeHandler {

		private byte roleTag;

		private boolean isHeader = true;

		public TcpPipeHandler() {

		}

		public boolean isHeader() {
			return isHeader;
		}

		public void dealWith(SocketChannel soChannel, ByteBuffer bb) {
			try {
				if (isHeader) {
					dealWithHeader(soChannel, bb);
					isHeader = false;
					return;
				}

				dealWithData(soChannel, bb);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		public void dealWithData(SocketChannel soChannel, ByteBuffer bb)
				throws IOException {
			// System.out.print('[');
			// System.out.write(bb.array(), 0, bb.position());
			// System.out.print(']');

			bb.flip();
			pipeMap.get(soChannel).write(bb);
		}

		public void dealWithHeader(SocketChannel soChannel, ByteBuffer bb)
				throws IOException, InterruptedException, TcpPipeException {
			// System.out.print('[');
			// System.out.write(bb.array(), 0, bb.position());
			// System.out.print(']');

			// read role tag
			byte[] bs = bb.array();
			roleTag = bs[0];

			switch (roleTag) {
			case TcpPipe.ROLE_TAG_SERVER:
				Logger.info("role=server connect in");
				dealWithServerHeader(soChannel, bs);
				break;
			case TcpPipe.ROLE_TAG_CLIENT:
				Logger.info("role=client connect in");
				dealWithCleintHeader(soChannel, bs);
				break;
			default:
				throw new TcpPipeException("Illegal role tag.");
			}
		}

		private void dealWithServerHeader(SocketChannel soChannel, byte[] bs)
				throws IOException, InterruptedException {
			// read destination server virtual address
			byte[] myAddr = new byte[4];
			System.arraycopy(bs, 1, myAddr, 0, 4);
			Logger.info("Dest Server myAddr=" + ByteUtils.bs2ip(myAddr, 0));

			setDestServerSoChannel(myAddr, soChannel);

		}

		private void dealWithCleintHeader(SocketChannel soChannel, byte[] bs)
				throws IOException, TcpPipeException, InterruptedException {
			// read destination server virtual address
			byte[] destAddr = new byte[4];
			System.arraycopy(bs, 1, destAddr, 0, 4);
			String destAddrStr = ByteUtils.bs2ip(destAddr, 0);
			Logger.info("Dest Server destAddr=" + destAddrStr);

			byte[] myAddr = new byte[4];
			System.arraycopy(bs, 5, myAddr, 0, 4);
			String myAddrStr = ByteUtils.bs2ip(myAddr, 0);
			Logger.info("Client myAddr=" + myAddrStr);
			setClientSoChannel(myAddr, soChannel);

			SocketChannel destSoChannel = getDestServerSoChannel(destAddr);
			if (destSoChannel == null)
				throw new TcpPipeException("No Dest Server socket.");

			// send begin tag
			destSoChannel.write(ByteBuffer.wrap(new byte[] { 1 }));

			Logger.info("tcp Pipe connect: => " + destAddrStr);
			pipeMap.put(soChannel, destSoChannel);
			pipeMap.put(destSoChannel, soChannel);

		}

	}

}
