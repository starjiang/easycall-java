package com.github.easycall.client.lb;

import com.github.easycall.client.Node;

import java.util.List;

public class RoundRobinLoadBalance implements LoadBalance {

    private List<Node> list;
    private static long seq;
    @Override
    public void setNodeList(List<Node> list) {
        this.list = list;
    }

    @Override
    public Node getNode() {

        if(list.size() == 0){
            return null;
        }

        int index = (int)seq++%list.size();

        return list.get(index);
    }
}
