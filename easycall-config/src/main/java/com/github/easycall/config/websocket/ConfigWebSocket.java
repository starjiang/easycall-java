package com.github.easycall.config.websocket;

import org.springframework.web.socket.WebSocketSession;

public class ConfigWebSocket {
    private WebSocketSession session;
    private Long version;
    private Long lastActiveTime;

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(Long lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }
}
