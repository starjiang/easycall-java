package com.github.easycall.core.client;

import java.util.concurrent.atomic.AtomicLong;

public class Node
{
    public String name;
    public String ip;
    public int port;
    public int weight;
    public AtomicLong active;

    public Node(String name,String ip,int port,int weight)
    {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.weight = weight;
        this.active = new AtomicLong(0);
    }
}
