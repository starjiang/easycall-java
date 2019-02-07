package com.github.easycall.core.test;

import com.github.easycall.core.util.EasyConfig;
import org.junit.Test;

public class TestConfig {
    @Test
    public void testConfigLoad() throws Exception{
        System.out.println(EasyConfig.instance.getString("age","hello"));
        System.in.read();
    }
}
