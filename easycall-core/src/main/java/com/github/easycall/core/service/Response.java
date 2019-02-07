package com.github.easycall.core.service;
import com.github.easycall.core.exception.EasyException;
import com.github.easycall.core.util.EasyHead;
import com.github.easycall.core.util.EasyPackage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public class Response
{
    private int format;
	private EasyHead head;
	private Object body;
	private ChannelHandlerContext ctx;
	private Boolean haveFlushed  = false;

    public Response setCtx(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        return this;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public void flush() throws Exception{
        if (ctx!= null){

        	if (haveFlushed) {
        		return;
			}

            EasyPackage respPkg = new EasyPackage((byte) format,head, body);
            ctx.writeAndFlush(Unpooled.wrappedBuffer(respPkg.encode()));
            haveFlushed = true;

        }else{
            throw new EasyException("sync model:the ctx is null,can not call flush()");
        }
    }

    public Response setHead(EasyHead head)
	{
		if (head != null && head.getRet() == null){
			head.setRet(0).setMsg("ok");
		}
		this.head = head;
		return this;
	}
	
	public EasyHead getHead()
	{
		return head;
	}
	
	public Response setBody(Object body)
	{
		this.body = body;
		return this;
	}
	
	public Object getBody()
	{
		return body;
	}
}
