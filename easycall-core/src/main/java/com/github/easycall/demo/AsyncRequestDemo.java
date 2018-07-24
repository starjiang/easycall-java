package com.github.easycall.demo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.client.EasyClient;
import com.github.easycall.client.ResponseFuture;
import com.github.easycall.client.lb.LoadBalance;
import com.github.easycall.util.EasyHead;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;

public class AsyncRequestDemo {

    public static void main(String[] args) throws Exception
    {
        System.out.println(args.length);
        if(args.length < 2){
            System.out.println("usage:request zk request_count");
            return;
        }

        String zkConnStr = args[0];
        int requestCount = Integer.valueOf(args[1]);

        try
        {
            EasyClient client = new EasyClient(zkConnStr,2, LoadBalance.LB_ACTIVE);

            for(int i=0;i<requestCount;i++){
                EasyHead reqHead = EasyHead.newInstance().setService("profile").setMethod("getProfile");
                ObjectNode reqBody = Utils.json.createObjectNode().put("uid",100000);

                try{
                    ResponseFuture future = client.asyncRequest(EasyPackage.FORMAT_MSGPACK,reqHead, reqBody, 1000);

                    future.setCallback((responseFuture) ->{

                        if(responseFuture.isException()){
                            responseFuture.getException().printStackTrace();
                        }else{
                            System.out.println(responseFuture.getResult().getBody().toString());
                        }

                    });

                }catch (Exception e){
                    e.printStackTrace();
                }
            }

        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }

        System.in.read();
    }

}
