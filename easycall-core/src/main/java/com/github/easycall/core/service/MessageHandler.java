package com.github.easycall.core.service;
import com.github.easycall.core.util.EasyPackage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

import java.lang.reflect.Method;
import java.util.Map;

@Sharable
public class MessageHandler extends ChannelInboundHandlerAdapter {

	private MessageDispatcher syncDispatcher;
	private MessageDispatcher asyncDispatcher;
	private Map<String,Boolean> asyncMap;
	
	public MessageHandler(MessageDispatcher syncDispatcher,MessageDispatcher asyncDispatcher,Map<String,Boolean> asyncMap) {
		this.syncDispatcher = syncDispatcher;
		this.asyncDispatcher = asyncDispatcher;
		this.asyncMap = asyncMap;


	}
		
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        EasyPackage pkg = (EasyPackage)msg;

        if (asyncMap.get(pkg.getHead().getMethod()) != null){
            asyncDispatcher.dispatch(new Message(ctx, msg));
        }else{
            syncDispatcher.dispatch(new Message(ctx, msg));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	asyncDispatcher.dispatch(new Message(ctx, cause));
    }
}
