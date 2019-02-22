package com.github.easycall.demo;

import com.github.easycall.core.client.EasyClient;
import com.github.easycall.core.client.lb.LoadBalance;
import com.github.easycall.core.util.EasyPackage;
import com.github.easycall.core.util.Utils;

import java.util.concurrent.CompletableFuture;

public class CompletableRequestDemo {

    public static void main(String[] args) throws Exception {

        EasyClient client = new EasyClient("127.0.0.1:2181", 4, LoadBalance.LB_ACTIVE);
        CompletableFuture<EasyPackage> future = client.asyncCFRequest("profile", "getProfile", Utils.json.createObjectNode(), 1000);
        future.thenCompose(pkg-> client.asyncCFRequest("profile", "setProfile", Utils.json.createObjectNode(), 1000))
            .whenComplete((pkg,e)->{
                if(e == null){
                    System.out.println(pkg.getBody().toString());
                }else{
                    System.out.println("exception+++++++++++++");
                }
            });
        System.in.read();
    }
}
