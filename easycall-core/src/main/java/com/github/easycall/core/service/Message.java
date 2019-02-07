package com.github.easycall.core.service;
import io.netty.channel.ChannelHandlerContext;

public class Message
{
	private ChannelHandlerContext ctx;
	private Object msg;
	private long createTime;
	
	public Message(ChannelHandlerContext ctx, Object msg) {
		this.ctx = ctx;
		this.msg = msg;
		this.createTime = System.currentTimeMillis();
	}
	
	public ChannelHandlerContext getCtx()
	{
		return ctx;
	}
	public Object getMsg()
	{
		return msg;
	}
	public long getCreateTime()
	{
		return createTime;
	}
	
}
