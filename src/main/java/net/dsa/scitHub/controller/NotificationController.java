package net.dsa.scitHub.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.repository.board.SseEmitterRepository;
import net.dsa.scitHub.service.NotificationService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService ns;
    private final SseEmitterRepository ser;
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 1시간

    /**
     * SSE 구독 엔드포인트
     * @param userId 현재 로그인한 사용자의 ID (AuthenticationPrincipal을 통해 주입)
     * @return SseEmitter - 서버에서 클라이언트로 이벤트를 푸시하기 위한 객체
     */
    @GetMapping(value = "/notifications/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal(expression = "userId") Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        ser.save(userId, emitter);

        emitter.onCompletion(() -> ser.deleteById(userId));
        emitter.onTimeout(() -> ser.deleteById(userId));

        try {
            emitter.send(SseEmitter.event().name("connect").data("SSE 연결 성공 [userId=" + userId + "]"));
        } catch (Exception e) {
            log.error("SSE 연결 중 오류 발생 [userId={}]: {}", userId, e.getMessage());
            // 연결에 실패하면 Emitter를 제거
            ser.deleteById(userId);
        }

        return emitter;
    }

    @PostMapping("/notifications/read/all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal(expression = "userId") Integer userId) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        ns.markAllAsRead(userId);

        return ResponseEntity.ok().build();
    }
}
