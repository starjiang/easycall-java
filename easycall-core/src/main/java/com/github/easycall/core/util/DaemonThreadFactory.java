package com.github.easycall.core.util;

import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory
{
	private String name;
	public DaemonThreadFactory(String name){
		this.name = name;
	}
	public Thread newThread(Runnable r)
	{
		Thread thread = new Thread(r);
		thread.setDaemon(true);
		thread.setName(name+"-"+thread.getId());

		return thread;
	}
}