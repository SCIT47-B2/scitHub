package net.dsa.scitHub.entity.reservation;

import jakarta.persistence.*;
import lombok.*;
import net.dsa.scitHub.entity.classroom.Classroom;
import net.dsa.scitHub.entity.user.Account;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "reservation")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"classroom", "account"})
public class Reservation {
    
    /** 예약 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Integer reservationId;
    
    /** 예약 대상 강의실 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;
    
    /** 예약 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    /** 예약 시작 시간 */
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;
    
    /** 예약 종료 시간 */
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;
    
    /** 예약 생성 시간 */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation)) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(reservationId, that.reservationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }
}
