package com.pipe.common.net.message;

import java.io.Serializable;

/**
 * 
 * 
 * @author Administrator
 *
 */
public class OpenVirtualPortMessage implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8204865816551504058L;
	
	public OpenVirtualPortMessage() {
	}
	
	private String virtualHost;
	private int virtualPort;
	
	private String realHost;
	private int realPort;

	public OpenVirtualPortMessage(String virtualHost, int virtualPort, String realHost, int realPort) {
		super();
		this.virtualHost = virtualHost;
		this.virtualPort = virtualPort;
		this.realHost = realHost;
		this.realPort = realPort;
	}

	public String getVirtualHost() {
		return virtualHost;
	}

	public void setVirtualHost(String virtualHost) {
		this.virtualHost = virtualHost;
	}

	public int getVirtualPort() {
		return virtualPort;
	}

	public void setVirtualPort(int virtualPort) {
		this.virtualPort = virtualPort;
	}

	public String getRealHost() {
		return realHost;
	}

	public void setRealHost(String realHost) {
		this.realHost = realHost;
	}

	public int getRealPort() {
		return realPort;
	}

	public void setRealPort(int realPort) {
		this.realPort = realPort;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("OpenVirtualPortMessage [virtualHost=").append(virtualHost).append(", virtualPort=")
				.append(virtualPort).append(", realHost=").append(realHost).append(", realPort=")
				.append(realPort).append("]");
		return builder.toString();
	}
	
}
