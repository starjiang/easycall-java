package com.github.easycall.config.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static final ObjectMapper om = new ObjectMapper();

    public static Map<String, String>  parserQueryString(String queryString) throws Exception{

        Map<String, String> argMap = new HashMap<>();

        if (queryString == null) return argMap;

        String[] queryArr = queryString.split("&");
        for (int i = 0; i < queryArr.length; i++) {
            String string = queryArr[i];
            String keyAndValue[] = string.split("=", 2);
            if (keyAndValue.length < 2) {
                argMap.put(keyAndValue[0],"");
            } else {
                argMap.put(keyAndValue[0], URLDecoder.decode(keyAndValue[1],"UTF-8"));
            }
        }
        return argMap;
    }
}
