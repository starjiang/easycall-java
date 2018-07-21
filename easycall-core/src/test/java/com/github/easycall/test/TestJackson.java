package com.github.easycall.test;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.util.EasyHead;
import com.github.easycall.util.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

class User{
    public String name;
    public Integer uid;
}

public class TestJackson {

    @Test
    public void TestJsonAddList() throws  Exception{

        List<Integer> list = new ArrayList<Integer>();

        list.add(1);
        list.add(2);

        ObjectNode node = Utils.json.createObjectNode();

        User user = new User();
        user.name = "sxxx";
        user.uid = 100000;

        ObjectNode node2 =  Utils.json.convertValue(user,ObjectNode.class);

        node.put("dd",1);
        node.put("user",node2);

        node.putPOJO("name",list);
        String out = Utils.json.writeValueAsString(node);

        ObjectNode node1 = Utils.json.readValue(out,ObjectNode.class);

        System.out.println(node1.get("dd")+","+node2.get("name").asText());

        System.out.println(Utils.json.writeValueAsString(node));
    }

    @Test
    public void TestReadValue() throws  Exception{

        ObjectNode node = Utils.json.createObjectNode();

        node.put("service","profile");
        node.put("method","getProfile");
        node.put("uid1",100000L);

        byte [] out = Utils.json.writeValueAsBytes(node);

        EasyHead head = Utils.json.readValue(out, EasyHead.class);

        System.out.println("head="+head.toString());

    }
}
