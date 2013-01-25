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
	
	public JoinResponse() {
	}

	public JoinResponse(boolean accept, String dataConnectionKey, int minimalDurableDataConnectionNum,
			String dataConnectionHost, int dataConnectionPort) {
		super();
		this.accept = accept;
		this.dataConnectionKey = dataConnectionKey;
		this.minimalDurableDataConnectionNum = minimalDurableDataConnectionNum;
		this.dataConnectionHost = dataConnectionHost;
		this.dataConnectionPort = dataConnectionPort;
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JoinResponse [accept=").append(accept).append(", dataConnectionKey=").append(dataConnectionKey)
				.append(", minimalDurableDataConnectionNum=").append(minimalDurableDataConnectionNum)
				.append(", dataConnectionHost=").append(dataConnectionHost).append(", dataConnectionPort=")
				.append(dataConnectionPort).append("]");
		return builder.toString();
	}
	
}
