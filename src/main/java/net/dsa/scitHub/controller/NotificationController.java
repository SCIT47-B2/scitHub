package net.dsa.scitHub.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.service.NotificationService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService ns;

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

        return ns.subscribe(userId);
    }

    @PostMapping("/notifications/read/{notificationId}")
    public ResponseEntity<Void> markAsRead(@PathVariable("notificationId") Integer notificationId) {
        log.debug("notificationId가 {}로 설정되었습니다.", notificationId);

        // 알림을 '읽음' 상태로 업데이트
        ns.markAsRead(notificationId);

        // 성공적으로 처리되었음을 알리는 200 OK 응답 반환
        return ResponseEntity.ok().build();
    }
}
