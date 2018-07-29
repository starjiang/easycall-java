package com.github.easycall.client.lb;

import com.github.easycall.client.Node;

import java.util.List;

public interface LoadBalance {

    public final static int LB_HASH = 1;
    public final static int LB_ACTIVE = 2;
    public final static int LB_RANDOM = 3;
    public final static int LB_RANDOM_WEIGHT = 4;
    public final static int LB_ROUND_ROBIN = 5;

    public void setNodeList(List<Node> list);
    public Node getNode();
}
