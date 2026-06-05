package com.example.backend.config;

import com.example.backend.entity.User;
import com.example.backend.exception.UnauthorizedException;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CommentAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    private static final Pattern LESSON_COMMENT_DESTINATION =
            Pattern.compile("^/topic/lessons/(\\d+)/comments$");

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final CommentAccessService commentAccessService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(buildAuthentication(accessor));
            return message;
        }

        Principal principal = accessor.getUser();
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("WebSocket authentication is required");
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor, authentication);
        }

        return message;
    }

    private Authentication buildAuthentication(StompHeaderAccessor accessor) {
        String authorization = resolveAuthorizationHeader(accessor);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing bearer token for WebSocket connection");
        }

        String token = authorization.substring(7).trim();
        Jwt jwt = jwtDecoder.decode(token);
        String role = jwt.getClaimAsString("role");
        List<SimpleGrantedAuthority> authorities = StringUtils.hasText(role)
                ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                : List.of();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(jwt.getSubject(), jwt, authorities);
        authentication.setDetails(jwt);
        return authentication;
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor, Authentication authentication) {
        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination)) {
            throw new UnauthorizedException("Missing subscription destination");
        }

        if (destination.startsWith("/user/queue/")) {
            return;
        }

        Matcher lessonMatcher = LESSON_COMMENT_DESTINATION.matcher(destination);
        if (lessonMatcher.matches()) {
            Integer lessonId = Integer.valueOf(lessonMatcher.group(1));
            Integer userId = Integer.valueOf(authentication.getName());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("User not found for WebSocket session"));
            if (!commentAccessService.canAccessLesson(lessonId, user)) {
                throw new UnauthorizedException("You do not have access to this lesson comment channel");
            }
            return;
        }

        throw new UnauthorizedException("Subscription destination is not allowed");
    }

    private String resolveAuthorizationHeader(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader("Authorization");
        if (headers == null || headers.isEmpty()) {
            headers = accessor.getNativeHeader("authorization");
        }
        return (headers == null || headers.isEmpty()) ? null : headers.get(0);
    }
}
