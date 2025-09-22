package net.dsa.scitHub.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dsa.scitHub.dto.NotificationDTO;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.entity.schedule.Event;
import net.dsa.scitHub.entity.user.Message;
import net.dsa.scitHub.entity.user.Notification;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.enums.NotificationType;
import net.dsa.scitHub.repository.board.BoardRepository;
import net.dsa.scitHub.repository.user.NotificationRepository;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final NotificationRepository nr;
    private final BoardRepository br;

    // 1. Thread-safe한 ConcurrentHashMap을 사용하여 여러 사용자의 SseEmitter를 관리
    private static final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 1시간 타임아웃

    // 2. 사용자가 알림을 구독할 때 호출되는 메서드
    /**
     * 사용자가 알림을 구독
     * @param userId 구독하는 사용자의 ID
     * @return SseEmitter 객체
     */
    public SseEmitter subscribe(Integer userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);

        // 연결이 끊어지거나(onCompletion), 타임아웃(onTimeout)될 때 Emitter를 제거
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));

        //  Mime Type T-Mobile 오류 방지를 위한 더미 데이터 전송
        sendToClient(userId, "EventStream Created. [userId=" + userId + "]");

        return emitter;
    }

    // 3. 특정 사용자에게 알림을 전송하는 메서드
    /**
     * 특정 사용자에게 알림 전송
     * @param userId 알림을 받을 사용자의 ID
     * @param data 전송할 데이터
     */
    public void sendNotification(Integer userId, Object data) {
        sendToClient(userId, data);
    }

    /**
     * 클라이언트로 데이터 전송
     * @param userId 사용자 ID
     * @param data 전송할 데이터
     */
    private void sendToClient(Integer userId, Object data) {
        SseEmitter emitter = emitters.get(userId);
        log.debug("SSE Emitter 조회 [userId={}, emitter={}]", userId, emitter);

         // Emitter가 존재할 때만 전송 시도
        if (emitter != null) {
            try {
                emitter.send(
                    SseEmitter.event()
                        .id(String.valueOf(userId)) // 이벤트 ID 설정
                        .name("newNotification") // 이벤트 이름 설정(클라이언트에서 수신할 때 사용)
                        .data(data) // 전송할 데이터 설정(실제 데이터)
                );
                log.info("SSE 전송 완료 [userId={}]", userId);
            } catch (IOException e) {
                emitters.remove(userId); // 전송 실패 시 Emitter 제거
                log.error("SSE 전송 중 오류 발생", e);
            }
        }
    }

    /**
     * 알림을 생성, 저장하고 실시간으로 발송하는 통합 메서드
     * @param recipient 알림을 받을 사용자
     * @param type 알림 종류
     * @param relatedEntity 알림의 원인이 된 객체 (Post, Comment, Message, Event)
     */
    public void send(User recipient, NotificationType type, Object relatedEntity) {
        // 1. 알림 엔티티 생성
        Notification notification = createNotification(recipient, type, relatedEntity);
        if (notification == null) {
            log.debug("본인의 행동에 대한 알림은 생성하지 않습니다.");
            return;
        }

        // 2. DTO로 변환
        NotificationDTO notificationDTO = NotificationDTO.convertToDTO(notification);

        log.debug("알림 전송 준비 완료 [userId={}]", recipient.getUserId());

        // 3. SSE로 실시간 알림 발송
        sendToClient(recipient.getUserId(), notificationDTO);
    }

    /**
     * 알림 엔티티 생성 및 저장
     * @param recipient 알림을 받을 사용자
     * @param type 알림 종류
     * @param entity 알림의 원인이 된 객체 (Post, Comment, Message, Event)
     * @return 생성된 Notification 객체
     */
    private Notification createNotification(User recipient, NotificationType type, Object entity) {
        // 본인이 한 활동에 대해서는 알림을 생성하지 않음 (예, 내 글에 내가 댓글)
        // 이 부분을 확인하기 위해 관련 엔티티들의 정보 필요
        if (entity instanceof Post post) {  // Post에 대한 알림 (게시글 좋아요)
            // 본인이 작성한 글이어도, 어차피 자기 글에 좋아요 누를 수 없으므로 return null 필요없음
            // 즉, 게시글 좋아요에 대한 알림이면 무조건 알림 발생
            // if (post.getUser().equals(recipient)) {
            //     return null;
            // }
            String content = createNotificationContent(type, entity);
            String url = createNotificationUrl(type, entity);

            Notification notification = Notification.builder()
                .user(recipient)
                .title(type.getTitle())
                .content(content)
                .targetUrl(url)
                .notificationType(type)
                .post(post)
                .build();

            return nr.save(notification);
        } else if (entity instanceof Comment comment) { // 댓글에 대한 알림 (내 글에 댓글)
            // 본인이 단 댓글에 대한 알림은 생성하지 않음
            if (comment.getUser().equals(recipient)) {
                return null;
            }
            String content = createNotificationContent(type, entity);
            String url = createNotificationUrl(type, entity);

            Notification notification = Notification.builder()
                .user(recipient)
                .title(type.getTitle())
                .content(content)
                .targetUrl(url)
                .notificationType(type)
                .comment(comment)
                .build();

            return nr.save(notification);
        } else if (entity instanceof Message message) { // 메시지에 대한 알림 (새 메시지 도착)
            // 본인이 보낸 메시지에 대한 알림은 생성하지 않음
            if (message.getSender().equals(recipient)) {
                return null;
            }
            String content = createNotificationContent(type, entity);
            String url = createNotificationUrl(type, entity);

            Notification notification = Notification.builder()
                .user(recipient)
                .title(type.getTitle())
                .content(content)
                .targetUrl(url)
                .notificationType(type)
                .message(message)
                .build();

            return nr.save(notification);
        } else if (entity instanceof Event event) { // 일정에 대한 알림 (새 일정 등록)
            // 일정은 특정 사용자가 만든 것이 아니므로 본인 확인 불필요
            String content = createNotificationContent(type, entity);
            String url = createNotificationUrl(type, entity);

            Notification notification = Notification.builder()
                .user(recipient)
                .title(type.getTitle())
                .content(content)
                .targetUrl(url)
                .notificationType(type)
                .event(event)
                .build();

            return nr.save(notification);
        } else {
            log.warn("알림 생성 실패: 알 수 없는 엔티티 타입 - {}", entity.getClass().getName());
            return null;
        }
    }

    /**
     * 알림 내용 생성
     * @param type 알림 종류
     * @param entity 알림의 원인이 된 객체
     * @return 생성된 알림 내용
     */
    private String createNotificationContent(NotificationType type, Object entity) {
        return switch (type) {
            case NEW_LIKE_ON_POST -> {
                String postNotificationContent = "タイトル: " + ((Post) entity).getTitle();
                yield checkContentLength(postNotificationContent, 30);
            }
            case NEW_COMMENT_ON_POST -> {
                String commentContent = ((Comment) entity).getContent();
                yield checkContentLength(commentContent, 30);
            }
            case NEW_MESSAGE -> {
                Message message = (Message) entity;
                String messageContent = "送信者: " + message.getSender().getNameKor() + ", 内容: " + message.getContent();
                yield checkContentLength(messageContent, 30);
            }
            case NEW_EVENT -> {
                Event event = (Event) entity;
                String eventContent = "タイトル: " + event.getTitle();
                yield checkContentLength(eventContent, 30);
            }
        };
    }

    /**
     * 내용 길이 확인 및 필요시 자르기
     * @param content 원본 내용
     * @param maxLength 최대 길이
     * @return 최대 길이를 초과하면 자르고 "..." 추가, 아니면 원본 내용 반환
     */
    private String checkContentLength(String content, int maxLength) {
        return content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
    }

    /**
     * 알림 URL 생성
     * @param type 알림 종류
     * @param entity 알림의 원인이 된 객체
     * @return 생성된 URL
     */
    private String createNotificationUrl(NotificationType type, Object entity) {
        return switch (type) {
            case NEW_LIKE_ON_POST -> {
                Post post = (Post) entity;
                yield resolvePostUrlByBoard(post);
            }
            case NEW_COMMENT_ON_POST -> {
                Comment comment = (Comment) entity;
                Post post = comment.getPost();
                yield resolvePostUrlByBoard(post);
            }
            case NEW_MESSAGE -> "/scitHub/mypage/messages";
            case NEW_EVENT -> "/scitHub/calendar/schedule";
        };
    }

    /**
     * 게시판에 따라 게시글 URL 생성
     * @param post 게시글 객체
     * @return 게시판에 따른 게시글 URL
     */
    private String resolvePostUrlByBoard(Post post) {
        if (post.getBoard().getBoardId() == br.findByName("inquiry").get().getBoardId()) {
            return "/scitHub/admin/inquiryRead?postId=" + post.getPostId();
        } else {
            return "/scitHub/community/readPost?postId=" + post.getPostId();
        }
    }
}
