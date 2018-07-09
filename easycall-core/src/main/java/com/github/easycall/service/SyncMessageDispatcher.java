package com.github.easycall.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.exception.EasyException;
import com.github.easycall.util.EasyMethod;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class SyncMessageDispatcher implements WorkerPool,MessageDispatcher {
    private static Logger log = LoggerFactory.getLogger(SyncMessageDispatcher.class);
    private int queueSize;
    private int threadNum;
    private ArrayList<WorkerThread> threadList;
    private ArrayList<LinkedBlockingQueue<Object>> queueList;
    private ArrayList<Object> workerList;
    private Class<?> clazz;
    private int workType;

    private static volatile int seq;
    private HashMap<String, Method> methodMap = new HashMap<String,Method>();
    public final static int WORKER_TYPE_HASH = 1;
    public final static int WORKER_TYPE_RANDOM = 2;

    public SyncMessageDispatcher(int queueSize,int threadNum,int workType,Class<?> clazz)
    {
        this.queueSize = queueSize;
        this.threadNum = threadNum;
        this.clazz = clazz;
        this.workType = workType;
        this.init();
    }

    public void dispatch(Message msg)
    {
        if (workType == WORKER_TYPE_HASH){

            int routeHash = 0;
            if(msg.getMsg() instanceof EasyPackage){
                EasyPackage pkg = (EasyPackage)msg.getMsg();
                String routeKey = pkg.getHead().get("routeKey") == null ? "" : pkg.getHead().get("routeKey").asText();
                routeHash = Utils.hash(routeKey);
            }
            int index = routeHash % threadList.size();
            queueList.get(index).add(msg);

        }else if (workType == WORKER_TYPE_RANDOM) {
            queueList.get(0).add(msg);
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
            LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<>(size);
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
                Response response = new Response();

                EasyPackage reqPkg = (EasyPackage) msg.getMsg();

                Request request = new Request(reqPkg.getFormat(),msg.getCtx().channel().remoteAddress(),msg.getCreateTime(),reqPkg.getHead(),reqPkg.getBody());
                response.setFormat(request.getFormat());
                onRequest(request,response,index);
                EasyPackage respPkg = EasyPackage.newInstance().setFormat(reqPkg.getFormat()).setHead(response.getHead()).setBody(response.getBody());
                msg.getCtx().writeAndFlush(Unpooled.wrappedBuffer(respPkg.encode()));

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

    private void onRequest(Request request, Response response,int index)
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
                response.setHead(request.getHead()).setBody(respBody);
                log.error("method not found,req={}",request.getHead().toString());
            }
            else
            {
                Object obj = workerList.get(index);
                method.invoke(obj, request,response);
            }

        }
        catch (Exception e)
        {
            ObjectNode respBody = Utils.json.createObjectNode();
            respBody.put("msg",e.getMessage());
            respBody.put("ret", EasyPackage.ERROR_SERVER_INTERNAL);
            response.setHead(request.getHead()).setBody(respBody);
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
