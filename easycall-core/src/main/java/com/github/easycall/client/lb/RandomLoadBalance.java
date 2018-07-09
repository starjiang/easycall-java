package com.github.easycall.client.lb;
import com.github.easycall.client.Node;

import java.util.List;

public class RandomLoadBalance implements LoadBalance{
    private List<Node> list;

    @Override
    public void setNodeList(List<Node> list) {
        this.list = list;
    }

    @Override
    public Node getNode() {
        if(list.size() == 0){
            return null;
        }
        int index = (int)Math.floor(Math.random() * list.size());

        return list.get(index);
    }
}
