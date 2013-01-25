package com.pipe.common.entity;

import java.io.Serializable;

public class DataConnectionEntry implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -627496002762416369L;
	
	private String peerID;
	
	public String getPeerID() {
		return peerID;
	}

	public void setPeerID(String peerID) {
		this.peerID = peerID;
	}
	
	private String connectionID;

	public String getConnectionID() {
		return connectionID;
	}

	public void setConnectionID(String connectionID) {
		this.connectionID = connectionID;
	}
	
	public DataConnectionEntry() {
	}

	public DataConnectionEntry(String peerID, String connectionID) {
		super();
		this.peerID = peerID;
		this.connectionID = connectionID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((connectionID == null) ? 0 : connectionID.hashCode());
		result = prime * result + ((peerID == null) ? 0 : peerID.hashCode());
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
		DataConnectionEntry other = (DataConnectionEntry) obj;
		if (connectionID == null) {
			if (other.connectionID != null)
				return false;
		} else if (!connectionID.equals(other.connectionID))
			return false;
		if (peerID == null) {
			if (other.peerID != null)
				return false;
		} else if (!peerID.equals(other.peerID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DataConnectionEntry [peerID=").append(peerID).append(", connectionID=").append(connectionID)
				.append("]");
		return builder.toString();
	}
	
}
