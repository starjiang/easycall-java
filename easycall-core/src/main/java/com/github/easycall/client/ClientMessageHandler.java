package com.github.easycall.client;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
public class ClientMessageHandler extends ChannelInboundHandlerAdapter {

    private ClientMessageDispatcher messageDispatcher;
    public ClientMessageHandler(ClientMessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx)
    {
        messageDispatcher.onConnection(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        messageDispatcher.onMessage(ctx,msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
    {
        messageDispatcher.onClose(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        messageDispatcher.onError(ctx,cause);
    }
}