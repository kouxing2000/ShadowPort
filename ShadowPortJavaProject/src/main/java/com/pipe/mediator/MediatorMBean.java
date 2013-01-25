package com.pipe.mediator;

public interface MediatorMBean {

	boolean match(String virtualPortJson, String realPortJson);
	
	String verbose();
	
}
