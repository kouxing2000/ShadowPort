package com.pipe.common.net;

import java.io.Serializable;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pipe.mediator.Mediator;
import com.thoughtworks.xstream.XStream;

public class XStreamEncoder extends OneToOneEncoder{
	private static final Logger logger = LoggerFactory.getLogger(XStreamEncoder.class);
	
	private static final Charset charset = Charset.forName("UTF-8");
	
	private final XStream xstream = new XStream();
	
	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		
		if (msg instanceof Serializable){
			String content = xstream.toXML(msg);
			
			ChannelBuffer buffer = ChannelBuffers.copiedBuffer(content, charset);
			
			if (logger.isDebugEnabled()){
				logger.debug("encode to [{}]", ChannelBuffers.hexDump(buffer));
			}
			
			return buffer;
		}
		
		return msg;
	}

}
