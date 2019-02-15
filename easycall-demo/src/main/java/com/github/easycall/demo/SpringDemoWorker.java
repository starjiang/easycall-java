package com.github.easycall.demo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.core.client.EasyClient;
import com.github.easycall.core.service.Request;
import com.github.easycall.core.service.Response;
import com.github.easycall.core.util.EasyMethod;
import com.github.easycall.core.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * worker instance create by easycall self,so spring can't inject for it,we need to use SpringContextUtil
 */
public class SpringDemoWorker {

	private Logger log = LoggerFactory.getLogger(SpringDemoWorker.class);

	private EasyClient client;

	public SpringDemoWorker(){
		client = SpringContextUtil.getBean(EasyClient.class);
	}

    @EasyMethod(method="getProfile")
    public Response onGetProfile(Request request) {

    	log.info("req getProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString());

    	ObjectNode respBoby = Utils.json.createObjectNode();
    	ObjectNode info =  respBoby.putObject("info");
		info.put("name","hello");
		info.put("tag","xxxxxxxx");
		info.put("headPic","http://www.xxxx.com/xxx/xxxx.jpg");
		info.put("uid",10000);
    	return new Response().setHead(request.getHead().setRet(0).setMsg("ok")).setBody(respBoby);
    }
    
    @EasyMethod(method="setProfile")
    public Response onSetProfile(Request request) {
    	
    	//log.info("req setProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString());


		ObjectNode respBoby = Utils.json.createObjectNode();

    	respBoby.put("msg","ok");
    	respBoby.put("ret",0);
    	return new Response().setHead(request.getHead().setRet(0).setMsg("ok")).setBody(respBoby);
    }

}
