package com.github.easycall.service;

import java.net.SocketAddress;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class Request
{
	private int format;
	private ObjectNode head;
	private ObjectNode body;
	private long createTime;
	SocketAddress address;
	
	public Request(int format,SocketAddress address,long createTime,ObjectNode head,ObjectNode body)
	{
		this.address = address;
		this.createTime = createTime;
		this.head = head;
		this.body = body;
		this.format = format;
	}
	
	public long getCreateTime()
	{
		return createTime;
	}
	
	public ObjectNode getHead()
	{
		return head;
	}
	
	public ObjectNode getBody()
	{
		return body;
	}
	
	public SocketAddress getRemoteAddress()
	{
		return address;
	}

	public int getFormat(){
		return format;
	}
}