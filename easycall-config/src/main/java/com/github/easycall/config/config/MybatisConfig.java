package com.github.easycall.config.config;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.InputStream;

@Configuration
public class MybatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception{

        InputStream inputStream = Resources.getResourceAsStream("mybatis.xml");
        return new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory factory){
        return  new SqlSessionTemplate(factory);
    }

    @Bean
    public PlatformTransactionManager transactionManager(SqlSessionFactory factory){
        return new DataSourceTransactionManager(factory.getConfiguration().getEnvironment().getDataSource());
    }
}
