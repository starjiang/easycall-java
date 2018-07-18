package com.github.easycall.client;
import io.netty.channel.ChannelHandlerContext;

public interface ClientMessageDispatcher {

    public void onConnection(ChannelHandlerContext ctx);
    public void onClose(ChannelHandlerContext ctx);
    public void onMessage(ChannelHandlerContext ctx, Object msg);
    public void onError(ChannelHandlerContext ctx,Throwable cause);
    public void onTimeout(String name,long sessionId);
}
