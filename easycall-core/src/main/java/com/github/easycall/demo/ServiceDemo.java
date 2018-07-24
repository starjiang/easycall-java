package com.github.easycall.demo;
import com.github.easycall.service.EasyService;
import com.github.easycall.util.EasyConfig;

/**
 * Echoes back any received data from a client.
 */
public final class ServiceDemo {

    public static void main(String[] args) throws Exception {

    	String zkConnStr = EasyConfig.instance.getString("service.zk","127.0.0.1:2181");

		EasyService service = new EasyService(zkConnStr);
    	service.createSync("profile", 8001, SyncDemoWorker.class);
    	service.createAsync("profileAsync",8002,AsyncDemoWorker.class);

    	service.startAndWait();
    	
    }
}
