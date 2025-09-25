package net.dsa.scitHub.dto;

import java.time.LocalDate;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dsa.scitHub.entity.schedule.Dday;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DdayDTO {
    
    private Integer ddayId;
    private String username;
    private LocalDate dday;
    private String title;
    private boolean isPinned;

    public static DdayDTO convertToDdayDTO(Dday dday) {
        return DdayDTO.builder()
                        .ddayId(dday.getDdayId())
                        .username(dday.getUser() != null ? dday.getUser().getUsername() : null)
                        .dday(dday.getDday())
                        .title(dday.getTitle())
                        .isPinned(dday.isPinned())
                        .build();
    }
}
