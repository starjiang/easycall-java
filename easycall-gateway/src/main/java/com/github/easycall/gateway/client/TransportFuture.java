package com.github.easycall.gateway.client;

public class TransportFuture {

    private volatile TransportPackage result;
    private volatile boolean done = false;
    private volatile Exception cause;
    private volatile TransportCallbackHandler handler;

    synchronized public void setResult(TransportPackage result) {

        this.result = result;
        done = true;
        if(handler != null){
            handler.onResult(this);
        }
        notify();
    }

    public void setCallback(TransportCallbackHandler handler){
        this.handler = handler;
        if(done){
            this.handler.onResult(this);
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

   synchronized public TransportPackage getResult(){

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
