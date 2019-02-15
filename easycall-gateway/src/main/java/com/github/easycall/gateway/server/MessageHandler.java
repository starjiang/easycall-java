package com.github.easycall.gateway.server;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.gateway.client.TransportFuture;
import com.github.easycall.gateway.client.TransportClient;
import com.github.easycall.gateway.client.TransportPackage;
import com.github.easycall.core.util.EasyPackage;
import com.github.easycall.core.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class MessageHandler extends ChannelInboundHandlerAdapter {

	static Logger logger = LoggerFactory.getLogger(MessageHandler.class);
	private TransportClient client;
	private int timeout;
	
	public MessageHandler(TransportClient client,int timeout) {
		this.client = client;
		this.timeout = timeout;
	}
		
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
		ByteBuf buf = (ByteBuf)msg;
		try
		{
			TransportPackage pkg = TransportPackage.newInstance().decode(buf);
			TransportFuture transportFuture = client.asyncRequest(pkg, timeout);

			transportFuture.setCallback(future -> {

				try{
					if(future.isException()){

						EasyPackage respPkg = EasyPackage.newInstance();
						ObjectNode respBody = Utils.json.createObjectNode();
						pkg.getHead().setMsg(future.getException().getMessage());
						pkg.getHead().setRet(EasyPackage.ERROR_SERVER_INTERNAL);
						respPkg.setHead(pkg.getHead()).setBody(respBody);
						ctx.writeAndFlush(respPkg.encode());
						ReferenceCountUtil.release(buf);
						logger.error("req={}",pkg.getHead().toString(),future.getException());

					}else{
						TransportPackage respPkg = future.getResult();
						ctx.writeAndFlush(respPkg.encode());
					}
				}catch (Exception e){
					logger.error(e.getMessage(),e);
				}
			});
    	}
    	catch(Throwable e)
    	{
			ReferenceCountUtil.release(buf);
			throw  e;
    	}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	logger.error("exception:"+cause.getMessage(), cause);
    	ctx.close();
    }
}
