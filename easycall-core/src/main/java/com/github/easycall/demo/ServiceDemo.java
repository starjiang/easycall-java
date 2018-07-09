package com.github.easycall.demo;
import com.github.easycall.service.Service;
import com.github.easycall.util.EasyConfig;

/**
 * Echoes back any received data from a client.
 */
public final class ServiceDemo {

    public static void main(String[] args) throws Exception {
    	
  	
    	String zkConnStr = EasyConfig.instance.getString("service.zk","127.0.0.1:2181");
    	String serviceName= EasyConfig.instance.getString("service.name","profile");
    	int servicePort = EasyConfig.instance.getInt("service.port",8001);
    	int serviceThreadNum = EasyConfig.instance.getInt("service.threadNum",32);
    	
    	Service.instance.init(zkConnStr);
    	Service.instance.createSync(serviceName, servicePort,serviceThreadNum,10000, Service.WORK_TYPE_RANDOM, SyncDemoWorker.class);
    	//Service.instance.createAsync("profile1",servicePort+1,8,AsyncDemoWorker.class);

    	Service.instance.startAndWait();
    	
    }
}
