package com.github.easycall.proxy;

import com.github.easycall.proxy.server.ProxyServer;
import com.github.easycall.proxy.util.ProxyConfig;
import com.github.easycall.util.EasyConfig;

public class Proxy {

	public static void main(String[] args) throws Exception{

		ProxyServer server = new ProxyServer(new ProxyConfig(EasyConfig.instance.getByPrefix("proxy.")));
		server.startAndWait();
	}

}
