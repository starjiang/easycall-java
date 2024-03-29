package com.github.easycall.core.util;
import com.github.easycall.core.exception.EasyException;
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
	private Object body;
	private byte format;
	
	public static EasyPackage newInstance()
	{
		return new EasyPackage();
	}
	
	public EasyPackage()
	{
		format = 0;
	}
	
	public EasyPackage(byte format,EasyHead head, Object body)
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
			body = Utils.msgpack.readTree(bodyStream);

		} else if (getFormat() == FORMAT_JSON){
			InputStream headStream = new ByteBufInputStream(data.slice(10,headLen));
			InputStream bodyStream = new ByteBufInputStream(data.slice(10+headLen,bodyLen));
			head = Utils.json.readValue(headStream,EasyHead.class);
			body = Utils.json.readTree(bodyStream);
		} else{
			throw new EasyException("invalid package format");
		}

		if(head == null || body == null){
			throw new EasyException("head or body is null");
		}

		return this;
	}

	public <T> EasyPackage decode(ByteBuf data,Class<T> valueType) throws Exception
	{
		setFormat(data.getByte(1));

		int headLen = data.getInt(2);
		int bodyLen = data.getInt(6);

		if (getFormat() == FORMAT_MSGPACK){
			InputStream headStream = new ByteBufInputStream(data.slice(10,headLen));
			InputStream bodyStream = new ByteBufInputStream(data.slice(10+headLen,bodyLen));
			head = Utils.msgpack.readValue(headStream,EasyHead.class);
			body = Utils.msgpack.readValue(bodyStream,valueType);

		} else if (getFormat() == FORMAT_JSON){
			InputStream headStream = new ByteBufInputStream(data.slice(10,headLen));
			InputStream bodyStream = new ByteBufInputStream(data.slice(10+headLen,bodyLen));
			head = Utils.json.readValue(headStream,EasyHead.class);
			body = Utils.json.readValue(bodyStream,valueType);
		} else{
			throw new EasyException("invalid package format");
		}

		if(head == null || body == null){
			throw new EasyException("head or body is null");
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
	
	public Object getBody()
	{
		return body;
	}

	public <T> T getBody(Class<T> valueType){
		return Utils.json.convertValue(body,valueType);
	}

	public EasyPackage setBody(Object body)
	{
		this.body = body;
		return this;
	}
	
}
