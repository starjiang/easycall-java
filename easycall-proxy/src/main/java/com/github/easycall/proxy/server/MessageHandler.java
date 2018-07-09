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
package com.github.easycall.proxy.server;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.proxy.client.ResponseFuture;
import com.github.easycall.proxy.client.TransportClient;
import com.github.easycall.proxy.client.TransportPackage;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;
import io.netty.buffer.ByteBuf;
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
		TransportPackage pkg = TransportPackage.newInstance().decode(buf);

    	try
    	{
			ResponseFuture responseFuture = client.asyncRequest(pkg, timeout);

			responseFuture.setCallback(future -> {

				try{
					if(future.isException()){

						EasyPackage respPkg = EasyPackage.newInstance();
						ObjectNode respBody = Utils.json.createObjectNode();
						respBody.put("msg",future.getException().getMessage());
						respBody.put("ret", EasyPackage.ERROR_SERVER_INTERNAL);
						respPkg.setHead(pkg.getHead()).setBody(respBody);
						ctx.writeAndFlush(respPkg.encode());
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
			EasyPackage respPkg = EasyPackage.newInstance();
			ObjectNode respBody = Utils.json.createObjectNode();
			respBody.put("msg",e.getMessage());
			respBody.put("ret", EasyPackage.ERROR_SERVER_INTERNAL);
			respPkg.setHead(pkg.getHead()).setBody(respBody);
			ctx.writeAndFlush(respPkg.encode());
			buf.release();
	    	logger.error("Exception:"+e.getMessage(),e);
    	}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	logger.error("exception:"+cause.getMessage(), cause);
    	ctx.close();
    }
}
