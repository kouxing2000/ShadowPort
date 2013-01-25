package com.pipe.common.net;

import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

public class XStreamDecoder extends OneToOneDecoder {
	private static final Logger logger = LoggerFactory.getLogger(XStreamDecoder.class);

	private static final Charset charset = Charset.forName("UTF-8");

	private final XStream xstream = new XStream();

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
		if (!(msg instanceof ChannelBuffer)) {
			return msg;
		}

		ChannelBuffer channelBuffer = (ChannelBuffer) msg;
		
		if (logger.isDebugEnabled()){
			logger.debug("decode from [{}]", ChannelBuffers.hexDump(channelBuffer));
		}
		
		String content = channelBuffer.toString(charset);

		try{
			
			Object result = xstream.fromXML(content);
			
			if (logger.isDebugEnabled()){
				logger.debug("decode result: [{}]", result);
			}
			
			return result;
		
		}catch (Exception e) {
			logger.error("failed to decode " + ChannelBuffers.hexDump(channelBuffer));
			return null;
		}
	}

}
