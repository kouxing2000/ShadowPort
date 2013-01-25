package com.pipe.common.net.message;

import java.io.Serializable;

import com.pipe.common.service.PeerType;

/**
 * send after signal connect, from virtual peer to medicator, through signal
 * channel
 * 
 * @author Administrator
 * 
 */
public class JoinRequest implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7514667451070916463L;

	private String clientID;

	private PeerType clientType;

	public JoinRequest() {
	}

	public JoinRequest(String clientID, PeerType clientType) {
		super();
		this.clientID = clientID;
		this.clientType = clientType;
	}

	public String getClientID() {
		return clientID;
	}

	public PeerType getClientType() {
		return clientType;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public void setClientType(PeerType clientType) {
		this.clientType = clientType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JoinRequest [clientID=").append(clientID).append(", clientType=").append(clientType)
				.append("]");
		return builder.toString();
	}

}
