package com.github.easycall.service;

import java.net.SocketAddress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.util.EasyHead;
import com.github.easycall.util.Utils;


public class Request
{
	private int format;
	private EasyHead head;
	private JsonNode body;
	private long createTime;
	SocketAddress address;
	
	public Request(int format,SocketAddress address,long createTime,EasyHead head,JsonNode body)
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
	
	public EasyHead getHead()
	{
		return head;
	}
	
	public JsonNode getBody()
	{
		return body;
	}

	public <T> T getBody(Class<T> valueType){
		return Utils.json.convertValue(body,valueType);
	}
	
	public SocketAddress getRemoteAddress()
	{
		return address;
	}

	public int getFormat(){
		return format;
	}
}