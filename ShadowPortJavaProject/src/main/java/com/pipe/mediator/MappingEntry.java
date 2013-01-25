package com.pipe.mediator;

import java.io.Serializable;

import com.pipe.common.entity.PeerPortEntry;

public class MappingEntry implements Serializable{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 483244237890050363L;
	
	private PeerPortEntry virtualPort;
	private PeerPortEntry realPort;
	
	public MappingEntry() {
	}
	
	public MappingEntry(PeerPortEntry virtualPort, PeerPortEntry realPort) {
		super();
		this.virtualPort = virtualPort;
		this.realPort = realPort;
	}

	public PeerPortEntry getVirtualPort() {
		return virtualPort;
	}

	public PeerPortEntry getRealPort() {
		return realPort;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((realPort == null) ? 0 : realPort.hashCode());
		result = prime * result + ((virtualPort == null) ? 0 : virtualPort.hashCode());
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
		MappingEntry other = (MappingEntry) obj;
		if (realPort == null) {
			if (other.realPort != null)
				return false;
		} else if (!realPort.equals(other.realPort))
			return false;
		if (virtualPort == null) {
			if (other.virtualPort != null)
				return false;
		} else if (!virtualPort.equals(other.virtualPort))
			return false;
		return true;
	}
	
	

}