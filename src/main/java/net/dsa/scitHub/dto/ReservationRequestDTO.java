package net.dsa.scitHub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReservationRequestDTO {
    private Integer classroomId;
    private Integer timeSlotId;
}
