package net.dsa.scitHub.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;
import net.dsa.scitHub.entity.user.Notification;

@Data
@Builder
public class NotificationDTO {
    private Integer notificationId;
    private String title;
    private String content;
    private String targetUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationDTO convertToDTO(Notification notification) {
        return NotificationDTO.builder()
            .notificationId(notification.getNotificationId())
            .title(notification.getTitle())
            .content(notification.getContent())
            .targetUrl(notification.getTargetUrl())
            .isRead(notification.getIsRead())
            .createdAt(notification.getCreatedAt())
            .build();
    }
}
