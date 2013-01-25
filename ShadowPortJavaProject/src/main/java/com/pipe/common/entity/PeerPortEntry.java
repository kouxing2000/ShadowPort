package com.pipe.common.entity;

import java.io.Serializable;

public class PeerPortEntry implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5577025056259253861L;
	
	private String peerID;
	
	public String getPeerID() {
		return peerID;
	}

	public void setPeerID(String peerID) {
		this.peerID = peerID;
	}
	private String host;
	private int port;

	public PeerPortEntry() {
	}
	
	public PeerPortEntry(String id, String host, int port) {
		super();
		this.peerID = id;
		this.host = host;
		this.port = port;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PeerPortEntry [peerID=").append(peerID).append(", host=").append(host).append(", port=").append(port)
				.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((peerID == null) ? 0 : peerID.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PeerPortEntry other = (PeerPortEntry) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (peerID == null) {
			if (other.peerID != null)
				return false;
		} else if (!peerID.equals(other.peerID))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
	
	
}
