package net.dsa.scitHub.entity.user;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.board.Comment;
import net.dsa.scitHub.entity.board.Post;
import net.dsa.scitHub.entity.reservation.Reservation;
import net.dsa.scitHub.entity.schedule.Event;
import net.dsa.scitHub.entity.studentGroup.StudentGroup;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "notification")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user", "post", "comment", "message", "studentGroup", "event", "reservation"})
public class Notification {
    
    /** 알림 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;
    
    /** 알림 대상 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /** 알림 제목 */
    @Column(name = "title", nullable = false, length = 150)
    private String title;
    
    /** 알림 내용 */
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
    
    /** 알림 클릭 시 이동할 URL */
    @Column(name = "target_url", length = 500)
    private String targetUrl;
    
    /** 알림 읽음 여부 */
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    /** 알림 생성 시간 */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /** 알림과 관련된 게시글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
    
    /** 알림과 관련된 댓글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;
    
    /** 알림과 관련된 메시지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;
    
    /** 알림과 관련된 학생 그룹 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_group_id")
    private StudentGroup studentGroup;
    
    /** 알림과 관련된 일정 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;
    
    /** 알림과 관련된 예약 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        Notification that = (Notification) o;
        return Objects.equals(notificationId, that.notificationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(notificationId);
    }
}
