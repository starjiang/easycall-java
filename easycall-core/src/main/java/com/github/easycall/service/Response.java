package com.github.easycall.service;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.exception.EasyException;
import com.github.easycall.util.EasyHead;
import com.github.easycall.util.EasyPackage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public class Response
{
    private int format;
	private EasyHead head;
	private ObjectNode body;
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

            EasyPackage respPkg = new EasyPackage((byte) format,head, body);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(respPkg.encode()));

        }else{
            throw new EasyException("sync model ctx is null,do not call flush");
        }
    }

    public Response setHead(EasyHead head)
	{
		this.head = head;
		return this;
	}
	
	public EasyHead getHead()
	{
		return head;
	}
	
	public Response setBody(ObjectNode body)
	{
		this.body = body;
		return this;
	}
	
	public ObjectNode getBody()
	{
		return body;
	}
}
