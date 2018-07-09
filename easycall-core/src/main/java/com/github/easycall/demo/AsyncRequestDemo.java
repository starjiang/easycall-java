package com.github.easycall.demo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.client.EasyClient;
import com.github.easycall.client.ResponseFuture;
import com.github.easycall.client.lb.LoadBalance;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;

public class AsyncRequestDemo {
    static final String SERVICE_DEMO = "profile";

    public static void main(String[] args) throws Exception
    {
        String zkConnStr = "127.0.0.1:2181";

        try
        {
            EasyClient client = new EasyClient(zkConnStr,2, LoadBalance.LB_ACTIVE);

            while(true){
                for(int i=0;i<10;i++){
                    ObjectNode reqHead = Utils.json.createObjectNode();
                    ObjectNode reqBody = Utils.json.createObjectNode();
                    reqHead.put("service","profile");
                    reqHead.put("method","getProfile");
                    reqBody.put("uid",100000);

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

                Thread.sleep(2000);
            }


        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

}
