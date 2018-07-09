package com.github.easycall.demo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.client.EasyClient;
import com.github.easycall.client.lb.LoadBalance;
import com.github.easycall.util.EasyPackage;
import com.github.easycall.util.Utils;


public class RequestDemo {

	static final String SERVICE_DEMO = "profile";
	
	public static void main(String[] args) throws Exception
	{
    	String zkConnStr = "172.28.2.162:2181";

		try
		{
			EasyClient client = new EasyClient(zkConnStr,4, LoadBalance.LB_ROUND_ROBIN);

			System.out.println("client...");
			ObjectNode reqBody = Utils.json.createObjectNode();
			reqBody.put("uid",100000).put("seq",0);

			EasyPackage pkg = client.syncRequest("profile","getProfile",reqBody, 1000);
			System.out.println(pkg.getBody().toString());

		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}

}
