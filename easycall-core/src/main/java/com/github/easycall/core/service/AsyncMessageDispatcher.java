package com.github.easycall.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.core.exception.EasyException;
import com.github.easycall.core.util.EasyPackage;
import com.github.easycall.core.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AsyncMessageDispatcher implements MessageDispatcher{

    private static Logger log = LoggerFactory.getLogger(AsyncMessageDispatcher.class);
    private Map<String, Method> methodMap;
    private Map<Long,Object> objMap;
    private Class<?> clazz;


    public AsyncMessageDispatcher(Class<?> clazz) {
        this.clazz = clazz;
        this.objMap = new HashMap<>();
        this.methodMap = Utils.getMethodMap(clazz);
    }

    public void dispatch(Message msg)
    {
        if(msg.getMsg() instanceof EasyPackage)
        {
            EasyPackage reqPkg = (EasyPackage) msg.getMsg();
            Request request = new Request(reqPkg.getFormat(),msg.getCtx().channel().remoteAddress(),msg.getCreateTime(),reqPkg.getHead(),(JsonNode) reqPkg.getBody());
            Object ret = onRequest(request);

            if (ret instanceof CompletableFuture){

                CompletableFuture<Response> completableFuture = (CompletableFuture<Response>)ret;
                if(completableFuture == null){
                    onIOException(new EasyException("method "+reqPkg.getHead().getMethod()+" response is null or void"));
                    return;
                }
                completableFuture.whenComplete((response,throwable) -> {
                    if (throwable != null) {
                        ObjectNode respBody = Utils.json.createObjectNode();
                        request.getHead().setMsg(throwable.getMessage());
                        request.getHead().setRet(EasyPackage.ERROR_SERVER_INTERNAL);
                        response = new Response().setHead(request.getHead()).setBody(respBody);
                        log.error(throwable.getMessage(), throwable);
                    }
                    EasyPackage respPkg = EasyPackage.newInstance().setFormat(reqPkg.getFormat()).setHead(response.getHead()).setBody(response.getBody());
                    try {
                        msg.getCtx().writeAndFlush(respPkg.encode());
                    } catch (Exception e) {
                        onIOException(e);
                    }
                });

            }
            else{
                log.error("return type not support");
            }
        }
        else if(msg.getMsg() instanceof Throwable)
        {
            onIOException((Throwable)msg.getMsg());
            msg.getCtx().close();
        }
        else
        {
            onIOException(new EasyException("invalid msg type"));
            msg.getCtx().close();
        }

    }

    private Object onRequest(Request request)
    {
        try
        {
            String callMethod = request.getHead().getMethod();

            if(callMethod == null){
                throw new EasyException("head method field not settle");
            }

            Method method = methodMap.get(callMethod);
            if(method == null)
            {
                CompletableFuture<Response> completableFuture = new CompletableFuture<>();
                ObjectNode respBody = Utils.json.createObjectNode();
                request.getHead().setMsg("method "+callMethod+" not found");
                request.getHead().setRet(EasyPackage.ERROR_METHOD_NOT_FOUND);
                completableFuture.complete(new Response().setHead(request.getHead()).setBody(respBody));
                log.error("method not found,req={}",request.getHead().toString());
                return completableFuture;
            }
            else
            {
                Object obj;

                synchronized (this){
                    obj = objMap.get(Thread.currentThread().getId());
                    if (obj == null) {
                        obj = clazz.newInstance();
                        objMap.put(Thread.currentThread().getId(),obj);
                    }
                }

                return method.invoke(obj, request);
            }

        }
        catch (Exception e)
        {
            CompletableFuture<Response> completableFuture = new CompletableFuture<>();
            ObjectNode respBody = Utils.json.createObjectNode();
            request.getHead().setMsg(e.getMessage());
            request.getHead().setRet(EasyPackage.ERROR_SERVER_INTERNAL);
            completableFuture.complete(new Response().setHead(request.getHead()).setBody(respBody));
            log.error("req={}",request.getHead().toString(),e);
            return completableFuture;
        }

    }

    private void onIOException(Throwable cause)
    {
        log.error(cause.getMessage(),cause);
    }
}
