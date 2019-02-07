package com.github.easycall.core.client.lb;

import com.github.easycall.core.client.Node;
import com.github.easycall.core.util.Utils;
import java.util.List;

public class HashLoadBalance implements LoadBalance{

    private List<Node> list;
    private String key;

    @Override
    public void setNodeList(List<Node> list) {
        this.list = list;
    }

    @Override
    public Node getNode() {
        if(list.size() == 0){
            return null;
        }
        int index = Utils.hash(key) % list.size();

        return list.get(index);
    }

    public void setRouteKey(String key){
        this.key = key;
    }

}
