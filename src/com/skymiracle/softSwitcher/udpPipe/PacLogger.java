package com.skymiracle.softSwitcher.udpPipe;

import java.net.SocketAddress;

import com.skymiracle.logger.Logger;
import com.skymiracle.util.ByteUtils;

public class PacLogger {

	public final static int TAG_PACSWITCH = 0;
	public final static int TAG_REG = 1;
	public final static int TAG_UNREG = 2;
	public final static int TAG_BROADCAST = 3;

	public int logTag = 0;

	public PacLogger() {
		// addLogTag(TAG_PACSWITCH);
		// addLogTag(TAG_REG);
		// addLogTag(TAG_UNREG);
		// addLogTag(TAG_BROADCAST);
	}

	public void addLogTag(int tag) {
		logTag = logTag | (1 << tag);
	}

	public void delLogTag(int tag) {
		logTag = logTag & (~(1 << tag));
	}

	public void pacSwitch(String desc, byte[] srcIpbs, int srcPort,
			byte[] destIpbs, int destPort, byte[] bs, int len) {
		if ((logTag & (1<< TAG_PACSWITCH)) == 0)
			return;
		Logger.debug(desc + ": " + ByteUtils.bs2ip(srcIpbs, 0) + ":" + srcPort
				+ " => " + ByteUtils.bs2ip(destIpbs, 0) + ":" + destPort);
	}

	public void broadcast(byte[] srcIpbs, int srcPort, int destPort, byte[] bs,
			int len) {
		if ((logTag & (1<< TAG_BROADCAST)) == 0)
			return;
		Logger.debug("Broadcast from: " + ByteUtils.bs2ip(srcIpbs, 0) + ":"
				+ srcPort + " => " + "255.255.255.255:" + destPort);
	}

	public void broadcastSend(Integer ipi, int destPort, byte[] bs, int len,
			SocketAddress sockAddr) {
		if ((logTag & (1<< TAG_BROADCAST)) == 0)
			return;
		Logger.debug("\t => " + ByteUtils.uint2ip(ipi) + ":" + destPort + ' '
				+ sockAddr.toString());
	}

	public void reg(byte[] ipbs, int port, String sockStr) {
		if ((logTag & (1<< TAG_REG)) == 0)
			return;
		Logger.debug("Reg: " + ByteUtils.bs2ip(ipbs, 0) + ":" + port + " -> "
				+ sockStr);

	}

	public void unReg(int ipi) {
		if ((logTag & (1<< TAG_UNREG)) == 0)
			return;
		Logger.debug("UnReg: " + ByteUtils.uint2ip(ipi));
	}

}
