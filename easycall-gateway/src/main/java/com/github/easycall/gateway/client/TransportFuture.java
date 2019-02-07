package com.github.easycall.gateway.client;

public class TransportFuture {

    private TransportPackage result;
    private volatile boolean done = false;
    private Exception cause;
    private TransportCallbackHandler handler;

    public synchronized void setResult(TransportPackage result) {

        this.result = result;
        done = true;
        if(handler != null){
            handler.onResult(this);
        }
        notify();
    }

    public synchronized void setCallback(TransportCallbackHandler handler){
        this.handler = handler;
        if(done){
            this.handler.onResult(this);
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

    public synchronized TransportPackage getResult() {

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
