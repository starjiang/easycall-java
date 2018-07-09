package com.github.easycall.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.HashSet;
import java.util.List;
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

	private ConcurrentHashMap<String,CopyOnWriteArrayList<Node>> serverMap;
	private ZkClient client;
	private final static int ZK_SESSION_TIMEOUT = 10000;
	private final static int ZK_CONNECT_TIMEOUT = 2000;
	
	public NodeManager(String zkConnStr)
	{
		client = new ZkClient(zkConnStr, ZK_SESSION_TIMEOUT,ZK_CONNECT_TIMEOUT,new ZkStringSerializer());
		serverMap = new ConcurrentHashMap<>();
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
		
		List<String> list = client.getChildren(path);
		
		if(list == null)
		{
			return;
		}
		
		CopyOnWriteArrayList<Node> nodeList = new CopyOnWriteArrayList<>();

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
		CopyOnWriteArrayList<Node> list;
		
		synchronized (this)
		{
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
		}

		if(loadBalanceType == LoadBalance.LB_ACTIVE){
            LoadBalance lb = new ActiveLoadBalance();
            lb.setNodeList(list);
            return lb.getNode();
        } else if(loadBalanceType == LoadBalance.LB_CONSISTENT_HASH){
            LoadBalance lb = new ConsistentHashLoadBalance(routeKey);
            lb.setNodeList(list);
            return lb.getNode();
        }else if(loadBalanceType == LoadBalance.LB_RANDOM){
            LoadBalance lb = new RandomLoadBalance();
            lb.setNodeList(list);
            return lb.getNode();
        }else if(loadBalanceType == LoadBalance.LB_RANDOM_WEIGHT){
            LoadBalance lb = new RandomWeightLoadBalance();
            lb.setNodeList(list);
            return lb.getNode();
        }else if(loadBalanceType == LoadBalance.LB_ROUND_ROBIN){
            LoadBalance lb = new RoundRobinLoadBalance();
            lb.setNodeList(list);
            return lb.getNode();
        }else {
		    throw new EasyException("invalid loadbalance type");
        }
	}
}
