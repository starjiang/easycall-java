package com.github.easycall.core.client.lb;

import com.github.easycall.core.client.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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


        long total = 0;

        for(int i=0;i<list.size();i++)
        {
            Node node = list.get(i);
            total+=node.active.get();
        }

        ArrayList<Long> activeList = new ArrayList<>(list.size());

        long sum = 0;
        for(int i=0;i<list.size();i++)
        {
            long weight = total-list.get(i).active.get();
            sum+=weight;
            activeList.set(i,weight);
        }

        if(sum == 0){

            int index = (int)Math.floor(Math.random() * list.size());
            return list.get(index);
        }

        long random = Math.round(Math.random() * sum);

        for(int i=0;i<activeList.size();i++)
        {
            random-= activeList.get(i);
            if(random <= 0)
            {
                return list.get(i);
            }
        }

        return list.get(0);
    }
}
