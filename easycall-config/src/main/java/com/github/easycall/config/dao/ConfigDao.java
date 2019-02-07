package com.github.easycall.config.dao;

import com.github.easycall.config.dao.entities.Config;
import com.github.easycall.config.dao.mapper.ConfigMapper;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConfigDao {

    @Autowired
    private SqlSessionTemplate template;

    public Config get(String name){

        ConfigMapper mapper = template.getMapper(ConfigMapper.class);
        return mapper.get(name);
    }

    public void create(Config config){
        ConfigMapper mapper = template.getMapper(ConfigMapper.class);
        mapper.create(config);
    }

    public void delete(String name){
        ConfigMapper mapper = template.getMapper(ConfigMapper.class);
        mapper.delete(name);
    }

    public void updateData(Config config){
        ConfigMapper mapper = template.getMapper(ConfigMapper.class);
        mapper.updateData(config);
    }

    public void updateVersion(Config config){
        ConfigMapper mapper = template.getMapper(ConfigMapper.class);
        mapper.updateVersion(config);
    }

    public List<Config> all(){
        ConfigMapper mapper = template.getMapper(ConfigMapper.class);
        return mapper.all();
    }
}
