package com.github.easycall.core.test;

import com.github.easycall.core.util.CircuitBreaker;
import com.github.easycall.core.util.EasyPackage;
import com.github.easycall.core.exception.EasyException;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;


public class TestCircuitBreaker {

    static int seq = 0;
    public String call() throws Exception {
        seq++;
        System.out.println("seq="+seq);
        if(seq > 40 && seq < 90){
            throw new EasyException("test exception");
        }
        return "haha";
    }

    @Test
    public void testCircuitBreaker(){

        for(int i=0;i<300;i++){
            try{
                CircuitBreaker.call("cb",()-> call(),()->"default");
                Thread.sleep(100);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
    }

    public CompletableFuture<String> asyncCall() throws Exception{

        CompletableFuture<String> future = new CompletableFuture<>();

        new Thread(()->{
            try{
                Thread.sleep(100);
            }catch (Exception e){}

            seq++;
            System.out.println("seq="+seq);
            if(seq > 40 && seq < 90){
                future.completeExceptionally(new EasyException("test exception"));
            }else{
                future.complete("haha");
            }
        }).start();
        return future;
    }

    @Test
    public void testCircuitBreakerAsync(){

        for(int i=0;i<300;i++){
            try{
                CircuitBreaker.asyncCall("cb",()->asyncCall(),()->CompletableFuture.supplyAsync(()->"default")).thenAccept(msg->System.out.println(msg));
                Thread.sleep(100);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
    }
}
