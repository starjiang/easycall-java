package com.github.easycall.client.lb;

import com.github.easycall.client.Node;

import java.util.List;

public class RandomWeightLoadBalance  implements LoadBalance{

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

        int total = 0;
        for(int i=0;i<list.size();i++)
        {
            Node node = list.get(i);
            total+=node.weight;
        }

        long random = Math.round(Math.random() * total);

        for(int i=0;i<list.size();i++)
        {
            Node node = list.get(i);
            random-= node.weight;
            if(random <= 0)
            {
                return node;
            }
        }

        return list.get(0);
    }
}
