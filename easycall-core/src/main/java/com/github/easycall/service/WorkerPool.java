package com.github.easycall.service;

public interface WorkerPool {

	public int getMaxQueueSize();
	public int getQueueSize();
	public void start() throws Exception;
	public void stop();
	public Message consume(int index) throws Exception;
	public void onMessage(Message msg,int index);
}
