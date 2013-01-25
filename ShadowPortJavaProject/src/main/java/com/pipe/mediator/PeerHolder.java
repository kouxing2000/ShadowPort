package com.pipe.mediator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.channel.Channel;

import com.pipe.common.entity.ChannelHolder;
import com.pipe.common.service.PeerType;

public class PeerHolder {
	
	private String peerID;
	private PeerType peerType;
	private Channel signalChannel;
	private String dataConnectionKey;

	/**
	 * <ConnectionID, ChannelHolder>
	 */
	private Map<String, ChannelHolder> allDataChannels = new HashMap<String, ChannelHolder>();
	private Map<String, ChannelHolder> freeDataChannels = new HashMap<String, ChannelHolder>();

	public PeerHolder(String peerID, PeerType peerType, Channel signalChannel, String dataConnectionKey) {
		super();
		this.signalChannel = signalChannel;
		this.peerID = peerID;
		this.peerType = peerType;
		this.dataConnectionKey = dataConnectionKey;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PeerHolder [peerID=").append(peerID).append(", signalChannel=").append(signalChannel)
				.append(", dataConnectionKey=").append(dataConnectionKey).append("]");
		return builder.toString();
	}
	
	public ChannelHolder getConnectionByID(String connectionID){
		return allDataChannels.get(connectionID);
	}

	public String getPeerID() {
		return peerID;
	}

	public PeerType getPeerType() {
		return peerType;
	}

	public Channel getSignalChannel() {
		return signalChannel;
	}

	public String getDataConnectionKey() {
		return dataConnectionKey;
	}

	//TODO using a better policy rather than synchronized
	
	public synchronized boolean recycleConnection(String connectionID) {
		
		ChannelHolder c = getConnectionByID(connectionID);
		
		if (c == null){
			return false;
		}
		
		c.setPipedChannel(null);
		
		freeDataChannels.put(connectionID, c);
		
		return true;
	}
	
	public synchronized boolean notifyConnectionInUse(ChannelHolder connection){
		return freeDataChannels.remove(connection.getConnectionId()) != null;
	}
	
	public synchronized ChannelHolder rentConnection(){
	
		Collection<ChannelHolder> values = freeDataChannels.values();
		if (values.isEmpty()){
			return null;
		}
		ChannelHolder theOne = values.iterator().next();
		freeDataChannels.remove(theOne.getConnectionId());
		return theOne;
	}

	public void addDataConnection(ChannelHolder channelHolder) {
		String connectionId = channelHolder.getConnectionId();
		allDataChannels.put(connectionId, channelHolder);
		freeDataChannels.put(connectionId, channelHolder);
	}

}