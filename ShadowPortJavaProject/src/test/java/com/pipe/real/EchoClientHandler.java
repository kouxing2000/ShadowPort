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

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Handler implementation for the echo client. It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */
public class EchoClientHandler extends SimpleChannelUpstreamHandler {

	private static final Logger logger = Logger.getLogger(EchoClientHandler.class.getName());

	private final ChannelBuffer firstMessage;
	private final AtomicLong transferredBytes = new AtomicLong();
	
	private int firstMessageSize;

	/**
	 * Creates a client-side handler.
	 */
	public EchoClientHandler(int firstMessageSize) {
		if (firstMessageSize <= 0) {
			throw new IllegalArgumentException("firstMessageSize: " + firstMessageSize);
		}
		this.firstMessageSize = firstMessageSize;
		firstMessage = ChannelBuffers.buffer(firstMessageSize);
		for (int i = 0; i < firstMessage.capacity(); i++) {
			firstMessage.writeByte((byte) i);
		}
	}

	public long getTransferredBytes() {
		return transferredBytes.get();
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
		// Send the first message. Server will not send anything here
		// because the firstMessage's capacity is 0.
		e.getChannel().write(firstMessage);
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
		
		if (transferredBytes.get() % firstMessageSize == 0){
			System.out.println("it takes " + (System.currentTimeMillis() - startTime) + " ms for message echo back!");
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			e.getChannel().write(e.getMessage());
			startTime = System.currentTimeMillis();
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		// Close the connection when an exception is raised.
		logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
		e.getChannel().close();
	}
}
