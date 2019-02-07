package com.github.easycall.demo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.core.service.Request;
import com.github.easycall.core.service.Response;
import com.github.easycall.core.util.EasyMethod;

import com.github.easycall.core.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncDemoWorker {

	private Logger log = LoggerFactory.getLogger(SyncDemoWorker.class);
    
    @EasyMethod(method="getProfile")
    public void onGetProfile(Request request, Response response) {
    	
    	log.info("req getProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString());

    	ObjectNode respBoby = Utils.json.createObjectNode();
    	ObjectNode info =  respBoby.putObject("info");
		info.put("name","hello");
		info.put("tag","xxxxxxxx");
		info.put("headPic","http://www.xxxx.com/xxx/xxxx.jpg");
		info.put("uid",10000);
    	response.setHead(request.getHead().setRet(0).setMsg("ok")).setBody(respBoby);
    }
    
    @EasyMethod(method="setProfile")
    public void onSetProfile(Request request, Response response) {
    	
    	//log.info("req setProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString());


		ObjectNode respBoby = Utils.json.createObjectNode();

    	respBoby.put("msg","ok");
    	respBoby.put("ret",0);
    	response.setHead(request.getHead().setRet(0).setMsg("ok")).setBody(respBoby);
    }

}
