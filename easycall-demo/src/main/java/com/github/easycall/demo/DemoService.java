package com.github.easycall.demo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.core.service.Request;
import com.github.easycall.core.service.Response;
import com.github.easycall.core.util.EasyMethod;
import com.github.easycall.core.util.Utils;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class DemoService {

	private Logger log = LoggerFactory.getLogger(DemoService.class);
    
    @EasyMethod(method="getIpInfo")
    public CompletableFuture<Response> getIpInfo(Request request) throws Exception {

		log.info("head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString());

		return getIpDataByHttp(request.getBody().get("ip").asText()).thenApply(new Response().setHead(request.getHead())::setBody);
    }

    private CompletableFuture<JsonNode> getIpDataByHttp(String ip){

		CompletableFuture<JsonNode> completableFuture = new CompletableFuture<>();
		WebClient client = WebClient.create(VertxUtils.vertx);
		client.get("ip-api.com","/json/"+ip).send(result -> {
			try{
				if(result.succeeded()){
					completableFuture.complete(Utils.json.readTree(result.result().bodyAsString()));
				}else{
					completableFuture.completeExceptionally(result.cause());
				}
			}catch(Exception e){
				completableFuture.completeExceptionally(e);
			}
		});

		return completableFuture;
	}

	@EasyMethod(method="setProfile")
    public Response onSetProfile(Request request) {
    	
    	//log.info("req setProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString());


		ObjectNode respBody = Utils.json.createObjectNode();

    	respBody.put("msg","ok");
    	respBody.put("ret",0);
    	return new Response().setHead(request.getHead().setRet(0).setMsg("ok")).setBody(respBody);
    }

}
