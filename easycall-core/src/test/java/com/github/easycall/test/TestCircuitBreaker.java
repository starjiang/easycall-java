package com.github.easycall.test;

import com.github.easycall.client.CircuitBreaker;
import com.github.easycall.exception.EasyException;
import com.github.easycall.util.EasyPackage;
import org.junit.Test;

public class TestCircuitBreaker {

    static int seq = 0;
    public EasyPackage call() throws Exception {
        EasyPackage pkg = EasyPackage.newInstance();
        seq++;
        System.out.println("seq="+seq);
        if(seq > 40 && seq < 90){
            throw new EasyException("excpetion:is null");
        }
        return EasyPackage.newInstance();
    }


    public void testCircuitBreaker(){

        for(int i=0;i<100000;i++){
            try{
                CircuitBreaker.call("cb",()->{ return call();},EasyPackage.newInstance());
                Thread.sleep(100);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }
    }
}
