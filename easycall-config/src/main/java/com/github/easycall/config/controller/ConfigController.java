package com.github.easycall.config.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.config.dao.ConfigDao;
import com.github.easycall.config.dao.entities.Config;
import com.github.easycall.config.utils.Utils;
import com.github.easycall.config.websocket.WebSocketNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sun.rmi.runtime.Log;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/config")
public class ConfigController {

    final static Logger logger = LoggerFactory.getLogger(ConfigController.class);

    @Autowired
    private ConfigDao configDao;

    @RequestMapping("/list")
    public ResponseEntity<JsonNode> getConfigList(){

        ObjectNode respBody = Utils.om.createObjectNode();
        respBody.put("ret",0);
        respBody.put("msg","ok");

        List<Config> list = configDao.all();

        List<String> configList = list.stream().map(config -> config.getName()).collect(Collectors.toList());

        respBody.putPOJO("data",configList);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(respBody);
    }

    @RequestMapping("/info")
    public ResponseEntity<String> getConfigInfo(@RequestParam String name){

        Config config = configDao.get(name);

        if(config == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
        }

        String data = config.getData();

        return ResponseEntity.ok(data);
    }

    @RequestMapping("/version")
    public ResponseEntity<String> getConfigVersion(@RequestParam String name){

        Config config = configDao.get(name);

        if(config == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
        }

        Long version = config.getVersion();

        return ResponseEntity.ok(version.toString());
    }

    @RequestMapping("/save")
    public ResponseEntity<JsonNode> saveConfig(@RequestParam String name, @RequestBody String data){

        ObjectNode respBody = Utils.om.createObjectNode();
        respBody.put("ret",0);
        respBody.put("msg","ok");

        Config config = configDao.get(name);
        if(config == null){
            respBody.put("ret",1);
            respBody.put("msg","config not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respBody);
        }

        config.setData(data);
        configDao.updateData(config);

        return ResponseEntity.ok(respBody);
    }

    @RequestMapping("/save_version")
    public ResponseEntity<JsonNode> saveConfigVersion(@RequestParam String name, @RequestBody String version) throws Exception{

        ObjectNode respBody = Utils.om.createObjectNode();
        respBody.put("ret",0);
        respBody.put("msg","ok");

        Config config = configDao.get(name);
        if(config == null){
            respBody.put("ret",1);
            respBody.put("msg","config not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respBody);
        }

        config.setVersion(Long.valueOf(version));
        configDao.updateVersion(config);

        WebSocketNotification.notifyConfigChanged(name,Long.valueOf(version));

        return ResponseEntity.ok(respBody);
    }

    @RequestMapping("/delete")
    public ResponseEntity<JsonNode> deleteConfig(@RequestParam String name){

        ObjectNode respBody = Utils.om.createObjectNode();
        respBody.put("ret",0);
        respBody.put("msg","ok");

        Config config = configDao.get(name);
        if(config == null){
            respBody.put("ret",1);
            respBody.put("msg","config not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respBody);
        }

        configDao.delete(name);

        return ResponseEntity.ok(respBody);
    }

    @RequestMapping("/create")
    public ResponseEntity<JsonNode> createConfig(@RequestParam String name){
        ObjectNode respBody = Utils.om.createObjectNode();
        respBody.put("ret",0);
        respBody.put("msg","ok");

        Config config = configDao.get(name);
        if(config != null){
            respBody.put("ret",1);
            respBody.put("msg","config have exist");
            return ResponseEntity.ok(respBody);
        }
        config = new Config();
        config.setVersion(0L);
        config.setData("");
        config.setName(name);
        config.setOwner("");
        config.setCreateTime(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        configDao.create(config);
        return ResponseEntity.ok(respBody);
    }

}
