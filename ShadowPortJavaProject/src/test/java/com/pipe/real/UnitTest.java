package com.pipe.real;

import junit.framework.Assert;

import org.junit.Test;

import com.pipe.common.entity.PeerPortEntry;
import com.pipe.common.service.Service;
import com.pipe.mediator.MappingEntry;
import com.pipe.mediator.Mediator;
import com.pipe.mediator.MediatorConfiguration;
import com.pipe.virtual.peer.VirtualPeer;
import com.pipe.virtual.proxy.Proxy;

public class UnitTest {


	/**
	 * this is target app which only work in a LAN
	 * <P>
	 * 127.0.0.1:XXXX (Client) -> 127.0.0.1:9999 (Server)
	 * @throws Exception 
	 */
	@Test
	public void testClient2Server() throws Exception {
		String host = "localhost";
		int port = 9999;
		EchoServer echoServer = new EchoServer(host, port).setUsingSSL(true).start();

		int firstMessageSize = 100;
		EchoClient echoClient = new EchoClient(host, port, firstMessageSize).setTotalSendNum(10).setUsingSSL(true).start();
		
		resultChecker(echoClient, echoServer);
	}

	private void resultChecker(EchoClient echoClient, Service... otherServices) {
		long totalWaitMS = 0;
		
		while(true){
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
			
			totalWaitMS+= 100;
			
			if (echoClient.isSuccess()){
				break;
			}
			
			if (totalWaitMS > 1500 * echoClient.getTotalSendNum()){
				System.err.println("waiting too long! Get angry!");
				break;
			}
		}
		
		if (!echoClient.isSuccess()){
			Assert.fail("not echo back in time!");
		} else {
			System.out.println("Test sucess, now try reallocate resources!!");
		}
		
		echoClient.stop();
		
		for (Service service : otherServices) {
			service.stop();
		}
	}

	/**
	 * this is target app which only work in a LAN
	 * <P>
	 * 127.0.0.1:XXXX (Client) -> 127.0.0.1:9999 (Server)
	 */
	@Test
	public void testClient2Proxy2Server() {
		String host = "localhost";
		int port = 9999;
		EchoServer echoServer = new EchoServer(host, port).setUsingSSL(true).start();
		
		int proxyProt = 6869;
		Proxy proxy = new Proxy(host, port, host, proxyProt).start();

		int firstMessageSize = 100;
		EchoClient echoClient = new EchoClient(host, port, firstMessageSize).setTotalSendNum(10).setUsingSSL(true).start();
		
		resultChecker(echoClient, proxy, echoServer);
		
	}
	
	private boolean mediatorUsingSSL = false;
	private boolean clientServerUsingSSL = false;
	
	@Test
	public void testClient2VirtualPeer2Mediator2VirtualPeer2Server_SSL_Mediator() {
		mediatorUsingSSL = true;
		testClient2VirtualPeer2Mediator2VirtualPeer2Server();
		mediatorUsingSSL = false;
	}
	@Test
	public void testClient2VirtualPeer2Mediator2VirtualPeer2Server_SSL_ClientServer() {
		clientServerUsingSSL = true;
		testClient2VirtualPeer2Mediator2VirtualPeer2Server();
		clientServerUsingSSL = false;
	}
	@Test
	public void testClient2VirtualPeer2Mediator2VirtualPeer2Server_SSL_Both() {
		mediatorUsingSSL = true;
		clientServerUsingSSL = true;
		testClient2VirtualPeer2Mediator2VirtualPeer2Server();
		mediatorUsingSSL = false;
		clientServerUsingSSL = false;
	}

	/**
	 * this is enhanced app powered by Pipe, work across internet
	 * <P>
	 * 127.0.0.1:XXXX (Client) -> 127.0.0.1:8888 (V S) -> 127.0.0.2:7777 (M) ->
	 * 127.0.0.3:XXXX(V C) -> 127.0.0.3:9999 (Server)
	 */
	@Test
	public void testClient2VirtualPeer2Mediator2VirtualPeer2Server() {

		/**
		 * start the real server
		 */
		String serverHost = "127.0.0.3";
		int serverPort = 9999;

		EchoServer echoServer = new EchoServer(serverHost, serverPort).setUsingSSL(clientServerUsingSSL).start();

		/**
		 * build the pipe
		 */
		String mediatorHost = "127.0.0.2";
		int mediatorPort = 6666;

		int dataPort = 7777;

		Mediator mediator = new Mediator(new MediatorConfiguration(mediatorHost, mediatorPort, mediatorHost, dataPort)
				.setUsingSSLForDataConnection(mediatorUsingSSL));
		mediator.start();

		// virtual client, virtual server build the pipe through the mediator
		VirtualPeer virtualServer = new VirtualPeer("vs1", mediatorHost, mediatorPort);
		virtualServer.start();

		 VirtualPeer virtualClient = new VirtualPeer("vc1", mediatorHost,
		 mediatorPort);
		 virtualClient.start();

		String virtualServerHost = "127.0.0.1";
		int virtualServerPort = 8888;

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		// just mock
		mediator.match(new PeerPortEntry("vs1", virtualServerHost, virtualServerPort), new PeerPortEntry("vc1",
				serverHost, serverPort));

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		/**
		 * start the real client, connect to the virtual server
		 */
		int firstMessageSize = 100;
		EchoClient echoClient = new EchoClient(virtualServerHost, virtualServerPort, firstMessageSize).setUsingSSL(clientServerUsingSSL).start();
		
		echoClient.setTotalSendNum(10);
		
		resultChecker(echoClient, echoServer, virtualServer, virtualClient, mediator);
		
	}

