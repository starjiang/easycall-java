package com.github.easycall.demo;

import com.github.easycall.core.client.EasyClient;
import com.github.easycall.core.client.lb.LoadBalance;
import com.github.easycall.core.util.EasyPackage;
import com.github.easycall.core.util.Utils;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.internal.operators.single.SingleCreate;

import java.util.concurrent.CompletableFuture;

public class RxRequestDemo {

    public static void main(String[] args) throws Exception {

        EasyClient client = new EasyClient("127.0.0.1:2181", 4, LoadBalance.LB_ACTIVE);
        Single<EasyPackage> single = client.asyncRxRequest("profile", "getProfile", Utils.json.createObjectNode(), 1000);
        single.doOnEvent((respPkg,throwable)->{
            if(throwable == null){
                System.out.println(respPkg.getBody().toString());
            }else{
                throwable.printStackTrace();
            }
        }).subscribe((respPkg,throwable)->{
            if(throwable == null){
                System.out.println(respPkg.getBody().toString());
            }else{
                throwable.printStackTrace();
            }
        });

        System.out.println(SingleCreate.class.getName()+CompletableFuture.class.getName());
        System.in.read();
    }
}
