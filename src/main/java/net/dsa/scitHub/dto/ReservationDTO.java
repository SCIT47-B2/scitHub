package net.dsa.scitHub.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.reservation.Reservation;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationDTO {
    private Integer reservationId;
    private Integer classroomId;
    private Integer userId;
    private String username;
    private String name;
    private Integer timeSlotId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private LocalDateTime createdAt;

    public static ReservationDTO convertToReservationDTO(Reservation reservation) {

        // timeSlotId 계산
        int hour = reservation.getStartAt().getHour();
        int timeSlotId = hour - 17;

        return ReservationDTO.builder()
                .reservationId(reservation.getReservationId())
                .classroomId(reservation.getClassroom().getClassroomId())
                .userId(reservation.getUser().getUserId())
                .username(reservation.getUser().getUsername())
                .name(reservation.getUser().getNameKor())
                .timeSlotId(timeSlotId)
                .startAt(reservation.getStartAt())
                .endAt(reservation.getEndAt())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
