package com.pipe.virtual.server;

import org.jboss.netty.channel.ChannelPipeline;

import com.pipe.common.service.PeerType;
import com.pipe.virtual.peer.VirtualPeer;

public class VirtualServer extends VirtualPeer {

	public VirtualServer(String id, String mediatorHost, int mediatorPort) {
		super(id, mediatorHost, mediatorPort);
	}
	
	@Override
	protected PeerType getPeerType() {
		return PeerType.SERVER;
	}
	
	@Override
	protected void addDectectConnectionCreatedHandler(ChannelPipeline pipeline) {
		//do nothing
	}
	
	public static void main(String[] args) throws Exception {
		// Print usage if no argument is specified.
		if (args.length < 3) {
			System.err.println("Usage:  <id> <mediator_host> <mediator_port>");
			return;
		}

		// Parse options.
		final String id = args[0];
		final String mediatorHost = args[1];
		final int mediatorPort = Integer.parseInt(args[2]);
		
		VirtualPeer vp = new VirtualServer(id, mediatorHost, mediatorPort);
		vp.start();
	}
}
