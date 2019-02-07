/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.easycall.gateway.server;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.gateway.client.TransportFuture;
import com.github.easycall.gateway.client.TransportClient;
import com.github.easycall.gateway.client.TransportPackage;
import com.github.easycall.core.util.EasyPackage;
import com.github.easycall.core.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

/**
 * Handler implementation for the echo server.
 */
@Sharable
public class MessageHandler extends ChannelInboundHandlerAdapter {

	static Logger logger = LoggerFactory.getLogger(MessageHandler.class);
	private TransportClient client;
	private int timeout;
	
	public MessageHandler(TransportClient client,int timeout) {
		this.client = client;
		this.timeout = timeout;
	}
		
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
		ByteBuf buf = (ByteBuf)msg;
		try
		{
			TransportPackage pkg = TransportPackage.newInstance().decode(buf);
			TransportFuture transportFuture = client.asyncRequest(pkg, timeout);

			transportFuture.setCallback(future -> {

				try{
					if(future.isException()){

						EasyPackage respPkg = EasyPackage.newInstance();
						ObjectNode respBody = Utils.json.createObjectNode();
						pkg.getHead().setMsg(future.getException().getMessage());
						pkg.getHead().setRet(EasyPackage.ERROR_SERVER_INTERNAL);
						respPkg.setHead(pkg.getHead()).setBody(respBody);
						ctx.writeAndFlush(respPkg.encode());
						ReferenceCountUtil.release(buf);
						logger.error("req={}",pkg.getHead().toString(),future.getException());

					}else{
						TransportPackage respPkg = future.getResult();
						ctx.writeAndFlush(respPkg.encode());
					}
				}catch (Exception e){
					logger.error(e.getMessage(),e);
				}
			});
    	}
    	catch(Throwable e)
    	{
			ReferenceCountUtil.release(buf);
			throw  e;
    	}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	logger.error("exception:"+cause.getMessage(), cause);
    	ctx.close();
    }
}
