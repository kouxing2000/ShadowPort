package com.pipe.common.net.message;

import java.io.Serializable;

/**
 * send after real client/server disconnect with virtual server/client, from
 * virtual peer to medicator then to piped peer, through signal channel
 * 
 * @author Administrator
 * 
 */
public class DataConnectionReleasedMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 996377394449068308L;

	private String connectionId;

	public DataConnectionReleasedMessage() {
	}

	public DataConnectionReleasedMessage(String connectionId) {
		super();
		this.connectionId = connectionId;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DataConnectionReleasedMessage [connectionId=").append(connectionId).append("]");
		return builder.toString();
	}

}
