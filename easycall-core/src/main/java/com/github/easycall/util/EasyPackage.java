package com.github.easycall.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.exception.EasyException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;


public class EasyPackage {

	final public static byte STX = 0x2;
	final public static byte ETX = 0x3;
	final public static byte FORMAT_MSGPACK = 0;
	final public static byte FORMAT_JSON = 1;
	final public static int HEAD_MAX_LEN = 64*1024;
	final public static int BODY_MAX_LEN = 2*1024*1024;

	final public static int ERROR_TIME_OUT = 1001;
	final public static int ERROR_SERVER_INTERNAL = 1002;
	final public static int ERROR_METHOD_NOT_FOUND = 1003;
	private ObjectNode head;
	private ObjectNode body;
	private byte format;
	
	public static EasyPackage newInstance()
	{
		return new EasyPackage();
	}
	
	public EasyPackage()
	{
		format = 0;
	}
	
	public EasyPackage(ObjectNode head, ObjectNode body)
	{
		this.head = head;
		this.body = body;
	}

	public byte getFormat() {
		return format;
	}

	public EasyPackage setFormat(byte format) {
		this.format = format;
		return this;
	}
	
	public ByteBuf encode() throws Exception
	{
		if(head == null || body == null){
			throw new EasyException("head or body is null");
		}

		if(getFormat() == FORMAT_MSGPACK)
		{
			CompositeByteBuf compBuf = Unpooled.compositeBuffer();

			ByteBuf stxBuf = Unpooled.buffer(10);

			byte[] headBytes = Utils.msgpack.writeValueAsBytes(head);
			byte[] bodyBytes = Utils.msgpack.writeValueAsBytes(body);
			ByteBuf headBuf = Unpooled.wrappedBuffer(headBytes);
			ByteBuf bodyBuf = Unpooled.wrappedBuffer(bodyBytes);
			ByteBuf etxBuf  = Unpooled.buffer(1);


			stxBuf.writeByte(STX);
			stxBuf.writeByte(getFormat());
			stxBuf.writeInt(headBytes.length);
			stxBuf.writeInt(bodyBytes.length);
			etxBuf.writeByte(ETX);

			compBuf.addComponents(true,stxBuf,headBuf,bodyBuf,etxBuf);

			return compBuf;
		}
		else if(getFormat() == FORMAT_JSON)
		{
			CompositeByteBuf compBuf = Unpooled.compositeBuffer();

			ByteBuf stxBuf = Unpooled.buffer(10);

			byte[] headBytes = Utils.json.writeValueAsBytes(head);
			byte[] bodyBytes = Utils.json.writeValueAsBytes(body);
			ByteBuf headBuf = Unpooled.wrappedBuffer(headBytes);
			ByteBuf bodyBuf = Unpooled.wrappedBuffer(bodyBytes);
			ByteBuf etxBuf  = Unpooled.buffer(1);

			stxBuf.writeByte(STX);
			stxBuf.writeByte(getFormat());
			stxBuf.writeInt(headBytes.length);
			stxBuf.writeInt(bodyBytes.length);
			etxBuf.writeByte(ETX);

			compBuf.addComponents(true,stxBuf,headBuf,bodyBuf,etxBuf);

			return compBuf;
		}
		else 
		{
			throw new EasyException("invalid package format");
		}

	}
		
	public EasyPackage decode(ByteBuf data) throws Exception
	{
		setFormat(data.getByte(1));

		int headLen = data.getInt(2);
		int bodyLen = data.getInt(6);

		if (getFormat() == FORMAT_MSGPACK){
			byte [] headBytes = new byte[headLen];
			byte [] bodyBytes = new byte[bodyLen];
			data.getBytes(10,headBytes);
			data.getBytes(10+headLen,bodyBytes);
			head = Utils.msgpack.readValue(headBytes,ObjectNode.class);
			body = Utils.msgpack.readValue(bodyBytes,ObjectNode.class);

		} else if (getFormat() == FORMAT_JSON){
			byte [] headBytes = new byte[headLen];
			byte [] bodyBytes = new byte[bodyLen];
			data.getBytes(10,headBytes);
			data.getBytes(10+headLen,bodyBytes);
			head = Utils.json.readValue(headBytes,ObjectNode.class);
			body = Utils.json.readValue(bodyBytes,ObjectNode.class);
		} else{
			throw new EasyException("invalid package format");
		}

		return this;
	}
	
	public ObjectNode getHead() {
		return head;
	}
	
	public EasyPackage setHead(ObjectNode head)
	{
		this.head = head;
		return this;
	}
	
	public ObjectNode getBody()
	{
		return body;
	}
	
	public EasyPackage setBody(ObjectNode body)
	{
		this.body = body;
		return this;
	}
	
}
