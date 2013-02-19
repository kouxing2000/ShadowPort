package com.pipe.common.net.message;

import java.io.Serializable;

/**
 * send after receive JoinRequest, from medicator to virtual peer, through signal
 * channel
 * 
 * @author Administrator
 *
 */
public class JoinResponse implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1747180946863297219L;

	private boolean accept;
	
	private String dataConnectionKey;
	
	private int minimalDurableDataConnectionNum;
	
	private String dataConnectionHost;
	
	private int dataConnectionPort;
	
	private boolean usingSSLForDataConnection;

	public JoinResponse() {
	}

	public JoinResponse(boolean accept, String dataConnectionKey, int minimalDurableDataConnectionNum,
			String dataConnectionHost, int dataConnectionPort, boolean usingSSLForDataConnection) {
		super();
		this.accept = accept;
		this.dataConnectionKey = dataConnectionKey;
		this.minimalDurableDataConnectionNum = minimalDurableDataConnectionNum;
		this.dataConnectionHost = dataConnectionHost;
		this.dataConnectionPort = dataConnectionPort;
		this.usingSSLForDataConnection = usingSSLForDataConnection;
	}

	public boolean isAccept() {
		return accept;
	}

	public String getDataConnectionKey() {
		return dataConnectionKey;
	}

	public int getMinimalDurableDataConnectionNum() {
		return minimalDurableDataConnectionNum;
	}

	public String getDataConnectionHost() {
		return dataConnectionHost;
	}

	public int getDataConnectionPort() {
		return dataConnectionPort;
	}

	public void setAccept(boolean accept) {
		this.accept = accept;
	}

	public void setDataConnectionKey(String dataConnectionKey) {
		this.dataConnectionKey = dataConnectionKey;
	}

	public void setMinimalDurableDataConnectionNum(int minimalDurableDataConnectionNum) {
		this.minimalDurableDataConnectionNum = minimalDurableDataConnectionNum;
	}

	public void setDataConnectionHost(String dataConnectionHost) {
		this.dataConnectionHost = dataConnectionHost;
	}

	public void setDataConnectionPort(int dataConnectionPort) {
		this.dataConnectionPort = dataConnectionPort;
	}

	public void setUsingSSLForDataConnection(boolean usingSSLForDataConnection) {
		this.usingSSLForDataConnection = usingSSLForDataConnection;
	}

	public boolean isUsingSSLForDataConnection() {
		return usingSSLForDataConnection;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JoinResponse [accept=");
		builder.append(accept);
		builder.append(", dataConnectionKey=");
		builder.append(dataConnectionKey);
		builder.append(", minimalDurableDataConnectionNum=");
		builder.append(minimalDurableDataConnectionNum);
		builder.append(", dataConnectionHost=");
		builder.append(dataConnectionHost);
		builder.append(", dataConnectionPort=");
		builder.append(dataConnectionPort);
		builder.append(", usingSSLForDataConnection=");
		builder.append(usingSSLForDataConnection);
		builder.append("]");
		return builder.toString();
	}
	
}
