package com.github.easycall.client.lb;

import com.github.easycall.client.Node;
import com.github.easycall.util.Utils;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHashLoadBalance implements LoadBalance{

    private List<Node> list;
    private static SortedMap<Integer, Node> virtualNodes = new TreeMap<>();
    private String key;
    private final static int VIRTUAL_NODES = 16;

    @Override
    public void setNodeList(List<Node> list) {
        this.list = list;
        genVirtualNodes();
    }

    @Override
    public Node getNode() {

        if (list.size() == 0){
            return null;
        }
        int hash = Utils.hash(key);
        SortedMap<Integer, Node> subMap = virtualNodes.tailMap(hash);
        Integer i = subMap.firstKey();
        return subMap.get(i);
    }

    public void setRouteKey(String key){
        this.key = key;
    }

    private void genVirtualNodes(){

        for (int i=0;i<list.size();i++)
        {
            Node node = list.get(i);

            for (int j = 0; j < VIRTUAL_NODES; j++)
            {
                String virtualNodeName = node.name +":"+ node.ip+":" + j;
                int hash = Utils.hash(virtualNodeName);
                virtualNodes.put(hash, node);
            }
        }
    }
}
