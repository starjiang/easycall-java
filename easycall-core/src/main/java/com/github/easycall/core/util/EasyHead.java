package com.github.easycall.core.util;
import com.fasterxml.jackson.annotation.JsonInclude;

/*
 * service string 服务名，必须字段
 * method string 方法名，必须字段
 * routeKey string 路由key，负载均衡用,当采用consistent hash算法时用到，可选字段
 * requestIp string 请求ip，内网请求为请求机器ip,外网请求为外网ip,可选字段
 * traceId string 请求跟踪id,全局唯一，可选字段
 * seq long 内网异步调用时,回调时用，可选字段
 * ret int 错误码 必填字段，0 为正常返回，非0 为错误码
 * msg string 错误信息  可选字段，ret 非0，时需要设置msg
 * uid long 用户id，可选字段
 * sig 用户登录签名，可选字段
 * 头部字段可随意添加
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EasyHead {
    private String service;
    private String method;
    private String routeKey;
    private String requestIp;
    private String traceId;
    private Long seq;
    private Integer ret;
    private String msg;
    private Long uid;
    private String sig;

    static public EasyHead newInstance(){
        return new EasyHead();
    }

    public EasyHead(){

    }

    public String getService() {
        return service;
    }

    public EasyHead setService(String service) {
        this.service = service;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public EasyHead setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public EasyHead setRouteKey(String routeKey) {
        this.routeKey = routeKey;
        return this;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public EasyHead setRequestIp(String requestIp) {
        this.requestIp = requestIp;
        return this;
    }

    public String getTraceId() {
        return traceId;
    }

    public EasyHead setTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    public Long getSeq() {
        return seq;
    }

    public EasyHead setSeq(Long seq) {
        this.seq = seq;
        return this;
    }

    public Integer getRet() {
        return ret;
    }

    public EasyHead setRet(Integer ret) {
        this.ret = ret;
        return this;
    }

    public String getMsg() {
        return msg;
    }

    public EasyHead setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    public Long getUid() {
        return uid;
    }

    public EasyHead setUid(Long uid) {
        this.uid = uid;
        return this;
    }

    public String getSig() {
        return sig;
    }

    public EasyHead setSig(String sig) {
        this.sig = sig;
        return this;
    }

    public String toString(){
        String data = "";
        try{
            data  = Utils.json.writeValueAsString(this);
        }catch (Exception e){
            e.printStackTrace();
        }
        return data;
    }
}
