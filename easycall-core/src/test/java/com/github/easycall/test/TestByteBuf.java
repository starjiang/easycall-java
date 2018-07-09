package com.github.easycall.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestByteBuf {

    ExecutorService pool = Executors.newCachedThreadPool();//线程池里面

    @Test
    public void TestByteBuf1(){

        CompositeByteBuf buf = Unpooled.compositeBuffer();

        ByteBuf buf1 = Unpooled.buffer(10);
        buf1.writeByte(2);
        buf1.writeByte(0);
        buf1.writeInt(100);
        buf1.writeInt(100);
        buf.addComponents(true,buf1);


        System.out.println(buf.readableBytes());
    }

    class Task implements Runnable{

        ByteBuf buf;

        public Task(ByteBuf buf){
            this.buf = buf;
        }


        @Override
        public void run() {
            try {
                Thread.sleep(10000);
                byte [] data = new byte[buf.readableBytes()];

                buf.getBytes(0,data);
                System.out.println(new String(data));

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void TestByteBuf2(){

        String data = "1234567890abcdefghijklmnopqrstuvwxyz";

        ByteBuf buf = Unpooled.wrappedBuffer(data.getBytes());

        ByteBuf buf1 = buf.slice(0,10);

        pool.execute(new Task(buf1));

        try{
            Thread.sleep(12000);
        }catch (Exception e){

        }

    }
}
