easycall 协议说明
============

easycall 字节约定
---------------

* c 一个字节
* w 两个字节
* dw 四个字节
* ddw 八个字节
* s 为buffer
* 字节顺序为网络序

easycall 具体协议如下
--------------------

# cStx+cFormat+dwHeadLen+dwBodyLen+sHeadData+sBodyData+cEtx

* cStx magic number 固定为0x2，为包的起始位置
* cFormat 为head 跟 body 序列化格式 0 为msgpack 1为json,这里可以扩展为其他序列化格式，比如protobuf
* dwHeadLen head buffer 长度,内容为相应序列化格式对应的buffer,最大长度64K
* dwBodyLen body buffer 长度,内容为相应序列化格式对应的buffer,最大长度2M
* sHeadData head 序列化后的buffer
* sBodyData body 序列化后的buffer
* cEtx magic number 固定为0x3，为包的结束位置

协议字段命名约定，统一用驼峰命名
---------------------------
### head 字段说明
##### 字段名 类型 说明
* service string 服务名，必须字段
* method string 方法名，必须字段
* routeKey string 路由key，负载均衡用,当采用consistent hash算法时用到，可选字段
* requestIp string 请求ip，内网请求为请求机器ip,外网请求为外网ip,可选字段
* traceId string 请求跟踪id,全局唯一，可选字段
* seq long 内网异步调用时,回调时用，可选字段
* ret int 错误码 必填字段，0 为正常返回，非0 为错误码
* msg string 错误信息  可选字段，ret 非0，时需要设置msg
* uid long 用户id 可选字段
* sig string 用户登录签名 可选字段
<pre>
头部字段可以随意添加，建议请求的一些公用字段放到头部，比如登录验证的 userId,token
</pre>
