package com.github.easycall.service;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;

public class ServerInfo {
	public String serviceName;
	public int port;
	public ChannelFuture future;
	public WorkerPool pool;
	public ServerInfo(String serviceName,ChannelFuture future,int port,WorkerPool pool)
	{
		this.serviceName = serviceName;
		this.port = port;
		this.future = future;
		this.pool = pool;
	}
	
	
}
