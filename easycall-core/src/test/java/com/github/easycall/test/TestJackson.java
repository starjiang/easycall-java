package com.github.easycall.test;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.util.Utils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestJackson {

    @Test
    public void TestJsonAddList() throws  Exception{

        List<Integer> list = new ArrayList<Integer>();

        list.add(1);
        list.add(2);

        ObjectNode node = Utils.json.createObjectNode();

        node.put("dd",1);

        node.putPOJO("name",list);
        String out = Utils.json.writeValueAsString(node);

        ObjectNode node1 = Utils.json.readValue(out,ObjectNode.class);

        System.out.println(node1.get("dd"));

        System.out.println(Utils.json.writeValueAsString(node));
    }
}
