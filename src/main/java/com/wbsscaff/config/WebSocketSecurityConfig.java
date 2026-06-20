package com.wbsscaff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;

/**
 * WebSocket 安全設定：確保所有 STOMP 連線、訂閱、訊息均需通過身份驗證（Finding 3）
 * Spring Security 6.x 使用 @EnableWebSocketSecurity + AuthorizationManager 取代舊 AbstractSecurityWebSocketMessageBrokerConfigurer
 */
@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        messages
            .simpTypeMatchers(SimpMessageType.CONNECT).authenticated()
            .simpTypeMatchers(SimpMessageType.SUBSCRIBE).authenticated()
            .simpTypeMatchers(SimpMessageType.MESSAGE).authenticated()
            .anyMessage().authenticated();
        return messages.build();
    }
}
