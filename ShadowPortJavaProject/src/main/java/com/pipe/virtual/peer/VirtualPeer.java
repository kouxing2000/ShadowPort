package com.pipe.virtual.peer;

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pipe.common.entity.ChannelHolder;
import com.pipe.common.net.Utils;
import com.pipe.common.net.message.DataConnectionEmployedMessage;
import com.pipe.common.net.message.DataConnectionRegisterMessage;
import com.pipe.common.net.message.DataConnectionReleasedMessage;
import com.pipe.common.net.message.JoinRequest;
import com.pipe.common.net.message.JoinResponse;
import com.pipe.common.net.message.OpenVirtualPortMessage;
import com.pipe.common.service.PeerType;

public class VirtualPeer {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final String mediatorHost;
	private final int mediatorPort;

	private final String id;

	private JoinResponse joinResponse;

	private ClientBootstrap clientBootstrap;

	private ServerBootstrap serverBootstrap;

	private Channel signalChannel;

	public VirtualPeer(String id, String mediatorHost, int mediatorPort) {
		super();
		this.id = id;
		this.mediatorHost = mediatorHost;
		this.mediatorPort = mediatorPort;
	}

	private List<ChannelHolder> connectionsPool = new ArrayList<ChannelHolder>();

	protected ChannelHolder getHolderByID(String connectionId) {
		for (ChannelHolder holder : connectionsPool) {
			if (holder.getConnectionId().equals(connectionId)) {
				return holder;
			}
		}
		return null;
	}

	protected ChannelHolder getHolderByDataChannel(Channel dataChannel) {
		for (ChannelHolder holder : connectionsPool) {
			if (holder.getChannel() == dataChannel) {
				return holder;
			}
		}
		return null;
	}

	protected ChannelHolder getHolderByPipedChannel(Channel pipedChannel) {
		for (ChannelHolder holder : connectionsPool) {
			if (holder.getPipedChannel() == pipedChannel) {
				return holder;
			}
		}
		return null;
	}

	protected ChannelHolder getFreeChannel() {
		for (ChannelHolder holder : connectionsPool) {
			if (holder.getPipedChannel() == null) {
				logger.info("!rent channel:" + holder.getConnectionId());
				return holder;
			}
		}

		//TODO if no free channel
		
		return null;
	}

	@Override
	public String toString() {
		return super.toString() + "-" + id + "\n" + connectionsPool;
	}
	
	private ExecutorService executor;

