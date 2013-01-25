package com.pipe.mediator;

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.FileReader;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.pipe.common.entity.ChannelHolder;
import com.pipe.common.entity.DataConnectionEntry;
import com.pipe.common.entity.PeerPortEntry;
import com.pipe.common.net.Utils;
import com.pipe.common.net.message.DataConnectionEmployedMessage;
import com.pipe.common.net.message.DataConnectionRegisterMessage;
import com.pipe.common.net.message.DataConnectionReleasedMessage;
import com.pipe.common.net.message.JoinRequest;
import com.pipe.common.net.message.JoinResponse;
import com.pipe.common.net.message.OpenVirtualPortMessage;
import com.pipe.common.service.PeerType;

public class Mediator implements MediatorMBean {

	private static final Logger logger = LoggerFactory.getLogger(Mediator.class);

	private static final int DEFAULT_MINIMAL_DATA_CONNECTION_NUM = 7;

	private final String signalHost;
	private final int signalPort;

	/**
	 * bind address
	 */
	private final String dataHost;
	private final int dataPort;

	/**
	 * address which peers used to connect
	 */
	private final String publicDataHost;
	private final int publicDataPort;

	private final MediatorConfiguration mediatorConfiguration;

	private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(2);

	public Mediator(String signalHost, int signalPort, String dataHost, int dataPort) {
		this(signalHost, signalPort, dataHost, dataPort, null);
	}

	public Mediator(String signalHost, int signalPort, String dataHost, int dataPort, MediatorConfiguration mc) {
		this(signalHost, signalPort, dataHost, dataPort, dataHost, dataPort, mc);
	}

	public Mediator(String signalHost, int signalPort, String dataHost, int dataPort, String publicDataHost,
			int publicDataPort, MediatorConfiguration mc) {
		super();
		this.signalHost = signalHost;
		this.signalPort = signalPort;
		this.dataHost = dataHost;
		this.dataPort = dataPort;
		this.publicDataHost = publicDataHost;
		this.publicDataPort = publicDataPort;

		if (mc != null) {
			this.mediatorConfiguration = mc;
		} else {
			this.mediatorConfiguration = new MediatorConfiguration();
		}
	}

	/**
	 * <clientID, PeerHolder>
	 */
	private Map<String, PeerHolder> allPeers = new ConcurrentHashMap<String, PeerHolder>();

	private PeerHolder getPeerBySignalChannel(Channel signalChannel) {
		for (PeerHolder peerHolder : allPeers.values()) {
			if (peerHolder.getSignalChannel().equals(signalChannel)) {
				return peerHolder;
			}
		}
		return null;
	}

	private PeerHolder getPeerByConnectionKey(String dataConnectionKey) {
		for (PeerHolder peerHolder : allPeers.values()) {
			if (peerHolder.getDataConnectionKey().equals(dataConnectionKey)) {
				return peerHolder;
			}
		}
		return null;
	}

