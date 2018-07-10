# 
easycall 是一款java 微服务框架，轻量,高性能，类似dubbo,motan 微服务框架，主要特性如下：
========================
* 轻量，依赖少，代码量少，方便阅读，虽然轻量，功能齐全
* 完全 scheme free 接口调用
* 支持跨语言调用
* 数据序列化支持 json/msgpack
* 支持同步，异步调用，支持服务异步写法
* 负责均衡支持随机，轮询，随机权重，动态负载，一致性hash 五种负载均衡算法
* 已经集成配置中心，配置中心基于zookeepr
* 支持熔断机制
* 支持接入网关，网关支持http,easycall 自定义协议
* 目前服务注册只支持zookeeper
* 后续会支持go/c++语言版本，还有其他语言客户端

关于easycall 学习使用建议
=============
* easycall 代码量非常少，没有java 大部分框架的繁杂的类的继承实现，层级
* 不论是学习微服务框架，还是使用，建议把代码阅读一遍，了解其基本实现思路
* 如有问题，欢迎咨询

关于对spring 支持
===============
* 考虑到spring/spring boot 跟微服务的思路有大的不同（好用，但屏蔽了太多细节，无论在代码可调试性，启动速度，都显得过于繁杂，重量级），不打算支持spring,也推荐微服务不要依赖spring,虽然你获得了写代码的便利，但失去的对代码的控制
* 有时候重造轮子，未必不是一件好事，原则就是，你能够掌控你自己造的轮子

服务端列子
========
服务主类
--------
public final class ServiceDemo {

    public static void main(String[] args) throws Exception {	
    	Service.instance.init("127.0.0.1:2181");//初始化服务
	//创建服务,并把服务注册到zookeeper，服务名为profile,端口8001，线程数 32，队列长度 10000，线程池工作模型为随机分发,SyncDemoWorker 为业务类具体实现
    	Service.instance.createSync("profile", 8001,32,10000, Service.WORK_TYPE_RANDOM, SyncDemoWorker.class);
    	Service.instance.startAndWait();启动服务，并阻塞
    	
    }
}
具体业务类
---------
public class SyncDemoWorker {

    private Logger log = LoggerFactory.getLogger(SyncDemoWorker.class);
    /*
    * 服务的方法通过注解映射到对应方法函数
    **/
    @EasyMethod(method="getProfile")
    public void onGetProfile(Request request, Response response) {
    	log.info("req getProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString()); 	
    	ObjectNode respBoby = Utils.json.createObjectNode();//返回包体
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
			//创建客户端调用类参数为zk地址，io 线程数，负载均衡类型
			EasyClient client = new EasyClient(zkConnStr,4, LoadBalance.LB_ROUND_ROBIN);

			ObjectNode reqBody = Utils.json.createObjectNode();
			reqBody.put("uid",100000).put("seq",0);
			//调用profile 服务的getProfile 方法，请求body 为reqBody，默认用msgpack 方式序列化，超时时间1000ms	
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
