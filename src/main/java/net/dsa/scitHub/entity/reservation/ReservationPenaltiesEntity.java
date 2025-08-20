package net.dsa.scitHub.entity.reservation;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

import net.dsa.scitHub.entity.user.UsersEntity;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "reservation_penalties",
    uniqueConstraints = @UniqueConstraint(name = "uq_penalty_user", columnNames = "user_id")
)
public class ReservationPenaltiesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "penalty_id", columnDefinition = "int unsigned")
    private Integer penaltyId;

    // 1인 1레코드 → OneToOne(Unique)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_penalty_user"))
    private UsersEntity user;

    @Column(name = "strike_count", nullable = false, columnDefinition = "int default 0")
    private Integer strikeCount;

    @Column(name = "banned_until")
    private LocalDateTime bannedUntil;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (strikeCount == null) strikeCount = 0;
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
