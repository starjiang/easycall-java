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
	private HashMap<String,ReentrantLock> lockMap;
	private ZkClient client;
	private final static int ZK_SESSION_TIMEOUT = 10000;
	private final static int ZK_CONNECT_TIMEOUT = 2000;
	private ActiveLoadBalance activeLoadBalance;
	private ConsistentHashLoadBalance consistentHashLoadBalance;
	private RandomLoadBalance randomLoadBalance;
	private RandomWeightLoadBalance randomWeightLoadBalance;
	private RoundRobinLoadBalance roundRobinLoadBalance;
	
	public NodeManager(String zkConnStr)
	{
		client = new ZkClient(zkConnStr, ZK_SESSION_TIMEOUT,ZK_CONNECT_TIMEOUT,new ZkStringSerializer());
		serverMap = new ConcurrentHashMap<>();
		lockMap = new HashMap<>();
		activeLoadBalance = new ActiveLoadBalance();
		consistentHashLoadBalance = new ConsistentHashLoadBalance();
		randomLoadBalance = new RandomLoadBalance();
		randomWeightLoadBalance = new RandomWeightLoadBalance();
		roundRobinLoadBalance = new RoundRobinLoadBalance();
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

		try{

			lock.lock();

			name = Utils.ZOOKEEPER_SERVICE_PREFIX+"/"+name+"/nodes";
			list = serverMap.get(name);

			if(list == null)
			{
				getNodesFromZk(name);
				list = serverMap.get(name);

				if(list == null)
				{
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
            activeLoadBalance.setNodeList(list);
            return activeLoadBalance.getNode();
        } else if(loadBalanceType == LoadBalance.LB_CONSISTENT_HASH){
            consistentHashLoadBalance.setRouteKey(routeKey);
            consistentHashLoadBalance.setNodeList(list);
            return consistentHashLoadBalance.getNode();
        }else if(loadBalanceType == LoadBalance.LB_RANDOM){
            randomLoadBalance.setNodeList(list);
            return randomLoadBalance.getNode();
        }else if(loadBalanceType == LoadBalance.LB_RANDOM_WEIGHT){
            randomWeightLoadBalance.setNodeList(list);
            return randomWeightLoadBalance.getNode();
        }else if(loadBalanceType == LoadBalance.LB_ROUND_ROBIN){
            roundRobinLoadBalance.setNodeList(list);
            return roundRobinLoadBalance.getNode();
        }else {
		    throw new EasyException("invalid loadbalance type");
        }
	}
}
