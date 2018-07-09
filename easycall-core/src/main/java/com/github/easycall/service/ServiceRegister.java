package com.github.easycall.service;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.util.Utils;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.easycall.util.ZkStringSerializer;

class HostInfo
{
	public HostInfo(String ip,int port,int weight)
	{
		this.ip = ip;
		this.port = port;
		this.weight = weight;
	}
	public String ip;
	public int port;
	public int weight;
}


public class ServiceRegister {

	public final static Logger log = LoggerFactory.getLogger(ServiceRegister.class);
	public final static ServiceRegister instance = new ServiceRegister();
	private ZkClient zkClient;
	boolean bInit = false;
	private HashMap<String, HostInfo> hostMap;
	private final static int ZK_SESSION_TIMEOUT = 10000;
	private final static int ZK_CONNECT_TIMEOUT = 2000;
	
	protected ServiceRegister(){}
	
	public void init(String zkConnStr)
	{
		if(bInit) 
		{
			return;
		}
		
		hostMap = new HashMap<>();
		
		zkClient = new ZkClient(zkConnStr,ZK_SESSION_TIMEOUT,ZK_CONNECT_TIMEOUT,new ZkStringSerializer());
		
		zkClient.subscribeStateChanges(new IZkStateListener() {
			
			public void handleStateChanged(KeeperState state) throws Exception {
				log.info("zookeeper state:"+state.toString());
			}
			
			public void handleNewSession() throws Exception {
				// TODO Auto-generated method stub
				rebuildSession();
				log.info("session rebuild complete");
			}

			public void handleSessionEstablishmentError(Throwable arg0) throws Exception {
				// TODO Auto-generated method stub
				log.error("session establistment error");
			}
		});
		
		bInit = true;
	}
	
	
	private void rebuildSession() throws Exception
	{
		Iterator<Entry<String,HostInfo>> iterator = hostMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, HostInfo> entry = (Map.Entry<String, HostInfo>) iterator.next();
			String serviceName = entry.getKey();
			HostInfo info = entry.getValue();
			register(serviceName, info.port, info.weight);
		}
	}
	
	public void register(String serviceName,int port,int weight) throws Exception
	{
		if(!zkClient.exists(Utils.ZOOKEEPER_SERVICE_PREFIX+"/"+serviceName+"/nodes"))
		{
			zkClient.createPersistent(Utils.ZOOKEEPER_SERVICE_PREFIX+"/"+serviceName+"/nodes",true);
		}
		
		String ip = Utils.getLocalIp();
		String nodePath = Utils.ZOOKEEPER_SERVICE_PREFIX+"/"+serviceName+"/nodes/"+ip+":"+port;
		
		if(zkClient.exists(nodePath))
		{
			zkClient.delete(nodePath);
		}
		zkClient.createEphemeral(nodePath, toNodeData(ip, port, weight));
		hostMap.put(serviceName, new HostInfo(ip, port, weight));
	}
		
	public void register(String serviceName,int port) throws Exception
	{
		register(serviceName, port, 100);
	}
	
	public void unregister(String serviceName,int port) throws Exception
	{
		String ip = Utils.getLocalIp();
		String nodePath = Utils.ZOOKEEPER_SERVICE_PREFIX+"/"+serviceName+"/nodes/"+ip+":"+port;
		
		if(zkClient.exists(nodePath))
		{
			zkClient.delete(nodePath);
			hostMap.remove(serviceName);
		}
	}
	
	private String toNodeData(String ip,int port,int weight) throws Exception
	{
		ObjectNode node = Utils.json.createObjectNode();
		node.put("ip",ip);
		node.put("port",port);
		node.put("weight",weight);
		node.put("startTime",System.currentTimeMillis());
		return Utils.json.writeValueAsString(node);
	}
}
