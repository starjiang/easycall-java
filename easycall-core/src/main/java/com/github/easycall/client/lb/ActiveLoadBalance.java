package com.github.easycall.client.lb;

import com.github.easycall.client.Node;

import java.util.List;

public class ActiveLoadBalance  implements LoadBalance{
    private List<Node> list;

    @Override
    public void setNodeList(List<Node> list) {

        this.list = list;

    }

    @Override
    public Node getNode() {

        if (list.size() == 0){
            return null;
        }

        int index = (int)Math.floor(Math.random() * list.size());
        long min = list.get(0).active.get();
        for(int i=1;i<list.size();i++){
            long v = list.get(i).active.get();
            if ( v < min){
                min = v;
                index = i;
            }
        }

        return list.get(index);
    }
}
