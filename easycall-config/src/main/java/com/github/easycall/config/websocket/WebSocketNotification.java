package com.github.easycall.config.websocket;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.easycall.config.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketNotification {
    private static Logger logger = LoggerFactory.getLogger(WebSocketNotification.class);
    private static Map<String,List<ConfigWebSocket>> configConns = new HashMap<>();
    private static Map<String,String> connConfigs = new HashMap<>();
    private static Map<String,ConfigWebSocket> sessions = new ConcurrentHashMap<>();

    private static Timer timer = new Timer(true);
    private static final Long CHECK_LIVE_INTERVAL = 180000L;

    static {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("check connections are living starting");
                try{
                    checkConnectionLive();
                }catch (Exception e){
                    logger.error(e.getMessage(),e);
                }
                logger.info("check connections are living end");
            }
        },0,CHECK_LIVE_INTERVAL);
    }


    public static void checkConnectionLive() throws Exception{

        Iterator<Map.Entry<String,ConfigWebSocket>> it = sessions.entrySet().iterator();

        while(it.hasNext()){
           ConfigWebSocket configWebSocket =  it.next().getValue();
           if(System.currentTimeMillis() - configWebSocket.getLastActiveTime()> CHECK_LIVE_INTERVAL){
               logger.error("{} connection heartbeat expired,close it",configWebSocket.getSession().getId());
               if(configWebSocket.getSession().isOpen()){
                   configWebSocket.getSession().close();
               }
           }
        }
    }

    public static boolean addConfigConnection(WebSocketSession session) throws Exception{

        Map<String,String> params = Utils.parserQueryString(session.getUri().getQuery());

        String config = params.get("name");
        Long version = Long.valueOf(params.getOrDefault("version","0"));

        if (config == null){
            logger.error("param name is empty,name must be required");
            return false;
        }

        synchronized (WebSocketNotification.class){

            if (connConfigs.get(session.getId()) != null){
                logger.error("{} connection have been added");
                return false;
            }
            connConfigs.put(session.getId(),config);

            List<ConfigWebSocket> conns = configConns.get(config);
            if(conns == null){
                conns = new LinkedList<>();
                configConns.put(config,conns);
            }
            ConfigWebSocket configWebSocket = new ConfigWebSocket();
            configWebSocket.setLastActiveTime(System.currentTimeMillis());
            configWebSocket.setVersion(version);
            configWebSocket.setSession(session);
            conns.add(configWebSocket);
            sessions.put(session.getId(),configWebSocket);
            return true;
        }
    }

    public static void updateConfigConnection(WebSocketSession session) {

        synchronized (WebSocketNotification.class){
            ConfigWebSocket configWebSocket = sessions.get(session.getId());
            if(configWebSocket == null){
                logger.error("{} connection not found",session.getId());
                return;
            }
            configWebSocket.setLastActiveTime(System.currentTimeMillis());
        }
    }

    public static boolean removeConfigConnection(WebSocketSession session) throws Exception{

        Map<String,String> params = Utils.parserQueryString(session.getUri().getQuery());
        String config = params.get("name");
        if (config == null){
            logger.error("param name is empty,name must be required");
            return false;
        }
        synchronized (WebSocketNotification.class){
            if (connConfigs.get(session.getId()) == null){
                logger.error("{} connection not found",session.getId());
                return false;
            }
            connConfigs.remove(session.getId());

            List<ConfigWebSocket>  conns = configConns.get(config);

            if(conns == null || conns.size() == 0 ){
                return true;
            }

            Iterator<ConfigWebSocket> it = conns.iterator();
            while (it.hasNext()){
                ConfigWebSocket configWebSocket = it.next();
                if(configWebSocket.getSession().getId() == session.getId()){
                    conns.remove(configWebSocket);
                }
            }
            sessions.remove(session.getId());

            if(session.isOpen()){
                session.close();
            }
            return true;
        }
    }

    public static void notifyConfigChanged(String config,Long version) throws Exception{
        synchronized (WebSocketNotification.class){

            List<ConfigWebSocket>  conns = configConns.get(config);

            if(conns == null || conns.size() == 0 ){
                logger.info("{} config have no node to notify",config);
                return;
            }

            logger.info("notify config {}",config);

            Iterator<ConfigWebSocket> it = conns.iterator();

            ObjectNode respPkg = Utils.om.createObjectNode();
            ObjectNode respBody = Utils.om.createObjectNode();

            respBody.put("name",config);
            respBody.put("version",version);

            respPkg.put("event","configChanged");
            respPkg.put("data",respBody);


            while(it.hasNext()){
                ConfigWebSocket configWebSocket = it.next();
                logger.info("notify {} node {},{}",config,configWebSocket.getSession().getId(),configWebSocket.getSession().getRemoteAddress());
                configWebSocket.getSession().sendMessage(new TextMessage(Utils.om.writeValueAsString(respPkg)));
            }
        }
    }
}
