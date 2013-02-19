/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.pipe.real;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslHandler;

import com.pipe.common.net.ssl.PipeSslContextFactory;

/**
 * Echoes back any received data from a client.
 */
public class EchoServer {

	private static final Logger logger = Logger.getLogger(EchoServer.class.getName());

	private final String host;

	private final int port;

	public EchoServer(String host, int port) {
		this.host = host;
		this.port = port;
	}

	private boolean usingSSL;

	public EchoServer setUsingSSL(boolean usingSSL) {
		this.usingSSL = usingSSL;
		return this;
	}

	public void run() {
		// Configure the server.
		ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				
				if (usingSSL) {
					SSLEngine engine = PipeSslContextFactory.getServerContext().createSSLEngine();
					engine.setUseClientMode(false);
					pipeline.addLast("ssl", new SslHandler(engine));
				}
				
				pipeline.addLast("echo", new SimpleChannelUpstreamHandler() {

					private final AtomicLong transferredBytes = new AtomicLong();

					@Override
					public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {

						if (usingSSL) {
							// Get the SslHandler in the current pipeline.
							// We added it in SecureChatPipelineFactory.
							final SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);

							// Get notified when SSL handshake is done.
							ChannelFuture handshakeFuture = sslHandler.handshake();
							handshakeFuture.addListener(new ChannelFutureListener() {

								@Override
								public void operationComplete(ChannelFuture future) throws Exception {
									if (future.isSuccess()) {
										logger.info("handshake success");
									} else {
										future.getChannel().close();
									}
								}
							});
						}
					}

					@Override
					public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
						// Send back the received message to the remote peer.
						transferredBytes.addAndGet(((ChannelBuffer) e.getMessage()).readableBytes());

						System.out.println("Server " + ctx.getChannel() + " - Count:" + transferredBytes.get());

						e.getChannel().write(e.getMessage());
					}

					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
						// Close the connection when an exception is raised.
						logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
						e.getChannel().close();
					}

					@Override
					public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
						System.out.println("childChannelOpen " + e.getChildChannel());
						super.childChannelOpen(ctx, e);
					}

					@Override
					public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
						System.out.println("channelOpen " + e.getChannel());
						super.channelOpen(ctx, e);
					}

				});
				return pipeline;
			}
		});

		// Bind and start to accept incoming connections.
		bootstrap.bind(new InetSocketAddress(host, port));
	}

	public static void main(String[] args) throws Exception {
		String host;
		int port;
		if (args.length > 1) {
			host = args[0];
			port = Integer.parseInt(args[1]);
		} else {
			host = "localhost";
			port = 8080;
		}
		new EchoServer(host, port).run();
	}
}
