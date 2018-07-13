package com.github.easycall.config;

import com.github.easycall.config.servlet.ConfigServlet;
import com.github.easycall.config.util.Config;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;

public class Application {

    public static void main(String []args) throws Exception{

        Server server = new Server(Config.instance.getInt("manage.port",8080));
        ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        ResourceHandler resourceHandler = new ResourceHandler();

        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{ "index.html" });
        resourceHandler.setResourceBase(Config.instance.getString("manage.rootPath","./htdocs"));
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resourceHandler, handler });
        server.setHandler(handlers);


        handler.addServletWithMapping(ConfigServlet.class,"/config/*");

        server.start();
        server.join();
    }
}
