package com.github.easycall.service;

import java.net.SocketAddress;
import com.fasterxml.jackson.databind.JsonNode;


public class Request
{
	private int format;
	private Throwable cause;
	private JsonNode head;
	private JsonNode body;
	private long createTime;
	SocketAddress address;
	
	public Request(int format,SocketAddress address,long createTime,JsonNode head,JsonNode body)
	{
		this.address = address;
		this.createTime = createTime;
		this.head = head;
		this.body = body;
		this.format = format;
	}
	
	public Request(Throwable cause)
	{
		this.cause = cause;
	}
	
	public long getCreateTime()
	{
		return createTime;
	}
	
	public JsonNode getHead()
	{
		return head;
	}
	
	public Throwable getThrowable()
	{
		return cause;
	}
	
	public JsonNode getBody()
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