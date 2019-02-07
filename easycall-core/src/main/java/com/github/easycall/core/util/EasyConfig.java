package com.github.easycall.core.util;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.easycall.core.exception.EasyException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyConfig {

	static Logger logger = LoggerFactory.getLogger(EasyConfig.class);
	private final static String CONFIG_PATH="./conf";
	private final static Integer RECONNECT_INTERVAL = 30000;
	private final static Long PING_INTERVAL = 90L;
	private final static String CONFIG_HOST="config.easycall.com:8080";
	private  HashMap<String, String> config = new HashMap<>();
	public final static EasyConfig instance  = new EasyConfig();
	private WebSocket webSocket;
	private String configName;

	private EasyConfig()
	{
		load();
	}
	
	private void load()
	{
		try
		{
			InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties");
			if(in == null){
				logger.error("application.properties not found");
				return;
			}

			logger.info("load config from application.properties");
			loadFileConfig(in);
			configName = getString("config.name");

			if (configName == null){
			    logger.error("remote config not settle");
			    return;
            }

            if (webSocket == null){
				logger.info("subscribe {} config from config center",configName);
				subscribeRemoteConfig();
			}

			logger.info("load remote {} config from config center",configName);
			loadRemoteConfig();

            logger.info("load local {} config",configName);
            loadLocalConfig();

            printConfig();

		}
		catch(Exception e)
		{
			logger.error("Exception:"+e.getMessage(),e);
		}
	}

	private String getString(String name){
	    if(config.containsKey(name)){
	        return config.get(name);
        }else{
	        return null;
        }
    }

	private void printConfig(){
        Iterator<Entry<String, String>> it = config.entrySet().iterator();

        logger.info("========================Config Information=========================");
        while(it.hasNext())
        {
            Entry<String,String> en = it.next();
            logger.info("{}={}",en.getKey(),en.getValue());
        }
        logger.info("========================Config Information=========================");
    }

	private void subscribeRemoteConfig(){

		OkHttpClient okHttpClient = new OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).pingInterval(PING_INTERVAL,TimeUnit.SECONDS).build();
		Request request = new Request.Builder().url("ws://" + CONFIG_HOST+"/websocket?name="+configName).build();
		webSocket = okHttpClient.newWebSocket(request, new WebSocketListener() {
			@Override
			public void onOpen(WebSocket webSocket, Response response) {
				logger.info("config websocket connected");
				super.onOpen(webSocket, response);
			}

			@Override
			public void onMessage(WebSocket webSocket, String text) {

				try{
					JsonNode respPkg = Utils.json.readTree(text);
					String event = respPkg.get("event").asText();
					if (event.equals("configChanged")){
						logger.info("reloading config......");
						load();
					}

				}catch (Exception e){
					logger.error(e.getMessage(),e);
				}
				super.onMessage(webSocket, text);
			}

			@Override
			public void onClosing(WebSocket webSocket, int code, String reason) {
				webSocket.close(1000,"closed by remote");
				super.onClosing(webSocket, code, reason);
			}

			@Override
			public void onClosed(WebSocket webSocket, int code, String reason) {
				logger.error("websocket closed,{} seconds later will reconnect",RECONNECT_INTERVAL);
				try {
					Thread.sleep(RECONNECT_INTERVAL);
					subscribeRemoteConfig();
				}catch (Exception e){
					logger.error(e.getMessage(),e);
				}
				super.onClosed(webSocket, code, reason);
			}

			@Override
			public void onFailure(WebSocket webSocket, Throwable t, Response response) {
				logger.error(t.getMessage());
				try {
					logger.error("websocket exception,{} seconds later will reconnect",RECONNECT_INTERVAL);
					Thread.sleep(RECONNECT_INTERVAL);
					subscribeRemoteConfig();
				}catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
				super.onFailure(webSocket, t, response);
			}
		});
    }

	private void loadFileConfig(InputStream in)
	{
		try
		{
			Properties prop = new Properties();
	        if(in == null)
	        {
	        	throw new EasyException("inputStream is null");
	        }
	        prop.load(new InputStreamReader(in, "utf-8"));
	        in.close();
	        Iterator<Entry<Object,Object>> it = prop.entrySet().iterator();
	        while(it.hasNext())
	        {
	        	Entry<Object,Object> en = it.next();
	        	config.put((String)en.getKey(), (String)en.getValue());
	        }
        }
		catch(Exception e)
		{
			logger.error("Exception:"+e.getMessage(),e);
		}
	}

	private static boolean writeFileData(String fileName,byte[] data){

		File file = new File(fileName);
		FileOutputStream out;
		try{
			out = new FileOutputStream(file);
			out.write(data);
			out.close();
			return true;
		}catch (Exception e){
			logger.error(e.getMessage(),e);
			return  false;
		}
	}

	private static  String getFileData(String fileName) throws Exception {

		File file = new File(fileName);
		Long length = file.length();
		byte[] fileContent = new byte[length.intValue()];
		try {
			FileInputStream in = new FileInputStream(file);
			in.read(fileContent);
			in.close();
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return null;
		}
		String content = new String(fileContent);
		return content;

	}

	private static  InputStream getFileStream(String fileName) {

		File file = new File(fileName);
		try {
			FileInputStream in = new FileInputStream(file);
			return in;
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return null;
		}
	}

	private static boolean isFileExist(String filePath){
		File f = new File(filePath);
		return f.exists();
	}
	private static boolean createDirectory(String filePath){
		File f = new File(filePath);
		return f.mkdirs();
	}

	private void loadLocalConfig(){
	    try{
            String localConfigPath = CONFIG_PATH + "/" + configName + "/local";
            if (!isFileExist(localConfigPath)) {
                if(!createDirectory(localConfigPath)){
                    logger.error("create directory {} fail",localConfigPath);
                }
            }
            String localDataPath = localConfigPath + "/" + configName + ".properties";

            if(isFileExist(localDataPath)){
                File file = new File(localDataPath);
                FileInputStream in = new FileInputStream(file);
                loadFileConfig(in);
            }
	    }catch (Exception e){
	        logger.error(e.getMessage(),e);
        }
    }
	private void loadRemoteConfig()
	{
		try {
			String remoteConfigPath = CONFIG_PATH + "/" + configName + "/remote";

			if (!isFileExist(remoteConfigPath)) {
				if(!createDirectory(remoteConfigPath)){
					logger.error("create directory {} fail",remoteConfigPath);
				}
			}

			String versionPath = remoteConfigPath + "/version";
			String remoteDataPath = remoteConfigPath + "/" + configName + ".properties";


			int localVersion = 0;

			if (isFileExist(versionPath)) {
				String localVersionStr = getFileData(versionPath);
				localVersion = localVersionStr != null ? Integer.valueOf(localVersionStr) : 0;
			}

			try{
				int remoteVersion = 0;
				String remoteVersionStr = Utils.getHttpData("http://"+CONFIG_HOST+"/config/version?name="+configName);
				remoteVersion = remoteVersionStr != null ? Integer.valueOf(remoteVersionStr) : 0;

				if (localVersion < remoteVersion) {

					String remoteDataStr = Utils.getHttpData("http://"+CONFIG_HOST+"/config/info?name="+configName);
					boolean loadSuccess = writeFileData(remoteDataPath,remoteDataStr.getBytes());
					if(loadSuccess){
						writeFileData(versionPath,remoteVersionStr.getBytes());
					}
				}
			}catch (Exception e){
				logger.error(e.getMessage());
			}
			if (isFileExist(remoteDataPath)) {
				File file = new File(remoteDataPath);
				FileInputStream in = new FileInputStream(file);
				loadFileConfig(in);
			}

		}catch (Exception e){
			logger.error(e.getMessage(),e);
		}

	}

	public boolean hasConfig(String name){
	    return config.containsKey(name);
    }

    public boolean loadFile(String fileName){
        InputStream in = getClass().getClassLoader().getResourceAsStream(fileName);

        if(in == null){
            logger.error(fileName+" not found");
            return false;
        }

        loadFileConfig(in);

        return true;
    }

    public Properties getByPrefixWith(String prefix){

	    Properties properties = new Properties();

        Iterator<Entry<String, String>> it = config.entrySet().iterator();
        while(it.hasNext())
        {
            Entry<String,String> en = it.next();
            if(en.getKey().startsWith(prefix)){
                properties.put(en.getKey(),en.getValue());
            }
        }
        return properties;
    }

	public Properties getProperties(){

		Properties properties = new Properties();
		Iterator<Entry<String, String>> it = config.entrySet().iterator();
		while(it.hasNext())
		{
			Entry<String,String> en = it.next();
			properties.put(en.getKey(),en.getValue());

		}
		return properties;
	}

    public Properties getByPrefix(String prefix){

        Properties properties = new Properties();

        Iterator<Entry<String, String>> it = config.entrySet().iterator();
        while(it.hasNext())
        {
            Entry<String,String> en = it.next();
            if(en.getKey().startsWith(prefix)){
                String key = en.getKey().substring(prefix.length(),en.getKey().length());
                properties.put(key,en.getValue());
            }
        }
        return properties;
    }

	public Integer getInt(String name,Integer defaultValue) 
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			return Integer.valueOf(value);
		}
		else
		{
			return defaultValue;
		}
	}

	public Long getLong(String name,Long defaultValue)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			return Long.valueOf(value);
		}
		else
		{
			return defaultValue;
		}
	}
	
	public Float getFloat(String name,Float defaultValue)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			return Float.valueOf(value);
		}
		else
		{
			return defaultValue;
		}
	}
	
	public Double getDouble(String name,Double defaultValue)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			return Double.valueOf(value);
		}
		else
		{
			return defaultValue;
		}
	}
	
	public Short getShort(String name,Short defaultValue)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			return Short.valueOf(value);
		}
		else
		{
			return defaultValue;
		}
	}
	
	public String getString(String name,String defaultValue)
	{
		if(config.containsKey(name))
		{
			return config.get(name);
		}
		else
		{
			return defaultValue;
		}
	}

	public Boolean getBoolean(String name,Boolean defaultValue){

	    if(config.containsKey(name)){
	        return Boolean.valueOf(config.get(name));
        }else {
	        return defaultValue;
        }
    }
	
	
	public ArrayList<String> getStringList(String name)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			
			String []values = value.split(",");
			
			ArrayList<String> list = new ArrayList<String>(values.length);
			
			for(int i=0;i<values.length;i++)
			{
				list.add(i, values[i]);
			}
			
			return list;
		}
		else
		{
			return new ArrayList<String>();
		}
	}
	
	public ArrayList<Integer>  getIntList(String name)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			
			String []values = value.split(",");
			
			ArrayList<Integer> list = new ArrayList<Integer>(values.length);
			
			for(int i=0;i<values.length;i++)
			{
				list.add(i, Integer.valueOf(values[i]));
			}
			
			return list;
		}
		else
		{
			return new ArrayList<Integer>();
		}
	}
	
	public ArrayList<Long> getLongList(String name)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			
			String []values = value.split(",");
			
			ArrayList<Long> list = new ArrayList<Long>(values.length);
			
			for(int i=0;i<values.length;i++)
			{
				list.add(i, Long.valueOf(values[i]));
			}
			
			return list;
		}
		else
		{
			return new ArrayList<Long>();
		}
	}
	
	public ArrayList<Short> getShortList(String name)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			
			String []values = value.split(",");
			
			ArrayList<Short> list = new ArrayList<Short>(values.length);
			
			for(int i=0;i<values.length;i++)
			{
				list.add(i, Short.valueOf(values[i]));
			}
			
			return list;
		}
		else
		{
			return new ArrayList<Short>();
		}
	}
	
	public HashMap<String,Integer> getIntMap(String name)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			
			String []values = value.split(",");
			
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			
			for(int i=0;i<values.length;i++)
			{
				String [] values2 = values[i].split(":");
				if(values2.length == 2)
				{
					map.put(values2[0], Integer.valueOf(values2[1]));
				}
			}
			
			return map;
		}
		else
		{
			return new HashMap<String,Integer>();
		}
	}
	
	public HashMap<String,Long> getLongMap(String name)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			
			String []values = value.split(",");
			
			HashMap<String, Long> map = new HashMap<String, Long>();
			
			for(int i=0;i<values.length;i++)
			{
				String [] values2 = values[i].split(":");
				if(values2.length == 2)
				{
					map.put(values2[0], Long.valueOf(values2[1]));
				}
			}
			
			return map;
		}
		else
		{
			return new HashMap<String,Long>();
		}
	}

	public HashMap<String,Double> getDoubleMap(String name)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);

			String []values = value.split(",");

			HashMap<String, Double> map = new HashMap<String, Double>();

			for(int i=0;i<values.length;i++)
			{
				String [] values2 = values[i].split(":");
				if(values2.length == 2)
				{
					map.put(values2[0], Double.valueOf(values2[1]));
				}
			}

			return map;
		}
		else
		{
			return new HashMap<String,Double>();
		}
	}

	public HashMap<String,Boolean> getBooleanMap(String name)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);

			String []values = value.split(",");

			HashMap<String, Boolean> map = new HashMap<String, Boolean>();

			for(int i=0;i<values.length;i++)
			{
				String [] values2 = values[i].split(":");
				if(values2.length == 2)
				{
					map.put(values2[0], Boolean.valueOf(values2[1]));
				}
			}

			return map;
		}
		else
		{
			return new HashMap<String,Boolean>();
		}
	}
	
	public HashMap<String,String> getStringMap(String name)
	{
		if(config.containsKey(name))
		{
			String value = config.get(name);
			
			String []values = value.split(",");
			
			HashMap<String, String> map = new HashMap<String, String>();
			
			for(int i=0;i<values.length;i++)
			{
				String [] values2 = values[i].split(":");
				if(values2.length == 2)
				{
					map.put(values2[0], values2[1]);
				}
			}
			
			return map;
		}
		else
		{
			return new HashMap<String,String>();
		}
	}
}
