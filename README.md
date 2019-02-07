# 
easycall 是一款java 微服务框架，轻量,高性能，类似dubbo,motan 微服务框架，主要特性如下：
========================
* 轻量，依赖少，代码量少，方便阅读，虽然轻量，功能齐全
* 完全 scheme free 接口调用,无需定义interface 接口文件
* 支持跨语言调用python,php,java,c/c++等，凡是支持json/msgpack 序列化的语言都没问题
* 数据序列化支持 json/msgpack
* 客户端支持同步，异步调用；服务端同样支持同步，异步模型，可以实现全异步操作，吞吐量大增
* 负载均衡支持随机，轮询，随机权重，动态负载，hash 五种负载均衡算法
* 已经集成配置中心,实现配置动态加载，集中管理
* 支持熔断机制,方便服务降级，熔断器支持同步，异步模式
* 支持API网关，网关支持http json,easycall协议
* 目前服务注册只支持zookeeper
* go语言版本已经发布，后续会支持c++语言，还有其他语言客户端

关于easycall 学习使用建议
=====================
* easycall 代码量非常少，没有java 大部分框架的繁杂的类的继承实现，层级
* 不论是学习微服务框架，还是使用，建议把代码阅读一遍，了解其基本实现思路
* 如有问题，欢迎咨询,关于本人以前在某大型互联网公司做码农，对大型分布式系统，网络编程，微服务化实践，存储系统有深入研究

关于对spring 支持
===============
* easycall 是轻量级微服务框架，整合到spring 非常容易，demo 里面有相关跟spring 整合的例子

服务端列子
========
服务主类
--------
```
public final class ServiceDemo {

    public static void main(String[] args) throws Exception {

    	String zkConnStr = EasyConfig.instance.getString("service.zk","127.0.0.1:2181");
	EasyService service = new EasyService(zkConnStr);
    	service.createSync("profile", 8001, SyncDemoWorker.class);//创建一个profile 同步微服务，监听端口8001，业务工作类为SyncDemoWorker
    	service.createAsync("profileAsync",8002,AsyncDemoWorker.class);//创建一个profileAsync 异步微服务，监听端口8002，业务工作类为AsyncDemoWorker
    	service.startAndWait();
    	
    }
}
```

具体业务类
---------
```
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
```
客户端调用
---------
```
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
```
以上代码可以在com.github.easycall.demo 包底下找到

配置中心使用说明
----------
* 1.EasyConfig类目前基于http+websocket实现,http 获取远程配置，websocket 解决配置修改通知
* 2.EasyConfig类会默认读取classpath 下的application.properties 配置文件，根据config.name 配置项来读取远程配置
* 3.EasyConfig类功能实现在easycall-core模块里，管理功能实现在easycall-config 模块里
* 4.配置中心使用前，需要设置host,向 /etc/hosts 文件添加 127.0.0.1 config.easycall.com，因为域名写死在代码里面
* 5.配置默认保存运行目录的conf 下，不能修改
配置加载，持久化机制
-----------------
* config.name 配置名，用来区分各模块配置

* 1.EasyConfig类 先读取application.properties 配置获取config.name 配置项
* 2.根据配置检查比较远程配置版本,从./conf/${config.name}/remote/version 文件读取本地版本，从http://config.easycall.com:8080/config/version?name=${config.name} 读取远程版本，本地版本小于远程版本，进入下一步,否则进入到第4步
* 3.从http://config.easycall.com:8080/config/info?name=${config.name} ，持久化到本地,命名为./conf/${config.name}/remote/${config.name}.properties
* 4.读取持久化到本地的配置./conf/${config.name}/remote/${config.name}.properties 如果存在的话
* 5.读取./conf/${config.name}/local/${config.name}.properties 配置，如果存在的话
* 6.EasyConfig类 通过websocket接收版本变化，收到通知，EasyConfig类会reload 配置。

