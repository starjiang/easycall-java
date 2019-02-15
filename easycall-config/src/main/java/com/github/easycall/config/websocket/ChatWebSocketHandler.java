package com.github.easycall.config.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.easycall.config.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ChatWebSocketHandler extends TextWebSocketHandler{

    private final static Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final static List<WebSocketSession> sessions = Collections.synchronizedList(new ArrayList<WebSocketSession>());

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        logger.info("{},{}",session.getId(), message.getPayload());
        JsonNode pkg = Utils.om.readTree(message.getPayload());
        String event = pkg.get("event").asText();
        if(event.equals("ping")){
            session.sendMessage(new TextMessage("{\"event\":\"pong\",\"data\":{}}"));
            WebSocketNotification.updateConfigConnection(session);
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
        logger.info("{},{} pong",session.getId(),session.getRemoteAddress());
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        logger.info("{},{} connect to the websocket success",session.getId(),session.getRemoteAddress());
        boolean added = WebSocketNotification.addConfigConnection(session);
        if(!added) {
            session.close();
        }

    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

        if(session.isOpen()){
            session.close();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {

        logger.info("{},{} connection closed",session.getId(),session.getRemoteAddress());
        WebSocketNotification.removeConfigConnection(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}