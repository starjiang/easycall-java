package com.github.easycall.proxy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.exception.EasyException;
import com.github.easycall.util.EasyHead;
import com.github.easycall.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;


public class TransportPackage {

	final public static byte STX = 0x2;
	final public static byte ETX = 0x3;
	final public static byte FORMAT_MSGPACK = 0;
	final public static byte FORMAT_JSON = 1;
	private EasyHead head;
	private ByteBuf body;
	private byte format;
	
	public static TransportPackage newInstance()
	{
		return new TransportPackage();
	}
	
	public TransportPackage()
	{
		format = 0;
	}
	
	public TransportPackage(byte format,EasyHead head, ByteBuf body)
	{
	    this.format = format;
		this.head = head;
		this.body = body;
	}

	public byte getFormat() {
		return format;
	}

	public TransportPackage setFormat(byte format) {
		this.format = format;
		return this;
	}
	
	public ByteBuf encode() throws Exception
	{
		if(getFormat() == FORMAT_MSGPACK)
		{
			CompositeByteBuf compBuf = Unpooled.compositeBuffer();

			ByteBuf stxBuf = Unpooled.buffer(10);

			byte[] headBytes = Utils.msgpack.writeValueAsBytes(head);
			ByteBuf headBuf = Unpooled.wrappedBuffer(headBytes);
			ByteBuf etxBuf  = Unpooled.buffer(1);


			stxBuf.writeByte(STX);
			stxBuf.writeByte(getFormat());
			stxBuf.writeInt(headBytes.length);
			stxBuf.writeInt(body.readableBytes());
			etxBuf.writeByte(ETX);

			compBuf.addComponents(true,stxBuf,headBuf,body,etxBuf);

			return compBuf;
		}
		else if(getFormat() == FORMAT_JSON)
		{
			CompositeByteBuf compBuf = Unpooled.compositeBuffer();

			ByteBuf stxBuf = Unpooled.buffer(10);

			byte[] headBytes = Utils.json.writeValueAsBytes(head);
			ByteBuf headBuf = Unpooled.wrappedBuffer(headBytes);
			ByteBuf etxBuf  = Unpooled.buffer(1);

            stxBuf.writeByte(STX);
            stxBuf.writeByte(getFormat());
            stxBuf.writeInt(headBytes.length);
            stxBuf.writeInt(body.readableBytes());
			etxBuf.writeByte(ETX);

			compBuf.addComponents(true,stxBuf,headBuf,body,etxBuf);

			return compBuf;
		}
		else 
		{
			throw new EasyException("invalid package format");
		}

	}

	public TransportPackage decode(ByteBuf data) throws Exception{

		setFormat(data.getByte(1));

		int headLen = data.getInt(2);
		int bodyLen = data.getInt(6);

		if (getFormat() == FORMAT_MSGPACK){
			byte [] headBytes = new byte[headLen];
			data.getBytes(10,headBytes);
			head = Utils.msgpack.readValue(headBytes, EasyHead.class);
			body = data.slice(10+headLen,bodyLen);


		} else if (getFormat() == FORMAT_JSON){
			byte [] headBytes = new byte[headLen];
			data.getBytes(10,headBytes);
			head = Utils.json.readValue(headBytes,EasyHead.class);
			body = data.slice(10+headLen,bodyLen);
		} else{
			throw new EasyException("invalid package format");
		}
		return this;
	}
	
	public EasyHead getHead() {
		return head;
	}
	
	public TransportPackage setHead(EasyHead head)
	{
		this.head = head;
		return this;
	}
	
	public ByteBuf getBody()
	{
		return body;
	}
	
	public TransportPackage setBody(ByteBuf body)
	{
		this.body = body;
		return this;
	}
	
}
