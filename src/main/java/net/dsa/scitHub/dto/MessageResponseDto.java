package net.dsa.scitHub.dto;

import lombok.Builder;
import lombok.Getter;
import net.dsa.scitHub.entity.user.Message;
import net.dsa.scitHub.entity.user.User;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponseDto {
    private Integer messageId;
    private Integer senderId;
    private String senderName;
    private String senderUsername; // 답장 기능을 위해 발신자 username 추가
    private Integer receiverId;
    private String receiverName;
    private String title;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static MessageResponseDto from(Message message) {
        User sender = message.getSender();
        User receiver = message.getReceiver();

        // 발신자 또는 수신자 정보가 없을 경우 (예: 사용자 탈퇴)를 대비한 Null-safe 처리
        Integer senderId = (sender != null) ? sender.getUserId() : null;
        String senderUsername = (sender != null) ? sender.getUsername() : null;
        String senderName = (sender != null)
                ? String.format("%s (@%s)", sender.getNameKor(), sender.getUsername())
                : "알 수 없음";
        Integer receiverId = (receiver != null) ? receiver.getUserId() : null;
        String receiverName = (receiver != null)
                ? String.format("%s (@%s)", receiver.getNameKor(), receiver.getUsername())
                : "알 수 없음";

        return MessageResponseDto.builder()
                .messageId(message.getMessageId())
                .senderId(senderId)
                .senderName(senderName)
                .senderUsername(senderUsername)
                .receiverId(receiverId)
                .receiverName(receiverName)
                .title(message.getTitle())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
