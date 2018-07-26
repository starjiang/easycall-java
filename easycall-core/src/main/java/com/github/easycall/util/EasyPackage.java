package com.github.easycall.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.exception.EasyException;
import io.netty.buffer.*;

import java.io.InputStream;
import java.io.OutputStream;


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
	private EasyHead head;
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
	
	public EasyPackage(byte format,EasyHead head, ObjectNode body)
	{
		this.format = format;
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
			CompositeByteBuf compBuf = PooledByteBufAllocator.DEFAULT.compositeBuffer();

			ByteBuf stxBuf = PooledByteBufAllocator.DEFAULT.directBuffer(10);
			ByteBuf headBuf = PooledByteBufAllocator.DEFAULT.directBuffer();
			ByteBuf bodyBuf = PooledByteBufAllocator.DEFAULT.directBuffer();

			OutputStream headStream = new ByteBufOutputStream(headBuf);
			OutputStream bodyStream = new ByteBufOutputStream(bodyBuf);

			Utils.msgpack.writeValue(headStream,head);
			Utils.msgpack.writeValue(bodyStream,body);

			ByteBuf etxBuf  = PooledByteBufAllocator.DEFAULT.directBuffer(1);

			stxBuf.writeByte(STX);
			stxBuf.writeByte(getFormat());
			stxBuf.writeInt(headBuf.readableBytes());
			stxBuf.writeInt(bodyBuf.readableBytes());
			etxBuf.writeByte(ETX);

			compBuf.addComponents(true,stxBuf,headBuf,bodyBuf,etxBuf);

			return compBuf;
		}
		else if(getFormat() == FORMAT_JSON)
		{
			CompositeByteBuf compBuf = PooledByteBufAllocator.DEFAULT.compositeBuffer();

			ByteBuf stxBuf = PooledByteBufAllocator.DEFAULT.directBuffer(10);
			ByteBuf headBuf = PooledByteBufAllocator.DEFAULT.directBuffer();
			ByteBuf bodyBuf = PooledByteBufAllocator.DEFAULT.directBuffer();

			OutputStream headStream = new ByteBufOutputStream(headBuf);
			OutputStream bodyStream = new ByteBufOutputStream(bodyBuf);

			Utils.json.writeValue(headStream,head);
			Utils.json.writeValue(bodyStream,body);

			ByteBuf etxBuf  = PooledByteBufAllocator.DEFAULT.directBuffer(1);

			stxBuf.writeByte(STX);
			stxBuf.writeByte(getFormat());
			stxBuf.writeInt(headBuf.readableBytes());
			stxBuf.writeInt(bodyBuf.readableBytes());
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
			InputStream headStream = new ByteBufInputStream(data.slice(10,headLen));
			InputStream bodyStream = new ByteBufInputStream(data.slice(10+headLen,bodyLen));
			head = Utils.msgpack.readValue(headStream,EasyHead.class);
			body = Utils.msgpack.readValue(bodyStream,ObjectNode.class);

		} else if (getFormat() == FORMAT_JSON){
			InputStream headStream = new ByteBufInputStream(data.slice(10,headLen));
			InputStream bodyStream = new ByteBufInputStream(data.slice(10+headLen,bodyLen));
			head = Utils.json.readValue(headStream,EasyHead.class);
			body = Utils.json.readValue(bodyStream,ObjectNode.class);
		} else{
			throw new EasyException("invalid package format");
		}

		return this;
	}
	
	public EasyHead getHead() {
		return head;
	}
	
	public EasyPackage setHead(EasyHead head)
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
