package com.github.easycall.util;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import com.github.easycall.exception.EasyException;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EasyConfig {

	static Logger logger = LoggerFactory.getLogger(EasyConfig.class);
	private final  int ZK_SESSION_TIMEOUT = 10000;
	private final  int ZK_CONNECT_TIMEOUT = 2000;
	private  ZkClient client;
	private  HashMap<String, String> config = new HashMap<String, String>();
	static final public EasyConfig instance  = new EasyConfig();

	private String configName;
	private String configPath;
	private String configZk;
	
	private EasyConfig()
	{
		load();
	}
	
	private void load()
	{
		try
		{
			InputStream in = getClass().getClassLoader().getResourceAsStream("system.properties");
			if(in == null){
				throw  new EasyException("system.properties not found");
			}

			logger.info("load config from system.properties");
			loadFileConfig(in);

			configName = getString("config.name");
			configPath = getString("config.path");
			configZk = getString("config.zk");

			if (configName == null || configPath == null || configZk == null){
			    throw new EasyException("local remote config not set");
            }
			
			client = new ZkClient(configZk, ZK_SESSION_TIMEOUT,ZK_CONNECT_TIMEOUT,new ZkStringSerializer());
			
			logger.info("load remote {} config from zookeeper",configName);
			loadRemoteConfig();

            logger.info("load local {} config",configName);
            loadLocalConfig();

            logger.info("subscribe {} config from zookeeper",configName);
            subscribeRemoteConfig();

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

        logger.info("========================Start Config=========================");
        while(it.hasNext())
        {
            Entry<String,String> en = it.next();
            logger.info("{}={}",en.getKey(),en.getValue());
        }
        logger.info("========================End Config=========================");
    }

	private void subscribeRemoteConfig(){

	    String versionPath = Utils.ZOOKEEPER_CONFIG_PREFIX + "/" + configName + "/version";

        client.subscribeDataChanges(versionPath, new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {
                logger.info("{} node changed,reload remote config",s);
                loadRemoteConfig();
                printConfig();
            }

            @Override
            public void handleDataDeleted(String s) throws Exception {

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
	        prop.load(in);  
	        in.close();
	        Iterator<Object> it = prop.keySet().iterator();
	        while(it.hasNext())
	        {
	        	String key = it.next().toString();
	        	config.put(key, prop.getProperty(key).trim());
	        }
        }
		catch(Exception e)
		{
			logger.error("Exception:"+e.getMessage(),e);
		}
	}
	
	
	private boolean loadZookeeperConfig(String path,String fileName)
	{
		String data = client.readData(path, true);
		
		if(data == null) 
		{
			logger.error("config path:"+path+" not found");
			return false;
		}

		return writeFileData(fileName,data.getBytes());

	}

	private String getZkNodeData(String path){

		String data = client.readData(path, true);

		if(data == null)
		{
			return null;
		}

		return data;
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

	private static  String getFileData(String fileName) {

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
		return new String(fileContent);

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

	private static boolean isFileExsit(String filePath){
		File f = new File(filePath);
		return f.exists();
	}
	private static boolean createDirectory(String filePath){
		File f = new File(filePath);
		return f.mkdirs();
	}

	private void loadLocalConfig(){
	    try{
            String localConfigPath = configPath + "/" + configName + "/local";
            if (!isFileExsit(localConfigPath)) {
                if(!createDirectory(localConfigPath)){
                    logger.error("create directory {} fail",localConfigPath);
                }
            }
            String localDataPath = localConfigPath + "/" + configName + ".properties";

            if(isFileExsit(localDataPath)){
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
			String remoteConfigPath = configPath + "/" + configName + "/remote";

			if (!isFileExsit(remoteConfigPath)) {
				if(!createDirectory(remoteConfigPath)){
					logger.error("create directory {} fail",remoteConfigPath);
				}
			}

			String versionPath = remoteConfigPath + "/version";
			String remoteDataPath = remoteConfigPath + "/" + configName + ".properties";


			int localVersion = 0;

			if (isFileExsit(versionPath)) {
				String localVersionStr = getFileData(versionPath);
				localVersion = localVersionStr != null ? Integer.valueOf(localVersionStr) : 0;
			}

			int remoteVersion = 0;
			String remoteVersionStr = getZkNodeData(Utils.ZOOKEEPER_CONFIG_PREFIX + "/" + configName + "/version");
			remoteVersion = remoteVersionStr != null ? Integer.valueOf(remoteVersionStr) : 0;

			if (localVersion < remoteVersion) {
				boolean loadSuccess = loadZookeeperConfig(Utils.ZOOKEEPER_CONFIG_PREFIX + "/" + configName + "/data", remoteDataPath);
				if(loadSuccess){
				    writeFileData(versionPath,remoteVersionStr.getBytes());
                }
			}

			if (isFileExsit(remoteDataPath)) {
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
