package com.github.easycall.demo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.client.EasyClient;
import com.github.easycall.client.lb.LoadBalance;
import com.github.easycall.util.EasyHead;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;


public class RequestDirectDemo {

    public static void main(String[] args) throws Exception
    {

        if(args.length < 1){
            System.out.println("usage:request zk");
            return;
        }

        String zkConnStr = args[0];
        try
        {
            EasyClient client = new EasyClient(zkConnStr,4, LoadBalance.LB_ROUND_ROBIN);
            EasyHead reqHead = EasyHead.newInstance().setService("profile").setMethod("getProfile");
            ObjectNode reqBody = Utils.json.createObjectNode().put("uid",100000).put("version",11);
            EasyPackage pkg = client.syncRequest(EasyPackage.FORMAT_JSON,reqHead,reqBody, 1000,"127.0.0.1",8001);
            System.out.println(pkg.getBody().toString());

        }
        catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

}

