package com.github.easycall.gateway.util;

import java.util.Properties;

public class GatewayConfig {
    private String zk;
    private int port;
    private int httpPort;
    private int acceptThreadNum;
    private int workThreadNum;
    private int httpAcceptThreadNum;
    private int httpWorkThreadNum;
    private int backendThreadNum;
    private int backendTimeout;
    private boolean httpEnableGZip;

    public GatewayConfig(){
        init(new Properties());
    }

    public GatewayConfig(Properties properties){

        init(properties);
    }

    private void init(Properties properties){
        this.zk = properties.getProperty("zk","127.0.0.1:2181");
        this.port = Integer.valueOf(properties.getProperty("port","8008"));
        this.httpPort = Integer.valueOf(properties.getProperty("httpPort","8080"));
        this.acceptThreadNum = Integer.valueOf(properties.getProperty("acceptThreadNum","1"));
        this.workThreadNum = Integer.valueOf(properties.getProperty("workThreadNum","4"));
        this.httpWorkThreadNum = Integer.valueOf(properties.getProperty("httpWorkThreadNum","4"));
        this.httpAcceptThreadNum = Integer.valueOf(properties.getProperty("httpAcceptThreadNum","1"));
        this.backendThreadNum = Integer.valueOf(properties.getProperty("backendThreadNum","4"));
        this.backendTimeout = Integer.valueOf(properties.getProperty("backendTimeout","2000"));
        this.httpEnableGZip = Boolean.valueOf(properties.getProperty("httpEnableGZip","false"));
    }

    public String getZk() {
        return zk;
    }

    public void setZk(String zk) {
        this.zk = zk;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getAcceptThreadNum() {
        return acceptThreadNum;
    }

    public void setAcceptThreadNum(int acceptThreadNum) {
        this.acceptThreadNum = acceptThreadNum;
    }

    public int getWorkThreadNum() {
        return workThreadNum;
    }

    public void setWorkThreadNum(int workThreadNum) {
        this.workThreadNum = workThreadNum;
    }

    public int getHttpAcceptThreadNum() {
        return httpAcceptThreadNum;
    }

    public void setHttpAcceptThreadNum(int httpAcceptThreadNum) {
        this.httpAcceptThreadNum = httpAcceptThreadNum;
    }

    public int getHttpWorkThreadNum() {
        return httpWorkThreadNum;
    }

    public void setHttpWorkThreadNum(int httpWorkThreadNum) {
        this.httpWorkThreadNum = httpWorkThreadNum;
    }

    public int getBackendThreadNum() {
        return backendThreadNum;
    }

    public void setBackendThreadNum(int backendThreadNum) {
        this.backendThreadNum = backendThreadNum;
    }

    public int getBackendTimeout() {
        return backendTimeout;
    }

    public void setBackendTimeout(int backendTimeout) {
        this.backendTimeout = backendTimeout;
    }

    public boolean isHttpEnableGZip() {
        return httpEnableGZip;
    }

    public void setHttpEnableGZip(boolean httpEnableGZip) {
        this.httpEnableGZip = httpEnableGZip;
    }
}
