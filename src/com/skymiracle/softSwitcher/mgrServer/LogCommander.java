package com.skymiracle.softSwitcher.mgrServer;

import com.skymiracle.server.tcpServer.cmdServer.ConnHandler;
import com.skymiracle.softSwitcher.udpPipe.PacLogger;

public class LogCommander extends AbsCommander {

	public LogCommander(ConnHandler connHandler) {
		super(connHandler);
	}

	private StringBuffer getLogDesc() {
		StringBuffer desc = new StringBuffer();
		desc.append("[Log Help]\r\n");
		desc.append((PacLogger.TAG_PACSWITCH + 1) + " - TAG_PACSWITCH\r\n");
		desc.append((PacLogger.TAG_REG + 1)+ " - TAG_REG\r\n");
		desc.append((PacLogger.TAG_UNREG + 1)+ " - TAG_UNREG\r\n");
		desc.append((PacLogger.TAG_BROADCAST + 1)+ " - TAG_BROADCAST\r\n");		
		
		return desc;
	}
	
	@Override
	public byte[] doCmd(String head, String tail) throws Exception {
		tail = tail.trim();
		if (tail.length() == 0)
			return getBytesCRLF(getLogDesc().toString());

		int tag = Integer.parseInt(tail);
		if (tag > 4)
			return this.getBytesCRLF("550 Error Tag");
		if (tag == 0) 
			return this.getBytesCRLF("550 Error Tag");
		
		if (tag > 0)
			this.udpPipeServer.pacLogger.addLogTag(tag - 1);
		else
			this.udpPipeServer.pacLogger.delLogTag(-(tag + 1));
		return this.getBytesCRLF("250 OK");
	}

}
