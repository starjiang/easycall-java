package com.github.easycall.demo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.client.EasyClient;
import com.github.easycall.client.lb.LoadBalance;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;


public class RequestDirectDemo {

    static final String SERVICE_DEMO = "profile";

    public static void main(String[] args) throws Exception
    {
        String zkConnStr = "127.0.0.1:2181";

        try
        {
            EasyClient client = new EasyClient(zkConnStr,4, LoadBalance.LB_ROUND_ROBIN);

            System.out.println("client...");
            ObjectNode reqHead = Utils.json.createObjectNode();
            ObjectNode reqBody = Utils.json.createObjectNode();
            reqHead.put("service","profile").put("method","getProfile");
            reqBody.put("uid",100000).put("version",11);

            EasyPackage pkg1 = client.syncRequest(EasyPackage.FORMAT_MSGPACK,reqHead,reqBody, 1000,"127.0.0.1",8001);
            System.out.println(pkg1.getBody().toString());

        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

}

