package com.github.easycall.config.dao.mapper;

import com.github.easycall.config.dao.entities.Config;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface ConfigMapper {

    @Insert("insert into data(name,data,version,owner,createTime) values(#{name},#{data},#{version},#{owner},#{createTime})")
    public void create(Config config);

    @Delete("delete from data where name=#{name}")
    public void delete(String name);

    @Update("update data set data=#{data} where name=#{name}")
    public void updateData(Config config);

    @Update("update data set version=#{version} where name=#{name}")
    public void updateVersion(Config config);

    @Select("select * from data where name=#{name}")
    public Config get(String name);

    @Select("select name from data")
    public List<Config> all();
}
