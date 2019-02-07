package com.github.easycall.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class WorkerThread extends Thread
{
    private static Logger log = LoggerFactory.getLogger(WorkerThread.class);
    private WorkerPool pool;
    private boolean start;
    private int index;

    public WorkerThread(WorkerPool pool,int index)
    {
        this.start = false;
        this.pool = pool;
        this.index = index;
        setDaemon(true);
        setName("WorkerThread-"+getId());
    }

    public void startThread()
    {
        start = true;
        start();
    }

    public void stopThread()
    {
        start = false;
    }

    public void run()
    {
        while(start)
        {
            try
            {
                Object item = pool.consume(index);
                pool.onMessage((Message) item,index);

            }
            catch(Exception e)
            {
                log.error("work process message exception:"+e.getMessage(),e);
            }
        }
    }
}