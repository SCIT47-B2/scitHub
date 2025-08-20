package net.dsa.scitHub.entity.reservation;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

import net.dsa.scitHub.entity.classroom.RoomsEntity;
import net.dsa.scitHub.entity.user.UsersEntity;
import java.util.List;
import java.util.ArrayList;

@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "reservations",
    indexes = {
        @Index(name = "idx_res_user_time", columnList = "user_id, start_at"),
        @Index(name = "idx_res_room_time", columnList = "room_id, start_at"),
        @Index(name = "idx_res_status_time", columnList = "status, start_at")
    }
)
public class ReservationsEntity {

    public enum Status { ACTIVE, CANCELLED, NO_SHOW, BLOCKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id", columnDefinition = "int unsigned")
    private Integer reservationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_res_room"))
    private RoomsEntity room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_res_user"))
    private UsersEntity user;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private Status status; // default ACTIVE

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (status == null) status = Status.ACTIVE;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // 이 예약에 걸린 신고들
    @Builder.Default
    @OneToMany(mappedBy = "reservation", fetch = FetchType.LAZY)
    private List<ReservationReportsEntity> reports = new ArrayList<>();
}
