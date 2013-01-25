package com.pipe.common.entity;

import org.jboss.netty.channel.Channel;

public class ChannelHolder {

	private Channel channel;
	
	private String connectionId;
	
	public ChannelHolder(String connectionId, Channel channel) {
		super();
		this.channel = channel;
		this.connectionId = connectionId;
	}

	private Channel pipedChannel;

	public Channel getPipedChannel() {
		return pipedChannel;
	}

	public void setPipedChannel(Channel pipedChannel) {
		this.pipedChannel = pipedChannel;
	}

	public Channel getChannel() {
		return channel;
	}

	public String getConnectionId() {
		return connectionId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ChannelHolder [connectionId=").append(connectionId).append(", channel=").append(channel)
				.append(", pipedChannel=").append(pipedChannel).append("]");
		return builder.toString();
	}
	
}