	public void start() {

		// Configure the bootstrap.
		executor = Executors.newCachedThreadPool();

		// Set up the event pipeline factory.
		NioServerSocketChannelFactory sf = new NioServerSocketChannelFactory(executor, executor);

		NioClientSocketChannelFactory cf = new NioClientSocketChannelFactory(executor, executor);

		serverBootstrap = new ServerBootstrap(sf);
		clientBootstrap = new ClientBootstrap(cf);

		serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();

				Utils.addEncoder(pipeline);

				addOnConnectionFromClientOpenHandler(pipeline);

				pipeline.addLast("disconnect_listener", getRealConnectionDisconnectHandler());

				return pipeline;
			}
		});

		ChannelFuture toMediatorFuture = clientBootstrap.connect(new InetSocketAddress(mediatorHost, mediatorPort));
		toMediatorFuture.awaitUninterruptibly();

		signalChannel = toMediatorFuture.getChannel();
		ChannelPipeline pipeline = signalChannel.getPipeline();

		Utils.addCodec(pipeline);

		pipeline.addLast("signal-handler", new SimpleChannelUpstreamHandler() {
			@Override
			public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

				Object messageObject = e.getMessage();

				logger.info(id + " signal receive message:" + messageObject);

				if (messageObject instanceof JoinResponse) {

					if (joinResponse == null) {
						joinResponse = (JoinResponse) e.getMessage();
						createDataConnections();
					}

				} else if (messageObject instanceof DataConnectionReleasedMessage) {

					DataConnectionReleasedMessage drm = (DataConnectionReleasedMessage) messageObject;

					onMessage(drm);

				} else if (messageObject instanceof OpenVirtualPortMessage) {

					OpenVirtualPortMessage ovpm = (OpenVirtualPortMessage) messageObject;

					onMessage(ovpm);

				}

				super.messageReceived(ctx, e);
			}

		});

		JoinRequest req = new JoinRequest(id, getPeerType());
		sendMessage(signalChannel, req);

	}

	protected PeerType getPeerType() {
		return PeerType.PEER;
	}

	private Map<InetSocketAddress, OpenVirtualPortMessage> portMapping = new HashMap<InetSocketAddress, OpenVirtualPortMessage>();

	protected void createDataConnections() {

		// create data connection pool
		final InetSocketAddress remoteAddress = new InetSocketAddress(joinResponse.getDataConnectionHost(),
				joinResponse.getDataConnectionPort());

		for (int i = 0; i < joinResponse.getMinimalDurableDataConnectionNum(); i++) {
			ChannelFuture dataConnectionFuture = clientBootstrap.connect(remoteAddress);
			Channel channel = dataConnectionFuture.getChannel();

			dataConnectionPreparedForUsage(channel);
			final String connectionId = "con_" + i;

			dataConnectionFuture.addListener(new ChannelFutureListener() {

				@Override
				public void operationComplete(ChannelFuture future) throws Exception {

					if (future.isSuccess()) {
						Channel channel = future.getChannel();

						sendMessage(channel, new DataConnectionRegisterMessage(joinResponse.getDataConnectionKey(),
								connectionId));

						connectionsPool.add(new ChannelHolder(connectionId, channel));
					} else {
						logger.error("failed to connect to " + remoteAddress);
					}
				}
			});

		}

	}

	protected void dataConnectionPreparedForUsage(Channel channel) {

		ChannelPipeline pipeline = channel.getPipeline();

		Utils.clearAllHandlers(pipeline);

		Utils.addCodec(pipeline);

		addDectectConnectionCreatedHandler(pipeline);
	}

	protected void addDectectConnectionCreatedHandler(ChannelPipeline pipeline) {
		pipeline.addLast("connection_open_intercept", new SimpleChannelUpstreamHandler() {
			@Override
			public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
				
				final Channel dataChannel = e.getChannel();

				dataChannel.setReadable(false);
				
				Object messageObject = e.getMessage();

				if (messageObject instanceof DataConnectionEmployedMessage) {

					DataConnectionEmployedMessage dem = (DataConnectionEmployedMessage) messageObject;

					logger.info("data channel " + dataChannel + " first receive :" + messageObject);

					final InetSocketAddress remoteAddress = new InetSocketAddress(dem.getRealHost(), dem
							.getRealPort());
					ChannelFuture connect = clientBootstrap.connect(remoteAddress);

					connect.addListener(new ChannelFutureListener() {

						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							if (future.isSuccess()) {
								ChannelHolder channelHolder = getHolderByDataChannel(dataChannel);
								channelHolder.setPipedChannel(future.getChannel());

								Utils.clearAllHandlers(dataChannel.getPipeline());
								Utils.clearAllHandlers(future.getChannel().getPipeline());
								Utils.bridge(future.getChannel(), dataChannel);

								future.getChannel().getPipeline()
										.addLast("disconnect_listener", getRealConnectionDisconnectHandler());
								
								dataChannel.setReadable(true);
							} else {
								logger.error("Error, failed to connect to " + remoteAddress);
							}
						}
					});

				}

				// super.messageReceived(ctx, e);

			}
		});
	}

	public void stop() {
		if (signalChannel.isConnected()){
			ChannelFuture closeFuture = signalChannel.close();
			closeFuture.awaitUninterruptibly(1000);
		}
		
		for (ChannelHolder handler : connectionsPool){
			handler.getChannel().close();
		}
		
		connectionsPool.clear();
		
		portMapping.clear();
		
		serverBootstrap.releaseExternalResources();
		clientBootstrap.releaseExternalResources();
		
		executor.shutdown();
	}

	protected SimpleChannelUpstreamHandler getRealConnectionDisconnectHandler() {

		return new SimpleChannelUpstreamHandler() {
			@Override
			public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

				notifyRemote(e.getChannel());

				super.channelDisconnected(ctx, e);
			}

			@Override
			public void childChannelClosed(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
				notifyRemote(e.getChannel());
				
				super.childChannelClosed(ctx, e);
			}

			protected void notifyRemote(final Channel channel) {
				
				executor.execute(new Runnable() {
					
					@Override
					public void run() {
						ChannelHolder channelHolder = getHolderByPipedChannel(channel);

						//let channel dead normally
						try {
							Thread.sleep(600);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						// clear the piped channel
						channelHolder.setPipedChannel(null);

						// notify remote part to disconnect too
						sendMessage(signalChannel, new DataConnectionReleasedMessage(channelHolder.getConnectionId()));

						//recycle the data connection
						dataConnectionPreparedForUsage(channelHolder.getChannel());
					}
				});
				
			}

		};
	}

	private ChannelFuture sendMessage(Channel channel, Serializable message) {
		logger.info("send " + message + " to " + channel);

		return channel.write(message);
	}

	protected void onMessage(DataConnectionReleasedMessage drm) {
		//TODO need change, if peer can match to several other peers
		ChannelHolder dataChannelHolder = getHolderByID(drm.getConnectionId());
		
		Utils.clearAllHandlers(dataChannelHolder.getPipedChannel().getPipeline());

		dataChannelHolder.getPipedChannel().disconnect();

		dataChannelHolder.setPipedChannel(null);

		dataConnectionPreparedForUsage(dataChannelHolder.getChannel());
	}
	
	protected void onMessage(OpenVirtualPortMessage ovpm) {
		InetSocketAddress virtualAddress = new InetSocketAddress(ovpm.getVirtualHost(), ovpm
				.getVirtualPort());

		portMapping.put(virtualAddress, ovpm);

		serverBootstrap.bind(virtualAddress);
	}

	protected void addOnConnectionFromClientOpenHandler(ChannelPipeline pipeline) {
		pipeline.addLast("connection_open_notify", new SimpleChannelUpstreamHandler() {

			//?? why childChannelOpen not work
			@Override
			public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
				final Channel realChannel = e.getChannel();

				logger.info("channel Open : " + realChannel);

				// stop read any message
				realChannel.setReadable(false);

				InetSocketAddress localAddress = (InetSocketAddress) realChannel.getLocalAddress();

				OpenVirtualPortMessage openVirtualPortMessage = portMapping.get(localAddress);

				DataConnectionEmployedMessage dem = new DataConnectionEmployedMessage(openVirtualPortMessage
						.getVirtualHost(), openVirtualPortMessage.getVirtualPort(), openVirtualPortMessage
						.getRealHost(), openVirtualPortMessage.getRealPort());

				ChannelHolder freeChannelHolder = getFreeChannel();
				
				if (freeChannelHolder != null){

					final Channel dataChannel = freeChannelHolder.getChannel();
	
					freeChannelHolder.setPipedChannel(realChannel);
	
					ChannelFuture writeFuture = sendMessage(dataChannel, dem);
	
					writeFuture.addListener(new ChannelFutureListener() {
	
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							if (future.isSuccess()) {
	
								Utils.clearAllHandlers(dataChannel.getPipeline());
								Utils.clearAllHandlers(realChannel.getPipeline());
								Utils.bridge(dataChannel, realChannel);
	
								//??
								Thread.sleep(100);
								
								// let the new traffic in
								realChannel.setReadable(true);
							} else {
								logger.error("Error to write to" + dataChannel);
							}
						}
					});
				} else {
					logger.error("No free connection available!");
				}

				super.channelOpen(ctx, e);
			}
		});
	}
	
	public static void main(String[] args) throws Exception {
		// Print usage if no argument is specified.
		if (args.length < 3) {
			System.err.println("Usage: <id> <mediator_host> <mediator_port>");
			return;
		}

		// Parse options.
		final String id = args[0];
		final String mediatorHost = args[1];
		final int mediatorPort = Integer.parseInt(args[2]);
		
		VirtualPeer vp = new VirtualPeer(id, mediatorHost, mediatorPort);
		vp.start();
	}
}
