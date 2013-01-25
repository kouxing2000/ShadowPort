package com.pipe.common.net.message;

import java.io.Serializable;

/**
 * after data connection build up, from virtual peer to medicator, through data channel
 * 
 * @author Administrator
 *
 */
public class DataConnectionRegisterMessage implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4677246527818724185L;

	private String dataConnectionKey;
	
	private String connectionId;

	public DataConnectionRegisterMessage() {
	}
	
	public DataConnectionRegisterMessage(String dataConnectionKey, String connectionId) {
		super();
		this.dataConnectionKey = dataConnectionKey;
		this.connectionId = connectionId;
	}

	public String getDataConnectionKey() {
		return dataConnectionKey;
	}

	public void setDataConnectionKey(String dataConnectionKey) {
		this.dataConnectionKey = dataConnectionKey;
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
		builder.append("DataConnectionRegisterMessage [dataConnectionKey=").append(dataConnectionKey)
				.append(", connectionId=").append(connectionId).append("]");
		return builder.toString();
	}
	
}
