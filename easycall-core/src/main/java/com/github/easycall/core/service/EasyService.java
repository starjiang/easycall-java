package com.github.easycall.core.service;

import com.github.easycall.core.util.DaemonThreadFactory;
import com.github.easycall.core.util.PackageFilter;
import com.github.easycall.core.util.Utils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyService {

	private static final Logger log = LoggerFactory.getLogger(EasyService.class);
	private ArrayList<ServerInfo> serverList = new ArrayList<>();
	private final static int DEFAULT_ACCEPT_THREAD_NUM = 1;
	private final static int DEFAULT_WORKER_THREAD_NUM = 100;
	private final static int DEFAULT_WORKER_QUEUE_SIZE = 2000;
	private final static int DEFAULT_ACCEPT_BACKLOG = 2048;
	private final static int DEFAULT_WEIGHT = 100;

	private ServiceRegister serviceRegister;

	public EasyService(String zkConnStr)
	{
		serviceRegister = new ServiceRegister(zkConnStr);
	}

	private ChannelFuture createServer(int port, int bossThreadNum, int workThreadNum, final MessageHandler handler) throws Exception {

		DaemonThreadFactory threadFactory = new DaemonThreadFactory("Netty");
		EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreadNum, threadFactory);
		EventLoopGroup workerGroup = new NioEventLoopGroup(workThreadNum, threadFactory);
		try {
			ServerBootstrap boot = new ServerBootstrap();
			boot.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, DEFAULT_ACCEPT_BACKLOG)
				.option(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.TCP_NODELAY, true).handler(new LoggingHandler(LogLevel.INFO))
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {

						ChannelPipeline p = ch.pipeline();
						p.addLast(new PackageFilter());
						p.addLast(handler);
					}
				});
			return boot.bind(port).sync();
		}catch (Exception e) {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			throw e;
		}
	}

	private Map<String,Boolean> getAsyncMethodMap(Class<?> clazz){


		Map<String,Method> methodMap = Utils.getMethodMap(clazz);
		Map<String,Boolean> asyncMap = new HashMap<>();

		Iterator<Map.Entry<String,Method>> iterator = methodMap.entrySet().iterator();

		while (iterator.hasNext()){
			Map.Entry<String,Method> entry = iterator.next();
			Method method = entry.getValue();
			if(method.getReturnType().getName().equals(CompletableFuture.class.getName())){
				asyncMap.put(entry.getKey(),true);
			}
		}

		return asyncMap;
	}

	public void create(String serviceName,int port,int threadNum,int queueSize,int weight,Object service) throws Exception
	{
		MessageDispatcher syncDispatcher = new SyncMessageDispatcher(queueSize,threadNum,service);
		MessageDispatcher asyncDispatcher = new AsyncMessageDispatcher(service);
		int ioEventThreadNum =  Runtime.getRuntime().availableProcessors() ;
		ChannelFuture future = createServer(port,DEFAULT_ACCEPT_THREAD_NUM,ioEventThreadNum,new MessageHandler(syncDispatcher,asyncDispatcher,getAsyncMethodMap(service.getClass())));
		ServerInfo info = new ServerInfo(serviceName, future, port,weight,(WorkerPool) syncDispatcher);
		serverList.add(info);
	}

	public void create(String serviceName,int port,Object worker) throws Exception {
		create(serviceName,port,DEFAULT_WORKER_THREAD_NUM,DEFAULT_WORKER_QUEUE_SIZE,DEFAULT_WEIGHT,worker);
	}

	public void startAndWait() throws Exception
	{
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.error("Execute ShutdownHook.....");
			for(int i=0;i<serverList.size();i++)
			{
				ServerInfo info = serverList.get(i);
				log.error("unregister service={},port={}",info.serviceName,info.port);
				try{
					serviceRegister.unregister(info.serviceName, info.port);
				}catch (Exception e){
					log.error(e.getMessage(),e);
				}
			}
		}));

		try
		{
			ChannelFuture f = null;
			for(int i=0;i<serverList.size();i++)
			{

				ServerInfo info = serverList.get(i);

				if (info.pool != null) info.pool.start();

				serviceRegister.register(info.serviceName,info.port,info.weight);

				log.info("service {} binded on port:{},weight={}", info.serviceName, info.port,info.weight);

				f = info.future;
			}

			if(f != null)
			{
				f.channel().closeFuture().sync();
			}
		}
		finally
		{
			for(int i=0;i<serverList.size();i++)
			{
				ServerInfo info = serverList.get(i);
				serviceRegister.unregister(info.serviceName, info.port);
				if (info.pool!= null){
					info.pool.stop();
				}
			}
		}
	}

}
