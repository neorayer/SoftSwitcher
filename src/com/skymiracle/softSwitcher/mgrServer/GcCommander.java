package com.skymiracle.softSwitcher.mgrServer;

import com.skymiracle.server.tcpServer.cmdServer.ConnHandler;
import com.skymiracle.sysinfo.JvmMemInfo;

public class GcCommander extends AbsCommander {

	public GcCommander(ConnHandler connHandler) {
		super(connHandler);
	}

	private static JvmMemInfo getJvmMemInfo() {
		JvmMemInfo jvmMemInfo = new JvmMemInfo();
		Runtime runtime = Runtime.getRuntime();

		jvmMemInfo.setFree(runtime.freeMemory());

		jvmMemInfo.setTotal(runtime.totalMemory());

		runtime.freeMemory();
		jvmMemInfo.setMax(runtime.maxMemory());
		return jvmMemInfo;
	}
	@Override
	public byte[] doCmd(String head, String tail) throws Exception {
		String res = getJvmMemInfo().toString();
		System.gc();
		res += getJvmMemInfo().toString();

		return this.getBytesCRLF(res);
	}

}
