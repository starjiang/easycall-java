package com.github.easycall.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.exception.EasyException;
import com.github.easycall.util.EasyPackage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public class Response
{
    private int format;
	private JsonNode head;
	private JsonNode body;
	private ChannelHandlerContext ctx;

    public Response setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        return this;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public void flush() throws Exception{
        if (ctx!= null){

            EasyPackage respPkg = new EasyPackage(getHead(), getBody());
            respPkg.setFormat((byte) format);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(respPkg.encode()));

        }else{
            throw new EasyException("sync model ctx is null,call flush function not used");
        }
    }

    public Response setHead(JsonNode head)
	{
		this.head = head;
		return this;
	}
	
	public JsonNode getHead()
	{
		return head;
	}
	
	public Response setBody(ObjectNode body)
	{
		this.body = body;
		return this;
	}
	
	public JsonNode getBody()
	{
		return body;
	}
}
