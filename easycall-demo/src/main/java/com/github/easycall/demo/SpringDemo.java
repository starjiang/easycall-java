package com.github.easycall.demo;

import com.github.easycall.core.client.EasyClient;
import com.github.easycall.core.client.lb.LoadBalance;
import com.github.easycall.core.service.EasyService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(value="com.github.easycall.demo")
@Configuration
public class SpringDemo {

    @Bean
    public EasyService easyService(){
        EasyService service = new EasyService("127.0.0.1:2181");
        return service;
    }

    @Bean
    public EasyClient easyClient(){
        EasyClient client = new EasyClient("127.0.0.1:2181",4, LoadBalance.LB_ACTIVE);
        return client;
    }

    public static void main(String[] args) throws Exception {

        ConfigurableApplicationContext context =  SpringApplication.run(SpringDemo.class, args);

        EasyService service = context.getBean(EasyService.class);
        service.createSync("profile", 8001, SpringDemoWorker.class);
        service.startAndWait();
    }

}

