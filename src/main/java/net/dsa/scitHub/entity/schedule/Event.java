package net.dsa.scitHub.entity.schedule;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.user.User;
import net.dsa.scitHub.enums.Visibility;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user"})
public class Event {
    
    /** 이벤트 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Integer eventId;
    
    /** 이벤트 공개 범위 (PUBLIC: 공개, PRIVATE: 비공개) */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private Visibility visibility = Visibility.PUBLIC;
    
    /** 이벤트 생성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    /** 이벤트 제목 */
    @Column(name = "title", nullable = false, length = 150)
    private String title;
    
    /** 이벤트 내용 */
    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;
    
    /** 이벤트 시작 시간 */
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;
    
    /** 이벤트 종료 시간 */
    @Column(name = "end_at")
    private LocalDateTime endAt;
    
    /** 종일 이벤트 여부 */
    @Builder.Default
    @Column(name = "is_all_day", nullable = false)
    private Boolean isAllDay = false;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;
        Event event = (Event) o;
        return Objects.equals(eventId, event.eventId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}
