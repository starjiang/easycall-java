package com.github.easycall.config.servlet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.config.dao.ZookeeperDao;
import com.github.easycall.config.util.Config;
import com.github.easycall.config.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ConfigServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(ConfigServlet.class);

    private ZookeeperDao zookeeperDao;

    public ConfigServlet(){
        zookeeperDao = new ZookeeperDao(Config.instance.getString("manage.zk","127.0.0.1:2181"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try{
            if(req.getRequestURI().equals("/config/list")){
                getConfigList(req,resp);
            }else if(req.getRequestURI().equals("/config/info")){
                getConfigInfo(req,resp);
            }else if(req.getRequestURI().equals("/config/version")){
                getConfigVersion(req,resp);
            }
        }catch (Exception e){
            ObjectNode result = Utils.om.createObjectNode();
            result.put("ret",1);
            result.put("msg",e.getMessage());
            resp.getWriter().write(Utils.om.writeValueAsString(result));
            log.error(e.getMessage(),e);
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try{
            if(req.getRequestURI().equals("/config/save")){
                saveConfig(req,resp);

            }else if(req.getRequestURI().equals("/config/save_version")){
                saveConfigVersion(req,resp);
            }else if(req.getRequestURI().equals("/config/create")){
                createConfig(req,resp);
            }else if(req.getRequestURI().equals("/config/delete")){
                deleteConfig(req,resp);
            }
        }catch (Exception e){
            ObjectNode result = Utils.om.createObjectNode();
            result.put("ret",1);
            result.put("msg",e.getMessage());
            resp.getWriter().write(Utils.om.writeValueAsString(result));
            log.error(e.getMessage(),e);
        }
    }

    private void createConfig(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        ObjectNode result = Utils.om.createObjectNode();
        result.put("ret",0);
        result.put("msg","ok");

        String configName = req.getParameter("name");

        if(configName == null || configName == ""){
            throw new Exception("param name is empty");
        }

        boolean exsit = zookeeperDao.isNodeExsit(Utils.ZK_CONFIG_PATH+"/"+configName);

        if(exsit == true){
            result.put("ret",1);
            result.put("msg","config have exsit");
        }else{
            zookeeperDao.setNodeData(Utils.ZK_CONFIG_PATH+"/"+configName+"/data","");
            zookeeperDao.setNodeData(Utils.ZK_CONFIG_PATH+"/"+configName+"/version","0");
        }

        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().write(Utils.om.writeValueAsString(result));
    }

    private void deleteConfig(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        ObjectNode result = Utils.om.createObjectNode();
        result.put("ret",0);
        result.put("msg","ok");

        String configName = req.getParameter("name");

        if(configName == null || configName == ""){
            throw new Exception("param name is empty");
        }

        zookeeperDao.deleteNode(Utils.ZK_CONFIG_PATH+"/"+configName+"/data");
        zookeeperDao.deleteNode(Utils.ZK_CONFIG_PATH+"/"+configName+"/version");
        zookeeperDao.deleteNode(Utils.ZK_CONFIG_PATH+"/"+configName);

        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().write(Utils.om.writeValueAsString(result));

    }

    private void getConfigList(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        ObjectNode result = Utils.om.createObjectNode();
        result.put("ret",0);
        result.put("msg","ok");

        List<String> configList = zookeeperDao.getChildNodeList(Utils.ZK_CONFIG_PATH);
        result.putPOJO("data",configList);
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().write(Utils.om.writeValueAsString(result));

    }

    private void getConfigInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        try{
            String configName = req.getParameter("name");

            if(configName == null || configName == ""){
                throw new Exception("param name is empty");
            }

            String data = zookeeperDao.getNodeData(Utils.ZK_CONFIG_PATH+"/"+configName+"/data");

            if(data == null){
                throw new Exception("config "+configName+" not found");
            }

            resp.setContentType("text/plain;charset=utf-8");
            resp.getWriter().write(data);

        }catch (Exception e){
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(e.getMessage());
        }

    }

    private void getConfigVersion(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        try{
            String configName = req.getParameter("name");

            if(configName == null || configName == ""){
                throw new Exception("param name is empty");
            }

            String data = zookeeperDao.getNodeData(Utils.ZK_CONFIG_PATH+"/"+configName+"/version");

            if(data == null){
                throw new Exception("config "+configName+" version not found");
            }

            resp.setContentType("text/plain;charset=utf-8");
            resp.getWriter().write(data);

        }catch (Exception e){
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(e.getMessage());
        }

    }

    private void saveConfig(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        String configName = req.getParameter("name");

        if(configName == null || configName == ""){
            throw new Exception("param name is empty");
        }

        int len = req.getContentLength();
        InputStream in = req.getInputStream();
        byte[] buffer = new byte[len];
        in.read(buffer, 0, len);

        String data = new String(buffer);

        zookeeperDao.setNodeData(Utils.ZK_CONFIG_PATH+"/"+configName+"/data",data);

        ObjectNode result = Utils.om.createObjectNode();
        result.put("ret",0);
        result.put("msg","ok");

        resp.getWriter().write(Utils.om.writeValueAsString(result));
    }

    private void saveConfigVersion(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        String configName = req.getParameter("name");

        if(configName == null || configName == ""){
            throw new Exception("param name is empty");
        }

        int len = req.getContentLength();
        InputStream in = req.getInputStream();
        byte[] buffer = new byte[len];
        in.read(buffer, 0, len);

        String configVersion = new String(buffer);

        if(configVersion.isEmpty()){
            throw new Exception("config "+configName+" version is empty");
        }

        int version = Integer.valueOf(configVersion);

        zookeeperDao.setNodeData(Utils.ZK_CONFIG_PATH+"/"+configName+"/version",configVersion);

        ObjectNode result = Utils.om.createObjectNode();
        result.put("ret",0);
        result.put("msg","ok");

        resp.getWriter().write(Utils.om.writeValueAsString(result));

    }

}
