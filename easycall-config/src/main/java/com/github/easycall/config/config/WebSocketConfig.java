package com.github.easycall.config.config;

import com.github.easycall.config.websocket.ChatWebSocketHandler;
import com.github.easycall.config.websocket.WebSocketHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(),"/websocket").setAllowedOrigins("*").addInterceptors(new WebSocketHandshakeInterceptor());
    }
}

