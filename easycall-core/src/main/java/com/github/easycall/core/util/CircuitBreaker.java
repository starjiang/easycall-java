package com.github.easycall.core.util;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

class CbInfo{

    static public final int CB_OPEN = 1;
    static public final int CB_CLOSE = 0;
    static public final int CB_LIMIT = 2;

    public AtomicLong invokeCount;
    public AtomicLong failCount;
    public long lastCircuitBreakerTime;
    public long lastLimitPassTime;
    public long lastResetTime;
    public volatile  int status;
    public double failRate;
    public double limitRate;
    public long countBase;
    public long failTime;
    public long limitTime;
    public long resetTime;

    public CbInfo(){
        invokeCount = new AtomicLong(1);
        failCount = new AtomicLong(0);
        lastCircuitBreakerTime = 0;
        lastLimitPassTime = 0;
        lastResetTime = System.currentTimeMillis();
        status = CB_CLOSE;
        failRate = CircuitBreaker.FAIL_RATE;
        limitRate = CircuitBreaker.LIMIT_RATE;
        countBase = CircuitBreaker.COUNT_BASE;
        failTime = CircuitBreaker.FAIL_TIME;
        limitTime = CircuitBreaker.LIMIT_TIME;
        resetTime = CircuitBreaker.RESET_TIME;
    }
}



public class CircuitBreaker {

    public static Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    public final static double FAIL_RATE = 0.5;
    public final static double LIMIT_RATE = 0.2;
    public final static long COUNT_BASE = 10;
    public final static long FAIL_TIME = 30000;
    public final static long LIMIT_TIME = 30000;
    public final static long RESET_TIME = 60000;

    private static ConcurrentHashMap<String, CbInfo> infoMap = new ConcurrentHashMap<>();
    private static HashMap<String, Object> lockMap = new HashMap<>();


    public static void configure(String cbName, double failRate, double limitRate, long countBase, long failTime, long limitTime, long resetTime) {

        Object cbLock;
        synchronized (CircuitBreaker.class) {
            cbLock = lockMap.get(cbName);
            if (cbLock == null) {
                cbLock = new Object();
                lockMap.put(cbName, cbLock);
            }
        }

        synchronized (cbLock){
            CbInfo info;
            info = infoMap.get(cbName);
            if (info == null) {
                info = new CbInfo();
                infoMap.put(cbName, info);
            }
            info.failRate = failRate;
            info.limitRate = limitRate;
            info.countBase = countBase;
            info.failTime = failTime;
            info.limitTime = limitTime;
            info.resetTime = resetTime;
        }
    }

    public static boolean checkStatus(String cbName){

        long timeNow = System.currentTimeMillis();

        Object cbLock;
        synchronized (CircuitBreaker.class) {
            cbLock = lockMap.get(cbName);
            if (cbLock == null) {
                cbLock = new Object();
                lockMap.put(cbName, cbLock);
            }
        }

        synchronized (cbLock){
            CbInfo info;
            info = infoMap.get(cbName);

            if (info == null) {
                info = new CbInfo();
                infoMap.put(cbName, info);
            }

            //熔断过期后，把熔断器状态设置为半开状
            if (info.status == CbInfo.CB_OPEN && info.lastCircuitBreakerTime + info.failTime < timeNow) {
                info.status = CbInfo.CB_LIMIT;
                info.failCount.set(0);
                info.invokeCount.set(1);
                info.lastLimitPassTime = timeNow;
                info.lastCircuitBreakerTime = 0;
                log.error("CircuitBreaker {} set status limit", cbName);
            }

            boolean ptFlag = false;
            if (info.status == CbInfo.CB_OPEN) {
                ptFlag = false;
            } else if (info.status == CbInfo.CB_LIMIT) {
                //半开状态下，随机计算允许通过的请求
                double rand = Math.random();
                if (rand < info.limitRate) {
                    ptFlag = true;
                }
            } else {
                ptFlag = true;
            }

            if (!ptFlag) {
                return false;
            }

            //计算熔断阀值，超过阀值，熔断
            double f = (double) info.failCount.get() / (double) info.invokeCount.get();

            if (f > info.failRate && info.invokeCount.get() > info.countBase) {
                info.status = CbInfo.CB_OPEN;
                info.lastCircuitBreakerTime = timeNow;
                log.error("CircuitBreaker {} set status open", cbName);
                return false;
            }

            //半开状态下，请求没超过阀值，关闭熔断器
            if (info.status == CbInfo.CB_LIMIT && info.lastLimitPassTime + info.limitTime < timeNow) {
                info.status = CbInfo.CB_CLOSE;
                info.invokeCount.set(1);
                info.failCount.set(0);
                info.lastLimitPassTime = 0;
                log.error("CircuitBreaker {} set status close", cbName);
            }

            //重置统计
            if (info.status == CbInfo.CB_CLOSE && info.lastResetTime + info.resetTime < timeNow) {
                info.lastResetTime = timeNow;
                info.failCount.set(0);
                info.invokeCount.set(1);
                log.info("CircuitBreaker {} reset", cbName);
            }
            return true;
        }

    }

    public static <R> R call(String cbName, UncheckedFunction<R> supplier, UncheckedFunction<R> supplierDefault) throws Exception {

        if(!checkStatus(cbName)){
            return supplierDefault.apply();
        }

        CbInfo info = infoMap.get(cbName);
        info.invokeCount.incrementAndGet();

        try {
            return supplier.apply();
        } catch (Exception e) {
            info.failCount.incrementAndGet();
            throw e;
        }
    }

    public static <T> CompletableFuture<T> asyncCall(String cbName, UncheckedFunction<CompletableFuture<T>> supplier, UncheckedFunction<CompletableFuture<T>> supplierDefault) throws Exception {

        if(!checkStatus(cbName)){
            return supplierDefault.apply();
        }

        CbInfo info = infoMap.get(cbName);
        info.invokeCount.incrementAndGet();

        CompletableFuture<T> future = supplier.apply();
        future.whenComplete((t, e) -> {
            if(e != null){
                info.failCount.incrementAndGet();
            }
        });

        return future;
    }

    public static <T> Single<T> asyncRxCall(String cbName, UncheckedFunction<Single<T>> supplier, UncheckedFunction<Single<T>> supplierDefault) throws Exception {

        if(!checkStatus(cbName)){
            return supplierDefault.apply();
        }

        CbInfo info = infoMap.get(cbName);
        info.invokeCount.incrementAndGet();

        Single<T> single = supplier.apply();
        return single.doOnEvent((t, e) -> {
            if(e != null){
                info.failCount.incrementAndGet();
            }
        });
    }
}