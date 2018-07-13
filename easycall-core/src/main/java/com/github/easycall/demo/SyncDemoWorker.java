package com.github.easycall.demo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.service.Request;
import com.github.easycall.service.Response;
import com.github.easycall.util.EasyMethod;

import com.github.easycall.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncDemoWorker {

	private Logger log = LoggerFactory.getLogger(SyncDemoWorker.class);
    
    @EasyMethod(method="getProfile")
    public void onGetProfile(Request request, Response response) {
    	
    	log.info("req getProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString());
    	

    	ObjectNode respBoby = Utils.json.createObjectNode();
    	respBoby.put("msg","ok");
    	respBoby.put("ret",0);
    	response.setHead(request.getHead()).setBody(respBoby);
    }
    
    @EasyMethod(method="setProfile")
    public void onSetProfile(Request request, Response response) {
    	
    	//log.info("req setProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString());


		ObjectNode respBoby = Utils.json.createObjectNode();

    	respBoby.put("msg","ok");
    	respBoby.put("ret",0);
    	response.setHead(request.getHead()).setBody(respBoby);
    }

}
