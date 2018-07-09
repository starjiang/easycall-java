package com.github.easycall.service;

import com.github.easycall.util.DaemonThreadFactory;
import com.github.easycall.util.PackageFilter;
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

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ShutDownHandler extends Thread{

}

public class Service {

    private static final Logger log = LoggerFactory.getLogger(Service.class);
	private ArrayList<ServerInfo> serverList = new ArrayList<ServerInfo>();
	private final static int BOSS_THREAD_NUM = 1;
	private final static int WORKER_THREAD_NUM = 4;
	private final static int ACCEPT_BACKLOG = 1024;

	public final static int WORK_TYPE_HASH = 1;
	public final static int WORK_TYPE_RANDOM = 2;

	public final static Service instance = new Service();
	protected Service(){}
		
	public void init(String zkConnStr)
	{
    	ServiceRegister.instance.init(zkConnStr);
	}

	private ChannelFuture createServer(int port, int bossThreadNum, int workThreadNum, final MessageHandler handler) throws Exception {

		DaemonThreadFactory threadFactory = new DaemonThreadFactory("Netty");
		EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreadNum, threadFactory);
		EventLoopGroup workerGroup = new NioEventLoopGroup(workThreadNum, threadFactory);
		try {
			ServerBootstrap boot = new ServerBootstrap();
			boot.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, ACCEPT_BACKLOG)
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


	public void createAsync(String serviceName,int port,int threadNum,Class<?> clazz) throws Exception
	{
		MessageDispatcher dispatcher = new AsyncMessageDispatcher(clazz);
		ChannelFuture future = createServer(port,BOSS_THREAD_NUM,threadNum,new MessageHandler(dispatcher));

    	ServerInfo info = new ServerInfo(serviceName, future, port,null);
    	serverList.add(info);
	}
	
	public void createSync(String serviceName,int port,int threadNum,int queueSize,int workerType,Class<?> clazz) throws Exception
	{
		MessageDispatcher dispatcher = new SyncMessageDispatcher(queueSize,threadNum,workerType,clazz);
		ChannelFuture future = createServer(port,BOSS_THREAD_NUM,WORKER_THREAD_NUM,new MessageHandler(dispatcher));
		ServerInfo info = new ServerInfo(serviceName, future, port,(WorkerPool) dispatcher);
		serverList.add(info);
	}

	public void startAndWait() throws Exception
	{
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run()
			{
				log.error("Execute ShutdownHook.....");
				for(int i=0;i<serverList.size();i++)
				{
					ServerInfo info = serverList.get(i);
					try{
						ServiceRegister.instance.unregister(info.serviceName, info.port);
						log.error("unregister service={},port={}",info.serviceName,info.port);
					}catch (Exception e){
						log.error(e.getMessage(),e);
					}
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

				ServiceRegister.instance.register(info.serviceName,info.port);

				log.info(String.format("service %s binded on port %d", info.serviceName, info.port));

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
        		ServiceRegister.instance.unregister(info.serviceName, info.port);
        	}
        }
    }

}
