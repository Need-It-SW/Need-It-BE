package com.needitbe.global.security;

import com.needitbe.domain.chat.service.ChatService;
import com.needitbe.domain.member.entity.Member;
import com.needitbe.domain.member.repository.MemberRepository;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    private static final String WS_AUTH_KEY = "NEEDIT_WS_AUTH";

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final ChatService chatService;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // =====================================================
        // 1. WebSocket 연결 인증 (JWT)
        // =====================================================
        if (StompCommand.CONNECT.equals(command)) {

            String authorization =
                    accessor.getFirstNativeHeader("Authorization");

            if (authorization == null
                    || !authorization.startsWith("Bearer ")) {

                throw new IllegalArgumentException(
                        "WebSocket 인증 토큰이 필요합니다."
                );
            }

            String token = authorization.substring(7);

            Claims claims = jwtTokenProvider.parseClaims(token);

            if (!"access".equals(claims.get("type", String.class))) {

                throw new IllegalArgumentException(
                        "Access Token만 사용할 수 있습니다."
                );
            }

            Long memberId = Long.valueOf(claims.getSubject());
            System.out.println("🔐 WebSocket 연결 사용자 ID: " + memberId);

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "존재하지 않는 회원입니다."
                            )
                    );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            member.getId().toString(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + member.getRole().name()
                                    )
                            )
                    );

            accessor.setUser(authentication);
            // Persist the validated authentication for subsequent STOMP frames.
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put(WS_AUTH_KEY, authentication);
            }
        }

        // =====================================================
        // 2. WebSocket 구독 및 메시지 전송 권한 검증
        // =====================================================
        if (StompCommand.SUBSCRIBE.equals(command)
                || StompCommand.SEND.equals(command)) {

            Principal principal = accessor.getUser();
            // Some transports expose the CONNECT principal only via session attributes
            // when processing subsequent SUBSCRIBE/SEND frames.
            if (!(principal instanceof Authentication)) {
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                Object saved = sessionAttributes == null ? null : sessionAttributes.get(WS_AUTH_KEY);
                if (saved instanceof Authentication authentication && authentication.isAuthenticated()) {
                    principal = authentication;
                    accessor.setUser(authentication);
                }
            }
            log.debug("STOMP {} destination={} session={} principal={}", command,
                    accessor.getDestination(), accessor.getSessionId(),
                    principal == null ? "null" : principal.getName());

            if (principal == null) {
                throw new IllegalArgumentException(
                        "인증되지 않은 WebSocket 사용자입니다."
                );
            }

            String destination = accessor.getDestination();

            if (destination == null) {
                throw new IllegalArgumentException(
                        "메시지 경로가 없습니다."
                );
            }

            // Authenticated members may subscribe to public request-feed changes.
            // Clients still fetch the actual post details through the REST API.
            if (StompCommand.SUBSCRIBE.equals(command)
                    && "/topic/requests".equals(destination)) {
                return message;
            }

            // 관리자 Kafka 행동 로그 실시간 갱신 구독
            if (StompCommand.SUBSCRIBE.equals(command)
                    && ("/topic/admin/user-actions".equals(destination)
                        || "/topic/admin/ad-statistics".equals(destination)
                        || "/topic/admin/filter-analysis".equals(destination))) {

                if (!(principal instanceof Authentication authentication)) {
                    log.warn("Admin STOMP subscription rejected: missing authentication, session={}", accessor.getSessionId());
                    throw new org.springframework.security.access.AccessDeniedException("관리자 인증 정보가 없습니다.");
                }
                boolean isAdmin = authentication
                        .getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                "ROLE_ADMIN".equals(authority.getAuthority())
                        );

                if (!isAdmin) {
                    throw new org.springframework.security.access.AccessDeniedException(
                            "관리자만 관리자 실시간 채널을 구독할 수 있습니다."
                    );
                }

                return message;
            }

            // =====================================================
// 3. 사용자별 알림 및 오류 메시지 구독
// =====================================================
            if (StompCommand.SUBSCRIBE.equals(command)
                    && (
                    destination.equals("/user/queue/notifications")
                            || destination.equals("/user/queue/errors")
                            || destination.equals("/user/queue/account-status")
            )) {

                return message;
            }

            Long chatRoomId;

            // =====================================================
            // 4. 채팅방 구독 권한 검증
            // =====================================================
            if (StompCommand.SUBSCRIBE.equals(command)) {

                // 허용 경로:
                // /topic/chats/{chatRoomId}
                // /topic/chats/{chatRoomId}/read

                if (!destination.matches(
                        "^/topic/chats/\\d+(/read)?$"
                )) {

                    throw new IllegalArgumentException(
                            "허용되지 않은 구독 경로입니다."
                    );
                }

                String[] parts = destination.split("/");

                chatRoomId = Long.valueOf(parts[3]);

            } else {

                // =====================================================
                // 5. 채팅 메시지 전송 권한 검증
                // =====================================================

                // 허용 경로:
                // /app/chats/{chatRoomId}/send

                if (!destination.matches(
                        "^/app/chats/\\d+/send$"
                )) {

                    throw new IllegalArgumentException(
                            "허용되지 않은 메시지 전송 경로입니다."
                    );
                }

                String id = destination.substring(
                        "/app/chats/".length(),
                        destination.length() - "/send".length()
                );

                chatRoomId = Long.valueOf(id);
            }

            // =====================================================
            // 6. 채팅방 참여자 검증
            // =====================================================
            Long memberId = Long.valueOf(principal.getName());

            chatService.validateChatParticipant(
                    memberId,
                    chatRoomId
            );
        }

        return message;
    }
}