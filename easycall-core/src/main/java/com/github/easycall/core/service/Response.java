package com.github.easycall.core.service;
import com.github.easycall.core.util.EasyHead;

public class Response
{
	private EasyHead head;
	private Object body;

	public static Response newInstance(){
		return new Response();
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
