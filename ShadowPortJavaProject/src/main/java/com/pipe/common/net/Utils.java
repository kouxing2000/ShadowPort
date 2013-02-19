package com.pipe.common.net;

import java.util.Arrays;
import java.util.List;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	
	private static final Logger logger = LoggerFactory.getLogger(Utils.class);
	
	public static final void bridge(final Channel channel_a, final Channel channel_b) {
		if (logger.isDebugEnabled()){
			logger.debug("bridge " + channel_a + " - " + channel_b);
		}
		
		channel_b.getPipeline().addLast("bridger", new ProxyHandler(channel_a));
		channel_a.getPipeline().addLast("bridger", new ProxyHandler(channel_b));
		
	}
	
	public static final void addCodec(ChannelPipeline pipeline){
		addDecoder(pipeline);
		addEncoder(pipeline);
	}

	public static final void addEncoder(ChannelPipeline pipeline) {
		// Encoder
		pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		pipeline.addLast("xstreamEncoder", new XStreamEncoder());
	}

	public static final void addDecoder(ChannelPipeline pipeline) {
		// Decoders
		pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
		pipeline.addLast("xstreamDecoder", new XStreamDecoder());
	}
	
	public static final void clearAllHandlers(ChannelPipeline pipeline){
		List<String> names = pipeline.getNames();
		for (String name : names) {
			pipeline.remove(name);
		}
	}
	
	public static final void clearAllHandlersExcept(ChannelPipeline pipeline, String... excludeNames){
		List<String> names = pipeline.getNames();
		for (String name : names) {
			if (Arrays.binarySearch(excludeNames, name) < 0){
				pipeline.remove(name);
			}
		}
	}
}
