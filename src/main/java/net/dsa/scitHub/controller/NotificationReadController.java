package net.dsa.scitHub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.service.NotificationService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationReadController {

    private final NotificationService ns;

    @PostMapping("/notifications/read/{notificationId}")
    public ResponseEntity<Void> markAsRead(@PathVariable("notificationId") Integer notificationId) {
        log.debug("notificationId가 {}로 설정되었습니다.", notificationId);

        // 알림을 '읽음' 상태로 업데이트
        ns.markAsRead(notificationId);

        // 성공적으로 처리되었음을 알리는 200 OK 응답 반환
        return ResponseEntity.ok().build();
    }
}
