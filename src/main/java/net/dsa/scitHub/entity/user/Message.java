package net.dsa.scitHub.entity.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "message")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"sender", "receiver", "notifications"})
public class Message {
    
    /** 메시지 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Integer messageId;
    
    /** 메시지 발신자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    /** 메시지 수신자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;
    
    /** 메시지 제목 */
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    /** 메시지 내용 */
    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
    
    /** 메시지 읽음 여부 */
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;
    
    /** 메시지 발송 시간 */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    /** 메시지 관련 알림 목록 */
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL)
    private List<Notification> notifications;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return Objects.equals(messageId, message.messageId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
}
