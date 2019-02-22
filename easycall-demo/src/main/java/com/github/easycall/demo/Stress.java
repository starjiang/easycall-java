package com.github.easycall.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.core.client.EasyClient;
import com.github.easycall.core.client.lb.LoadBalance;
import com.github.easycall.core.util.Utils;

class Task implements Runnable
{
	static private AtomicInteger currCount = new AtomicInteger(1);

	private EasyClient client;

	public Task(EasyClient client){
	    this.client = client;
    }
	
	public void run()
	{
		try 
		{
			ObjectNode reqBody = Utils.json.createObjectNode();
			
			client.syncRequest("profile","setProfile", reqBody,1000);
			int count = currCount.getAndAdd(1);
			if(count % 10000 == 0)
			{
				System.out.println(count+" requests completed");
			}
		} 
		catch (Throwable e) 
		{
			e.printStackTrace();			
		}
	}
	
	public static int getCurRCount()
	{
		return currCount.get();
	}
}

public class Stress {

	public static void main(String[] args) throws Exception
	{
		// TODO Auto-generated method stub
		if(args.length < 3)
		{
			System.out.println("usage:stress zkConnStr request_num request_threads");
			System.exit(0);
		}
		String zkConnStr = args[0];
		int requestSize = Integer.parseInt(args[1]);
		int poolSize = Integer.parseInt(args[2]);
		
		ExecutorService pool = Executors.newFixedThreadPool(poolSize);
		EasyClient client = new EasyClient(zkConnStr, 8, LoadBalance.LB_ACTIVE);
		
		long start = System.currentTimeMillis();

		for(int i=0;i<requestSize;i++)
		{
			pool.submit(new Task(client));
		}
		
		pool.shutdown();

		while(true)
		{
			if(pool.awaitTermination(100, TimeUnit.MILLISECONDS))
			{
				break;
			}
		}

		long end = System.currentTimeMillis();
		double requests = (requestSize /(float)(end-start))*1000.0;
		
		System.out.println("client per secend:"+requests);
	}

}
