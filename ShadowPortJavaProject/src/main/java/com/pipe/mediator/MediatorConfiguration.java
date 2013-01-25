package com.pipe.mediator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MediatorConfiguration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8962964002992687281L;

	/**
	 * MappingEntry<VirtualPort, RealPort>
	 */
	private List<MappingEntry> portMappings;

	public MediatorConfiguration() {
	}

	public MediatorConfiguration addMapping(MappingEntry entry) {
		if (portMappings == null) {
			portMappings = new ArrayList<MappingEntry>();
		}

		portMappings.add(entry);

		return this;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MediatorConfiguration [portMappings=").append(portMappings).append("]");
		return builder.toString();
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
}
