package com.github.easycall.core.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

public class Utils {

    final public static ObjectMapper json = new ObjectMapper();
    final public static ObjectMapper msgpack = new ObjectMapper(new MessagePackFactory());
    final public static String ZOOKEEPER_SERVICE_PREFIX = "/easycall/services";

    static {
        Utils.json.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Utils.msgpack.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public static String getHttpData(String url) throws Exception{

        OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build();
        Request request = new Request.Builder().url(url).build();
        Response response = okHttpClient.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        }else {
            throw new Exception(url+",status="+response.code());
        }
    }

    public static String getLocalIp() throws Exception {
        String ip = Inet4Address.getLocalHost().getHostAddress();
        if (!ip.startsWith("127")) {
            return ip;
        } else {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();

            while (en.hasMoreElements()) {
                NetworkInterface intf = en.nextElement();
                Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
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
        return hash;
    }
}
