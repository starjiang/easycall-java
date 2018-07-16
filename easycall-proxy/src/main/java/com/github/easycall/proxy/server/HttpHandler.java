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

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import com.github.easycall.exception.EasyInvalidPkgException;
import com.github.easycall.proxy.client.ResponseFuture;
import com.github.easycall.proxy.client.TransportClient;
import com.github.easycall.proxy.client.TransportPackage;
import com.github.easycall.proxy.util.PackageFilter;
import com.github.easycall.util.EasyHead;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

@Sharable
public class HttpHandler extends ChannelInboundHandlerAdapter {

	static Logger logger = LoggerFactory.getLogger(HttpHandler.class);
    private TransportClient client;
    private int timeout;
    private HashMap<String,Field> fields;

	public HttpHandler(TransportClient client, int timeout) {
		this.client = client;
	    this.timeout = timeout;

	    Field [] fieldList = EasyHead.class.getDeclaredFields();
	    this.fields = new HashMap<>();

	    for(int i=0;i<fieldList.length;i++){
            this.fields.put(fieldList[i].getName(),fieldList[i]);
        }
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {

		try {
			if (msg instanceof FullHttpRequest) {
				final FullHttpRequest req = (FullHttpRequest) msg;

				URI uri = new URI(req.getUri());

				String path = uri.getPath();

				if (!path.equals("/call") && !path.equals("/rawcall")) {
	               respData(ctx,Unpooled.wrappedBuffer("Not Found".getBytes()),NOT_FOUND,false,"text/plain");
					return;
				}

				if(!req.method().name().equals("POST")){
                    respData(ctx,Unpooled.wrappedBuffer("Method Not Allowed".getBytes()),METHOD_NOT_ALLOWED,false,"text/plain");
                    return;
                }

                ByteBuf content = req.content();

                if(content.readableBytes() == 0){
                    respData(ctx,Unpooled.wrappedBuffer("Bad Request".getBytes()),BAD_REQUEST,false,"text/plain");
                    return;
                }

                if(path.equals("/call")){
				    processJsonRequest(ctx,req);
                }else if(path.equals("/rawcall")){
				    processRawRequest(ctx,req);
                }
			}
		} catch (Throwable e) {
			respData(ctx,Unpooled.wrappedBuffer(e.getMessage().getBytes()),INTERNAL_SERVER_ERROR,false,"text/plain");
			logger.error(e.getMessage(),e);
		}
	}

	private void processRawRequest(ChannelHandlerContext ctx,FullHttpRequest req) throws Exception{

        TransportPackage  pkg = TransportPackage.newInstance();

        if(PackageFilter.isValidPackage(req.content()) < 0){
            throw new EasyInvalidPkgException("invalid pkg");
        }
        pkg.decode(req.content());

        boolean keepAlive = HttpUtil.isKeepAlive(req);
        requestRawAndResponse(ctx,pkg,keepAlive);

    }

    private void requestRawAndResponse(ChannelHandlerContext ctx,TransportPackage reqPkg,final boolean keepAlive){

	    try{
	        ResponseFuture responseFuture = client.asyncRequest(reqPkg, timeout);
            responseFuture.setCallback(future -> {
                try {
                    if (future.isException()) {

                        EasyPackage respPkg = EasyPackage.newInstance().setFormat(reqPkg.getFormat())
                                .setHead(reqPkg.getHead()).setBody(Utils.json.createObjectNode());
                        respPkg.getHead().setRet(EasyPackage.ERROR_SERVER_INTERNAL);
                        respPkg.getHead().setMsg(future.getException().getMessage());
                        ByteBuf respBuf = respPkg.encode();
                        respData(ctx, respBuf, OK, keepAlive, "application/octet-stream");
                        logger.error(future.getException().getMessage(),future.getException());

                    } else {
                        TransportPackage respPkg = future.getResult();
                        ByteBuf respBuf = respPkg.encode();
                        respData(ctx,respBuf,OK,keepAlive,"application/octet-stream");
                    }
                }catch (Exception e){
                    logger.error(e.getMessage(),e);
                }
            });
	    }catch(Exception e){

	        reqPkg.getBody().release();
            EasyPackage respPkg = EasyPackage.newInstance().setFormat(reqPkg.getFormat())
                    .setHead(reqPkg.getHead()).setBody(Utils.json.createObjectNode());
            respPkg.getHead().setRet(EasyPackage.ERROR_SERVER_INTERNAL);
            respPkg.getHead().setMsg(e.getMessage());

            try{
                ByteBuf respBuf = respPkg.encode();
                respData(ctx, respBuf, OK, keepAlive, "application/octet-stream");
            }catch (Exception e1){
                logger.error(e.getMessage(),e);
            }
            logger.error(e.getMessage(),e);
        }
    }

	private void processJsonRequest(ChannelHandlerContext ctx,FullHttpRequest req) throws  Exception{

        HttpHeaders headers = req.headers();

        Iterator<Entry<String, String>> it = headers.entries().iterator();

        TransportPackage  pkg = TransportPackage.newInstance().setFormat(TransportPackage.FORMAT_JSON);
        EasyHead head = EasyHead.newInstance();
        pkg.setHead(head);

        while (it.hasNext()) {
            Entry<String,String> en = it.next();
            String key = en.getKey();
            if (key.startsWith("X-Easycall-")) {
                String keySuffix = key.substring(12);
                String keyPrefix = key.substring(11,12);
                String fieldName = keyPrefix.toLowerCase()+keySuffix;
                Field field = fields.get(fieldName);
                if(field == null){
                    continue;
                }
                field.setAccessible(true);
                if(field.getType().getName().equals("java.lang.String")){
                    field.set(head,en.getValue());
                }else if (field.getType().getName().equals("java.lang.Long")){
                    field.set(head,Long.valueOf(en.getValue()));
                }else if(field.getType().getName().equals("java.lang.Integer")){
                    field.set(head,Integer.valueOf(en.getValue()));
                }else if(field.getType().getName().equals("java.lang.Boolean")){
                    field.set(head,Boolean.valueOf(en.getValue()));
                }else if(field.getType().getName().equals("java.lang.Short")){
                    field.set(head,Short.valueOf(en.getValue()));
                }
            }
        }

        pkg.setBody(req.content());

        boolean keepAlive = HttpUtil.isKeepAlive(req);

        requestJsonAndResponse(ctx,pkg,keepAlive);

    }

    private void requestJsonAndResponse(ChannelHandlerContext ctx,TransportPackage reqPkg,final boolean keepAlive){

        try{
            ResponseFuture responseFuture = client.asyncRequest(reqPkg, timeout);
            responseFuture.setCallback(future -> {
                try {
                    if (future.isException()) {

                        ByteBuf respBuf = Unpooled.wrappedBuffer(future.getException().getMessage().getBytes());
                        respData(ctx, respBuf,INTERNAL_SERVER_ERROR, keepAlive, "text/plain");
                        logger.error(future.getException().getMessage(),future.getException());
                    } else {
                        TransportPackage respPkg = future.getResult();
                        int ret = respPkg.getHead().getRet() == null ? 0 : respPkg.getHead().getRet();
                        String msg = respPkg.getHead().getMsg() == null ? "ok" : respPkg.getHead().getMsg();
                        ByteBuf respBuf = respPkg.getBody();
                        HttpResponseStatus status = OK;
                        String contentType = "application/json";
                        if(ret != 0){
                            contentType = "text/plain";
                            status = INTERNAL_SERVER_ERROR;
                            respBuf.release();
                            respBuf = Unpooled.wrappedBuffer(msg.getBytes());
                        }
                        respData(ctx,respBuf,status,keepAlive,contentType);
                    }
                }catch (Exception e){
                    logger.error(e.getMessage(),e);
                }
            });
        }catch(Exception e){
            reqPkg.getBody().release();
            ByteBuf respBuf = Unpooled.wrappedBuffer(e.getMessage().getBytes());
            respData(ctx, respBuf, INTERNAL_SERVER_ERROR, keepAlive, "text/plain");
            logger.error(e.getMessage(),e);
        }
    }

	private void respData(ChannelHandlerContext ctx,ByteBuf respBuf,HttpResponseStatus status,boolean keepAlive,String contentType) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,respBuf);
        response.headers().set(CONTENT_TYPE, contentType);
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        if(keepAlive){
            ctx.writeAndFlush(response);
        }else{
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}
