package com.github.easycall.demo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.client.CircuitBreaker;
import com.github.easycall.client.EasyClient;
import com.github.easycall.client.lb.LoadBalance;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;


public class CircuitBreakerDemo {

    public static void main(String[] args) throws Exception
    {
        String zkConnStr = "127.0.0.1:2181";

        try
        {
            EasyClient client = new EasyClient(zkConnStr,4, LoadBalance.LB_ROUND_ROBIN);
            ObjectNode reqBody = Utils.json.createObjectNode();
            reqBody.put("uid",100000).put("seq",0);

            EasyPackage pkg = CircuitBreaker.call("profile",
                    ()->client.syncRequest("profile","getProfile",reqBody, 1000),
                    EasyPackage.newInstance());

            System.out.println(pkg.getBody().toString());

        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

}
