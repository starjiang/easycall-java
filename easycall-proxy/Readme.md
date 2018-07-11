Proxy 网关支持的协议
1.easycall 原生协议
2.http json 协议
3.http 透传 easycall 协议

http json 协议传输数据如下

POST /call HTTP/1.1
Host: 127.0.0.1:8008
X-Easycall-Service: profile
X-Easycall-Method: getProfile
Cache-Control: no-cache
Postman-Token: c76b6f47-0ff1-4791-be47-9e2da2f195c2

{}
-------------------------------------------------------

X-Easycall-Service 服务名头部字段
X-Easycall-Method 方法名头部字段

如果需要增加头部字段，按照以下方式进行

X-Easycall-Xxxxxx 会转换为easycall 协议头部的xxxxxx字段，例如
X-Easycall-Service 会转换为 easycall 协议头部的service 字段
----------------------------------------------------------

easycall 原生协议
走的easycall 原生协议

----------------------------------------------------------

http 透传easycall 协议包
http post body 透传easycall 包

