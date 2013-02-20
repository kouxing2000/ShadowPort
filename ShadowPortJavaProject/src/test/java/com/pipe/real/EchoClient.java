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

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslHandler;

import com.pipe.common.net.ssl.PipeSslContextFactory;
import com.pipe.common.service.Service;

/**
 * Sends one message when a connection is open and echoes back any received data
 * to the server. Simply put, the echo client initiates the ping-pong traffic
 * between the echo client and server by sending the first message to the
 * server.
 */
public class EchoClient implements Service {

	private static final Logger logger = Logger.getLogger(EchoClient.class.getName());

	private final String host;
	private final int port;
	private final int firstMessageSize;
	private final ChannelBuffer firstMessage;
	private int totalSendNum = 20;

	public EchoClient setTotalSendNum(int totalSendNum) {
		this.totalSendNum = totalSendNum;
		return this;
	}

	public int getTotalSendNum() {
		return totalSendNum;
	}

	public EchoClient(String host, int port, int firstMessageSize) {
		this.host = host;
		this.port = port;
		this.firstMessageSize = firstMessageSize;
		firstMessage = ChannelBuffers.buffer(firstMessageSize);
		for (int i = 0; i < firstMessage.capacity(); i++) {
			firstMessage.writeByte((byte) i);
		}
	}

	private boolean usingSSL;

	public EchoClient setUsingSSL(boolean usingSSL) {
		this.usingSSL = usingSSL;
		return this;
	}
	
	private int sendCounter = 0;
	
	private boolean success = false;

	public boolean isSuccess() {
		return success;
	}
	
	private ClientBootstrap bootstrap;

	@Override
	public EchoClient start() {
		
		sendCounter = 0;
		
		success = false;
				
		// Configure the client.
		bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
				Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {

				ChannelPipeline pipeline = Channels.pipeline();

				if (usingSSL) {
					SSLEngine engine = PipeSslContextFactory.getClientContext().createSSLEngine();
					engine.setUseClientMode(true);
					pipeline.addLast("ssl", new SslHandler(engine));
				}

				pipeline.addLast("echo", new SimpleChannelUpstreamHandler() {

					private final AtomicLong transferredBytes = new AtomicLong();

					@Override
					public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e)
							throws Exception {
						if (usingSSL) {
							// Get the SslHandler from the pipeline
							// which were added in SecureChatPipelineFactory.
							SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);

							// Begin handshake.
							ChannelFuture handshakeFuture = sslHandler.handshake();
							handshakeFuture.addListener(new ChannelFutureListener() {

								@Override
								public void operationComplete(ChannelFuture future) throws Exception {
									if (future.isSuccess()) {
										logger.info("handshake success");

										startSend(ctx, e);
									} else {
										future.getChannel().close();
									}
								}

							});
						} else {
							startSend(ctx, e);
						}

					}

					private void startSend(final ChannelHandlerContext ctx, final ChannelStateEvent e) {
						// Send the first message. Server will not
						// send anything here
						// because the firstMessage's capacity is 0.
						e.getChannel().write(firstMessage);
						sendCounter++;
						startTime = System.currentTimeMillis();
						System.out.println(ctx.getChannel() + " send first message " + firstMessage);
					}

					private long startTime = 0;

					@Override
					public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {

						// TODO verify back message

						// Send back the received message to the remote peer.
						transferredBytes.addAndGet(((ChannelBuffer) e.getMessage()).readableBytes());

						System.out.println("Client " + ctx.getChannel() + " - Count:" + transferredBytes.get());

						if (transferredBytes.get() % firstMessageSize == 0) {
							System.out.println("it takes " + (System.currentTimeMillis() - startTime)
									+ " ms for message echo back!");

							try {
								Thread.sleep(1000);
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
							
							if (sendCounter >= totalSendNum){
								
								logger.info("test success!");
								
								success = true;
								
								ctx.getChannel().close();
								
							} else {
								
								e.getChannel().write(e.getMessage());
								sendCounter++;
							}

							startTime = System.currentTimeMillis();
						}

					}

					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
						// Close the connection when an exception is raised.
						logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
						e.getChannel().close();
					}
				});
				return pipeline;
			}
		});

		// Start the connection attempt.
		ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));

		return this;
	}
	
	@Override
	public EchoClient stop(){
		// Shut down thread pools to exit.
		if (bootstrap != null){
			bootstrap.releaseExternalResources();
			bootstrap = null;
		}
		
		return this;
	}

	public static void main(String[] args) throws Exception {
		// Print usage if no argument is specified.
		if (args.length < 2 || args.length > 3) {
			System.err.println("Usage: " + EchoClient.class.getSimpleName() + " <host> <port> [<first message size>]");
			return;
		}

		// Parse options.
		final String host = args[0];
		final int port = Integer.parseInt(args[1]);
		final int firstMessageSize;
		if (args.length == 3) {
			firstMessageSize = Integer.parseInt(args[2]);
		} else {
			firstMessageSize = 256;
		}

		new EchoClient(host, port, firstMessageSize).start();
	}
}
