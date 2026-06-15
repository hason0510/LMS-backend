package com.example.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${frontend.allowed-origins:${frontend.url}}")
    private String frontendAllowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(resolveAllowedOrigins())
                .withSockJS();
    }

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    public WebSocketConfig(JwtDecoder jwtDecoder,
                           JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    if (authorization == null || authorization.isEmpty() || !authorization.get(0).startsWith("Bearer ")) {
                        throw new AccessDeniedException("Không tìm thấy JWT Token hoặc sai định dạng");
                    }
                    String token = authorization.get(0).substring(7); // Remove "Bearer "
                    try {
                        // jwtDecoder.decode() tự động kiểm tra cả chữ ký (signature) và thời hạn (expiration)
                        Jwt jwt = jwtDecoder.decode(token);
                        JwtAuthenticationToken authentication =
                                new JwtAuthenticationToken(
                                        jwt,
                                        jwtAuthenticationConverter.convert(jwt).getAuthorities(),
                                        jwt.getSubject());
                        accessor.setUser(authentication);
                    } catch (Exception e) {
                        throw new AccessDeniedException("Token JWT không hợp lệ hoặc đã hết hạn: " + e.getMessage());
                    }
                }
                return message;
            }
        });
    }

    private String[] resolveAllowedOrigins() {
        return Arrays.stream(frontendAllowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }
}
