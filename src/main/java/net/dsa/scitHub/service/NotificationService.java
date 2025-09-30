package net.dsa.scitHub.service;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.persistence.EntityNotFoundException;
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
import net.dsa.scitHub.repository.board.SseEmitterRepository;
import net.dsa.scitHub.repository.user.NotificationRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository nr;
    private final SseEmitterRepository ser;
    private final BoardRepository br;

    /**
     * 알림을 생성, 저장하고 실시간으로 발송하는 통합 메서드
     * @param recipient 알림을 받을 사용자
     * @param type 알림 종류
     * @param relatedEntity 알림의 원인이 된 객체 (Post, Comment, Message, Event)
     */
    @Transactional
    public void send(User recipient, NotificationType type, Object relatedEntity) {
        // 1. 알림 엔티티 생성
        Notification notification = createNotification(recipient, type, relatedEntity);
        if (notification == null) {
            log.debug("본인의 행동에 대한 알림은 생성하지 않습니다.");
            return;
        }

        // 2. DTO로 변환
        NotificationDTO notificationDTO = NotificationDTO.convertToDTO(notification);

        // 3. 실시간 알림 발송
        // SseEmitterRepository에서 해당 사용자의 Emitter를 찾음
        SseEmitter emitter = ser.findById(recipient.getUserId());

        if (emitter == null) {
            log.debug("SSE Emitter가 존재하지 않습니다. 실시간 알림을 보낼 수 없습니다. [userId={}]", recipient.getUserId());
            return;
        }

        // 4. Emitter를 통해 클라이언트로 알림 전송
        try {
            emitter.send(SseEmitter.event()
                .name("newNotification")
                .data(notificationDTO));
        } catch (IOException e) {
            // 전송 실패 시 Emitter 제거
            log.error("SSE 전송 중 오류 발생 [userId={}]: {}", recipient.getUserId(), e.getMessage());
            ser.deleteById(recipient.getUserId());
        }
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
            // 만약 게시물이 공지사항이고, 작성자가 수신자라면 알림 생성하지 않음
            if (List.of("announcement", "announcementIT", "announcementJP").contains(post.getBoard().getName())
                && post.getUser().equals(recipient)) {
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
            case NEW_ANNOUNCEMENT -> {
                String announcementName = ((Post) entity).getBoard().getName();
                String announcementContent = announcementName.equals("announcement") ? "お知らせ - " : announcementName.equals("announcementIT") ? "ITお知らせ - " : "日本語お知らせ - ";
                announcementContent += ((Post) entity).getTitle();
                yield checkContentLength(announcementContent, 30);
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
            case NEW_MESSAGE -> "/mypage/messages";
            case NEW_EVENT -> "/calendar/schedule";
            case NEW_ANNOUNCEMENT -> {
                Post post = (Post) entity;
                yield resolvePostUrlByBoard(post);
            }
        };
    }

    /**
     * 게시판에 따라 게시글 URL 생성
     * @param post 게시글 객체
     * @return 게시판에 따른 게시글 URL
     */
    private String resolvePostUrlByBoard(Post post) {
        if (post.getBoard().getBoardId() == br.findByName("inquiry").get().getBoardId()) {
            return "/admin/inquiryRead?postId=" + post.getPostId();
        } else if (
            List.of(
                br.findByName("announcement").get().getBoardId(),
                br.findByName("announcementIT").get().getBoardId(),
                br.findByName("announcementJP").get().getBoardId()
            ).contains(post.getBoard().getBoardId())
        ) {
            return "/admin/announcement/read?postId=" + post.getPostId();
        } else {
            return "/community/readPost?postId=" + post.getPostId();
        }
    }

    /**
     * 특정 사용자의 읽지 않은 알림 개수 조회
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount(Integer userId) {
        User user = User.builder().userId(userId).build();  // 간단히 ID만 가진 User 객체 생성
        return nr.countByUserAndIsReadFalse(user);
    }

    /**
     * 특정 사용자의 최근 알림 목록 조회
     * @param userId 사용자 ID
     * @param limit 조회할 알림 개수
     * @return 최근 알림 목록
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getRecentNotifications(Integer userId, int limit) {
        User user = User.builder().userId(userId).build();  // 간단히 ID만 가진 User 객체 생성
        // PageRequest.of(0, limit)로 최신 limit개 알림 조회
        List<Notification> notifications = nr.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, limit));

        // Entity List -> DTO List 변환
        return notifications.stream()
                .map(NotificationDTO::convertToDTO)
                .toList();
    }

    /**
     * 알림을 '읽음' 상태로 표시
     * @param notificationId 읽음 처리할 알림 ID
     */
    @Transactional
    public void markAsRead(Integer notificationId) {
        Notification notification = nr.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("알림을 찾을 수 없습니다. ID=" + notificationId));

        // 이미 읽은 상태가 아니라면 '읽음'으로 변경
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            // @Transactional 덕분에 별도의 save 호출 없이 변경 사항이 자동으로 저장됩니다.
            log.debug("알림 읽음 처리 완료 [notificationId={}]", notificationId);
        }
    }

    /**
     * 특정 사용자의 모든 알림을 '읽음' 상태로 표시
     * @param userId 사용자 ID
     */
    @Transactional
    public void markAllAsRead(Integer userId) {
        User user = User.builder().userId(userId).build();  // 간단히 ID만 가진 User 객체 생성

        // 1. 해당 사용자의 읽지 않은 모든 알림 조회
        List<Notification> unreadNotifications = nr.findAllByUserAndIsReadFalse(user);

        // 2. 각 알림의 isRead 필드를 true로 설정
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
            log.debug("알림 읽음 처리 완료 [notificationId={}]", notification.getNotificationId());
        }
    }
}
