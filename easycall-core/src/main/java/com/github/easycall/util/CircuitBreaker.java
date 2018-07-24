package com.github.easycall.util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class CbInfo{

    static public final int CB_OPEN = 1;
    static public final int CB_CLOSE = 0;
    static public final int CB_LIMIT = 2;

    public AtomicLong invokeCount;
    public AtomicLong failCount;
    public long lastCircuitBreakerTime;
    public long lastLimitPassTime;
    public long lastResetTime;
    volatile  int status;

    public CbInfo(){
        invokeCount = new AtomicLong(1);
        failCount = new AtomicLong(0);
        lastCircuitBreakerTime = 0;
        lastLimitPassTime = 0;
        lastResetTime = System.currentTimeMillis();
        status = CB_CLOSE;
    }
}



public class CircuitBreaker {

    public static Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    private final static double FAIL_RATE = 0.5;
    private final static double LIMIT_RATE = 0.2;
    private final static long COUNT_BASE = 10;
    private final static long FAIL_TIME = 30000;
    private final static long LIMIT_TIME = 30000;
    private final static long RESET_TIME = 60000;


    private static ConcurrentHashMap<String,CbInfo> infoMap = new ConcurrentHashMap<>();

    public static <R> R call(String cbName, UncheckedFunction<R> supplier, R defaultValue) throws Exception{

        CbInfo info = infoMap.get(cbName);
        long timeNow = System.currentTimeMillis();

        if(info == null){
            info = new CbInfo();
            infoMap.put(cbName,info);
        }

        //熔断过期后，把熔断器状态设置为半开状
        if(info.status == CbInfo.CB_OPEN && info.lastCircuitBreakerTime+FAIL_TIME < timeNow){
            info.status = CbInfo.CB_LIMIT;
            info.failCount.set(0);
            info.invokeCount.set(1);
            info.lastLimitPassTime = timeNow;
            info.lastCircuitBreakerTime = 0;
            log.error("CircuitBreaker {} set status limit",cbName);
        }

        boolean ptFlag = false;
        if(info.status == CbInfo.CB_OPEN){
            ptFlag = false;
        }else if(info.status == CbInfo.CB_LIMIT){
            //半开状态下，随机计算允许通过的请求
            double rand = Math.random();
            if(rand < LIMIT_RATE){
                ptFlag = true;
            }
        }else{
            ptFlag = true;
        }

        if(!ptFlag) return defaultValue;

        //计算熔断阀值，超过阀值，熔断
        double f = (double)info.failCount.get()/(double) info.invokeCount.get();

        if(f > FAIL_RATE && info.invokeCount.get() > COUNT_BASE-1){
            info.status = CbInfo.CB_OPEN;
            info.lastCircuitBreakerTime = timeNow;
            log.error("CircuitBreaker {} set status open",cbName);
            return defaultValue;
        }

        //半开状态下，请求没超过阀值，关闭熔断器
        if(info.status == CbInfo.CB_LIMIT && info.lastLimitPassTime+LIMIT_TIME < timeNow){
            info.status = CbInfo.CB_CLOSE;
            info.invokeCount.set(1);
            info.failCount.set(0);
            info.lastLimitPassTime = 0;
            log.error("CircuitBreaker {} set status close",cbName);
        }

        //重置统计
        if(info.status == CbInfo.CB_CLOSE && info.lastResetTime + RESET_TIME < timeNow){
            info.lastResetTime = timeNow;
            info.failCount.set(0);
            info.invokeCount.set(1);
            log.info("CircuitBreaker {} reset",cbName);
        }

        info.invokeCount.incrementAndGet();

        try{
            return supplier.apply();
        }catch (Exception e){
            info.failCount.incrementAndGet();
            throw e;
        }
    }
}
