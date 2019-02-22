package com.github.easycall.demo;

import com.github.easycall.core.client.EasyClient;
import com.github.easycall.core.client.lb.LoadBalance;
import com.github.easycall.core.service.EasyService;
import com.github.easycall.core.util.EasyConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(value="com.github.easycall.demo")
@Configuration
public class SpringDemoApplication {

    @Value("${service.zk}")
    private String zkConnStr;

    @Value("${thread.num}")
    private int threadNum;

    @Bean
    public EasyService easyService(){
        EasyService service = new EasyService(zkConnStr);
        return service;
    }

    @Bean
    public EasyClient easyClient(){
        EasyClient client = new EasyClient(zkConnStr,threadNum, LoadBalance.LB_ACTIVE);
        return client;
    }

    @Bean
    public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setProperties(EasyConfig.instance.getProperties());
        return ppc;
    }

    public static void main(String[] args) throws Exception {

        ApplicationContext context =  SpringApplication.run(SpringDemoApplication.class, args);

        EasyService service = context.getBean(EasyService.class);
        service.create("profile", 8001,context.getBean(SpringDemoService.class));
        service.startAndWait();
    }

}

