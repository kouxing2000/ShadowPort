package com.pipe.virtual.proxy;

import static org.jboss.netty.channel.Channels.pipeline;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pipe.common.net.Utils;
import com.pipe.common.service.Service;
import com.pipe.real.EchoServer;

public class Proxy implements Service{

	private String realServerHost;
	private int realServerPort;

	private String proxyServerHost;
	private int proxyServerPort;

	private SocketAddress remoteServerAddress;
	private SocketAddress localProxyAddress;
	
	private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

	public Proxy(String realServerHost, int realServerPort, String proxyServerHost, int proxyServerPort) {
		super();
		this.realServerHost = realServerHost;
		this.realServerPort = realServerPort;
		this.proxyServerHost = proxyServerHost;
		this.proxyServerPort = proxyServerPort;

		remoteServerAddress = new InetSocketAddress(realServerHost, realServerPort);
		localProxyAddress = new InetSocketAddress(proxyServerHost, proxyServerPort);
	}

	private ClientBootstrap clientBootstrap;

	private ServerBootstrap serverBootstrap;
	
	private ExecutorService executor;
	
	private Channel bindChannel;
	
	private ChannelGroup allChannelInGroup;

	@Override
	public Proxy start() {
		
		allChannelInGroup = new DefaultChannelGroup();

		// Configure the bootstrap.
		executor = Executors.newCachedThreadPool();

		// Set up the event pipeline factory.
		NioServerSocketChannelFactory sf = new NioServerSocketChannelFactory(executor, executor);

		NioClientSocketChannelFactory cf = new NioClientSocketChannelFactory(executor, executor);

		serverBootstrap = new ServerBootstrap(sf);
		clientBootstrap = new ClientBootstrap(cf);

		serverBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			private Map<Channel, Channel> pipedChannels = new ConcurrentHashMap<Channel, Channel>();

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = pipeline();

				pipeline.addLast("connect_listener", new SimpleChannelUpstreamHandler() {

					@Override
					public void channelConnected(final ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
						
						allChannelInGroup.add(ctx.getChannel());
						
						ctx.getChannel().setReadable(false);

						ChannelFuture connectFuture = clientBootstrap.connect(remoteServerAddress);

						connectFuture.addListener(new ChannelFutureListener() {
							
							@Override
							public void operationComplete(ChannelFuture future) throws Exception {
								Channel channel = ctx.getChannel();
								
								if (future.isSuccess()) {
									Channel clientChannel = future.getChannel();
									logger.info("pipe: \n{} \n<--> \n{}", channel, clientChannel);
									Utils.bridge(channel, clientChannel);
									channel.setReadable(true);
									pipedChannels.put(channel, clientChannel);
									allChannelInGroup.add(clientChannel);
								} else {
									logger.warn("fail to connect {}", remoteServerAddress);
									channel.close();
								}								
							}
						});

						super.channelConnected(ctx, e);
					}

					@Override
					public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

						Channel pipedChannel = pipedChannels.remove(ctx.getChannel());

						if (pipedChannel != null) {
							pipedChannel.disconnect();
						}

						super.channelDisconnected(ctx, e);
					}
					
					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
						logger.warn("Except caught in " + ctx.getChannel(), e.getCause());
						super.exceptionCaught(ctx, e);
					}
				});

				return pipeline;
			}
		});

		bindChannel = serverBootstrap.bind(localProxyAddress);
		allChannelInGroup.add(bindChannel);
		
		return this;
	}
	
	@Override
	public Proxy stop(){
		
		if (allChannelInGroup != null){
			allChannelInGroup.close().awaitUninterruptibly();
			allChannelInGroup = null;
		}
		
		if (serverBootstrap != null){
			serverBootstrap.releaseExternalResources();
			serverBootstrap = null;
		}
		if (clientBootstrap != null){
			clientBootstrap.releaseExternalResources();
			clientBootstrap = null;
		}
		
		// Shut down thread pools to exit.
		if (executor != null){
			executor.shutdown();
			executor = null;
		}
		
		return this;
	}

	public static void main(String[] args) {
		// Print usage if no argument is specified.
		if (args.length < 4) {
			System.err.println("Usage: <real_host> <real_port> <proxy_host> <proxy_port> ");
			return;
		}

		// Parse options.
		final String realHost = args[0];
		final int realPort = Integer.parseInt(args[1]);
		final String proxyHost = args[2];
		final int proxyPort = Integer.parseInt(args[3]);

		new Proxy(realHost, realPort, proxyHost, proxyPort).start();
	}

}
