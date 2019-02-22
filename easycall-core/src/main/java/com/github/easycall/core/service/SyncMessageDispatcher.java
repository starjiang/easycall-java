package com.github.easycall.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.core.exception.EasyException;
import com.github.easycall.core.util.EasyPackage;
import com.github.easycall.core.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;


public class SyncMessageDispatcher implements WorkerPool,MessageDispatcher {
    private static Logger log = LoggerFactory.getLogger(SyncMessageDispatcher.class);
    private int queueSize;
    private int threadNum;
    private ArrayList<WorkerThread> threadList;
    private ArrayList<ArrayBlockingQueue<Object>> queueList;
    private Object service;
    private int workType;
    private int seq;
    private Map<String, Method> methodMap;

    public SyncMessageDispatcher(int queueSize,int threadNum,Object service)
    {
        this.seq = 0;
        this.queueSize = queueSize;
        this.threadNum = threadNum;
        this.service = service;
        this.methodMap = Utils.getMethodMap(service.getClass());
    }

    @Override
    public void dispatch(Message msg)
    {
        int routeHash = 0;
        if(msg.getMsg() instanceof EasyPackage){
            EasyPackage pkg = (EasyPackage)msg.getMsg();
            if(pkg.getHead().getRouteKey() != null && !pkg.getHead().getRouteKey().equals("")){
                routeHash = Utils.hash(pkg.getHead().getRouteKey());
            }else{
                routeHash = seq++;
            }
        }
        int index = routeHash % threadList.size();
        boolean full = true;
        if(!queueList.get(index).offer(msg)){
            for(int i=0;i<queueList.size();i++){
                if(queueList.get(i).remainingCapacity() != 0){
                    if(queueList.get(i).offer(msg)){
                        full = false;
                        break;
                    }
                }
            }
        }else{
            full = false;
        }

        if(full){
            log.error("work queue is full,throw away request");
        }
    }

    @Override
    public Message consume(int index) throws Exception {
        if(queueList.get(index).isEmpty()){
            for(int i=0;i<queueList.size();i++) {
                if (!queueList.get(i).isEmpty()) {
                    Object msg = queueList.get(i).peek();
                    if (msg != null && msg instanceof Message) {
                        EasyPackage pkg = (EasyPackage) ((Message) msg).getMsg();
                        if (pkg.getHead().getRouteKey() == null || pkg.getHead().getRouteKey().equals("")) {
                            return (Message) queueList.get(i).take();
                        }
                    }
                }
            }
        }
        return (Message) queueList.get(index).take();
    }

    @Override
    public int getMaxQueueSize()
    {
        return queueSize;
    }

    @Override
    public int getQueueSize(){

        int size = 0;
        for(int i=0;i<queueList.size();i++){
            size+= queueList.get(i).size();
        }
        return size;
    }

    @Override
    public void start() throws Exception
    {
        if(threadList != null) throw new EasyException("threadpool has been start");
        if(threadNum == 0) throw new EasyException("threadNum must >0");

        int size = queueSize / threadNum;

        queueList = new ArrayList<>();
        for(int i=0;i<threadNum;i++)
        {
            ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(size);
            queueList.add(queue);
        }

        threadList = new ArrayList<>();
        for(int i=0;i<threadNum;i++)
        {
            WorkerThread workerThread = new WorkerThread(this,i);
            workerThread.startThread();
            threadList.add(workerThread);
        }

    }

    @Override
    public void stop()
    {
        if(threadList == null) return;

        for(int i=0;i<threadList.size();i++)
        {
            threadList.get(i).stopThread();
        }
    }

    @Override
    public void onMessage(Message msg,int index)
    {
        if(msg.getMsg() instanceof EasyPackage)
        {
            try{
                EasyPackage reqPkg = (EasyPackage) msg.getMsg();
                Request request = new Request(reqPkg.getFormat(),msg.getCtx().channel().remoteAddress(),msg.getCreateTime(),reqPkg.getHead(),(JsonNode) reqPkg.getBody());
                Response response = onRequest(request);
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

    private Response onRequest(Request request)
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
                return (Response) method.invoke(service,request);
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
