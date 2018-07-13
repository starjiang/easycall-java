package com.github.easycall.config.dao;

import com.github.easycall.config.util.Config;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkMarshallingError;
import org.I0Itec.zkclient.serialize.ZkSerializer;

import java.util.List;

public class ZookeeperDao {
    private ZkClient client;
    private final static int ZK_SESSION_TIMEOUT = 10000;
    private final static int ZK_CONNECT_TIMEOUT = 2000;

    public ZookeeperDao(String zkConnStr){

        client = new ZkClient(zkConnStr, ZK_SESSION_TIMEOUT, ZK_CONNECT_TIMEOUT, new ZkSerializer() {
            @Override
            public byte[] serialize(Object o) throws ZkMarshallingError {
                return ((String)o).getBytes();
            }

            @Override
            public Object deserialize(byte[] bytes) throws ZkMarshallingError {
                return new String(bytes);
            }
        });
    }

    public List<String> getChildNodeList(String path){

        return client.getChildren(path);
    }

    public String getNodeData(String path){
        return client.readData(path,true);
    }

    public void setNodeData(String path,String data){

        if(!client.exists(path)){
            client.createPersistent(path,true);
        }
        client.writeData(path,data);
    }

    public boolean isNodeExsit(String path){
        return client.exists(path);
    }

    public boolean deleteNode(String path){
        return client.delete(path);
    }
}
