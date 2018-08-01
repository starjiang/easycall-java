package com.github.easycall.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.easycall.client.lb.*;
import com.github.easycall.exception.EasyException;
import com.github.easycall.util.Utils;
import com.github.easycall.util.ZkStringSerializer;

import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeManager {
	
	static Logger logger = LoggerFactory.getLogger(NodeManager.class);

	private ConcurrentHashMap<String,ArrayList<Node>> serverMap;
	private HashMap<String,Long> exsitMap;
	private HashMap<String,ReentrantLock> lockMap;
	private ZkClient client;
	private final static int ZK_SESSION_TIMEOUT = 10000;
	private final static int ZK_CONNECT_TIMEOUT = 2000;
	private final static int ZK_NOT_EXSIT_CACHE_TIME = 5000;
	
	public NodeManager(String zkConnStr)
	{
		client = new ZkClient(zkConnStr, ZK_SESSION_TIMEOUT,ZK_CONNECT_TIMEOUT,new ZkStringSerializer());
		serverMap = new ConcurrentHashMap<>();
		exsitMap = new HashMap<>();
		lockMap = new HashMap<>();
	}
		
	private void getNodesFromZk(String name) throws Exception
	{
		String path;
		
		if(name.startsWith("/"))
		{
			path = name;
		}
		else
		{
			path = "/"+name+"/nodes";
		}

		if(!client.exists(path)){
			return;
		}

		List<String> list = client.getChildren(path);
		
		if(list == null)
		{
			return;
		}
		
		ArrayList<Node> nodeList = new ArrayList<>();

		HashSet<String> set = new HashSet<>();
		for(int i=0;i<list.size();i++)
		{
			String data = client.readData(path+"/"+list.get(i));
			JsonNode value = Utils.json.readTree(data);
			Node node = new Node(name,value.get("ip").asText(), value.get("port").asInt(), value.get("weight").asInt());
			set.add(node.ip+":"+node.port);
			nodeList.add(node);		
		}

		logger.info(name+" "+set.toString());
		serverMap.put(name, nodeList);
	}
	
	public Node getNode(String name,int loadBalanceType,String routeKey) throws Exception
	{
		ReentrantLock lock;
		synchronized (this){
			lock =  lockMap.get(name);
			if(lock == null) {
				lock = new ReentrantLock();
				lockMap.put(name,lock);
			}
		}

		ArrayList<Node> list;
		lock.lock();
		try{

			name = Utils.ZOOKEEPER_SERVICE_PREFIX+"/"+name+"/nodes";
			list = serverMap.get(name);

			if(list == null)
			{
				Long currentTime = System.currentTimeMillis();

				if(exsitMap.getOrDefault(name,0L)+ZK_NOT_EXSIT_CACHE_TIME > currentTime){
					return null;
				}

				getNodesFromZk(name);
				list = serverMap.get(name);

				if(list == null)
				{
					exsitMap.put(name,currentTime);
					return null;
			 	}
				else
				{
					client.subscribeChildChanges(name, new IZkChildListener() {

						public void handleChildChange(String parentPath,List<String> currentChildren) throws Exception
						{
							logger.info(parentPath+" changes reload");
							getNodesFromZk(parentPath);
						}
					});
				}
			}
		}finally {
			lock.unlock();
		}


		if(loadBalanceType == LoadBalance.LB_ACTIVE){
			ActiveLoadBalance activeLoadBalance = new ActiveLoadBalance();
            activeLoadBalance.setNodeList(list);
            return activeLoadBalance.getNode();
        } else if(loadBalanceType == LoadBalance.LB_HASH){
            HashLoadBalance hashLoadBalance = new HashLoadBalance();
			hashLoadBalance.setRouteKey(routeKey);
            hashLoadBalance.setNodeList(list);
            return hashLoadBalance.getNode();
        }else if(loadBalanceType == LoadBalance.LB_RANDOM){
			RandomLoadBalance randomLoadBalance = new RandomLoadBalance();
            randomLoadBalance.setNodeList(list);
            return randomLoadBalance.getNode();
        }else if(loadBalanceType == LoadBalance.LB_RANDOM_WEIGHT){
			RandomWeightLoadBalance randomWeightLoadBalance = new RandomWeightLoadBalance();
            randomWeightLoadBalance.setNodeList(list);
            return randomWeightLoadBalance.getNode();
        }else if(loadBalanceType == LoadBalance.LB_ROUND_ROBIN){
			RoundRobinLoadBalance roundRobinLoadBalance = new RoundRobinLoadBalance();
            roundRobinLoadBalance.setNodeList(list);
            return roundRobinLoadBalance.getNode();
        }else {
		    throw new EasyException("invalid loadbalance type");
        }
	}
}
