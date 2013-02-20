package com.pipe.real;

import com.pipe.common.entity.PeerPortEntry;
import com.pipe.mediator.Mediator;
import com.pipe.mediator.MediatorConfiguration;
import com.pipe.virtual.peer.VirtualPeer;

public class SampleApps {
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		//virtualServer2Mediator2VirtualPeer();

		//setUpVirtualPeerAndServer();
	}
	
	/**
	 * start the real server manually, then start this method, then start the
	 * client
	 */
	static void virtualServer2Mediator2VirtualPeer() {

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

		Mediator mediator = new Mediator(new MediatorConfiguration(mediatorHost, mediatorPort, mediatorHost, dataPort));
		mediator.start();

		// virtual client, virtual server build the pipe through the mediator
		VirtualPeer virtualServer = new VirtualPeer("vs1", mediatorHost, mediatorPort);
		virtualServer.start();

		VirtualPeer virtualClient = new VirtualPeer("vc1", mediatorHost, mediatorPort);
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
	
	static void setUpVirtualPeerAndServer() {
		// {
		// "virtualPort":{
		// "peerID":"vs2",
		// "host":"127.0.0.1",
		// "port":8080
		// },
		// "realPort":{
		// "peerID":"vc2",
		// "host":"192.168.1.2",
		// "port":8080
		// }
		// }

		/**
		 * build the pipe
		 */
		String mediatorHost = "124.205.4.53";
		int mediatorPort = 6666;

		// virtual client, virtual server build the pipe through the mediator
		VirtualPeer virtualServer = new VirtualPeer("vs2", mediatorHost, mediatorPort);
		virtualServer.start();

		VirtualPeer virtualClient = new VirtualPeer("vc2", mediatorHost, mediatorPort);
		virtualClient.start();

	}
}
