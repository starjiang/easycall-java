package com.github.easycall.core.client;
import com.github.easycall.core.util.EasyPackage;

public class ResponseFuture {

    private volatile EasyPackage result;
    private volatile boolean done = false;
    private volatile Exception cause;
    private volatile ResponseCallbackHandler handler;

    synchronized public void setResult(EasyPackage result) {

        this.result = result;
        done = true;
        if(handler != null){
            handler.onResult(this);
        }
        notify();
    }

    public void setCallback(ResponseCallbackHandler handler){
        this.handler = handler;
        if(done){
            handler.onResult(this);
        }
    }

    public boolean isDone(){
        return done;
    }

    public boolean isException(){
        return cause != null;
    }

    synchronized public void setException(Exception cause) {

        this.cause = cause;
        done = true;
        if(handler != null){
            handler.onResult(this);
        }
        notify();
    }

    public Exception getException(){
        return cause;
    }

    synchronized public EasyPackage getResult() {

        while(!done){
            try{
                wait();
            }catch (InterruptedException e){
                setException(e);
                return result;
            }
        }
        return result;
    }

}
