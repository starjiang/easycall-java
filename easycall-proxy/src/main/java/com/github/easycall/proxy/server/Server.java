package com.github.easycall.proxy.server;

import com.github.easycall.client.lb.LoadBalance;
import com.github.easycall.proxy.client.TransportClient;
import com.github.easycall.proxy.util.PackageFilter;
import com.github.easycall.util.DaemonThreadFactory;
import io.netty.handler.codec.http.HttpContentCompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class Server {
	
	
	final static private int ACCEPT_BACKLOG = 1024;
	private int port;
	private int httpPort;
	private int bossThreadNum;
	private int workThreadNum;
	private int httpBossThreadNum;
	private int httpWorkThreadNum;
	private int timeout = 0;
	private TransportClient client;
	static Logger log = LoggerFactory.getLogger(Server.class);
	
	public void init(String zkConnStr,int port,int httpPort,int bossThreadNum,int workThreadNum,int httpBossThreadNum,int httpWorkThreadNum,int backendThreadNum,int timeout) throws Exception
	{
		
		this.port = port;
		this.httpPort = httpPort;
		this.bossThreadNum = bossThreadNum;
		this.workThreadNum = workThreadNum;
		this.httpBossThreadNum = httpBossThreadNum;
		this.httpWorkThreadNum = httpWorkThreadNum;
		this.timeout = timeout;
		this.client = new TransportClient(zkConnStr,backendThreadNum, LoadBalance.LB_ACTIVE);
	}
	
	public void startAndWait() throws Exception
	{
		DaemonThreadFactory threadFactory = new DaemonThreadFactory("Netty");
		EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreadNum,threadFactory);
        EventLoopGroup workerGroup = new NioEventLoopGroup(workThreadNum,threadFactory);
        EventLoopGroup bossGroupHttp = new NioEventLoopGroup(httpBossThreadNum,threadFactory);
        EventLoopGroup workerGroupHttp = new NioEventLoopGroup(httpWorkThreadNum,threadFactory);
        try 
        {
        	final MessageHandler messageHandler = new MessageHandler(this.client,timeout);
            ServerBootstrap boot = new ServerBootstrap();
            boot.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, ACCEPT_BACKLOG)
                    .option(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true).handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {

                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new PackageFilter());
                            p.addLast(messageHandler);
                        }
                    });
            
            log.info("tcp listen port "+port);

            ServerBootstrap httpBoot = new ServerBootstrap();
            httpBoot.option(ChannelOption.SO_BACKLOG, ACCEPT_BACKLOG);
            httpBoot.group(bossGroupHttp, workerGroupHttp)
            .option(ChannelOption.SO_REUSEADDR, true).childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.TCP_NODELAY, true).handler(new LoggingHandler(LogLevel.INFO))
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
            	
            	@Override
                public void initChannel(SocketChannel ch) throws Exception {
            		ChannelPipeline p = ch.pipeline();
	                p.addLast(new HttpRequestDecoder());
	                p.addLast(new HttpObjectAggregator(2*1024*1024));
	                p.addLast(new HttpResponseEncoder());
	                p.addLast(new HttpContentCompressor());
	                p.addLast(new HttpHandler(client,timeout));
            	}
			});

            httpBoot.bind(httpPort).sync();

            log.info("http listen port " + httpPort);

            
            ChannelFuture future = boot.bind(port).sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw e;
        }
	}

}
