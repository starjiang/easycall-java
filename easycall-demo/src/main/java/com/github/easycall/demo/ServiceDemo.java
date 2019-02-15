package com.github.easycall.demo;
import com.github.easycall.core.service.EasyService;
import com.github.easycall.core.util.EasyConfig;

/**
 * Echoes back any received data from a client.
 */
public final class ServiceDemo {

    public static void main(String[] args) throws Exception {

    	String zkConnStr = EasyConfig.instance.getString("service.zk","127.0.0.1:2181");

		EasyService service = new EasyService(zkConnStr);
    	service.create("profile", 8001, DemoWorker.class);
    	service.startAndWait();
    	
    }
}
