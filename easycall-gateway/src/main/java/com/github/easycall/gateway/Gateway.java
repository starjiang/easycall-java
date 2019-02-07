package com.github.easycall.gateway;

import com.github.easycall.gateway.server.GatewayServer;
import com.github.easycall.gateway.util.GatewayConfig;
import com.github.easycall.core.util.EasyConfig;


public class Gateway {

	public static void main(String[] args) throws Exception{

		GatewayServer server = new GatewayServer(new GatewayConfig(EasyConfig.instance.getByPrefix("gateway.")));
		server.startAndWait();
	}

}
