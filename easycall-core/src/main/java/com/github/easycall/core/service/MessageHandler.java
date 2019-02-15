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
package com.github.easycall.core.service;

import com.github.easycall.core.util.EasyPackage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

import java.lang.reflect.Method;
import java.util.Map;

@Sharable
public class MessageHandler extends ChannelInboundHandlerAdapter {

	private MessageDispatcher syncDispatcher;
	private MessageDispatcher asyncDispatcher;
	private Map<String,Method> methodMap;
	
	public MessageHandler(MessageDispatcher syncDispatcher,MessageDispatcher asyncDispatcher,Map<String,Method> methodMap) {
		this.syncDispatcher = syncDispatcher;
		this.asyncDispatcher = asyncDispatcher;
		this.methodMap = methodMap;
	}
		
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
    {
        EasyPackage pkg = (EasyPackage)msg;
        Method method = methodMap.get(pkg.getHead().getMethod());

        if (method != null && method.getReturnType().getName().equals("java.util.concurrent.CompletableFuture")){
            asyncDispatcher.dispatch(new Message(ctx, msg));
        }else{
            syncDispatcher.dispatch(new Message(ctx, msg));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	asyncDispatcher.dispatch(new Message(ctx, cause));
    }
}