	@Test
	public void testMediatorConfiguration() {

		/**
		 * start the real server
		 */
		String serverHost = "127.0.0.3";
		int serverPort = 9999;


		String virtualServerHost = "127.0.0.1";
		int virtualServerPort = 8888;

		/**
		 * build the pipe
		 */
		String mediatorHost = "127.0.0.2";
		int mediatorPort = 6666;

		int dataPort = 7777;

		MappingEntry entry = new MappingEntry(new PeerPortEntry("vs1", virtualServerHost, virtualServerPort),
				new PeerPortEntry("vc1", serverHost, serverPort));
		MappingEntry entry2 = new MappingEntry(new PeerPortEntry("vs2",  "127.0.0.1", 5555),
				new PeerPortEntry("vc2", serverHost, serverPort));

		MediatorConfiguration configuration = new MediatorConfiguration(mediatorHost, mediatorPort, mediatorHost,
				dataPort).addMapping(entry).addMapping(entry2).setPublicDataHost(mediatorHost).setPublicDataPort(dataPort);

		System.out.println(configuration);

	}

	/**
	 * bi-direction mapping test
	 * <p>
	 * peer-1 : "127.0.0.1" peer-2 : "127.0.0.3"
	 */
	@Test
	public void testPeer2Virtual2Mediator2Virtual2Peer() {
		/**
		 * build the pipe
		 */
		String mediatorHost = "127.0.0.2";
		int mediatorPort = 6666;

		int dataPort = 7777;

		Mediator mediator = new Mediator(new MediatorConfiguration(mediatorHost, mediatorPort, mediatorHost, dataPort));
		mediator.start();

		// virtual client, virtual server build the pipe through the mediator
		VirtualPeer virtualServer = new VirtualPeer("vp1", mediatorHost, mediatorPort);
		virtualServer.start();

		VirtualPeer virtualClient = new VirtualPeer("vp2", mediatorHost, mediatorPort);
		virtualClient.start();

		if (true) {
			/**
			 * peer-1 as client, peer-2 as server
			 */

			/**
			 * start the real server
			 */
			String serverHost = "127.0.0.3";
			int serverPort = 9999;

			EchoServer echoServer = new EchoServer(serverHost, serverPort);
			echoServer.start();

			String virtualServerHost = "127.0.0.1";
			int virtualServerPort = 8888;

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			// just mock
			PeerPortEntry virtualPort = new PeerPortEntry("vp1", virtualServerHost, virtualServerPort);
			PeerPortEntry realPort = new PeerPortEntry("vp2", serverHost, serverPort);
			mediator.match(virtualPort, realPort);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			/**
			 * start the real client, connect to the virtual server
			 */
			int firstMessageSize = 100;
			EchoClient echoClient = new EchoClient(virtualServerHost, virtualServerPort, firstMessageSize);
			echoClient.setTotalSendNum(10).start();
			
			resultChecker(echoClient, echoServer);

		}

		if (true) {
			/**
			 * peer-2 as client, peer-1 as server
			 */

			/**
			 * start the real server
			 */
			String serverHost = "127.0.0.1";
			int serverPort = 9999;

			EchoServer echoServer = new EchoServer(serverHost, serverPort);
			echoServer.start();

			String virtualServerHost = "127.0.0.3";
			int virtualServerPort = 8888;

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

			// just mock
			PeerPortEntry virtualPort = new PeerPortEntry("vp2", virtualServerHost, virtualServerPort);
			PeerPortEntry realPort = new PeerPortEntry("vp1", serverHost, serverPort);
			mediator.match(virtualPort, realPort);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

			/**
			 * start the real client, connect to the virtual server
			 */
			int firstMessageSize = 100;
			EchoClient echoClient = new EchoClient(virtualServerHost, virtualServerPort, firstMessageSize);
			echoClient.setTotalSendNum(10).start();
			
			resultChecker(echoClient, echoServer);
		}
		
		virtualClient.stop();
		virtualServer.stop();
		mediator.stop();

	}

	@Test
	public void testStop() {
		/**
		 * build the pipe
		 */
		String mediatorHost = "127.0.0.2";
		int mediatorPort = 6666;

		int dataPort = 7777;

		Mediator mediator = new Mediator(new MediatorConfiguration(mediatorHost, mediatorPort, mediatorHost, dataPort));
		mediator.start();

		// virtual client, virtual server build the pipe through the mediator
		VirtualPeer virtualServer = new VirtualPeer("vs1", mediatorHost, mediatorPort);
		virtualServer.start();

		VirtualPeer virtualClient = new VirtualPeer("vc1", mediatorHost, mediatorPort);
		virtualClient.start();

		String virtualServerHost = "127.0.0.1";
		int virtualServerPort = 1234;
		String serverHost = "127.0.0.3";
		int serverPort = 1234;

		mediator.match(new PeerPortEntry("vs1", virtualServerHost, virtualServerPort), new PeerPortEntry("vc1",
				serverHost, serverPort));

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		virtualClient.stop();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		virtualServer.stop();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		mediator.stop();

	}

}
