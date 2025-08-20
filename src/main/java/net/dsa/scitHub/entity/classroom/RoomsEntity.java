package net.dsa.scitHub.entity.classroom;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.dsa.scitHub.entity.reservation.ReservationsEntity;
import java.util.List;
import java.util.ArrayList;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "rooms")
public class RoomsEntity {

    public enum Type { CLASSROOM, STUDY, MEETING }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id", columnDefinition = "int unsigned")
    private Integer roomId;

    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private Type type;

    @Column(name = "floor_no")
    private Integer floorNo;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "is_active", nullable = false, columnDefinition = "tinyint default 1")
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (isActive == null) isActive = true;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // 이 방의 좌석 맵
    @Builder.Default
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private List<SeatMapsEntity> seatMaps = new ArrayList<>();

    // 이 방의 예약들
    @Builder.Default
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private List<ReservationsEntity> reservations = new ArrayList<>();
}
