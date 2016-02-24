package com.skymiracle.softSwitcher.tools.tcpTester;

import java.io.IOException;

public class TcpTesterClient extends TcpTester {

	public static void main(String[] args) throws Exception {
		for (int i = 2; i < 100; i++) {
			final int ci = i;
			Thread.sleep(500);
			new Thread() {
				public void run() {
					TcpTesterClient fws = new TcpTesterClient();
					fws.setPipeHost("10.1.1.221");
					fws.setPipePort(9333);

					fws.setVirtualIp("10.0.1." + ci);
					fws.setVirtualPort(6112);

					try {
					fws.connect("10.0.1.1", 6113);
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		}
	}
}
