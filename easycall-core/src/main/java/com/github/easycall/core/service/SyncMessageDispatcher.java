package com.github.easycall.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.core.exception.EasyException;
import com.github.easycall.core.util.EasyMethod;
import com.github.easycall.core.util.EasyPackage;
import com.github.easycall.core.util.Utils;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;


public class SyncMessageDispatcher implements WorkerPool,MessageDispatcher {
    private static Logger log = LoggerFactory.getLogger(SyncMessageDispatcher.class);
    private int queueSize;
    private int threadNum;
    private ArrayList<WorkerThread> threadList;
    private ArrayList<ArrayBlockingQueue<Object>> queueList;
    private ArrayList<Object> workerList;
    private Class<?> clazz;
    private int workType;
    private Map<String, Method> methodMap;
    public final static int WORKER_TYPE_HASH = 1;
    public final static int WORKER_TYPE_RANDOM = 2;

    public SyncMessageDispatcher(int queueSize,int threadNum,int workType,Class<?> clazz)
    {
        this.queueSize = queueSize;
        this.threadNum = threadNum;
        this.clazz = clazz;
        this.workType = workType;
        this.methodMap = Utils.getMethodMap(clazz);
    }

    public void dispatch(Message msg)
    {
        if (workType == WORKER_TYPE_HASH){

            int routeHash = 0;
            if(msg.getMsg() instanceof EasyPackage){
                EasyPackage pkg = (EasyPackage)msg.getMsg();
                String routeKey = pkg.getHead().getRouteKey() == null ? "" : pkg.getHead().getRouteKey();
                routeHash = Utils.hash(routeKey);
            }
            int index = routeHash % threadList.size();
            boolean success = queueList.get(index).offer(msg);
            if(!success){
                log.error("work queue {} is full,throw away request",index);
            }

        }else if (workType == WORKER_TYPE_RANDOM) {
            boolean success = queueList.get(0).offer(msg);
            if(!success){
                log.error("work queue is full,throw away request");
            }
        } else{
            log.error("workType not support");
        }

    }

    public Message consume(int index) throws Exception {
        if (workType == WORKER_TYPE_HASH){
            return (Message) queueList.get(index).take();
        }else if(workType == WORKER_TYPE_RANDOM){
            return (Message) queueList.get(0).take();
        }else {
            throw new EasyException("workType not support");
        }
    }

    public int getMaxQueueSize()
    {
        return queueSize;
    }

    public int getQueueSize(){

        if (workType == WORKER_TYPE_HASH){
            int size = 0;
            for(int i=0;i<queueList.size();i++){
                size+= queueList.get(i).size();
            }
            return size;

        }else if(workType == WORKER_TYPE_RANDOM){
            return queueList.get(0).size();
        }else {
            return 0;
        }
    }

    public void start() throws Exception
    {
        if(threadList != null) return;

        int size = queueSize;

        if(workType == WORKER_TYPE_HASH){
            size = queueSize / threadNum;
        }

        queueList = new ArrayList<>();
        for(int i=0;i<threadNum;i++)
        {
            ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(size);
            queueList.add(queue);
            if(workType == WORKER_TYPE_RANDOM){
                break;
            }
        }

        workerList = new ArrayList<>();

        for(int i=0;i<threadNum;i++){
            Object worker = clazz.newInstance();
            workerList.add(worker);
        }

        threadList = new ArrayList<>();
        for(int i=0;i<threadNum;i++)
        {
            WorkerThread workerThread = new WorkerThread(this,i);
            workerThread.startThread();
            threadList.add(workerThread);
        }

    }

    public void stop()
    {
        if(threadList == null) return;

        for(int i=0;i<threadList.size();i++)
        {
            threadList.get(i).stopThread();
        }
    }


    public void onMessage(Message msg,int index)
    {
        if(msg.getMsg() instanceof EasyPackage)
        {
            try{
                EasyPackage reqPkg = (EasyPackage) msg.getMsg();
                Request request = new Request(reqPkg.getFormat(),msg.getCtx().channel().remoteAddress(),msg.getCreateTime(),reqPkg.getHead(),(JsonNode) reqPkg.getBody());
                Response response = onRequest(request,index);
                if(response == null){
                    throw new EasyException("method "+reqPkg.getHead().getMethod()+" response is null or void");
                }
                EasyPackage respPkg = EasyPackage.newInstance().setFormat(reqPkg.getFormat()).setHead(response.getHead()).setBody(response.getBody());
                msg.getCtx().writeAndFlush(respPkg.encode());

            }catch (Exception e){
                onIOException(e);
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

    private Response onRequest(Request request,int index)
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
                Response response = new Response();
                ObjectNode respBody = Utils.json.createObjectNode();
                request.getHead().setMsg("method "+callMethod+" not found");
                request.getHead().setRet(EasyPackage.ERROR_METHOD_NOT_FOUND);
                response.setHead(request.getHead()).setBody(respBody);
                log.error("method not found,req={}",request.getHead().toString());
                return response;
            }
            else
            {
                Object obj = workerList.get(index);
                return (Response) method.invoke(obj,request);
            }

        }
        catch (Exception e)
        {
            Response response = new Response();
            ObjectNode respBody = Utils.json.createObjectNode();
            request.getHead().setMsg(e.getMessage());
            request.getHead().setRet(EasyPackage.ERROR_SERVER_INTERNAL);
            response.setHead(request.getHead()).setBody(respBody);
            log.error("req={}",request.getHead().toString(),e);
            return response;
        }
    }

    private void onIOException(Throwable cause)
    {
        log.error(cause.getMessage(),cause);
    }

}
