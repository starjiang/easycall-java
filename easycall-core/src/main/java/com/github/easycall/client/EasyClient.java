package com.github.easycall.client;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.exception.EasyConnectException;
import com.github.easycall.exception.EasyException;
import com.github.easycall.exception.EasyServiceNotFoundException;
import com.github.easycall.exception.EasyTimeoutException;
import com.github.easycall.util.*;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;



class Session
{
    public int format;
    public EasyHead head;
    public ObjectNode body;
    public ResponseFuture future;
    public long sessionId;
    public long createTime;
    public Long seq;
    public Node node;
    public Timeout timeout;

    public Session(Node node,long sessionId,Long seq,int format,EasyHead head,ObjectNode body,ResponseFuture future) {
        this.sessionId = sessionId;
        this.format = format;
        this.head = head;
        this.body = body;
        this.future = future;
        this.seq = seq;
        this.createTime = System.currentTimeMillis();
        this.node = node;
    }

    public String getKey()
    {
        return "/"+node.ip+":"+node.port;
    }
}

class SessionTimeoutTask implements TimerTask
{
    private long sessionId;
    private String name;
    private ClientMessageDispatcher dispatcher;

    public SessionTimeoutTask(ClientMessageDispatcher dispatcher,String name,long sessionId)
    {
        this.sessionId = sessionId;
        this.name = name;
        this.dispatcher = dispatcher;
    }

    public void run(Timeout timeout)
    {
        dispatcher.onTimeout(name,sessionId);
    }
}

class ConnStatus
{
    public final static int INIT = 0;
    public final static int CONNECTING = 1;
    public final static int CONNECTED = 2;

    public final static int CONNECT_TIMEOUT = 5000;

    private volatile int connectStatus;

    private long lastConnectTime;

    public ConcurrentHashMap<String,ChannelHandlerContext> connectionMap;
    private static int seq;


    public int getConnectStatus(){
        long diffTime = System.currentTimeMillis() - lastConnectTime;
        if(connectStatus == CONNECTING && diffTime > CONNECT_TIMEOUT ){
            connectStatus = INIT;
        }
        return connectStatus;
    }

    public void setConnectStatus(int connectStatus) {
        if(connectStatus == CONNECTING){
            lastConnectTime = System.currentTimeMillis();
        }
        this.connectStatus = connectStatus;
    }

    public void addConnection(ChannelHandlerContext ctx){
        connectStatus = CONNECTED;
        connectionMap.put(ctx.channel().localAddress().toString(),ctx);
    }

    public void removeConnection(ChannelHandlerContext ctx){
        connectionMap.remove(ctx.channel().localAddress().toString());
        if (connectionMap.size() == 0){
            connectStatus = INIT;
        }
    }

    public int getConnectionSize(){
        return connectionMap.size();
    }

    public ChannelHandlerContext getConnection(){

        if (connectionMap.size() == 0 ) {
            return  null;
        }

        int index = seq++ % connectionMap.size();

        Iterator<Map.Entry<String,ChannelHandlerContext>> iterator = connectionMap.entrySet().iterator();

        ChannelHandlerContext ctx = null;

        int count = 0;

        while (iterator.hasNext()){

            Map.Entry<String,ChannelHandlerContext> entry = iterator.next();
            ctx = entry.getValue();
            if(count == index){
                break;
            }
            count++;
        }

        return ctx;
    }

    public ConnStatus() {
        connectStatus = INIT;
        connectionMap = new ConcurrentHashMap<>();
    }
}

public final class EasyClient implements ClientMessageDispatcher {

    public static Logger log = LoggerFactory.getLogger(EasyClient.class);

    private static int DEFAULT_TICK_DURATION = 100;
    private static int DEFAULT_CONNECTION_NUM = 8;
    private static int MAX_SESSION_SIZE = 10000;

    private Bootstrap boot;
    private EventLoopGroup group;
    private AtomicLong seqNo;

    private HashedWheelTimer timer;
    private ConcurrentHashMap<String,ConcurrentLinkedQueue<Session>> waitSessions;
    private ConcurrentHashMap<String,ConcurrentHashMap<Long, Session>> requestSessions;
    private ConcurrentHashMap<String, ConnStatus> connStatus;
    private NodeManager nodeMgr;
    private int loadBalanceType;

