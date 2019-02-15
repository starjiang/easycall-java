package com.github.easycall.demo;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.core.service.Request;
import com.github.easycall.core.service.Response;
import com.github.easycall.core.util.EasyMethod;

import com.github.easycall.core.util.Utils;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class DemoWorker {

	private Logger log = LoggerFactory.getLogger(DemoWorker.class);
    
    @EasyMethod(method="getProfile")
    public CompletableFuture<Response> onGetProfile(Request request) throws Exception {

		CompletableFuture<Response> completableFuture = new CompletableFuture<>();
		log.info("req getProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString());


		new Thread(()->{

			ObjectNode respBody = Utils.json.createObjectNode();
			ObjectNode info =  respBody.putObject("info");
			info.put("name","hello");
			info.put("tag","xxxxxxxx");
			info.put("headPic","http://www.xxxx.com/xxx/xxxx.jpg");
			info.put("uid",10000);

			try{  Thread.sleep(500); } catch (Exception e){}

			completableFuture.complete(new Response().setHead(request.getHead().setRet(0).setMsg("ok")).setBody(respBody));
		}).start();

    	return completableFuture;
    }

	@EasyMethod(method="getProfile2")
	public Single<Response> onGetProfile2(Request request) throws Exception {
    	log.info("req getProfile head=[{}],body=[{}]",request.getHead().toString(),request.getBody().toString());
		return Single.create(em ->{
			new Thread(()->{

				ObjectNode respBody = Utils.json.createObjectNode();
				ObjectNode info =  respBody.putObject("info");
				info.put("name","hello");
				info.put("tag","xxxxxxxx");
				info.put("headPic","http://www.xxxx.com/xxx/xxxx.jpg");
				info.put("uid",10000);

				try{  Thread.sleep(500); } catch (Exception e){}

				em.onSuccess(new Response().setHead(request.getHead().setRet(0).setMsg("ok")).setBody(respBody));
			}).start();
		});
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
