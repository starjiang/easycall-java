/*
 * Copyright 2013 The Netty Project
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

import java.net.URI;
import java.util.Iterator;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.proxy.client.ResponseFuture;
import com.github.easycall.proxy.client.TransportClient;
import com.github.easycall.proxy.client.TransportPackage;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class HttpHandler extends ChannelInboundHandlerAdapter {

	static Logger logger = LoggerFactory.getLogger(HttpHandler.class);
    private TransportClient client;
    private int timeout;

	public HttpHandler(TransportClient client, int timeout) {
		this.client = client;
	    this.timeout = timeout;
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {

		try {
			if (msg instanceof FullHttpRequest) {
				final FullHttpRequest req = (FullHttpRequest) msg;

				URI uri = new URI(req.getUri());

				if (!uri.getPath().equals("/call")) {
					ctx.writeAndFlush(
							new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND,
									Unpooled.wrappedBuffer("{\"msg\":\"page not found\",\"ret\":1002}"
											.getBytes()))).addListener(
							ChannelFutureListener.CLOSE);
					return;
				}

				if(!req.method().name().equals("POST")){
                    ctx.writeAndFlush(
                            new DefaultFullHttpResponse(HTTP_1_1, METHOD_NOT_ALLOWED,
                                    Unpooled.wrappedBuffer("{\"msg\":\"method not allowed\",\"ret\":1002}"
                                            .getBytes()))).addListener(
                            ChannelFutureListener.CLOSE);
                    return;
                }

				HttpHeaders headers = req.headers();

				Iterator<Entry<String, String>> it = headers.entries().iterator();

				TransportPackage  pkg = TransportPackage.newInstance().setFormat(TransportPackage.FORMAT_JSON);
				ObjectNode head = Utils.json.createObjectNode();

				pkg.setHead(head);

				while (it.hasNext()) {
					Entry<String,String> en = it.next();
                    String key = en.getKey();
					if (key.startsWith("X-Easycall-")) {
                        String keySuffix = key.substring(12);
                        String keyPrefix = key.substring(11,12);
                        String fieldName = keyPrefix.toLowerCase()+keySuffix;
                        head.put(fieldName,en.getValue());
					}
				}

				ByteBuf content = req.content();

				if(content.readableBytes() == 0){

                    ctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST,
                            Unpooled.wrappedBuffer("{\"msg\":\"bad request\",\"ret\":1002}".getBytes())))
                            .addListener(ChannelFutureListener.CLOSE);
                    return;
                }

				pkg.setBody(content);

				ResponseFuture responseFuture = client.asyncRequest(pkg, timeout);

				responseFuture.setCallback(future -> {
				    if(future.isException()){
                        respError(ctx,future.getException());
                    }else{
				        TransportPackage respPkg = future.getResult();

                        boolean keepAlive = isKeepAlive(req);

                        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, respPkg.getBody());
                        response.headers().set(CONTENT_TYPE, "application/json");
                        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

                        if (!keepAlive) {
                            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                        } else {
                            response.headers().set(CONNECTION, Values.KEEP_ALIVE);
                            ctx.writeAndFlush(response);
                        }
                    }
                });
			}
		} catch (Throwable e) {
			respError(ctx, e);
		}
	}

	private void respError(final ChannelHandlerContext ctx, Throwable e) {
	    try{
            ObjectNode body = Utils.json.createObjectNode();
            body.put("ret", EasyPackage.ERROR_SERVER_INTERNAL);
            body.put("msg", e.getMessage());
            byte [] data = Utils.json.writeValueAsBytes(body);

            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK,Unpooled.wrappedBuffer(data));
            response.headers().set(CONTENT_TYPE, "application/json");
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            logger.error("Exception:" + e.getMessage(), e);
	    }catch (Exception e1){
	        logger.error("Exception:" + e1.getMessage(), e1);
        }
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