    public EasyClient(String zkConnStr,int workerThreadNum,int loadBalanceType)
    {

        this.loadBalanceType = loadBalanceType;
        nodeMgr = new NodeManager(zkConnStr);

        boot = new Bootstrap();

        DaemonThreadFactory threadFactoryTimer = new DaemonThreadFactory("WheelTimer");
        DaemonThreadFactory threadFactoryNetty = new DaemonThreadFactory("Netty-Client");
        final ClientMessageHandler clientMessageHandler = new ClientMessageHandler(this);

        timer = new HashedWheelTimer(threadFactoryTimer, DEFAULT_TICK_DURATION, TimeUnit.MILLISECONDS);
        waitSessions = new ConcurrentHashMap<>();
        requestSessions = new ConcurrentHashMap<>();
        connStatus = new ConcurrentHashMap<>();
        seqNo = new AtomicLong(1);

        group = new NioEventLoopGroup(workerThreadNum, threadFactoryNetty);
        boot.group(group);
        boot.channel(NioSocketChannel.class);
        boot.option(ChannelOption.TCP_NODELAY, true);
        boot.option(ChannelOption.SO_KEEPALIVE, true);
        boot.option(ChannelOption.SO_REUSEADDR, true);
        boot.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast(new PackageFilter());
                p.addLast(clientMessageHandler);
            }
        });
    }


    public void onTimeout(String name,long sessionId){

        ConcurrentHashMap<Long,Session> sessions = requestSessions.get(name);

        if (sessions == null){
            log.error("{} sessions can not found",name);
            return;
        }

        Session session = sessions.get(sessionId);

        if(session == null){
            return;
        }

        ConcurrentLinkedQueue<Session> waitSessionQueue =  waitSessions.get(session.getKey());

        if(session.future != null)
        {
            if(waitSessionQueue != null && waitSessionQueue.contains(session)){
                session.future.setException(new EasyConnectException("connect to "+session.node.ip+":"+session.node.port+" timeout"));
            }else{
                session.future.setException(new EasyTimeoutException("backend server "+session.node.ip+":"+session.node.port+" timeout"));
            }
        }

        session.node.active.decrementAndGet();
        sessions.remove(sessionId);

        if(waitSessionQueue != null && waitSessionQueue.size() > 0){
            waitSessionQueue.remove(session);
        }

    }

    public void onMessage(ChannelHandlerContext ctx, Object msg) {

        EasyPackage pkg = (EasyPackage)msg;

        Long seq = pkg.getHead().getSeq();
        String name = pkg.getHead().getService();

        if(seq == null){
            log.error("response head have not seq field resp={}",pkg.getHead().toString());
            return;
        }

        if(name == null){
            log.error("response head have not service field resp={}",pkg.getHead().toString());
            return;
        }

        ConcurrentHashMap<Long,Session> sessions = requestSessions.get(name);

        if (sessions == null){
            log.error("service sessions can not found,resp={}",pkg.getHead().toString());
            return;
        }

        Session session = sessions.get(seq);

        if(session == null)
        {
            log.error("request session can not found,resp={}",pkg.getHead().toString());
            return;
        }

        if(session.seq != null){
            pkg.getHead().setSeq(session.seq);
        }

        session.timeout.cancel();

        if(session.future != null)
        {
            session.future.setResult(pkg);
        }
        session.node.active.decrementAndGet();
        sessions.remove(session.sessionId);

    }

    public void onClose(ChannelHandlerContext ctx) {
        log.info("connect from {} to {} closed",ctx.channel().localAddress().toString(),ctx.channel().remoteAddress());
        ConnStatus status = connStatus.get(ctx.channel().remoteAddress().toString());
        if (status != null) {
            status.removeConnection(ctx);
        }
    }

    public void onConnection(ChannelHandlerContext ctx) {
        log.info("connect from {} to {} connected",ctx.channel().localAddress().toString(),ctx.channel().remoteAddress());
        String key = ctx.channel().remoteAddress().toString();
        ConnStatus status = connStatus.get(key);
        if (status != null) {
            status.addConnection(ctx);
        }
        sendWaitSessions(key,status);
    }

    public void onError(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
        log.error(cause.getMessage(),cause);
    }

    private void sendWaitSessions(String key,ConnStatus status){

        ConcurrentLinkedQueue<Session> sessionQueue = waitSessions.get(key);

        if (sessionQueue == null){
            return;
        }

        Session session;

        while(((session = sessionQueue.poll()) != null)){

            ChannelHandlerContext ctx = status.getConnection();
            if(ctx != null){
                try{
                    EasyPackage pkg = EasyPackage.newInstance();
                    ByteBuf buf = pkg.setFormat((byte)session.format).setHead(session.head).setBody(session.body).encode();
                    ctx.writeAndFlush(buf);
                }catch (Exception e){
                    log.error(e.getMessage(),e);
                }
            }
        }
    }

    public EasyPackage syncRequest(int format,EasyHead head,ObjectNode body, int timeout,String ip,int port) throws Exception{

        ResponseFuture responseFuture = asyncRequest(format,head,body,timeout,ip,port);

        EasyPackage pkg = responseFuture.getResult();

        if (responseFuture.isException()){
            throw responseFuture.getException();
        }

        return pkg;
    }

    public EasyPackage syncRequest(int format,EasyHead head,ObjectNode body, int timeout) throws Exception{
        return syncRequest(format,head,body,timeout,null,0);
    }


    public EasyPackage syncRequest(String service,String method,ObjectNode body, int timeout) throws Exception {

        EasyHead head = EasyHead.newInstance();
        head.setService(service).setMethod(method);
        return syncRequest(EasyPackage.FORMAT_MSGPACK,head,body,timeout);
    }

    public ResponseFuture asyncRequest(String service,String method,ObjectNode body, int timeout) throws Exception {

        EasyHead head = EasyHead.newInstance();
        head.setService(service).setMethod(method);
        return asyncRequest(EasyPackage.FORMAT_MSGPACK,head,body,timeout);
    }

    public ResponseFuture asyncRequest(int format,EasyHead head,ObjectNode body, int timeout) throws Exception {
        return asyncRequest(format,head,body,timeout,null,0);
    }

    public ResponseFuture asyncRequest(int format,EasyHead head,ObjectNode body, int timeout,String ip,int port) throws Exception
    {
        ResponseFuture future = new ResponseFuture();

        String name = head.getService();

        if (name == null){
            throw new EasyException("service field not found");
        }

        String method = head.getMethod();

        if (method == null){
            throw new EasyException("method field not found");
        }

        String routeKey = head.getRouteKey() == null ? "" : head.getRouteKey();

        Node node;
        if(ip != null){
            node = new Node(name,ip,port,100);
        }else{
            node = nodeMgr.getNode(name,loadBalanceType,routeKey);
        }

        if (node == null) {
            throw new EasyServiceNotFoundException("service " + name + " not found");
        }

        String key = "/" + node.ip + ":" + node.port;

        long sessionId = seqNo.addAndGet(1);

        Long seq = head.getSeq();

        Session session = new Session(node,sessionId,seq,format, head, body, future);

        head.setSeq(sessionId);

        ConcurrentLinkedQueue<Session> sessionQueue;
        ConnStatus status;
        ConcurrentHashMap<Long,Session> sessions;

        synchronized (this) {
            sessionQueue = waitSessions.get(key);
            if (sessionQueue == null) {
                sessionQueue = new ConcurrentLinkedQueue<>();
                waitSessions.put(key, sessionQueue);
            }

            status = connStatus.get(key);
            if (status == null) {
                status = new ConnStatus();
                connStatus.put(key, status);
            }

            sessions = requestSessions.get(name);

            if(sessions == null){
                sessions = new ConcurrentHashMap<>();
                requestSessions.put(name,sessions);
            }
        }

        if (sessions.size() > MAX_SESSION_SIZE){
            throw new EasyException("service "+name+" session size > "+MAX_SESSION_SIZE);
        }

        ChannelHandlerContext ctx = status.getConnection();

        if (ctx == null) {
            sessionQueue.add(session);

            if (status.getConnectStatus() == ConnStatus.INIT) {

                status.setConnectStatus(ConnStatus.CONNECTING);
                boot.connect(node.ip, node.port);
            }

        } else {

            int connectCount = status.getConnectionSize();
            if (connectCount < DEFAULT_CONNECTION_NUM && status.getConnectStatus() != ConnStatus.CONNECTING) {
                status.setConnectStatus(ConnStatus.CONNECTING);
                boot.connect(node.ip, node.port);
            }

            EasyPackage pkg = EasyPackage.newInstance();
            ByteBuf buf = pkg.setFormat((byte)format).setHead(head).setBody(body).encode();
            ctx.writeAndFlush(buf);
        }

        session.node.active.incrementAndGet();
        session.timeout = timer.newTimeout(new SessionTimeoutTask(this,name, session.sessionId), timeout, TimeUnit.MILLISECONDS);
        sessions.put(session.sessionId, session);

        return future;
    }

}
