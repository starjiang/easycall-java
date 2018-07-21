package com.github.easycall.proxy.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.exception.EasyException;
import com.github.easycall.util.EasyHead;
import com.github.easycall.util.Utils;
import io.netty.buffer.*;

import java.io.InputStream;
import java.io.OutputStream;


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

			ByteBuf stxBuf = Unpooled.directBuffer(10);
            ByteBuf headBuf = Unpooled.directBuffer();
            OutputStream headStream = new ByteBufOutputStream(headBuf);
			Utils.msgpack.writeValue(headStream,head);
			ByteBuf etxBuf  = Unpooled.directBuffer(1);

			stxBuf.writeByte(STX);
			stxBuf.writeByte(getFormat());
			stxBuf.writeInt(headBuf.readableBytes());
			stxBuf.writeInt(body.readableBytes());
			etxBuf.writeByte(ETX);

			compBuf.addComponents(true,stxBuf,headBuf,body,etxBuf);

			return compBuf;
		}
		else if(getFormat() == FORMAT_JSON)
		{
            CompositeByteBuf compBuf = Unpooled.compositeBuffer();

            ByteBuf stxBuf = Unpooled.directBuffer(10);
            ByteBuf headBuf = Unpooled.directBuffer();
            OutputStream headStream = new ByteBufOutputStream(headBuf);
            Utils.json.writeValue(headStream,head);
            ByteBuf etxBuf  = Unpooled.directBuffer(1);

            stxBuf.writeByte(STX);
            stxBuf.writeByte(getFormat());
            stxBuf.writeInt(headBuf.readableBytes());
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
            InputStream headStream = new ByteBufInputStream(data.slice(10,headLen));
            head = Utils.msgpack.readValue(headStream,EasyHead.class);
			body = data.slice(10+headLen,bodyLen);


		} else if (getFormat() == FORMAT_JSON){
            InputStream headStream = new ByteBufInputStream(data.slice(10,headLen));
			head = Utils.json.readValue(headStream,EasyHead.class);
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
