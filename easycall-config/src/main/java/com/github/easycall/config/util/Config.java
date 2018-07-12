package com.github.easycall.config.util;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {

    static Logger logger = LoggerFactory.getLogger(Config.class);
    private  HashMap<String, String> config = new HashMap<String, String>();
    static final public Config instance  = new Config();

    private Config()
    {
        load();
    }

    private void load()
    {
        try
        {
            InputStream in = getClass().getClassLoader().getResourceAsStream("system.properties");
            if(in == null){
                throw  new Exception("system.properties not found");
            }

            logger.info("load config from system.properties");
            loadFileConfig(in);
            printConfig();

        }
        catch(Exception e)
        {
            logger.error("Exception:"+e.getMessage(),e);
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

    private void loadFileConfig(InputStream in)
    {
        try
        {
            Properties prop = new Properties();
            if(in == null)
            {
                throw new Exception("inputStream is null");
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
