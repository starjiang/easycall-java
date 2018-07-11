package com.github.easycall.proxy.server;

import com.github.easycall.client.lb.LoadBalance;
import com.github.easycall.proxy.client.TransportClient;
import com.github.easycall.proxy.util.PackageFilter;
import com.github.easycall.proxy.util.ProxyConfig;
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

public class ProxyServer {
	
	
	final static private int ACCEPT_BACKLOG = 1024;
    private ProxyConfig config;
	private TransportClient client;
	static Logger log = LoggerFactory.getLogger(ProxyServer.class);
	
	public ProxyServer(ProxyConfig config) throws Exception
	{
        this.config = config;
		this.client = new TransportClient(config.getZk(),config.getBackendThreadNum(), LoadBalance.LB_ACTIVE);
	}
	
	public void startAndWait() throws Exception
	{
		DaemonThreadFactory threadFactory = new DaemonThreadFactory("Netty");
		EventLoopGroup bossGroup = new NioEventLoopGroup(config.getAcceptThreadNum(),threadFactory);
        EventLoopGroup workerGroup = new NioEventLoopGroup(config.getWorkThreadNum(),threadFactory);
        EventLoopGroup bossGroupHttp = new NioEventLoopGroup(config.getHttpAcceptThreadNum(),threadFactory);
        EventLoopGroup workerGroupHttp = new NioEventLoopGroup(config.getHttpWorkThreadNum(),threadFactory);
        try 
        {
        	final MessageHandler messageHandler = new MessageHandler(this.client,config.getBackendTimeout());
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
            
            log.info("tcp listen port "+config.getPort());

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
	                if(config.isHttpEnableGZip()){
	                    p.addLast(new HttpContentCompressor());
	                }
	                p.addLast(new HttpHandler(client,config.getBackendTimeout()));
            	}
			});

            httpBoot.bind(config.getHttpPort()).sync();

            log.info("http listen port " + config.getHttpPort());

            
            ChannelFuture future = boot.bind(config.getPort()).sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            throw e;
        }
	}

}
