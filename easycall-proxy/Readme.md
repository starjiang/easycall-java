Proxy 网关支持的协议
=================

##### 1.easycall 原生协议,proxy 负责把请求转发到相应的服务
##### 2.http json 协议，通过调用/call，proxy 提取http 头部字段,填充到easycall head 头部，post body json 作为 easycall body，然后转发
##### 3.http 透传 easycall 协议，通过调用/rawcall,proxy 直接把post body 数据当做easycall 包转发

http json 协议传输数据如下
-------------------------
<pre>
POST /call HTTP/1.1
Host: 127.0.0.1:8008
X-Easycall-Service: profile
X-Easycall-Method: getProfile
Cache-Control: no-cache
Postman-Token: c76b6f47-0ff1-4791-be47-9e2da2f195c2

{}
-------------------------------
以上是一个完整的向代理服务发的http 包
具体说明：
X-Easycall-Service 服务名头部字段
X-Easycall-Method 方法名头部字段

如果需要增加头部字段，按照以下方式进行

X-Easycall-Xxxxxx 会转换为easycall 协议头部的xxxxxx字段，例如
X-Easycall-Service 会转换为 easycall 协议头部的service 字段
</pre>

easycall 原生协议
-----------------
走的easycall 原生协议,透传easycall 包

http 透传easycall 协议包
----------------------
http post body 透传easycall 包

<pre>
网关使用了netty zero copy 技术，性能是非常之高
网关完全异步化，最大化了吞吐量
</pre>
