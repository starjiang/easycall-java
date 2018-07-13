package com.github.easycall.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.exception.EasyException;
import com.github.easycall.util.EasyMethod;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;

public class AsyncMessageDispatcher implements MessageDispatcher{

    private static Logger log = LoggerFactory.getLogger(AsyncMessageDispatcher.class);
    private HashMap<String, Method> methodMap = new HashMap<String,Method>();
    private HashMap<Long,Object> objMap;
    private Class<?> clazz;


    public AsyncMessageDispatcher(Class<?> clazz) {
        this.clazz = clazz;
        objMap = new HashMap<>();
        init();
    }

    public void dispatch(Message msg)
    {
        if(msg.getMsg() instanceof EasyPackage)
        {
            EasyPackage reqPkg = (EasyPackage) msg.getMsg();
            Request request = new Request(reqPkg.getFormat(),msg.getCtx().channel().remoteAddress(),msg.getCreateTime(),reqPkg.getHead(),reqPkg.getBody());

            Response response = new Response();
            response.setCtx(msg.getCtx());
            response.setFormat(request.getFormat());

            onRequest(request,response);

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

    private void onRequest(Request request, Response response)
    {
        try
        {
            String callMethod = request.getHead().get("method") == null ? null: request.getHead().get("method").asText();

            if(callMethod == null){
                throw new EasyException("head method feild not exsit");
            }

            Method method = methodMap.get(callMethod);
            if(method == null)
            {
                ObjectNode respBody = Utils.json.createObjectNode();
                respBody.put("msg","method not found:"+callMethod);
                respBody.put("ret", EasyPackage.ERROR_METHOD_NOT_FOUND);
                response.setHead(request.getHead()).setBody(respBody).flush();
                log.error("method not found,req={}",request.getHead().toString());
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

                method.invoke(obj, request,response);
            }

        }
        catch (Exception e)
        {
            ObjectNode respBody = Utils.json.createObjectNode();
            respBody.put("msg",e.getMessage());
            respBody.put("ret", EasyPackage.ERROR_SERVER_INTERNAL);
            try{
                response.setHead(request.getHead()).setBody(respBody).flush();
            }catch (Exception e1){
                log.error(e1.getMessage(),e1);
            }
            log.error("req={}",request.getHead().toString(),e);

        }

    }

    private void init()
    {

        Method[] methods = clazz.getMethods();

        for(int i=0;i<methods.length;i++)
        {
            boolean flag = methods[i].isAnnotationPresent(EasyMethod.class);
            if(flag)
            {
                EasyMethod handler = methods[i].getAnnotation(EasyMethod.class);
                methodMap.put(handler.method(), methods[i]);
            }
        }

    }

    private void onIOException(Throwable cause)
    {
        log.error(cause.getMessage(),cause);
    }
}
