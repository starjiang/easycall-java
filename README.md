# 
easycall 是一款java 微服务框架，轻量,高性能，类似dubbo,motan 微服务框架，主要特性如下：
========================
* 完全 scheme free
* 支持跨语言调用
* 数据序列化支持 json/msgpack
* 支持同步，异步调用，支持服务异步写法
* 负责均衡支持随机，轮询，随机权重，动态负载，一致性hash 五种负载均衡算法
* 已经集成配置中心，配置中心基于zookeepr
* 支持熔断
* 支持接入网关，网关支持http,easycall 自定义协议
* 目前服务注册只支持zookeeper
* 后续会支持go/c++语言版本，还有其他语言客户端

服务端列子
========
服务主类
--------
public final class ServiceDemo {

    public static void main(String[] args) throws Exception {	
    	Service.instance.init("127.0.0.1:2181");//初始化服务
        //创建服务，服务名为profile,端口8001，线程数 32，队列长度 10000，线程池工作模型为随机分发 SyncDemoWorker 为业务类
    	Service.instance.createSync("profile", 8001,32,10000, Service.WORK_TYPE_RANDOM, SyncDemoWorker.class);
    	Service.instance.startAndWait();
    	
    }
}
具体业务类
---------
public class SyncDemoWorker {

	private Logger log = LoggerFactory.getLogger(SyncDemoWorker.class);
    
    @EasyMethod(method="getProfile")
    public void onGetProfile(Request request, Response response) {
    	log.info("req getProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString()); 	
    	ObjectNode respBoby = Utils.json.createObjectNode();
    	respBoby.put("msg","ok");
    	respBoby.put("ret",0);
    	response.setHead(request.getHead()).setBody(respBoby);
    }
    
    @EasyMethod(method="setProfile")
    public void onSetProfile(Request request, Response response) {
    	
    	log.info("req setProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString())
		  ObjectNode respBoby = Utils.json.createObjectNode();
    	respBoby.put("msg","ok");
    	respBoby.put("ret",0);
    	response.setHead(request.getHead()).setBody(respBoby);
    }
}
客户端调用
---------

public class RequestDemo {
	
	public static void main(String[] args) throws Exception
	{
    String zkConnStr = "127.0.0.1:2181";

		try
		{
			EasyClient client = new EasyClient(zkConnStr,4, LoadBalance.LB_ROUND_ROBIN);
      
			ObjectNode reqBody = Utils.json.createObjectNode();
			reqBody.put("uid",100000).put("seq",0);

			EasyPackage pkg = client.syncRequest("profile","getProfile",reqBody, 1000);
			System.out.println(pkg.getBody().toString());

		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}
}

以上代码可以在com.github.easycall.demo 包底下找到