	public void start() {

		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

			ObjectName name = new ObjectName("com.pipe.mbeans:type=Mediator");

			mbs.registerMBean(this, name);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Configure the server.
		NioServerSocketChannelFactory channelFactory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

		ServerBootstrap signalBootstrap = new ServerBootstrap(channelFactory);

		// Set up the pipeline factory.
		signalBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				Utils.addCodec(pipeline);
				pipeline.addLast("message_handler", new SimpleChannelUpstreamHandler() {
					@Override
					public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
						Channel remoteSignalChannel = e.getChannel();

						Object message = e.getMessage();

						logger.info("mediator signal receive message:{} from channel:{}", message, remoteSignalChannel);

						if (message instanceof JoinRequest) {
							JoinRequest joinReq = (JoinRequest) message;

							/**
							 * XXX generate dataConnectionKey
							 */
							String dataConnectionKey = remoteSignalChannel.getLocalAddress().toString() + "-"
									+ (int) (Math.random() * 1000);
							PeerHolder peerHolder = new PeerHolder(joinReq.getClientID(), joinReq.getClientType(),
									remoteSignalChannel, dataConnectionKey);
							onPeerJoined(peerHolder);

							sendMessage(remoteSignalChannel, new JoinResponse(true, dataConnectionKey,
									DEFAULT_MINIMAL_DATA_CONNECTION_NUM, publicDataHost, publicDataPort));

							remoteSignalChannel.getPipeline().addLast("disconnect_handler",
									new SimpleChannelUpstreamHandler() {

										@Override
										public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
												throws Exception {
											super.channelClosed(ctx, e);

											logger.info("signal channel closed:" + e.getChannel());

											PeerHolder peer = getPeerBySignalChannel(e.getChannel());

											logger.info("disconnected peer: {}, release related!", peer);

											onPeerLeft(peer);
										}
									});

						} else if (message instanceof DataConnectionReleasedMessage) {

							DataConnectionReleasedMessage drm = (DataConnectionReleasedMessage) message;

							PeerHolder peer = getPeerBySignalChannel(remoteSignalChannel);

							DataConnectionEntry entry = new DataConnectionEntry(peer.getPeerID(), drm.getConnectionId());

							DataConnectionEntry peerEntry = existPipes.remove(entry);

							if (peerEntry != null) {

								existPipes.remove(peerEntry);

								// its peer need to release its corresponding
								// connection
								String peerConnectionID = peerEntry.getConnectionID();
								drm.setConnectionId(peerConnectionID);

								PeerHolder coupledPeer = allPeers.get(peerEntry.getPeerID());

								sendMessage(coupledPeer.getSignalChannel(), drm);

								String connectionID = entry.getConnectionID();

								recycleDataConnection(peer, connectionID);

								recycleDataConnection(coupledPeer, peerConnectionID);

							} else {
								logger.error("can not find coupled connection, {} in {}", entry, existPipes.keySet());
							}
						}

						super.messageReceived(ctx, e);
					}

				});
				return pipeline;
			}
		});

		// Bind and start to accept incoming connections.
		signalBootstrap.bind(new InetSocketAddress(signalHost, signalPort));

		ServerBootstrap dataBootstrap = new ServerBootstrap(channelFactory);

		dataBootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {

				ChannelPipeline pipeline = pipeline();

				Utils.addCodec(pipeline);

				pipeline.addLast("data_connection_come", new SimpleChannelUpstreamHandler() {

					@Override
					public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

						logger.info("data_connection_come message:" + e.getMessage());

						DataConnectionRegisterMessage drm = (DataConnectionRegisterMessage) e.getMessage();

						PeerHolder myPeerHolder = getPeerByConnectionKey(drm.getDataConnectionKey());

						ChannelHolder myChannelHolder = new ChannelHolder(drm.getConnectionId(), e.getChannel());

						myPeerHolder.addDataConnection(myChannelHolder);

						e.getChannel().getPipeline().remove(this);

						// waiting to receive DataConnectionEmployedMessage
						// assign one data connection of its corresponding peer
						// to it

						addDataChannelOccupiedListener(myPeerHolder, myChannelHolder);

						super.messageReceived(ctx, e);
					}

				});

				return pipeline;
			}
		});

		// Bind and start to accept incoming connections.
		dataBootstrap.bind(new InetSocketAddress(dataHost, dataPort));

	}

	// only used it for debug
	private static final Gson gson = new Gson();

	@Override
	public boolean match(String virtualPortJson, String realPortJson) {
		PeerPortEntry virtualPort = gson.fromJson(virtualPortJson, PeerPortEntry.class);
		PeerPortEntry realPort = gson.fromJson(realPortJson, PeerPortEntry.class);
		return match(virtualPort, realPort);
	}

	public boolean match(MappingEntry entry) {
		return match(entry.getVirtualPort(), entry.getRealPort());
	}

	/**
	 * <virtual, real>
	 */
	private final Map<PeerPortEntry, PeerPortEntry> existPortMappings = new ConcurrentHashMap<PeerPortEntry, PeerPortEntry>();

	/**
	 * with pair<BR>
	 * <vc1_c1, vs1_c2> and <vs1_c2, vc1_c1><BR>
	 * 
	 */
	private final Map<DataConnectionEntry, DataConnectionEntry> existPipes = new ConcurrentHashMap<DataConnectionEntry, DataConnectionEntry>();

	public synchronized boolean match(PeerPortEntry virtualPort, PeerPortEntry realPort) {

		logger.info("trying match {} to {}", virtualPort, realPort);

		if (virtualPort == null) {
			return false;
		}

		if (realPort == null) {
			return false;
		}

		if (virtualPort.getPort() < 0) {
			virtualPort.setPort(realPort.getPort());
		}

		if (virtualPort.getHost() == null || virtualPort.getHost().trim().isEmpty()) {
			virtualPort.setHost(realPort.getHost());
		}

		if (realPort.equals(existPortMappings.get(virtualPort))) {
			logger.info("already matched!");
			return false;
		}

		PeerHolder virtualServerPeer = allPeers.get(virtualPort.getPeerID());
		PeerHolder virtualClientPeer = allPeers.get(realPort.getPeerID());

		if (virtualServerPeer == null) {
			return false;
		}

		if (virtualClientPeer == null) {
			return false;
		}

		if (virtualServerPeer.getPeerType() == PeerType.CLIENT) {
			return false;
		}

		if (virtualClientPeer.getPeerType() == PeerType.SERVER) {
			return false;
		}

		logger.info("match between virtualServerPeer:{} and virtualClientPeer:{}", virtualServerPeer, virtualClientPeer);

		existPortMappings.put(virtualPort, realPort);

		sendMessage(
				virtualServerPeer.getSignalChannel(),
				new OpenVirtualPortMessage(virtualPort.getHost(), virtualPort.getPort(), realPort.getHost(), realPort
						.getPort()));

		return true;
	}

	protected void pipeConnections(PeerHolder peer, String peerConnectionID, PeerHolder coupledPeer,
			String coupledPeerConnectionID) {

		ChannelHolder connection = peer.getConnectionByID(peerConnectionID);
		ChannelHolder coupledConnection = coupledPeer.getConnectionByID(coupledPeerConnectionID);

		logger.info("piping!!!!:\n {} \n<---------->\n {}", connection, coupledConnection);

		if (connection == null || coupledConnection == null) {
			logger.error("can not piped between {}'s{} and {}'s{}", new Object[] { peer.getPeerID(), peerConnectionID,
					coupledPeer.getPeerID(), coupledPeerConnectionID });
			return;
		}

		connection.setPipedChannel(coupledConnection.getChannel());
		coupledConnection.setPipedChannel(connection.getPipedChannel());
		Utils.clearAllHandlers(connection.getChannel().getPipeline());
		Utils.clearAllHandlers(coupledConnection.getChannel().getPipeline());
		Utils.bridge(connection.getChannel(), coupledConnection.getChannel());

		DataConnectionEntry entry = new DataConnectionEntry(peer.getPeerID(), peerConnectionID);
		DataConnectionEntry coupledEntry = new DataConnectionEntry(coupledPeer.getPeerID(), coupledPeerConnectionID);

		existPipes.put(entry, coupledEntry);
		existPipes.put(coupledEntry, entry);
	}

	private ChannelFuture sendMessage(Channel channel, Serializable message) {

		logger.info("send " + message + " to " + channel);

		return channel.write(message);
	}

	@Override
	public String verbose() {

		StringBuilder sb = new StringBuilder();
		sb.append(new Date() + "\n");
		sb.append("peers:\n");
		for (PeerHolder peerHolder : allPeers.values()) {
			sb.append(" ").append(peerHolder.toString()).append("\n");
		}

		sb.append("port mappings:\n");
		for (Map.Entry<PeerPortEntry, PeerPortEntry> pm : existPortMappings.entrySet()) {
			sb.append(" ").append(pm.getKey()).append(" -> ").append(pm.getValue()).append("\n");
		}

		sb.append("pipes:\n");
		for (Map.Entry<DataConnectionEntry, DataConnectionEntry> pipe : existPipes.entrySet()) {
			sb.append(" ").append(pipe.getKey()).append(" -> ").append(pipe.getValue()).append("\n");
		}

		return sb.toString();
	}

	protected synchronized void onPeerJoined(final PeerHolder peerHolder) {

		allPeers.put(peerHolder.getPeerID(), peerHolder);

		// wait some time for their data connections come

		SCHEDULED_EXECUTOR_SERVICE.schedule(new Runnable() {

			@Override
			public void run() {
				List<MappingEntry> mappings = mediatorConfiguration.filterWithPeerId(peerHolder.getPeerID());

				for (MappingEntry mappingEntry : mappings) {
					match(mappingEntry);
				}
			}
		}, 2, TimeUnit.SECONDS);

	}

	//TODO more test
	protected synchronized void onPeerLeft(PeerHolder peer) {

		// clear existPortMappings
		String peerID = peer.getPeerID();

		for (Iterator<Entry<PeerPortEntry, PeerPortEntry>> iterator = existPortMappings.entrySet().iterator(); iterator
				.hasNext();) {
			Entry<PeerPortEntry, PeerPortEntry> mapping = iterator.next();

			if (peerID.equals(mapping.getKey().getPeerID())
					|| peerID.equals(mapping.getValue().getPeerID())) {
				iterator.remove();
				logger.info("remove port mapping {} -> {}", mapping.getKey(), mapping.getValue());
			}

		}

		// clean up pipes
		Set<Entry<DataConnectionEntry, DataConnectionEntry>> entrySet = existPipes.entrySet();

		for (Iterator<Entry<DataConnectionEntry, DataConnectionEntry>> iterator = entrySet.iterator(); iterator
				.hasNext();) {

			Entry<DataConnectionEntry, DataConnectionEntry> pipe = iterator.next();

			DataConnectionEntry pipeEntry = pipe.getKey();

			if (peerID.equals(pipeEntry.getPeerID())) {
				iterator.remove();
				DataConnectionEntry coupledEntry = pipe.getValue();

				PeerHolder coupledPeer = allPeers.get(coupledEntry.getPeerID());
				if (coupledPeer != null) {
					coupledPeer.recycleConnection(coupledEntry.getConnectionID());
				}
				logger.info("recycle pipe {} <-> {}", pipeEntry, coupledEntry);
			} else if (peerID.equals(pipe.getValue().getPeerID())){
				iterator.remove();
				logger.info("recycle pipe {} <-> {}", pipeEntry, pipe.getValue());
			}

		}
		
		allPeers.remove(peerID);

	}

	protected void addDataChannelOccupiedListener(final PeerHolder myPeerHolder, final ChannelHolder myChannelHolder) {

		if (myPeerHolder.getPeerType() == PeerType.SERVER || myPeerHolder.getPeerType() == PeerType.PEER) {

			myChannelHolder.getChannel().getPipeline()
					.addLast("first_message_detect", new SimpleChannelUpstreamHandler() {
						// TODO2 can reuse this handler
						@Override
						public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

							// stop read any message
							e.getChannel().setReadable(false);

							logger.info("first_message_detect message:" + e.getMessage());

							DataConnectionEmployedMessage dem = (DataConnectionEmployedMessage) e.getMessage();

							PeerPortEntry virtualPeerEntry = new PeerPortEntry(myPeerHolder.getPeerID(), dem
									.getVirtualHost(), dem.getVirtualPort());

							PeerPortEntry realPeerEntry = existPortMappings.get(virtualPeerEntry);

							myPeerHolder.notifyConnectionInUse(myChannelHolder);

							final PeerHolder realPeerHolder = allPeers.get(realPeerEntry.getPeerID());

							if (realPeerHolder == null) {
								// TODO to handle this case
								logger.error("", e);
								return;
							}

							final ChannelHolder rentConnection = realPeerHolder.rentConnection();

							if (rentConnection == null) {
								// TODO to handle this case
								logger.error("", e);
								return;
							}

							ChannelFuture write = rentConnection.getChannel().write(dem);
							write.addListener(new ChannelFutureListener() {

								@Override
								public void operationComplete(ChannelFuture future) throws Exception {

									pipeConnections(myPeerHolder, myChannelHolder.getConnectionId(), realPeerHolder,
											rentConnection.getConnectionId());

									myChannelHolder.getChannel().setReadable(true);

									myChannelHolder.getChannel().getPipeline()
											.addLast("disconnect listener", new SimpleChannelUpstreamHandler() {
												@Override
												public void channelDisconnected(ChannelHandlerContext ctx,
														ChannelStateEvent e) throws Exception {
													super.channelDisconnected(ctx, e);

													if (myChannelHolder != null) {
														// TODO recycle its
														// matched data
														// connections
													}
												}
											});
								}
							});

						}
					});
		}
	}

	protected void recycleDataConnection(PeerHolder peer, String connectionID) {

		ChannelHolder con = peer.getConnectionByID(connectionID);

		logger.info("recyle data connection:{} of {}", con, peer);

		if (con != null) {

			Utils.clearAllHandlers(con.getChannel().getPipeline());
			Utils.addCodec(con.getChannel().getPipeline());

			addDataChannelOccupiedListener(peer, con);

			peer.recycleConnection(connectionID);

		} else {
			logger.error("connect not exist!");
		}

	}

	public static void main(String[] args) throws Exception {
		{
			// print usage
			PeerPortEntry vpe = new PeerPortEntry("id", "host", 1234);
			PeerPortEntry vpe2 = new PeerPortEntry("id2", "host2", 2345);

			System.out.println("sample json format of PeerPortEntry, using it in MBeans\n" + gson.toJson(vpe));

			MediatorConfiguration mc = new MediatorConfiguration();
			mc.addMapping(new MappingEntry(vpe, vpe2));
			// mc.addMapping(new MappingEntry(vpe2, vpe));
			System.out.println("sample json format of MediatorConfiguration, using it as last parameter");
			System.out.println(gson.toJson(mc));

		}

		// Print usage if no argument is specified.
		if (args.length < 4) {
			System.err
					.println("Usage: <mediator_host> <mediator_port> <data_host> <data_port> (<public_data_host> <public_data_port> <config_file_path>)");
			return;
		}

		// Parse options.
		final String mediatorHost = args[0];
		final int mediatorPort = Integer.parseInt(args[1]);
		final String dataHost = args[2];
		final int dataPort = Integer.parseInt(args[3]);
		String publicDataHost = dataHost;
		int publicDataPort = dataPort;

		MediatorConfiguration mediatorConfiguration = null;
		String filePath = null;
		if (args.length == 5) {
			filePath = args[4];
		}

		if (args.length >= 6) {
			publicDataHost = args[4];
			publicDataPort = Integer.parseInt(args[5]);

			if (args.length >= 7) {
				filePath = args[6];
			}
		}

		if (filePath != null) {
			System.out.println("Parse config file:" + filePath);
			mediatorConfiguration = gson.fromJson(new FileReader(filePath), MediatorConfiguration.class);
		}

		Mediator m = new Mediator(mediatorHost, mediatorPort, dataHost, dataPort, publicDataHost, publicDataPort,
				mediatorConfiguration);
		m.start();

		System.out.println("mediator is started");

		System.out.println(m.mediatorConfiguration);

		while (true) {
			Thread.sleep(3000);

			System.out.println(m.verbose());
		}

	}
}
