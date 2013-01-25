package com.pipe.real;

import com.pipe.common.entity.PeerPortEntry;
import com.pipe.mediator.MappingEntry;
import com.pipe.mediator.Mediator;
import com.pipe.mediator.MediatorConfiguration;
import com.pipe.virtual.client.VirtualClient;
import com.pipe.virtual.peer.VirtualPeer;
import com.pipe.virtual.server.VirtualServer;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

//		 testClient2Server();

//		 testClient2VirtualServer2Mediator2VirtualClient2Server();

//		 virtualServer2Mediator2VirtualClient();

		// testPeer2Virtual2Mediator2Virtual2Peer();

		// testStop();

//		testMediatorConfiguration();
		
//		testClient2Pipe2Server();
		
		setUpVirtualClientAndServer();

	}

	/**
	 * this is target app which only work in a LAN
	 * <P>
	 * 127.0.0.1:XXXX (Client) -> 127.0.0.1:9999 (Server)
	 */
	protected static void testClient2Server() {
		String host = "localhost";
		int port = 9999;
		new EchoServer(host, port).run();

		int firstMessageSize = 100;
		new EchoClient(host, port, firstMessageSize).run();
	}

	/**
	 * this is enhanced app powered by Pipe, work across internet
	 * <P>
	 * 127.0.0.1:XXXX (Client) -> 127.0.0.1:8888 (V S) -> 127.0.0.2:7777 (M) ->
	 * 127.0.0.3:XXXX(V C) -> 127.0.0.3:9999 (Server)
	 */
	protected static void testClient2VirtualServer2Mediator2VirtualClient2Server() {

		/**
		 * start the real server
		 */
		String serverHost = "127.0.0.3";
		int serverPort = 9999;

		new EchoServer(serverHost, serverPort).run();

		/**
		 * build the pipe
		 */
		String mediatorHost = "127.0.0.2";
		int mediatorPort = 6666;

		int dataPort = 7777;

		Mediator mediator = new Mediator(mediatorHost, mediatorPort, mediatorHost, dataPort);
		mediator.start();

		// virtual client, virtual server build the pipe through the mediator
		VirtualPeer virtualServer = new VirtualServer("vs1", mediatorHost, mediatorPort);
		virtualServer.start();

		// VirtualPeer virtualClient = new VirtualClient("vc1", mediatorHost,
		// mediatorPort);
		// virtualClient.start();

		try {
			VirtualClient.main(new String[] { "vc1", mediatorHost, "" + mediatorPort });
		} catch (Exception e1) {
			e1.printStackTrace();
		}

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
		new EchoClient(virtualServerHost, virtualServerPort, firstMessageSize).run();
	}

	/**
	 * start the real server manually, then start this method, then start the
	 * client
	 */
	protected static void virtualServer2Mediator2VirtualClient() {

		/**
		 * start the real server
		 */
		String serverHost = "127.0.0.1";
		int serverPort = 80;

		/**
		 * build the pipe
		 */
		String mediatorHost = "127.0.0.2";
		int mediatorPort = 6666;

		int dataPort = 7777;

		Mediator mediator = new Mediator(mediatorHost, mediatorPort, mediatorHost, dataPort);
		mediator.start();

		// virtual client, virtual server build the pipe through the mediator
		VirtualPeer virtualServer = new VirtualServer("vs1", mediatorHost, mediatorPort);
		virtualServer.start();

		VirtualPeer virtualClient = new VirtualClient("vc1", mediatorHost, mediatorPort);
		virtualClient.start();

		String virtualServerHost = "127.0.0.3";
		int virtualServerPort = 8888;

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		// just mock
		mediator.match(new PeerPortEntry("vs1", virtualServerHost, virtualServerPort), new PeerPortEntry("vc1",
				serverHost, serverPort));

		/**
		 * start the real client, connect to the virtual server
		 */
	}

	public static void testMediatorConfiguration() {

		/**
		 * start the real server
		 */
		String serverHost = "127.0.0.3";
		int serverPort = 9999;

		new EchoServer(serverHost, serverPort).run();

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

		Mediator mediator = new Mediator(mediatorHost, mediatorPort, mediatorHost, dataPort,
				new MediatorConfiguration().addMapping(entry));
		mediator.start();

		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
		}

		// virtual client, virtual server build the pipe through the mediator
		VirtualPeer virtualServer = new VirtualServer("vs1", mediatorHost, mediatorPort);
		virtualServer.start();

		VirtualPeer virtualClient = new VirtualClient("vc1", mediatorHost, mediatorPort);
		virtualClient.start();

		//give mediator some time to pipe the peers
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
		}

		/**
		 * start the real client, connect to the virtual server
		 */
		int firstMessageSize = 100;
		new EchoClient(virtualServerHost, virtualServerPort, firstMessageSize).run();

	}

	/**
	 * bi-direction mapping test
	 * <p>
	 * peer-1 : "127.0.0.1" peer-2 : "127.0.0.3"
	 */
	protected static void testPeer2Virtual2Mediator2Virtual2Peer() {
		/**
		 * build the pipe
		 */
		String mediatorHost = "127.0.0.2";
		int mediatorPort = 6666;

		int dataPort = 7777;

		Mediator mediator = new Mediator(mediatorHost, mediatorPort, mediatorHost, dataPort);
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

			new EchoServer(serverHost, serverPort).run();

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
			new EchoClient(virtualServerHost, virtualServerPort, firstMessageSize).run();

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

			new EchoServer(serverHost, serverPort).run();

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
			new EchoClient(virtualServerHost, virtualServerPort, firstMessageSize).run();

		}

	}

	public static void testStop() {
		/**
		 * build the pipe
		 */
		String mediatorHost = "127.0.0.2";
		int mediatorPort = 6666;

		int dataPort = 7777;

		Mediator mediator = new Mediator(mediatorHost, mediatorPort, mediatorHost, dataPort);
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
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		virtualClient.stop();

		try {
			Thread.sleep(100000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected static void testClient2Pipe2Server() {

		/**
		 * start the real server
		 */
		String serverHost = "127.0.0.3";
		int serverPort = 9999;

		new EchoServer(serverHost, serverPort).run();

		/**
		 * build the pipe
		 */
		String mediatorHost = "124.205.4.53";
		int mediatorPort = 6666;

		// virtual client, virtual server build the pipe through the mediator
		VirtualPeer virtualServer = new VirtualServer("vs1", mediatorHost, mediatorPort);
		virtualServer.start();

		VirtualPeer virtualClient = new VirtualClient("vc1", mediatorHost,
		mediatorPort);
	    virtualClient.start();

		String virtualServerHost = "127.0.0.1";
		int virtualServerPort = 8888;

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		// just mock

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}

		/**
		 * start the real client, connect to the virtual server
		 */
		int firstMessageSize = 1000;
		new EchoClient(virtualServerHost, virtualServerPort, firstMessageSize).run();
	}
	

	private static void setUpVirtualClientAndServer() {
//	      {
//	          "virtualPort":{
//	             "peerID":"vs2",
//	             "host":"127.0.0.1",
//	             "port":8080
//	          },
//	          "realPort":{
//	             "peerID":"vc2",
//	             "host":"192.168.1.2",
//	             "port":8080
//	          }
//	       }
		
		/**
		 * build the pipe
		 */
		String mediatorHost = "124.205.4.53";
		int mediatorPort = 6666;

		// virtual client, virtual server build the pipe through the mediator
		VirtualPeer virtualServer = new VirtualServer("vs2", mediatorHost, mediatorPort);
		virtualServer.start();

		VirtualPeer virtualClient = new VirtualClient("vc2", mediatorHost,
		mediatorPort);
	    virtualClient.start();

	}
}
