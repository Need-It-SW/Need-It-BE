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
import java.util.List;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

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

        // 1. WebSocket 연결 인증
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
        }

        // 2. 채팅방 구독 및 메시지 전송 권한 검증
        if (StompCommand.SUBSCRIBE.equals(command)
                || StompCommand.SEND.equals(command)) {

            Principal principal = accessor.getUser();

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

            Long chatRoomId;

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

                // /topic/chats/1/read -> 1
                // /topic/chats/1      -> 1

                String[] parts = destination.split("/");

                chatRoomId = Long.valueOf(parts[3]);

            } else {

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

            Long memberId = Long.valueOf(principal.getName());

            // 채팅방 참여자 검증
            chatService.validateChatParticipant(
                    memberId,
                    chatRoomId
            );
        }

        return message;
    }
}