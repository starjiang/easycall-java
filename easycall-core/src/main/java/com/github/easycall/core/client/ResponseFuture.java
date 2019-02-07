package com.github.easycall.core.client;
import com.github.easycall.core.util.EasyPackage;


public class ResponseFuture {

    private EasyPackage result;
    private volatile boolean done = false;
    private Exception cause;
    private ResponseCallbackHandler handler;

    public synchronized void setResult(EasyPackage result) {

        this.result = result;
        done = true;
        if(handler != null){
            handler.onResult(this);
        }
        notify();
    }

    public synchronized void setCallback(ResponseCallbackHandler handler){
        this.handler = handler;
        if(done){
            handler.onResult(this);
        }
    }

    public synchronized boolean isDone(){
        return done;
    }

    public synchronized boolean isException(){
        return cause != null;
    }

    public synchronized void setException(Exception cause) {

        this.cause = cause;
        done = true;
        if(handler != null){
            handler.onResult(this);
        }
        notify();
    }

    public synchronized Exception getException(){
        return cause;
    }

    public synchronized EasyPackage getResult() {

        while(!done){
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}
