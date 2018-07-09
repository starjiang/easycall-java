package com.github.easycall.proxy;

import com.github.easycall.proxy.server.Server;
import com.github.easycall.util.EasyConfig;

public class Proxy {

	public static void main(String[] args) throws Exception{
		
		Server server = new Server();
		server.init(EasyConfig.instance.getString("service.zk", "172.28.2.162:2181"),
				EasyConfig.instance.getInt("service.port", 9999),
				EasyConfig.instance.getInt("service.http.port", 8008),
				EasyConfig.instance.getInt("service.accept_thread_num",1),
				EasyConfig.instance.getInt("service.work_thread_num",4),
				EasyConfig.instance.getInt("service.http.accept_thread_num",1),
				EasyConfig.instance.getInt("service.http.work_thread_num",4),
				EasyConfig.instance.getInt("service.backend_thread_num",4),
				EasyConfig.instance.getInt("service.timeout",2000));
		server.startAndWait();
	}

}
