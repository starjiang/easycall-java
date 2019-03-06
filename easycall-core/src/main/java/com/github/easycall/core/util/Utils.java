package com.github.easycall.core.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    final public static ObjectMapper json = new ObjectMapper();
    final public static ObjectMapper msgpack = new ObjectMapper(new MessagePackFactory());
    final public static String ZOOKEEPER_SERVICE_PREFIX = "/easycall/services";

    static {
        Utils.json.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Utils.msgpack.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static String getHttpData(String url) throws Exception{

        HttpURLConnection httpURLConnection = (HttpURLConnection)new URL(url).openConnection();
        InputStream inputStream = httpURLConnection.getInputStream();
        StringBuffer resultBuffer = new StringBuffer();

        if (httpURLConnection.getResponseCode() >= 300) {
            throw new Exception("http exception,code=" + httpURLConnection.getResponseCode()+",msg="+httpURLConnection.getResponseMessage());
        }

        try {
            byte [] buffer = new byte[4096];
            while(true){
                int n = inputStream.read(buffer);
                resultBuffer.append(new String(buffer,0,n));
                if(n < 4096) break;
            }

        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return resultBuffer.toString();
    }


    public static String getLocalIp() throws Exception {
        String ip = Inet4Address.getLocalHost().getHostAddress();
        if (!ip.startsWith("127")) {
            return ip;
        } else {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface networkInterface = en.nextElement();
                Enumeration<InetAddress> enumIpAddress = networkInterface.getInetAddresses();
                while (enumIpAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddress.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() &&
                            inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
            throw new Exception("getLocalIp fail,no network interface found");
        }
    }

    public static int hash(String str) {
        final int p = 16777619;
        int hash = (int) 2166136261L;
        for (int i = 0; i < str.length(); i++)
            hash = (hash ^ str.charAt(i)) * p;
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        return Math.abs(hash);
    }

    public static Map<String,Method> getMethodMap(Class<?> clazz){
        Map<String,Method> methodMap = new HashMap<>();
        Method[] methods = clazz.getMethods();

        for(int i=0;i<methods.length;i++)
        {
            boolean flag = methods[i].isAnnotationPresent(EasyMethod.class);
            if(flag)
            {
                EasyMethod handler = methods[i].getAnnotation(EasyMethod.class);
                methodMap.put(handler.method(), methods[i]);
            }
        }
        return methodMap;
    }
}
