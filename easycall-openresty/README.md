easycall openresty gateway
=========================
openresty 配置
------------
```
lua_package_path '/Users/mozat/work/easycall/easycall-openresty/?.lua;;';
lua_shared_dict node 10m;

server {
    listen       80;
    server_name  localhost;

    location /api {
        default_type  text/plain;
        if ($request_uri ~ '^/api/(.*?)/(.*?)$'){
            set $service $1;
            set $method $2;
        }
        content_by_lua_file /Users/mozat/work/easycall/easycall-openresty/gateway.lua;
    }
}
```

调用方式
------
 curl -v http://127.0.0.1/api -H'X-Easycall-service:profile' -H'X-Easycall-method:getProfile' -d'{}'
* profile 为 service 名字
* getProfile 为method 名字

http 头对应easycall 协议头如下：
----------
* X-Easycall-service 对应easycall 协议头的service 字段
* X-Easycall-method  对应easycall 协议头的method 字段
* X-Easycall-routeKey 对应easycall 协议头的routeKey 字段
* X-Easycall-requestIp 对应easycall 协议头的requestIp 字段
* X-Easycall-traceId 对应easycall 协议头的traceId 字段
* X-Easycall-seq 对应easycall 协议头的seq 字段
* X-Easycall-ret 对应easycall 协议头的ret字段
* X-Easycall-msg 对应easycall 协议头的msg 字段
* X-Easycall-uid 对应easycall 协议头的uid 字段
* X-Easycall-sig 对应easycall 协议头的sig 字段

通过url 指定 service,method
--------------------------
curl -v http://127.0.0.1/api/profile/getProfile -d'{}'
