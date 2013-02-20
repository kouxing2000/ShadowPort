package com.pipe.mediator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;

public class MediatorConfiguration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8962964002992687281L;

	/**
	 * MappingEntry<VirtualPort, RealPort>
	 */
	private List<MappingEntry> portMappings;
	
	private boolean usingSSLForDataConnection = false;

	private String signalHost;
	private int signalPort; 
	private String dataHost; 
	private int dataPort; 
	private String publicDataHost;
	private int publicDataPort = -1;
	private int minimalIdleDataConnections = 5;

	public MediatorConfiguration() {
	}

	public MediatorConfiguration(String signalHost, int signalPort, String dataHost, int dataPort) {
		super();
		this.signalHost = signalHost;
		this.signalPort = signalPort;
		this.dataHost = dataHost;
		this.dataPort = dataPort;
	}
	
	private static final Gson gson = new Gson();

	public MediatorConfiguration addMapping(MappingEntry entry) {
		if (portMappings == null) {
			portMappings = new ArrayList<MappingEntry>();
		}

		portMappings.add(entry);

		return this;
	}

	@Override
	public String toString() {
		return gson.toJson(this).toString();
	}

	/**
	 * 
	 * @return list of MappingEntry<VirtualPort, RealPort>
	 */
	public List<MappingEntry> getPortMappings() {
		if (portMappings == null){
			return Collections.emptyList();
		}
		return new ArrayList<MappingEntry>(portMappings);
	}

	public List<MappingEntry> filterWithPeerId(String peerId) {

		if (peerId == null) {
			return Collections.emptyList();
		}

		List<MappingEntry> result = getPortMappings();
		for (Iterator<MappingEntry> iterator = result.iterator(); iterator.hasNext();) {
			MappingEntry mappingEntry = iterator.next();

			if (peerId.equals(mappingEntry.getVirtualPort().getPeerID())) {
				continue;
			}
			if (peerId.equals(mappingEntry.getRealPort().getPeerID())) {
				continue;
			}
			iterator.remove();
		}
		return result;
	}
	

	public String getSignalHost() {
		return signalHost;
	}

	public MediatorConfiguration setSignalHost(String signalHost) {
		this.signalHost = signalHost;
		return this;
	}

	public int getSignalPort() {
		return signalPort;
	}

	public MediatorConfiguration setSignalPort(int signalPort) {
		this.signalPort = signalPort;
		return this;
	}

	public String getDataHost() {
		return dataHost;
	}

	public MediatorConfiguration setDataHost(String dataHost) {
		this.dataHost = dataHost;
		return this;
	}

	public int getDataPort() {
		return dataPort;
	}

	public MediatorConfiguration setDataPort(int dataPort) {
		this.dataPort = dataPort;
		return this;
	}

	public String getPublicDataHost() {
		return publicDataHost != null ? publicDataHost : dataHost;
	}

	public MediatorConfiguration setPublicDataHost(String publicDataHost) {
		this.publicDataHost = publicDataHost;
		return this;
	}

	public int getPublicDataPort() {
		return publicDataPort > 0 ? publicDataPort : dataPort;
	}

	public MediatorConfiguration setPublicDataPort(int publicDataPort) {
		this.publicDataPort = publicDataPort;
		return this;
	}

	public MediatorConfiguration setPortMappings(List<MappingEntry> portMappings) {
		this.portMappings = portMappings;
		return this;
	}

	public boolean isUsingSSLForDataConnection() {
		return usingSSLForDataConnection;
	}

	public MediatorConfiguration setUsingSSLForDataConnection(boolean usingSSLForDataConnection) {
		this.usingSSLForDataConnection = usingSSLForDataConnection;
		return this;
	}
	
	public int getMinimalIdleDataConnections() {
		return minimalIdleDataConnections;
	}

	public MediatorConfiguration setMinimalIdleDataConnections(int minimalIdleDataConnections) {
		this.minimalIdleDataConnections = minimalIdleDataConnections;
		return this;
	}

}
