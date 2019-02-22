package com.github.easycall.core.client.lb;

import com.github.easycall.core.client.Node;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RoundRobinLoadBalance implements LoadBalance {

    private List<Node> list;
    private static Map<String,AtomicLong> seqMap = new ConcurrentHashMap<>();
    private String name;
    @Override
    public void setNodeList(List<Node> list) {
        this.list = list;
    }

    @Override
    public Node getNode() {

        if(list.size() == 0){
            return null;
        }

        AtomicLong seq = seqMap.get(name);
        if(seq == null) {
            seq = new AtomicLong(0);
            seqMap.put(name,seq);
        }

        int index = (int)(seq.incrementAndGet()%list.size());

        return list.get(index);
    }

    public void setName(String name){
        this.name = name;
    }
}
